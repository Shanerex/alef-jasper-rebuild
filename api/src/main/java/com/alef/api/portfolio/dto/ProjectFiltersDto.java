package com.alef.api.portfolio.dto;

import java.util.List;

/**
 * Filter-options discoverability payload (architecture 3.3, DEC-009).
 *
 * Sectors and statuses are the fixed vocabulary served in enum order.
 * Countries are data-derived (SELECT DISTINCT) and may change as projects
 * are added or removed via feature 012.
 */
public record ProjectFiltersDto(
        List<String> sectors,
        List<String> statuses,
        List<String> countries
) {
}
