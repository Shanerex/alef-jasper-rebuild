package com.alef.api.lead.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Validated request body for POST /api/leads from the contact form (architecture §3.4).
 *
 * Bean Validation rules mirror the server rules in the architecture spec; the
 * frontend also validates client-side for instant feedback, but the server
 * re-validates authoritatively here.
 *
 * The website field is the honeypot: a real human leaves it blank. If non-empty
 * LeadService short-circuits before persisting (no validation annotation needed
 * on the honeypot -- it must accept any string, including non-blank, to avoid
 * returning a 400 that tips off a bot that a honeypot is present).
 *
 * source and created_at are never accepted from the client; LeadService sets them
 * server-side using LeadSource.CONTACT_FORM.
 */
public record ContactLeadRequest(

        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name must not exceed 200 characters")
        String name,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        @Size(max = 200, message = "email must not exceed 200 characters")
        String email,

        @Size(max = 50, message = "phone must not exceed 50 characters")
        String phone,        // optional

        @Size(max = 200, message = "company must not exceed 200 characters")
        String company,      // optional

        @NotBlank(message = "message is required")
        @Size(max = 5000, message = "message must not exceed 5000 characters")
        String message,

        String website       // honeypot field -- no validation; expected blank from real users
) {
}
