package com.alef.api.trust.dto;

import java.util.List;

/**
 * Composed trust overview payload returned by GET /api/trust/overview (architecture 3.1).
 *
 * Mixes stored editorial content (stats, software, standards from trust_content)
 * with live project-derived aggregates (marquee count in stats, client/contractor
 * name lists). Any array may be empty -- the frontend renders only the sections
 * that have content (graceful degradation).
 */
public record TrustOverviewDto(
        List<TrustStatDto> stats,
        List<String> clients,
        List<String> contractors,
        List<String> software,
        List<String> standards
) {
}
