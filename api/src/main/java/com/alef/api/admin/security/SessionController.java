package com.alef.api.admin.security;

import com.alef.api.admin.security.dto.LoginRequest;
import com.alef.api.admin.security.dto.SessionResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin login/session endpoints (design.md §A.1, F12-AC1..AC3).
 *
 * POST is permitAll in AdminSecurityConfig (the caller is not authenticated
 * yet). GET and DELETE fall under the general /api/admin/** hasRole(ADMIN)
 * rule, so an unauthenticated call never reaches this controller for those
 * two -- Spring Security's entry point returns 401 first (F12-AC2).
 */
@RestController
@RequestMapping("/api/admin/session")
public class SessionController {

    private final AdminSessionService service;

    public SessionController(AdminSessionService service) {
        this.service = service;
    }

    /**
     * POST /api/admin/session -- login (F12-AC1).
     * On success, establishes a server-side session and returns 200; the
     * ALEFADMIN cookie is set by the SecurityContextRepository as a side
     * effect (see AdminSessionService.login). Bad credentials and rate-limit
     * breaches are mapped to 401/429 by AdminAuthExceptionHandler.
     */
    @PostMapping
    public SessionResponseDto login(@Valid @RequestBody LoginRequest request,
                                     HttpServletRequest httpRequest,
                                     HttpServletResponse httpResponse) {
        String clientIp = extractClientIp(httpRequest);
        return service.login(request.username(), request.password(), clientIp, httpRequest, httpResponse);
    }

    /**
     * GET /api/admin/session -- current session check, used by the web
     * middleware and the admin UI on load. Only reachable when authenticated
     * (security config); Authentication is guaranteed non-null here.
     */
    @GetMapping
    public SessionResponseDto current(Authentication authentication) {
        return new SessionResponseDto(true, authentication.getName());
    }

    /** DELETE /api/admin/session -- logout; invalidates the session (F12-AC1). */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpRequest) {
        service.logout(httpRequest);
    }

    /**
     * Extracts the client IP for rate limiting, mirroring LeadController's
     * single-hop X-Forwarded-For policy (architecture §3.4 / §10 Decision 2).
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
