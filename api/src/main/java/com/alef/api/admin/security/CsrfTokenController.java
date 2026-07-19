package com.alef.api.admin.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/admin/csrf -- primes the XSRF-TOKEN cookie for the admin web client
 * (DEC-028, not in design.md -- see AdminSecurityConfig's class-level note).
 *
 * Gotcha this exists to work around: Spring Security 6's CsrfFilter only
 * issues (saves) a CSRF cookie on requests that actually require CSRF
 * protection -- i.e. an unsafe method (POST/PUT/DELETE) on a non-ignored
 * path. A plain GET never triggers issuance, and /api/admin/session is
 * itself CSRF-ignored (see AdminSecurityConfig), so there is otherwise no
 * request the web client can make to obtain a token before its first real
 * mutation (password change, project/team/trust/sample writes, uploads).
 *
 * Injecting CsrfToken as a controller parameter forces Spring MVC to resolve
 * it, which resolves the filter's DeferredCsrfToken supplier and causes
 * CookieCsrfTokenRepository to write the XSRF-TOKEN cookie on this response --
 * this is Spring Security's own documented pattern for SPA CSRF priming. The
 * web client calls this once (e.g. on app load / the login page) and echoes
 * the resulting cookie value back as X-XSRF-TOKEN on every mutating request.
 *
 * Permitted unauthenticated (AdminSecurityConfig) -- a token must be
 * obtainable before login, since login itself may be called through the
 * same client code path as later mutations.
 */
@RestController
public class CsrfTokenController {

    /**
     * The explicit token.getToken() call is required, not decorative: the
     * injected CsrfToken is a lazy SupplierCsrfToken wrapper (merely having it
     * as a resolved method parameter does not, by itself, trigger resolution).
     * Calling .getToken() forces the wrapped DeferredCsrfToken supplier to
     * run, which is what actually causes CookieCsrfTokenRepository to
     * generate and save the XSRF-TOKEN cookie on this response (confirmed
     * empirically while writing AdminSecurityWebTest -- see LEARNING-002).
     */
    @GetMapping("/api/admin/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void primeCsrfToken(CsrfToken token) {
        token.getToken();
    }
}
