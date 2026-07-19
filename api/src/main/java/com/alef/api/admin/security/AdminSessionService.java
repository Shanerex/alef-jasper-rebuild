package com.alef.api.admin.security;

import com.alef.api.admin.security.dto.SessionResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Login/logout orchestration for the admin session (architecture §2.1, design §A.1).
 *
 * Login is implemented manually (formLogin is disabled in AdminSecurityConfig)
 * because the contract is a small JSON request/response, not a redirect-based
 * form flow: authenticate via AuthenticationManager, then explicitly persist
 * the resulting SecurityContext into the HTTP session via the same
 * SecurityContextRepository the filter chain reads on every subsequent request
 * -- this is what actually establishes the server-side session backing the
 * ALEFADMIN cookie (server.servlet.session.cookie.name, application.yml).
 *
 * Rate limiting mirrors LeadService's Redis per-IP pattern (10 attempts / 15 min)
 * to blunt brute force against the single admin credential (architecture §2.1
 * "Testability" risk note).
 */
@Service
public class AdminSessionService {

    private static final String RATE_KEY_PREFIX = "rate:admin-login:";
    private static final int RATE_LIMIT = 10;
    private static final long RATE_WINDOW_SECONDS = 900L; // 15 minutes

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public AdminSessionService(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            RedisTemplate<String, String> redisTemplate) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Authenticates the given credentials and, on success, establishes a
     * server-side session (F12-AC1).
     *
     * Throws org.springframework.security.core.AuthenticationException (e.g.
     * BadCredentialsException) on bad credentials -- mapped to 401 by
     * AdminAuthExceptionHandler. Throws ResponseStatusException(429) if the
     * per-IP rate limit is breached, BEFORE attempting authentication (so a
     * brute-force attempt cannot burn the limit by trying many passwords
     * across many usernames without ever reaching the AuthenticationManager
     * unnecessarily -- consistent with LeadService's honeypot-before-rate-limit
     * ordering discipline, adapted here as rate-limit-before-auth-attempt).
     */
    public SessionResponseDto login(String username, String password, String clientIp,
                                     HttpServletRequest request, HttpServletResponse response) {
        enforceRateLimit(clientIp);

        Authentication authRequest = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authResult = authenticationManager.authenticate(authRequest);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        // Persists the context into the (Redis-backed) HttpSession and sets the
        // session cookie on the response -- this is the step a default
        // formLogin filter would normally do for us.
        securityContextRepository.saveContext(context, request, response);

        return new SessionResponseDto(true, authResult.getName());
    }

    /** Invalidates the current session and clears the security context (logout, F12-AC1). */
    public void logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    /**
     * Checks and increments the Redis per-IP login-attempt counter.
     * Same INCR + EXPIRE-on-first-hit device as LeadService.enforceRateLimit.
     */
    private void enforceRateLimit(String clientIp) {
        String key = RATE_KEY_PREFIX + clientIp;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, RATE_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        if (count != null && count > RATE_LIMIT) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many login attempts. Please wait before trying again.");
        }
    }
}
