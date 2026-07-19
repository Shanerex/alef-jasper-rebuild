package com.alef.api.admin.security;

import com.alef.api.admin.security.entity.AdminCredentialEntity;
import com.alef.api.admin.security.error.PasswordPolicyViolationException;
import com.alef.api.admin.security.error.WrongCurrentPasswordException;
import com.alef.api.admin.security.repository.AdminCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PasswordChangeService (design.md §A.7, F12-AC28, F12-AC29).
 *
 * Uses a real BCryptPasswordEncoder (cheap enough for a handful of test
 * invocations) so `matches`/`encode` behaviour is exercised for real, not
 * mocked away -- the whole point of this class is correct hash handling.
 */
@ExtendWith(MockitoExtension.class)
class PasswordChangeServiceTest {

    private static final String CURRENT_PASSWORD = "current-password-123";

    @Mock
    private AdminCredentialRepository repository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private PasswordChangeService service;
    private AdminCredentialEntity credential;

    @BeforeEach
    void setUp() {
        service = new PasswordChangeService(repository, passwordEncoder);
        credential = new AdminCredentialEntity();
        credential.setId((short) 1);
        credential.setUsername("admin");
        credential.setPasswordHash(passwordEncoder.encode(CURRENT_PASSWORD));
    }

    @Test
    void accepts_a_valid_change_and_rehashes_with_bcrypt() {
        String originalHash = credential.getPasswordHash();
        when(repository.findById((short) 1)).thenReturn(Optional.of(credential));
        when(repository.save(any())).thenReturn(credential);

        service.changePassword(CURRENT_PASSWORD, "a-brand-new-password-456");

        ArgumentCaptor<AdminCredentialEntity> captor = ArgumentCaptor.forClass(AdminCredentialEntity.class);
        verify(repository).save(captor.capture());
        AdminCredentialEntity saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isNotEqualTo(originalHash);
        assertThat(passwordEncoder.matches("a-brand-new-password-456", saved.getPasswordHash())).isTrue();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void rejects_wrong_current_password_without_touching_the_repository() {
        when(repository.findById((short) 1)).thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> service.changePassword("totally-wrong-password", "a-brand-new-password-456"))
                .isInstanceOf(WrongCurrentPasswordException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void rejects_a_new_password_shorter_than_twelve_characters() {
        when(repository.findById((short) 1)).thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> service.changePassword(CURRENT_PASSWORD, "short1"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .satisfies(ex -> assertThat(((PasswordPolicyViolationException) ex).getViolations())
                        .anyMatch(msg -> msg.contains("12 characters")));
        verify(repository, never()).save(any());
    }

    @Test
    void rejects_a_new_password_equal_to_the_current_password() {
        when(repository.findById((short) 1)).thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> service.changePassword(CURRENT_PASSWORD, CURRENT_PASSWORD))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .satisfies(ex -> assertThat(((PasswordPolicyViolationException) ex).getViolations())
                        .anyMatch(msg -> msg.contains("differ from the current password")));
        verify(repository, never()).save(any());
    }

    @Test
    void checks_current_password_before_new_password_policy() {
        // Both are wrong: current password is wrong AND new password is too short.
        // The wrong-current-password error must win (design §A.7 step order).
        when(repository.findById((short) 1)).thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> service.changePassword("totally-wrong-password", "short"))
                .isInstanceOf(WrongCurrentPasswordException.class);
    }
}
