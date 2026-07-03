package com.alef.api.team.dto;

/**
 * Public DTO for one team profile, returned inside the GET /api/team envelope.
 *
 * email, company, and photo are nullable to match the architecture §3.1 nullable
 * contract: a profile with only name + role is valid and the card degrades
 * gracefully by omitting absent fields (architecture §5.3, design §5).
 */
public record TeamMemberDto(
        String name,
        String role,
        String company,   // nullable
        String email,     // nullable; primary address only
        String photo      // nullable; public-relative path
) {
}
