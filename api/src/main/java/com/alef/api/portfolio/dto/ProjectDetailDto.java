package com.alef.api.portfolio.dto;

import java.util.List;

/**
 * Full public record for a single project (architecture 3.2, F3-AC3).
 *
 * Includes all credit fields and scope that the detail page needs.
 * Omits internal fields (id, createdAt, updatedAt) which are not part
 * of the public contract.
 */
public record ProjectDetailDto(
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
        boolean featurable
) {
}
