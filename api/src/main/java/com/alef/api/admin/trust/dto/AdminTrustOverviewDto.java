package com.alef.api.admin.trust.dto;

import java.util.List;

/**
 * Response body for GET /api/admin/trust (design.md §A.4): rows grouped by
 * item_type, plus the read-only derived facts panel (DEC-025).
 */
public record AdminTrustOverviewDto(
        List<AdminTrustRowDto> stats,
        List<AdminTrustRowDto> software,
        List<AdminTrustRowDto> standards,
        DerivedTrustFactsDto derived
) {
}
