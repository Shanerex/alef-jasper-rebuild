package com.alef.api.admin.sample.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Create/update request body for admin sample writes (design.md §A.5, F12-AC16/AC17).
 * preview/file are optional -- assets may be attached in a follow-up edit
 * (architecture §3.4).
 */
public record AdminSampleRequest(

        @NotBlank(message = "slug is required")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "slug must be lowercase alphanumeric with hyphens")
        String slug,

        @NotBlank(message = "title is required")
        String title,

        @NotBlank(message = "category is required")
        String category,

        String preview,
        String file,
        int displayOrder
) {
}
