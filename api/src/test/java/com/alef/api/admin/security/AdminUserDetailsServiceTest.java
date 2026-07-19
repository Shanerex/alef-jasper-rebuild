package com.alef.api.admin.security;

import com.alef.api.admin.security.entity.AdminCredentialEntity;
import com.alef.api.admin.security.repository.AdminCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AdminUserDetailsService (architecture §2.1, DEC-021 amended).
 *
 * Confirms the service reads the persisted store (not env), maps to
 * ROLE_ADMIN, and treats both "no row" and "wrong username" as the same
 * UsernameNotFoundException (so a caller cannot distinguish "bad username"
 * from "bad password" from this layer alone -- design §A.1).
 */
@ExtendWith(MockitoExtension.class)
class AdminUserDetailsServiceTest {

    @Mock
    private AdminCredentialRepository repository;

    private AdminUserDetailsService service;

    @Test
    void loads_admin_with_role_admin_when_username_matches() {
        service = new AdminUserDetailsService(repository);
        when(repository.findById((short) 1)).thenReturn(Optional.of(credential("admin", "hashed-value")));

        UserDetails result = service.loadUserByUsername("admin");

        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getPassword()).isEqualTo("hashed-value");
        assertThat(result.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void throws_when_no_credential_row_is_seeded_yet() {
        service = new AdminUserDetailsService(repository);
        when(repository.findById((short) 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("admin"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void throws_when_supplied_username_does_not_match_the_persisted_username() {
        service = new AdminUserDetailsService(repository);
        when(repository.findById((short) 1)).thenReturn(Optional.of(credential("admin", "hashed-value")));

        assertThatThrownBy(() -> service.loadUserByUsername("someone-else"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private AdminCredentialEntity credential(String username, String passwordHash) {
        AdminCredentialEntity entity = new AdminCredentialEntity();
        entity.setId((short) 1);
        entity.setUsername(username);
        entity.setPasswordHash(passwordHash);
        return entity;
    }
}
