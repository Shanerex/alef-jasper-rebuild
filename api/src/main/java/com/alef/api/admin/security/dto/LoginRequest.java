package com.alef.api.admin.security.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for POST /api/admin/session (design.md §A.1). */
public record LoginRequest(

        @NotBlank(message = "username is required")
        String username,

        @NotBlank(message = "password is required")
        String password
) {
}
