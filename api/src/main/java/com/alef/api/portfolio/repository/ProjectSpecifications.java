package com.alef.api.portfolio.repository;

import com.alef.api.portfolio.entity.ProjectEntity;
import org.springframework.data.jpa.domain.Specification;

/**
 * Static factory methods for composable JPA Specifications on ProjectEntity.
 *
 * Each method returns a Specification that either applies a predicate or is
 * a no-op (when the filter value is null). This lets the service compose
 * only the predicates actually supplied, avoiding combinatorial query methods
 * or nullable-parameter WHERE clauses (design 1.4).
 */
public final class ProjectSpecifications {

    private ProjectSpecifications() {
        // Utility class -- no instances.
    }

    /**
     * Matches projects with the given sector, or all projects if sector is null.
     * The sector value is expected to be pre-validated by the service layer.
     */
    public static Specification<ProjectEntity> hasSector(String sector) {
        if (sector == null) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.equal(root.get("sector"), sector);
    }

    /**
     * Matches projects in the given country, or all projects if country is null.
     * Country is not vocabulary-validated -- an unknown value simply yields empty results.
     */
    public static Specification<ProjectEntity> hasCountry(String country) {
        if (country == null) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.equal(root.get("country"), country);
    }

    /**
     * Matches projects with the given status, or all projects if status is null.
     * The status value is expected to be pre-validated by the service layer.
     */
    public static Specification<ProjectEntity> hasStatus(String status) {
        if (status == null) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Matches projects by their featurable flag, or all projects if featurable is null.
     * Supports the home-page marquee fetch (F3-AC4, feature 011).
     */
    public static Specification<ProjectEntity> isFeaturable(Boolean featurable) {
        if (featurable == null) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.equal(root.get("featurable"), featurable);
    }
}
