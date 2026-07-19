package com.alef.api.admin.team.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Create/update request body for admin team writes (design.md §A.3, F12-AC8/AC9).
 * name and role are required (F12-AC23); company/email/photo are optional,
 * matching the entity's nullable-credit-field stance (011 DEC-020).
 */
public record AdminTeamRequest(

        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "role is required")
        String role,

        String company,

        @Email(message = "email must be a valid address")
        String email,

        String photo,

        int displayOrder,

        boolean active
) {
}
