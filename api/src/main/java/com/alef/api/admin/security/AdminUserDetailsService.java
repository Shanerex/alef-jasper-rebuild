package com.alef.api.admin.security;

import com.alef.api.admin.security.repository.AdminCredentialRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supplies the single ROLE_ADMIN principal by reading the persisted
 * admin_credential store (architecture §2.1, §2.1.1, DEC-021 amended).
 *
 * Deliberately reads the DB row, not env -- env only seeds the row on first
 * boot (see AdminCredentialSeeder). This is what lets an in-app password
 * change (F12-AC28) actually take effect: every login re-reads the current
 * persisted hash.
 *
 * Single-admin: the singleton row (id=1) is the only credential; a username
 * that does not match it is treated as "not found" rather than leaking which
 * part of the pair was wrong (architecture §2.1, F12-AC1..AC3).
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

    /** The admin_credential table has exactly one row, fixed at id=1 (DDL CHECK constraint). */
    private static final short CREDENTIAL_ROW_ID = 1;

    private final AdminCredentialRepository repository;

    public AdminUserDetailsService(AdminCredentialRepository repository) {
        this.repository = repository;
    }

    /**
     * Loads the single admin principal if the supplied username matches the
     * persisted row.
     *
     * Gotcha: this does NOT check the password -- Spring Security's
     * DaoAuthenticationProvider does that via the configured PasswordEncoder
     * against the returned UserDetails' password hash. Throwing here only for
     * "no such user" keeps both failure modes (bad username, bad password)
     * indistinguishable to the caller (the login endpoint returns a generic
     * 401 either way -- design §A.1).
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var credential = repository.findById(CREDENTIAL_ROW_ID)
                .orElseThrow(() -> new UsernameNotFoundException("Admin credential not seeded"));

        if (!credential.getUsername().equals(username)) {
            throw new UsernameNotFoundException("Unknown admin username");
        }

        return User.builder()
                .username(credential.getUsername())
                .password(credential.getPasswordHash())
                .roles("ADMIN")
                .build();
    }
}
