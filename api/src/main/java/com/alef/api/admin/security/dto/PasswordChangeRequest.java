package com.alef.api.admin.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/admin/password (design.md §A.7, F12-AC28).
 *
 * confirmNewPassword is deliberately NOT part of this record -- it is a
 * UI-only field checked client-side; the server only ever sees current + new
 * (design §A.7).
 */
public record PasswordChangeRequest(

        @NotBlank(message = "currentPassword is required")
        String currentPassword,

        @NotBlank(message = "newPassword is required")
        String newPassword
) {
}
