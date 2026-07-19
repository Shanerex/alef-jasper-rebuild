package com.alef.api.admin.trust.dto;

/** One trust_content row as seen by the admin (design.md §A.4). */
public record AdminTrustRowDto(
        Long id,
        String itemKey,
        String itemType,
        String label,
        String value,
        String unit,
        int displayOrder
) {
}
