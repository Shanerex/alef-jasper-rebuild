package com.alef.api.admin.project.dto;

import java.time.Instant;
import java.util.List;

/**
 * Full admin record for a project (design.md §A.2) -- unlike the public
 * summary/detail projections (DEC-011), this carries every column including
 * the internal id (the admin edits by id since slug is itself editable).
 */
public record AdminProjectDto(
        Long id,
        String slug,
        String name,
        String sector,
        String country,
        String status,
        String image,
        String description,
        String mainContractor,
        String client,
        String consultant,
        String location,
        List<String> scope,
        boolean featurable,
        Instant createdAt,
        Instant updatedAt
) {
}
