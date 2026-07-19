package com.alef.api.admin.project;

import com.alef.api.admin.project.dto.AdminProjectDto;
import com.alef.api.portfolio.entity.ProjectEntity;

import java.util.List;

/**
 * Maps ProjectEntity to/from the full admin DTO (design.md §A.2).
 *
 * Distinct from the public-facing ProjectMapper (DEC-011 summary/detail
 * projections) -- the admin DTO carries every column, including id and the
 * audit timestamps that public projections deliberately omit.
 */
final class AdminProjectMapper {

    private AdminProjectMapper() {
        // Utility class -- no instances.
    }

    static AdminProjectDto toDto(ProjectEntity entity) {
        return new AdminProjectDto(
                entity.getId(),
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
                entity.isFeaturable(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
