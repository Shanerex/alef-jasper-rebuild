package com.alef.api.portfolio.service;

import com.alef.api.portfolio.dto.PagedResponse;
import com.alef.api.portfolio.dto.ProjectDetailDto;
import com.alef.api.portfolio.dto.ProjectFiltersDto;
import com.alef.api.portfolio.dto.ProjectSummaryDto;
import com.alef.api.portfolio.entity.ProjectEntity;
import com.alef.api.portfolio.error.InvalidFilterValueException;
import com.alef.api.portfolio.error.ProjectNotFoundException;
import com.alef.api.portfolio.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ProjectService validation logic, DTO mapping, and error paths.
 * The repository is mocked -- integration with the DB is tested at the controller level.
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository repository;

    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(repository);
    }

    @Test
    void listProjects_with_valid_sector_returns_summary_projection() {
        var entity = sampleEntity();
        var page = new PageImpl<>(List.of(entity), PageRequest.of(0, 50), 1);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        var query = new ProjectFilterQuery("airport", null, null, null);
        PagedResponse<ProjectSummaryDto> result = service.listProjects(query, PageRequest.of(0, 50));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).slug()).isEqualTo("test-project");
        assertThat(result.content().get(0).name()).isEqualTo("Test Project");
        assertThat(result.page()).isZero();
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void listProjects_with_invalid_sector_throws_InvalidFilterValueException() {
        var query = new ProjectFilterQuery("bogus_sector", null, null, null);

        assertThatThrownBy(() -> service.listProjects(query, PageRequest.of(0, 50)))
                .isInstanceOf(InvalidFilterValueException.class)
                .hasMessageContaining("bogus_sector")
                .hasMessageContaining("sector");
    }

    @Test
    void listProjects_with_invalid_status_throws_InvalidFilterValueException() {
        var query = new ProjectFilterQuery(null, null, "cancelled", null);

        assertThatThrownBy(() -> service.listProjects(query, PageRequest.of(0, 50)))
                .isInstanceOf(InvalidFilterValueException.class)
                .hasMessageContaining("cancelled")
                .hasMessageContaining("status");
    }

    @Test
    void listProjects_with_no_filters_passes_through_without_validation_error() {
        var page = new PageImpl<ProjectEntity>(List.of(), PageRequest.of(0, 50), 0);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        var query = new ProjectFilterQuery(null, null, null, null);
        PagedResponse<ProjectSummaryDto> result = service.listProjects(query, PageRequest.of(0, 50));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void getProjectBySlug_returns_detail_projection() {
        var entity = sampleEntity();
        when(repository.findBySlug("test-project")).thenReturn(Optional.of(entity));

        ProjectDetailDto detail = service.getProjectBySlug("test-project");

        assertThat(detail.slug()).isEqualTo("test-project");
        assertThat(detail.description()).isEqualTo("A test project description");
        assertThat(detail.mainContractor()).isEqualTo("Test Contractor");
        assertThat(detail.scope()).containsExactly("rebar", "BBS");
    }

    @Test
    void getProjectBySlug_throws_ProjectNotFoundException_for_unknown_slug() {
        when(repository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProjectBySlug("nonexistent"))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void getFilters_returns_vocabularies_and_distinct_countries() {
        when(repository.findDistinctCountries()).thenReturn(List.of("Bahrain", "Qatar", "UAE"));

        ProjectFiltersDto filters = service.getFilters();

        assertThat(filters.sectors()).containsExactly(
                "airport", "mall_retail", "hotel_hospitality",
                "residential", "infrastructure_rail", "leisure_museum"
        );
        assertThat(filters.statuses()).containsExactly("ongoing", "completed");
        assertThat(filters.countries()).containsExactly("Bahrain", "Qatar", "UAE");
    }

    /** Creates a sample ProjectEntity for test assertions. */
    private ProjectEntity sampleEntity() {
        var entity = new ProjectEntity();
        entity.setSlug("test-project");
        entity.setName("Test Project");
        entity.setSector("airport");
        entity.setCountry("UAE");
        entity.setStatus("completed");
        entity.setImage("/img/projects/test.jpg");
        entity.setDescription("A test project description");
        entity.setMainContractor("Test Contractor");
        entity.setClient("Test Client");
        entity.setConsultant("Test Consultant");
        entity.setLocation("Dubai, UAE");
        entity.setScope(List.of("rebar", "BBS"));
        entity.setFeaturable(true);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }
}
