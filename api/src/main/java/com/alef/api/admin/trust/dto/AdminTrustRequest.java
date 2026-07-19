package com.alef.api.admin.trust.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Create/update request body for an admin trust_content row (design.md §A.4).
 *
 * value is intentionally not @NotBlank here -- it is required for item_type
 * 'stat' but may be null for 'software'/'standard' badge rows (design §A.4);
 * that conditional rule is enforced in AdminTrustService, not Bean Validation.
 */
public record AdminTrustRequest(

        @NotBlank(message = "itemKey is required")
        String itemKey,

        @NotBlank(message = "itemType is required")
        String itemType,

        @NotBlank(message = "label is required")
        String label,

        String value,
        String unit,
        int displayOrder
) {
}
