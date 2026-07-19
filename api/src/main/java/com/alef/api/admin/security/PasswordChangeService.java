package com.alef.api.admin.security;

import com.alef.api.admin.security.entity.AdminCredentialEntity;
import com.alef.api.admin.security.error.PasswordPolicyViolationException;
import com.alef.api.admin.security.error.WrongCurrentPasswordException;
import com.alef.api.admin.security.repository.AdminCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Self-service password change for the single admin (architecture §2.1.1, design §A.7,
 * F12-AC28, F12-AC29).
 *
 * Never logs either password in plaintext (F12-AC29) -- log statements in this
 * class reference only outcomes ("password changed"), never field values. The
 * new password takes effect on next login; the caller's current session is
 * left untouched by a successful change (no forced re-login in v1).
 */
@Service
public class PasswordChangeService {

    private static final Logger log = LoggerFactory.getLogger(PasswordChangeService.class);

    /** The admin_credential table has exactly one row, fixed at id=1 (DDL CHECK constraint). */
    private static final short CREDENTIAL_ROW_ID = 1;

    /** Design §A.7 policy: minimum 12 characters. */
    private static final int MIN_LENGTH = 12;

    private final AdminCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;

    public PasswordChangeService(AdminCredentialRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Validates currentPassword against the stored hash, checks newPassword
     * against policy, then re-hashes and persists (design §A.7 steps 1-3).
     *
     * Evaluation order matters for the tests in the handoff: current-password
     * mismatch is checked BEFORE the new-password policy, so a caller who gets
     * both wrong sees the current-password error first (matches "reject on
     * mismatch" being step 1 in the design).
     *
     * @throws WrongCurrentPasswordException   if currentPassword does not match (F12-AC28)
     * @throws PasswordPolicyViolationException if newPassword fails policy
     */
    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        AdminCredentialEntity credential = repository.findById(CREDENTIAL_ROW_ID)
                .orElseThrow(() -> new IllegalStateException("admin_credential not seeded"));

        if (!passwordEncoder.matches(currentPassword, credential.getPasswordHash())) {
            throw new WrongCurrentPasswordException();
        }

        List<String> violations = validatePolicy(newPassword, currentPassword);
        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(violations);
        }

        credential.setPasswordHash(passwordEncoder.encode(newPassword));
        credential.setUpdatedAt(Instant.now());
        repository.save(credential);
        log.info("Admin password changed successfully (takes effect on next login).");
    }

    /** Applies the design §A.7 new-password policy; returns every violated rule's message. */
    private List<String> validatePolicy(String newPassword, String currentPassword) {
        List<String> violations = new ArrayList<>();
        if (newPassword == null || newPassword.isBlank()) {
            violations.add("New password must not be blank.");
            return violations; // no further checks make sense on a blank value
        }
        if (newPassword.length() < MIN_LENGTH) {
            violations.add("Must be at least " + MIN_LENGTH + " characters.");
        }
        if (newPassword.equals(currentPassword)) {
            violations.add("Must differ from the current password.");
        }
        return violations;
    }
}
