package com.alef.api.lead.controller;

import com.alef.api.lead.dto.LeadConfirmationDto;
import com.alef.api.lead.error.LeadExceptionHandler;
import com.alef.api.lead.service.LeadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests for POST /api/leads (architecture §3.4, feature 011).
 *
 * Risk-weighted: tests the 201/400/429 contract, Bean Validation error mapping,
 * and the ProblemDetail shape from LeadExceptionHandler. Service is mocked.
 *
 * Both LeadController and LeadExceptionHandler are loaded so the advice chain
 * is exercised, mirroring the portfolio controller test pattern.
 */
@WebMvcTest({LeadController.class, LeadExceptionHandler.class})
class LeadControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LeadService service;

    private static final String VALID_BODY = """
            {
              "name": "Alice Smith",
              "email": "alice@example.com",
              "phone": "+971 50 123456",
              "company": "ACME",
              "message": "Please send me a quote for shop drawings.",
              "website": ""
            }
            """;

    @Test
    void submit_valid_request_returns_201_with_confirmation() throws Exception {
        when(service.submit(any(), anyString()))
                .thenReturn(new LeadConfirmationDto(42L, "received"));

        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("received"));
    }

    @Test
    void submit_missing_name_returns_400_problem_detail_with_fields() throws Exception {
        String body = """
                {
                  "name": "",
                  "email": "alice@example.com",
                  "message": "Need shop drawings.",
                  "website": ""
                }
                """;

        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.fields.name").isArray());
    }

    @Test
    void submit_missing_email_returns_400() throws Exception {
        String body = """
                {
                  "name": "Alice",
                  "email": "",
                  "message": "Need shop drawings.",
                  "website": ""
                }
                """;

        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").isArray());
    }

    @Test
    void submit_invalid_email_format_returns_400() throws Exception {
        String body = """
                {
                  "name": "Alice",
                  "email": "not-an-email",
                  "message": "Need shop drawings.",
                  "website": ""
                }
                """;

        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").isArray());
    }

    @Test
    void submit_missing_message_returns_400() throws Exception {
        String body = """
                {
                  "name": "Alice",
                  "email": "alice@example.com",
                  "message": "",
                  "website": ""
                }
                """;

        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.message").isArray());
    }

    @Test
    void submit_rate_limit_breach_returns_429_with_retry_after_header() throws Exception {
        when(service.submit(any(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many enquiries from this IP."));

        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "3600"));
    }

    @Test
    void submit_honeypot_filled_still_returns_201_without_revealing_detection() throws Exception {
        // LeadService returns a fake confirmation for honeypot hits; controller
        // must return 201 identically -- bot must not be able to detect the trap.
        when(service.submit(any(), anyString()))
                .thenReturn(new LeadConfirmationDto(-1L, "received"));

        String body = """
                {
                  "name": "Bot",
                  "email": "bot@spam.com",
                  "message": "Buy cheap meds",
                  "website": "http://spam.example.com"
                }
                """;

        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("received"));
    }
}
