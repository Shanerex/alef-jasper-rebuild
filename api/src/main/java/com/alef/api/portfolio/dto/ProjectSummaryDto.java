package com.alef.api.portfolio.dto;

/**
 * Summary projection for the project list and marquee strip (DEC-011).
 *
 * Contains only the card-level fields needed by the ProjectCard component.
 * Detail-only fields (description, credit fields, scope) are excluded to
 * keep the list payload small and allow the full catalogue to be loaded
 * cheaply for client-side filtering.
 */
public record ProjectSummaryDto(
        String slug,
        String name,
        String sector,
        String country,
        String status,
        String image,
        String location,
        boolean featurable
) {
}
