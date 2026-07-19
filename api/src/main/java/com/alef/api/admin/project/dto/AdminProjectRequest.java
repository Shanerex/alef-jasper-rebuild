package com.alef.api.admin.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Create/update request body for admin project writes (design.md §A.2, F12-AC4/AC5).
 *
 * Slug/name/sector/country/status are the required fields (F12-AC23); sector,
 * status, and each scope entry are further checked against their app-layer
 * vocabularies in AdminProjectService (Bean Validation alone cannot express
 * "one of a dynamic enum list" cleanly, and the vocab check needs to produce
 * the same ProblemDetail 'fields' shape as everything else).
 */
public record AdminProjectRequest(

        @NotBlank(message = "slug is required")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "slug must be lowercase alphanumeric with hyphens (e.g. doha-metro-gold-line)")
        String slug,

        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "sector is required")
        String sector,

        @NotBlank(message = "country is required")
        String country,

        @NotBlank(message = "status is required")
        String status,

        String image,
        String description,
        String mainContractor,
        String client,
        String consultant,
        String location,

        List<String> scope,

        boolean featurable
) {
}
