package com.alef.api.admin.security;

import com.alef.api.admin.security.entity.AdminCredentialEntity;
import com.alef.api.admin.security.repository.AdminCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * First-boot-only seeder for the admin credential (architecture §2.1.1, DEC-021 amended).
 *
 * Runs once per application startup. If admin_credential is empty, inserts row
 * id=1 from ADMIN_USERNAME / ADMIN_PASSWORD_HASH. If a row already exists, env
 * is NOT re-applied -- this is deliberate: re-reading env on every boot would
 * silently revert an admin's in-app password change (F12-AC28) on the next
 * redeploy. The persisted row is authoritative after the first boot.
 *
 * ADMIN_PASSWORD_HASH must already be a BCrypt hash (never plaintext) -- this
 * class does not hash it, it only persists what env supplies (F12-AC29,
 * engineering-standards #8: secrets in env, not repo).
 */
@Component
public class AdminCredentialSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminCredentialSeeder.class);

    /** The admin_credential table has exactly one row, fixed at id=1 (DDL CHECK constraint). */
    private static final short CREDENTIAL_ROW_ID = 1;

    private final AdminCredentialRepository repository;
    private final String seedUsername;
    private final String seedPasswordHash;

    public AdminCredentialSeeder(
            AdminCredentialRepository repository,
            @Value("${alef.admin.username:#{null}}") String seedUsername,
            @Value("${alef.admin.password-hash:#{null}}") String seedPasswordHash) {
        this.repository = repository;
        this.seedUsername = seedUsername;
        this.seedPasswordHash = seedPasswordHash;
    }

    /**
     * Seeds the singleton row only when the table is empty.
     *
     * Gotcha: this must never call save() when a row already exists -- that is
     * the exact bug this class exists to prevent (see architecture §8 risk:
     * "a naive upsert-from-env-on-every-boot would silently revert an in-app
     * change"). The empty-check is the whole contract; a dedicated test
     * (AdminCredentialSeederTest) locks this in.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            log.debug("admin_credential already seeded; leaving the persisted row untouched.");
            return;
        }

        if (seedUsername == null || seedPasswordHash == null) {
            log.warn("ADMIN_USERNAME / ADMIN_PASSWORD_HASH not set; admin_credential left empty. "
                    + "The admin console cannot be logged into until these are set and the app restarts.");
            return;
        }

        AdminCredentialEntity entity = new AdminCredentialEntity();
        entity.setId(CREDENTIAL_ROW_ID);
        entity.setUsername(seedUsername);
        entity.setPasswordHash(seedPasswordHash);
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
        log.info("Seeded admin_credential from env on first boot (username='{}').", seedUsername);
    }
}
