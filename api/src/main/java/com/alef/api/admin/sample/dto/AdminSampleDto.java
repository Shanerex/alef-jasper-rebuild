package com.alef.api.admin.sample.dto;

/** Full admin record for a sample (design.md §A.5). */
public record AdminSampleDto(
        Long id,
        String slug,
        String title,
        String category,
        String preview,
        String file,
        int displayOrder
) {
}
