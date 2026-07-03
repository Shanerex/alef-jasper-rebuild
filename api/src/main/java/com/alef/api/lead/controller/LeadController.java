package com.alef.api.lead.controller;

import com.alef.api.lead.dto.ContactLeadRequest;
import com.alef.api.lead.dto.LeadConfirmationDto;
import com.alef.api.lead.service.LeadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Write endpoint for contact form lead submission (architecture §3.4, feature 011).
 *
 * The first mutation endpoint in the system. Accepts and validates the contact
 * form body, extracts the client IP for rate limiting, and delegates to
 * LeadService. Returns 201 Created with a minimal confirmation body on success.
 * Validation errors are handled by LeadExceptionHandler (400 ProblemDetail).
 * Rate-limit breaches propagate as 429 ResponseStatusException from LeadService.
 */
@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService service;

    public LeadController(LeadService service) {
        this.service = service;
    }

    /**
     * POST /api/leads -- submit a contact form lead (F11-AC6).
     *
     * @param request    validated contact form payload (name, email, phone, company,
     *                   message, website honeypot)
     * @param httpRequest used to extract the client IP from X-Forwarded-For
     * @return 201 Created with { id, status: "received" }
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeadConfirmationDto submit(
            @Valid @RequestBody ContactLeadRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = extractClientIp(httpRequest);
        return service.submit(request, clientIp);
    }

    /**
     * Extracts the real client IP using a single-hop X-Forwarded-For policy
     * (architecture §3.4 / §10 Decision 2).
     *
     * Takes the first value from X-Forwarded-For (the original client IP when
     * behind one reverse proxy). Falls back to getRemoteAddr() when the header
     * is absent (direct connection in local dev).
     *
     * Gotcha: in a multi-hop proxy chain only the leftmost value is trusted.
     * If the deployment introduces multiple hops, this extraction needs updating.
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
