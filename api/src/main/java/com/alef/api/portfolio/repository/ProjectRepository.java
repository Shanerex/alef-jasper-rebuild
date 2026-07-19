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

    /** Checks slug uniqueness on admin create (F12-AC4, design §A.2). */
    boolean existsBySlug(String slug);

    /**
     * Checks slug uniqueness on admin update, excluding the row being updated
     * (design §A.2: "on PUT, uniqueness excludes the row itself").
     */
    boolean existsBySlugAndIdNot(String slug, Long id);

    /**
     * Returns the distinct country values present in the data, ordered alphabetically.
     * Used by the /filters endpoint (architecture 3.3) -- countries are data-derived,
     * not a fixed vocabulary.
     */
    @Query("SELECT DISTINCT p.country FROM ProjectEntity p ORDER BY p.country")
    List<String> findDistinctCountries();

    /**
     * Returns distinct non-null client names, ordered alphabetically.
     * Used by the trust overview (architecture 3.1, F5-AC2) to derive the
     * client name strip from the portfolio without duplicating data.
     */
    @Query("SELECT DISTINCT p.client FROM ProjectEntity p WHERE p.client IS NOT NULL ORDER BY p.client")
    List<String> findDistinctClients();

    /**
     * Returns distinct non-null main contractor names, ordered alphabetically.
     * Used by the trust overview (architecture 3.1, F5-AC2) to derive the
     * contractor name strip from the portfolio without duplicating data.
     */
    @Query("SELECT DISTINCT p.mainContractor FROM ProjectEntity p WHERE p.mainContractor IS NOT NULL ORDER BY p.mainContractor")
    List<String> findDistinctMainContractors();

    /**
     * Counts projects marked as featurable (marquee projects).
     * Used by the trust overview (architecture 3.1, F5-AC1) to synthesize
     * the marquee project count stat live from the portfolio.
     */
    long countByFeaturableTrue();
}
