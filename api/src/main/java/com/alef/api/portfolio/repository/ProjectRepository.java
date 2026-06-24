package com.alef.api.portfolio.repository;

import com.alef.api.portfolio.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Read-only repository for the project table.
 *
 * Extends JpaSpecificationExecutor to support composable, optional filter
 * predicates for the list endpoint (architecture 3.1). The write path is
 * deferred to feature 012.
 */
public interface ProjectRepository
        extends JpaRepository<ProjectEntity, Long>, JpaSpecificationExecutor<ProjectEntity> {

    /** Finds a project by its public slug key (F3-AC2). */
    Optional<ProjectEntity> findBySlug(String slug);

    /**
     * Returns the distinct country values present in the data, ordered alphabetically.
     * Used by the /filters endpoint (architecture 3.3) -- countries are data-derived,
     * not a fixed vocabulary.
     */
    @Query("SELECT DISTINCT p.country FROM ProjectEntity p ORDER BY p.country")
    List<String> findDistinctCountries();
}
