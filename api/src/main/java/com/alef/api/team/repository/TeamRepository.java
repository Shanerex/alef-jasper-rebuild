package com.alef.api.team.repository;

import com.alef.api.team.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Read-only repository for the team table (feature 011).
 *
 * The sole read method returns only active profiles, ordered by display_order
 * so the frontend receives the intended sequence without any client-side sorting.
 * Feature 012 will add write operations without touching this interface.
 */
public interface TeamRepository extends JpaRepository<TeamEntity, Long> {

    /**
     * Returns all active team profiles in display order for the About page.
     *
     * Filtering on active=true at the database level means deactivated profiles
     * (managed by feature 012) are invisible to the public API without a
     * redeploy or code change — the DEC-020 requirement made literal.
     */
    List<TeamEntity> findAllByActiveTrueOrderByDisplayOrderAsc();
}
