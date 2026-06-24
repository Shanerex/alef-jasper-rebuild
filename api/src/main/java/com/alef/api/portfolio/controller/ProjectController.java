package com.alef.api.portfolio.controller;

import com.alef.api.portfolio.dto.PagedResponse;
import com.alef.api.portfolio.dto.ProjectDetailDto;
import com.alef.api.portfolio.dto.ProjectFiltersDto;
import com.alef.api.portfolio.dto.ProjectSummaryDto;
import com.alef.api.portfolio.service.ProjectFilterQuery;
import com.alef.api.portfolio.service.ProjectService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only REST endpoints for the project portfolio (architecture 3).
 *
 * All three endpoints delegate straight to ProjectService; the controller
 * owns no business logic. Size is capped to prevent abuse.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    /**
     * GET /api/projects -- filterable, paginated summary list (F3-AC1).
     *
     * All query parameters are optional and combine with AND semantics.
     * Invalid sector or status values produce a 400. Unknown country
     * returns an empty result set, not an error.
     */
    @GetMapping
    public PagedResponse<ProjectSummaryDto> listProjects(
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean featurable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        int cappedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var query = new ProjectFilterQuery(sector, country, status, featurable);
        return service.listProjects(query, PageRequest.of(page, cappedSize));
    }

    /**
     * GET /api/projects/filters -- sector/status vocabularies plus distinct countries.
     *
     * Provides filter-options discoverability so the frontend does not hardcode
     * vocabulary values (architecture 3.3, DEC-009).
     */
    @GetMapping("/filters")
    public ProjectFiltersDto getFilters() {
        return service.getFilters();
    }

    /**
     * GET /api/projects/{slug} -- full public detail record (F3-AC2, F3-AC3).
     *
     * Returns the complete project with credit fields and scope.
     * Unknown slug produces a 404.
     */
    @GetMapping("/{slug}")
    public ProjectDetailDto getProject(@PathVariable String slug) {
        return service.getProjectBySlug(slug);
    }
}
