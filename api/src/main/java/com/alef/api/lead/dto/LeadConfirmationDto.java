package com.alef.api.lead.dto;

/**
 * Confirmation payload returned by POST /api/leads on 201 Created (architecture §3.4).
 *
 * The id is the database-generated surrogate key; informational only — it is not
 * a public URL key and the frontend does not display it. The status field gives
 * the frontend a human-readable signal to drive the form success state.
 *
 * When the honeypot field is non-empty, LeadService returns this DTO with id=-1
 * and status="received" WITHOUT persisting a row. The bot gets a valid 201
 * response, indistinguishable from a real submission, so it cannot detect the
 * honeypot by response inspection.
 */
public record LeadConfirmationDto(long id, String status) {
}
