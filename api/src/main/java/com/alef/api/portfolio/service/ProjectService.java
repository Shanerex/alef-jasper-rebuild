package com.alef.api.portfolio.service;

import com.alef.api.portfolio.dto.PagedResponse;
import com.alef.api.portfolio.dto.ProjectDetailDto;
import com.alef.api.portfolio.dto.ProjectFiltersDto;
import com.alef.api.portfolio.dto.ProjectSummaryDto;
import com.alef.api.portfolio.error.InvalidFilterValueException;
import com.alef.api.portfolio.error.ProjectNotFoundException;
import com.alef.api.portfolio.repository.ProjectRepository;
import com.alef.api.portfolio.repository.ProjectSpecifications;
import com.alef.api.portfolio.vocabulary.ProjectStatus;
import com.alef.api.portfolio.vocabulary.Sector;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates read-only access to the project portfolio.
 *
 * Validates constrained-vocabulary filters (sector, status) against the app-layer
 * enums before querying, maps entities to DTOs, and assembles the paged envelope.
 * Country is NOT validated -- an unknown country yields an empty result set, not
 * an error (architecture 3.1).
 *
 * This is the authoritative validation guard so the rule holds regardless of caller.
 */
@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository repository;

    public ProjectService(ProjectRepository repository) {
        this.repository = repository;
    }

    /**
     * Lists projects matching optional filters with pagination.
     *
     * Validates sector and status against the vocabulary before building the
     * specification. Invalid values produce a 400 via InvalidFilterValueException.
     */
    public PagedResponse<ProjectSummaryDto> listProjects(ProjectFilterQuery query, Pageable pageable) {
        validateFilters(query);

        var spec = Specification
                .where(ProjectSpecifications.hasSector(query.sector()))
                .and(ProjectSpecifications.hasCountry(query.country()))
                .and(ProjectSpecifications.hasStatus(query.status()))
                .and(ProjectSpecifications.isFeaturable(query.featurable()));

        var page = repository.findAll(spec, pageable);
        var summaries = page.map(ProjectMapper::toSummary);

        return PagedResponse.from(summaries);
    }

    /**
     * Returns the full public record for a project by its slug.
     *
     * Throws ProjectNotFoundException (-> 404) if the slug does not match
     * any record. The slug is the public routing key per F3-AC2.
     */
    public ProjectDetailDto getProjectBySlug(String slug) {
        var entity = repository.findBySlug(slug)
                .orElseThrow(() -> new ProjectNotFoundException(slug));
        return ProjectMapper.toDetail(entity);
    }

    /**
     * Returns the filter vocabulary for the /filters endpoint.
     *
     * Sectors and statuses are the fixed app-layer enums (DEC-009).
     * Countries are data-derived (SELECT DISTINCT) and reflect whatever
     * projects currently exist in the database.
     */
    public ProjectFiltersDto getFilters() {
        return new ProjectFiltersDto(
                Sector.wireValues(),
                ProjectStatus.wireValues(),
                repository.findDistinctCountries()
        );
    }

    /**
     * Validates the constrained-vocabulary filter values before querying.
     * Sector and status must be recognized wire values when non-null.
     * Country is not validated -- unknown values simply yield empty results.
     */
    private void validateFilters(ProjectFilterQuery query) {
        if (query.sector() != null && !Sector.isValid(query.sector())) {
            throw new InvalidFilterValueException("sector", query.sector());
        }
        if (query.status() != null && !ProjectStatus.isValid(query.status())) {
            throw new InvalidFilterValueException("status", query.status());
        }
    }
}
