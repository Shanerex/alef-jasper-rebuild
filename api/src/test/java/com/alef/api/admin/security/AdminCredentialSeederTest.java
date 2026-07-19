package com.alef.api.admin.security;

import com.alef.api.admin.security.entity.AdminCredentialEntity;
import com.alef.api.admin.security.repository.AdminCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AdminCredentialSeeder (architecture §2.1.1, DEC-021 amended).
 *
 * This is the single most important guard in the whole feature: it protects
 * F12-AC28 (a password change must survive a restart). The naive bug this
 * class exists to prevent is "upsert from env on every boot," which would
 * silently revert an in-app password change on the next redeploy -- the
 * "does NOT overwrite a changed hash across a simulated restart" test below
 * is the one the handoff calls out by name.
 */
@ExtendWith(MockitoExtension.class)
class AdminCredentialSeederTest {

    @Mock
    private AdminCredentialRepository repository;

    @Test
    void seeds_row_from_env_when_table_is_empty() {
        when(repository.count()).thenReturn(0L);
        var seeder = new AdminCredentialSeeder(repository, "admin", "$2a$10$seededhash");

        seeder.run(null);

        ArgumentCaptor<AdminCredentialEntity> captor = ArgumentCaptor.forClass(AdminCredentialEntity.class);
        verify(repository).save(captor.capture());
        AdminCredentialEntity saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo((short) 1);
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$seededhash");
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void does_not_overwrite_an_existing_row_simulating_a_restart_after_a_password_change() {
        // Simulate: the admin already changed their password (row count is now 1),
        // then the app restarts with the ORIGINAL env values still set (as they
        // would be in a docker-compose redeploy that hasn't had its env touched).
        when(repository.count()).thenReturn(1L);
        var seeder = new AdminCredentialSeeder(repository, "admin", "$2a$10$originalSeedHash");

        seeder.run(null);

        // The seeder must NEVER call save() when a row already exists -- this is
        // the exact behaviour that protects F12-AC28.
        verify(repository, never()).save(any());
    }

    @Test
    void does_nothing_when_env_values_are_missing_and_table_is_empty() {
        when(repository.count()).thenReturn(0L);
        var seeder = new AdminCredentialSeeder(repository, null, null);

        seeder.run(null);

        verify(repository, never()).save(any());
    }

    @Test
    void does_not_overwrite_when_table_already_has_a_row_even_if_env_is_missing() {
        when(repository.count()).thenReturn(1L);
        var seeder = new AdminCredentialSeeder(repository, null, null);

        seeder.run(null);

        verify(repository, never()).save(any());
    }
}
