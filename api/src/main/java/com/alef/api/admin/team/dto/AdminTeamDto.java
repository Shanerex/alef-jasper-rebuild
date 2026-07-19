package com.alef.api.admin.team.dto;

/**
 * Full admin record for a team member (design.md §A.3), including the
 * internal id and the active/displayOrder fields the public TeamMemberDto
 * omits.
 */
public record AdminTeamDto(
        Long id,
        String name,
        String role,
        String company,
        String email,
        String photo,
        int displayOrder,
        boolean active
) {
}
