package com.alef.api.portfolio.service;

import com.alef.api.portfolio.dto.ProjectDetailDto;
import com.alef.api.portfolio.dto.ProjectSummaryDto;
import com.alef.api.portfolio.entity.ProjectEntity;

import java.util.List;

/**
 * Maps ProjectEntity to the public DTO projections (DEC-011).
 *
 * Plain static methods -- no MapStruct dependency needed for two shapes
 * over a single entity. The summary projection strips detail-only fields
 * to keep the list payload small.
 */
final class ProjectMapper {

    private ProjectMapper() {
        // Utility class -- no instances.
    }

    /** Maps an entity to the card-level summary used in the list and marquee. */
    static ProjectSummaryDto toSummary(ProjectEntity entity) {
        return new ProjectSummaryDto(
                entity.getSlug(),
                entity.getName(),
                entity.getSector(),
                entity.getCountry(),
                entity.getStatus(),
                entity.getImage(),
                entity.getLocation(),
                entity.isFeaturable()
        );
    }

    /** Maps an entity to the full public record used on the detail page. */
    static ProjectDetailDto toDetail(ProjectEntity entity) {
        return new ProjectDetailDto(
                entity.getSlug(),
                entity.getName(),
                entity.getSector(),
                entity.getCountry(),
                entity.getStatus(),
                entity.getImage(),
                entity.getDescription(),
                entity.getMainContractor(),
                entity.getClient(),
                entity.getConsultant(),
                entity.getLocation(),
                entity.getScope() != null ? List.copyOf(entity.getScope()) : List.of(),
                entity.isFeaturable()
        );
    }
}
