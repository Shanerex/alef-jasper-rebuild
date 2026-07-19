package com.alef.api.admin.security;

import com.alef.api.admin.error.AdminExceptionHandler;
import com.alef.api.admin.project.AdminProjectController;
import com.alef.api.admin.project.AdminProjectService;
import com.alef.api.admin.project.dto.AdminProjectDto;
import com.alef.api.admin.security.entity.AdminCredentialEntity;
import com.alef.api.admin.security.repository.AdminCredentialRepository;
import com.alef.api.lead.controller.LeadController;
import com.alef.api.lead.dto.LeadConfirmationDto;
import com.alef.api.lead.error.LeadExceptionHandler;
import com.alef.api.lead.service.LeadService;
import com.alef.api.portfolio.controller.ProjectController;
import com.alef.api.portfolio.dto.PagedResponse;
import com.alef.api.portfolio.error.PortfolioExceptionHandler;
import com.alef.api.portfolio.service.ProjectService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end security boundary tests over the real Spring Security filter
 * chain (architecture §2.1, design §A.1) -- the highest-risk surface in 012
 * (engineering-standards #3). Unlike AdminProjectControllerTest et al., this
 * class does NOT disable Spring Security filters: it is specifically testing
 * that they behave correctly (401/403/CSRF/public-stays-open/rate-limit).
 *
 * AdminProjectController is used as the representative protected mutating
 * endpoint and the public ProjectController as the representative endpoint
 * that must stay open -- CSRF and the hasRole(ADMIN) rule are chain-level
 * concerns, not per-controller, so one representative of each is sufficient
 * (architecture §2.7 risk note: this is UI/API-boundary behaviour, not a
 * combinatorial per-endpoint concern).
 *
 * LeadController is included specifically as a regression test for Bug #1
 * (handoffs/5-qa-to-dev.md): a public *mutating* endpoint outside
 * /api/admin/**, which is exactly the case the original (unscoped) filter
 * chain broke -- ProjectController's existing public-endpoint test only ever
 * drove a GET, which CSRF never touches regardless of scoping, so it could
 * not have caught this. See
 * public_lead_submission_endpoint_stays_open_without_authentication_or_csrf below.
 */
@WebMvcTest(controllers = {SessionController.class, CsrfTokenController.class, AdminProjectController.class,
        ProjectController.class, LeadController.class})
@Import({AdminSecurityConfig.class, AdminUserDetailsService.class, AdminSessionService.class,
        AdminAuthExceptionHandler.class, AdminExceptionHandler.class, PortfolioExceptionHandler.class,
        LeadExceptionHandler.class})
class AdminSecurityWebTest {

    private static final String RAW_PASSWORD = "correct-horse-battery-staple";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminCredentialRepository credentialRepository;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOps;

    @MockitoBean
    private AdminProjectService adminProjectService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private LeadService leadService;

    private static final String VALID_PROJECT_BODY = """
            {
              "slug": "test-project",
              "name": "Test Project",
              "sector": "airport",
              "country": "UAE",
              "status": "ongoing",
              "featurable": false
            }
            """;

    @BeforeEach
    void setUp() {
        AdminCredentialEntity credential = new AdminCredentialEntity();
        credential.setId((short) 1);
        credential.setUsername("admin");
        credential.setPasswordHash(new BCryptPasswordEncoder().encode(RAW_PASSWORD));
        credential.setUpdatedAt(Instant.now());
        when(credentialRepository.findById((short) 1)).thenReturn(Optional.of(credential));

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── Login ───────────────────────────────────────────────────────────────

    @Test
    void login_with_correct_credentials_returns_200_and_sets_the_session_cookie() throws Exception {
        when(valueOps.increment(org.mockito.ArgumentMatchers.anyString())).thenReturn(1L);

        MvcResult result = mvc.perform(post("/api/admin/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"%s"}
                                """.formatted(RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("admin"))
                .andReturn();

        // MockMvc does not simulate a real servlet container's Set-Cookie emission
        // for a newly created HttpSession (that is Tomcat's job at deploy time,
        // configured via server.servlet.session.cookie.name=ALEFADMIN); what IS
        // observable here, and what actually matters functionally, is that a
        // session now exists and carries the authenticated SecurityContext.
        assertThat(result.getRequest().getSession(false)).isNotNull();
    }

    @Test
    void login_with_wrong_password_returns_401_without_revealing_which_field_was_wrong() throws Exception {
        when(valueOps.increment(org.mockito.ArgumentMatchers.anyString())).thenReturn(1L);

        mvc.perform(post("/api/admin/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid Credentials"));
    }

    @Test
    void login_rate_limited_returns_429_with_retry_after() throws Exception {
        when(valueOps.increment(org.mockito.ArgumentMatchers.anyString())).thenReturn(11L); // over the 10/15min limit

        mvc.perform(post("/api/admin/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"%s"}
                                """.formatted(RAW_PASSWORD)))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Retry-After", "900"));
    }

    // ── Unauthenticated access to protected endpoints (F12-AC1, F12-AC2) ──────

    @Test
    void get_session_without_authentication_returns_401() throws Exception {
        mvc.perform(get("/api/admin/session"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_session_without_authentication_and_without_csrf_returns_403_not_401() throws Exception {
        // DEC-029 (post-QA): logout is no longer CSRF-exempt (only login, POST,
        // still is), so an unauthenticated DELETE with no CSRF token hits the
        // CsrfFilter -- which runs before the authorization check -- first,
        // exactly like every other protected mutating endpoint. See
        // mutating_admin_endpoint_without_authentication_and_without_csrf_is_rejected_not_silently_accepted
        // above for the same pattern on POST /api/admin/projects.
        mvc.perform(delete("/api/admin/session"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_session_with_valid_csrf_but_without_authentication_returns_401() throws Exception {
        // Proves the auth boundary still holds once CSRF alone is satisfied --
        // a forged cross-site logout still cannot be mistaken for a genuine,
        // authenticated one (F12-AC2).
        Cookie xsrfCookie = fetchCsrfCookie();

        mvc.perform(delete("/api/admin/session")
                        .cookie(xsrfCookie)
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_session_authenticated_with_valid_csrf_token_logs_out_successfully() throws Exception {
        // Confirms the real logout flow works end-to-end now that it requires
        // CSRF: the web client already holds a primed token by this point in
        // practice (see AdminSecurityConfig's DEC-029 note), so this is not
        // just a negative-path regression test but proof the happy path
        // still functions.
        MockHttpSession session = loginAndCaptureSession();
        Cookie xsrfCookie = fetchCsrfCookie();

        mvc.perform(delete("/api/admin/session")
                        .session(session)
                        .cookie(xsrfCookie)
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isNoContent());
    }

    @Test
    void mutating_admin_endpoint_without_authentication_and_without_csrf_is_rejected_not_silently_accepted() throws Exception {
        // No CSRF token at all: Spring Security's CsrfFilter runs before the
        // authorization check, so this is rejected 403 (CSRF) rather than 401
        // (auth) -- either way it is an explicit rejection, never a silent
        // success (F12-AC2). The next test proves the auth boundary (401) is
        // still enforced once CSRF alone is satisfied.
        mvc.perform(post("/api/admin/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PROJECT_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void mutating_admin_endpoint_with_valid_csrf_but_without_authentication_returns_401() throws Exception {
        // A valid CSRF cookie/header pair is obtainable by anyone (the priming
        // endpoint is public by design, matching a real browser's first visit)
        // -- it proves nothing about identity. The real security boundary is
        // authentication, which must still reject this request (F12-AC1/AC2).
        Cookie xsrfCookie = fetchCsrfCookie();

        mvc.perform(post("/api/admin/projects")
                        .cookie(xsrfCookie)
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PROJECT_BODY))
                .andExpect(status().isUnauthorized());
    }

    // ── CSRF (design §A.7 / architecture §2.1) ─────────────────────────────────

    @Test
    void mutating_admin_endpoint_authenticated_without_csrf_token_returns_403() throws Exception {
        MockHttpSession session = loginAndCaptureSession();

        mvc.perform(post("/api/admin/projects")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PROJECT_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void mutating_admin_endpoint_authenticated_with_valid_csrf_token_succeeds() throws Exception {
        MockHttpSession session = loginAndCaptureSession();
        Cookie xsrfCookie = fetchCsrfCookie();

        var dto = new AdminProjectDto(1L, "test-project", "Test Project", "airport", "UAE", "ongoing",
                null, null, null, null, null, null, List.of(), false, Instant.now(), Instant.now());
        when(adminProjectService.create(any())).thenReturn(dto);

        mvc.perform(post("/api/admin/projects")
                        .session(session)
                        .cookie(xsrfCookie)
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PROJECT_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("test-project"));
    }

    // ── Public endpoints stay open (architecture §2.1: "public read endpoints stay open") ──

    @Test
    void public_project_list_endpoint_stays_open_without_authentication() throws Exception {
        when(projectService.listProjects(any(), any()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 50, 0, 0));

        mvc.perform(get("/api/projects"))
                .andExpect(status().isOk());
    }

    @Test
    void public_lead_submission_endpoint_stays_open_without_authentication_or_csrf() throws Exception {
        // Regression test for Bug #1 (handoffs/5-qa-to-dev.md): POST /api/leads
        // is a pre-existing public *mutating* endpoint (feature 011), outside
        // /api/admin/**. Before AdminSecurityConfig gained .securityMatcher(
        // "/api/admin/**"), this chain's CSRF filter applied here too and
        // rejected every real submission with a 403 the form had no way to
        // satisfy. Driven with no session, no XSRF-TOKEN cookie, and no
        // X-XSRF-TOKEN header -- exactly how the real contact form calls it
        // (web/src/lib/api/leads.ts never sends a CSRF header, nor should it).
        when(leadService.submit(any(), anyString()))
                .thenReturn(new LeadConfirmationDto(42L, "received"));

        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test","email":"test@example.com","message":"hi there this is a test message"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("received"));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    /** Logs in with valid credentials and returns the resulting MockHttpSession for reuse. */
    private MockHttpSession loginAndCaptureSession() throws Exception {
        when(valueOps.increment(org.mockito.ArgumentMatchers.anyString())).thenReturn(1L);
        MvcResult result = mvc.perform(post("/api/admin/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"%s"}
                                """.formatted(RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    /**
     * Calls the public priming endpoint (CsrfTokenController) to obtain a real
     * XSRF-TOKEN cookie from CookieCsrfTokenRepository. No session needed --
     * this endpoint is deliberately public (see AdminSecurityConfig's
     * class-level gotcha note and CsrfTokenController's doc comment: a plain
     * GET never triggers cookie issuance on its own, so this endpoint forces
     * it by resolving the CsrfToken parameter).
     */
    private Cookie fetchCsrfCookie() throws Exception {
        MvcResult result = mvc.perform(get("/api/admin/csrf"))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).as("XSRF-TOKEN cookie should be issued by the priming endpoint").isNotNull();
        return cookie;
    }
}
