package com.alef.api.admin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * Spring Security configuration for the admin surface (architecture §2.1, DEC-021 amended).
 *
 * Locks down /api/admin/** to ROLE_ADMIN; every other endpoint stays public
 * (the public read endpoints and POST /api/leads predate this feature and must
 * not require auth -- F12-AC1/AC2). The single exception inside /api/admin/**
 * is the login route itself, POST /api/admin/session, which must be reachable
 * while unauthenticated.
 *
 * Bug fix (post-QA, see handoffs/5-qa-to-dev.md Bug #1 / DECISIONS.md DEC-029):
 * the filter chain is explicitly scoped with .securityMatcher("/api/admin/**")
 * below. Without it, this being the only registered SecurityFilterChain bean
 * meant Spring Security applied it -- CSRF filter included -- to every request
 * in the app, not just the paths this feature owns. authorizeHttpRequests'
 * .anyRequest().permitAll() only governs the authorization decision; it does
 * NOT stop the CSRF filter (a different filter earlier in the same chain) from
 * demanding a token on any unsafe-method request anywhere, including the
 * pre-existing public POST /api/leads (feature 011). Scoping the whole chain
 * to /api/admin/** means requests outside that prefix never enter this chain
 * at all -- no CSRF, no auth, no CORS -- restoring their exact pre-012 behavior.
 *
 * CSRF is enabled with a cookie-based token (XSRF-TOKEN, read by the web client
 * and echoed back as X-XSRF-TOKEN on mutating requests -- design.md Part A).
 * Session-based auth over a cookie is CSRF-exposed, so this is not optional.
 *
 * DEC-028 (implementation choice, not pinned down by design.md): POST
 * /api/admin/session (login only) is exempted from the CSRF check. Design's
 * error table for POST /api/admin/session lists only 401/429 outcomes, never
 * 403 CSRF -- while POST /api/admin/password explicitly documents a 403 CSRF
 * case (design §A.7). This also resolves a bootstrapping problem: before login
 * there is no session yet, so there is nothing for a forged cross-site login
 * POST to ride on (the classic CSRF attack needs an existing authenticated
 * session to abuse).
 *
 * DEC-029 (post-QA refinement, handoffs/5-qa-to-dev.md Concern #3): the CSRF
 * exemption originally covered the whole /api/admin/session path, which meant
 * DELETE /api/admin/session (logout) rode along with login's exemption for no
 * stated reason -- QA flagged this as an unreviewed forced-logout CSRF gap
 * (nuisance-level: a cross-site request can force-log-out an authenticated
 * admin, though it cannot read or mutate data). Deliberate call: logout is
 * NOT special the way login is (there IS an existing session for it to
 * protect), so it now requires CSRF like every other mutation on an
 * authenticated session. The exemption below is narrowed to the POST method
 * only. This costs nothing in practice: the web client already primes the
 * XSRF-TOKEN cookie before login (see admin-session.ts's login()), so by the
 * time an authenticated admin calls logout the token is already available.
 *
 * Gotcha (documented as LEARNING-002): Spring Security 6's default CSRF request
 * handler (XorCsrfTokenRequestAttributeHandler) defers token resolution until
 * something reads the CsrfToken request attribute -- which never happens for a
 * pure JSON API with no server-rendered view. That means the XSRF-TOKEN cookie
 * would never actually be written. Using the plain CsrfTokenRequestAttributeHandler
 * restores the Spring Security 5 behaviour of eagerly resolving the token when
 * something reads it (DEC-027).
 *
 * Second gotcha, found while testing this config (also LEARNING-002): even with
 * that handler, CsrfFilter only actually calls the handler (and therefore only
 * saves the cookie) on requests that require CSRF protection in the first place
 * -- i.e. an unsafe method (POST/PUT/DELETE) on a non-ignored path. A plain GET
 * never triggers cookie issuance, and POST /api/admin/session is itself
 * CSRF-ignored (see below), so there is no request that naturally hands the web
 * client a token before its first mutation. CsrfTokenController (GET
 * /api/admin/csrf) exists solely to force issuance on demand, per Spring
 * Security's own documented SPA-priming pattern (DEC-028).
 */
@Configuration
@EnableWebSecurity
public class AdminSecurityConfig {

    /** CORS allowed origin for credentialed admin requests (architecture §6). */
    @Value("${alef.web-origin:http://localhost:3000}")
    private String webOrigin;

    /**
     * BCrypt is the only password hashing scheme in this system (F12-AC29).
     * Ships with spring-boot-starter-security -- no extra dependency (architecture §7).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Wires the single admin UserDetailsService + the BCrypt encoder into an
     * AuthenticationManager, built directly (not via AuthenticationConfiguration)
     * so this config is self-contained and easy to @Import in slice tests.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AdminUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * Shared session-backed SecurityContextRepository, used both by the filter
     * chain (below) and by AdminSessionService's manual login/logout handling
     * so both sides agree on where the SecurityContext lives.
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * The main filter chain: CORS, CSRF, authorization rules, and JSON 401/403
     * responses (ProblemDetail shape, matching the rest of the API -- design.md
     * "Common error shapes").
     */
    @Bean
    public SecurityFilterChain adminSecurityFilterChain(
            HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {
        http
                // Scope this entire chain (including the CSRF filter, which
                // authorizeHttpRequests below does NOT govern) to the paths this
                // feature actually owns -- see the class-level bug-fix note above.
                .securityMatcher("/api/admin/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // Only login (POST) is exempt -- logout (DELETE) requires CSRF
                        // like every other authenticated mutation. See the DEC-028/
                        // DEC-029 class-level note above.
                        .ignoringRequestMatchers(new AntPathRequestMatcher("/api/admin/session", "POST"))
                )
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(auth -> auth
                        // Login must be reachable while unauthenticated.
                        .requestMatchers(HttpMethod.POST, "/api/admin/session").permitAll()
                        // CSRF priming (CsrfTokenController) must be reachable before login too.
                        .requestMatchers(HttpMethod.GET, "/api/admin/csrf").permitAll()
                        // Everything else under /api/admin/** requires the single admin role.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Defensive fallback only: .securityMatcher("/api/admin/**") above
                        // means no request outside that prefix ever reaches this chain at
                        // all (not even permitAll -- it skips the chain entirely), so this
                        // line is unreachable in practice. Kept so authorizeHttpRequests
                        // has an exhaustive rule and Spring Security doesn't warn about an
                        // unmatched request falling through with no explicit decision.
                        .anyRequest().permitAll()
                )
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::unauthenticated)
                        .accessDeniedHandler(this::forbidden)
                );
        return http.build();
    }

    /** CORS: only the configured web origin, with credentials (cookies) allowed (architecture §2.1). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(webOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 401 for any unauthenticated request to a protected /api/admin/** route
     * (F12-AC2: rejected with an appropriate error, never a silent success).
     */
    private void unauthenticated(jakarta.servlet.http.HttpServletRequest request,
                                  jakarta.servlet.http.HttpServletResponse response,
                                  org.springframework.security.core.AuthenticationException authException)
            throws IOException {
        writeProblemDetail(response, HttpStatus.UNAUTHORIZED, "Unauthorized",
                "Authentication is required to access this resource.");
    }

    /**
     * 403 for an authenticated-but-forbidden request -- most commonly a missing
     * or invalid CSRF token on a mutating /api/admin/** request (design.md
     * "Common error shapes").
     */
    private void forbidden(jakarta.servlet.http.HttpServletRequest request,
                            jakarta.servlet.http.HttpServletResponse response,
                            org.springframework.security.access.AccessDeniedException accessDeniedException)
            throws IOException {
        writeProblemDetail(response, HttpStatus.FORBIDDEN, "Forbidden",
                "You do not have permission to perform this action.");
    }

    private void writeProblemDetail(jakarta.servlet.http.HttpServletResponse response,
                                     HttpStatus status, String title, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        new ObjectMapper().writeValue(response.getWriter(), problem);
    }
}
