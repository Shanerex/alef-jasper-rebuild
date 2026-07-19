package com.alef.api.admin.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AdminSessionService's rate-limiting and delegation logic
 * (architecture §2.1, design §A.1). The actual AuthenticationManager wiring
 * and the CSRF/session filter chain are covered by AdminSecurityWebTest --
 * this class isolates the rate-limit math and the save-context delegation.
 */
@ExtendWith(MockitoExtension.class)
class AdminSessionServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private AdminSessionService service;

    @Test
    void login_success_saves_the_security_context_and_returns_authenticated_true() {
        service = new AdminSessionService(authenticationManager, securityContextRepository, redisTemplate);
        stubRateOk("1.2.3.4", 1L);
        Authentication authResult = new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN");
        when(authenticationManager.authenticate(any())).thenReturn(authResult);

        var result = service.login("admin", "correct-password", "1.2.3.4", request, response);

        assertThat(result.authenticated()).isTrue();
        assertThat(result.username()).isEqualTo("admin");
        verify(securityContextRepository).saveContext(any(), any(), any());
    }

    @Test
    void login_propagates_authentication_exception_on_bad_credentials() {
        service = new AdminSessionService(authenticationManager, securityContextRepository, redisTemplate);
        stubRateOk("1.2.3.4", 1L);
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> service.login("admin", "wrong", "1.2.3.4", request, response))
                .isInstanceOf(BadCredentialsException.class);

        verify(securityContextRepository, never()).saveContext(any(), any(), any());
    }

    @Test
    void login_throws_429_when_rate_limit_exceeded_before_attempting_authentication() {
        service = new AdminSessionService(authenticationManager, securityContextRepository, redisTemplate);
        stubRateBreach("9.9.9.9", 11L); // 11th attempt over the 10/15min limit

        assertThatThrownBy(() -> service.login("admin", "x", "9.9.9.9", request, response))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(429));

        // Rate limit is checked BEFORE authentication -- a breached IP must never
        // reach the AuthenticationManager.
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_sets_redis_expiry_on_first_attempt_from_ip() {
        service = new AdminSessionService(authenticationManager, securityContextRepository, redisTemplate);
        stubRateOk("10.0.0.5", 1L);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));

        service.login("admin", "correct-password", "10.0.0.5", request, response);

        verify(redisTemplate).expire("rate:admin-login:10.0.0.5", 900L, TimeUnit.SECONDS);
    }

    private void stubRateOk(String ip, long count) {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rate:admin-login:" + ip)).thenReturn(count);
    }

    private void stubRateBreach(String ip, long count) {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rate:admin-login:" + ip)).thenReturn(count);
    }

    // ── Logout ──────────────────────────────────────────────────────────────

    @Test
    void logout_invalidates_the_session_when_one_exists() {
        service = new AdminSessionService(authenticationManager, securityContextRepository, redisTemplate);
        jakarta.servlet.http.HttpSession session = org.mockito.Mockito.mock(jakarta.servlet.http.HttpSession.class);
        when(request.getSession(false)).thenReturn(session);

        service.logout(request);

        verify(session).invalidate();
    }

    @Test
    void logout_is_a_no_op_when_there_is_no_session() {
        service = new AdminSessionService(authenticationManager, securityContextRepository, redisTemplate);
        when(request.getSession(false)).thenReturn(null);

        // Must not throw even with no active session (e.g. a stale/expired cookie).
        service.logout(request);
    }
}
