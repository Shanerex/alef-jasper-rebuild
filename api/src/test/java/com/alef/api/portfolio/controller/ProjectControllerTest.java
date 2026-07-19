package com.alef.api.portfolio.controller;

import com.alef.api.portfolio.dto.PagedResponse;
import com.alef.api.portfolio.dto.ProjectDetailDto;
import com.alef.api.portfolio.dto.ProjectFiltersDto;
import com.alef.api.portfolio.dto.ProjectSummaryDto;
import com.alef.api.portfolio.error.InvalidFilterValueException;
import com.alef.api.portfolio.error.PortfolioExceptionHandler;
import com.alef.api.portfolio.error.ProjectNotFoundException;
import com.alef.api.portfolio.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HTTP-level tests for the project portfolio endpoints.
 *
 * Verifies the REST contract (status codes, JSON shape, error handling)
 * with a mocked service layer. Does not test DB integration -- that is
 * covered by the service tests and the full-stack docker compose flow.
 *
 * addFilters = false (feature 012): see OfficeControllerTest's class doc --
 * spring-boot-starter-security now auto-secures every @WebMvcTest slice
 * unless told otherwise; these endpoints stay public under the real
 * AdminSecurityConfig chain (proven by AdminSecurityWebTest), so filters are
 * disabled here to keep testing only this class's original concern.
 */
@WebMvcTest({ProjectController.class, PortfolioExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProjectService service;

    @Test
    void listProjects_returns_paged_summary_envelope() throws Exception {
        var summary = new ProjectSummaryDto(
                "test-project", "Test Project", "airport", "UAE",
                "completed", "/img/test.jpg", "Dubai, UAE", true);
        var response = new PagedResponse<>(List.of(summary), 0, 50, 1, 1);
        when(service.listProjects(any(), any())).thenReturn(response);

        mvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].slug").value("test-project"))
                .andExpect(jsonPath("$.content[0].name").value("Test Project"))
                .andExpect(jsonPath("$.content[0].sector").value("airport"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listProjects_with_invalid_sector_returns_400() throws Exception {
        when(service.listProjects(any(), any()))
                .thenThrow(new InvalidFilterValueException("sector", "bogus"));

        mvc.perform(get("/api/projects").param("sector", "bogus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Filter Value"))
                .andExpect(jsonPath("$.detail", containsString("bogus")));
    }

    @Test
    void getProject_returns_full_detail() throws Exception {
        var detail = new ProjectDetailDto(
                "test-project", "Test Project", "airport", "UAE", "completed",
                "/img/test.jpg", "A description", "Contractor", "Client",
                "Consultant", "Dubai, UAE", List.of("rebar", "BBS"), true);
        when(service.getProjectBySlug("test-project")).thenReturn(detail);

        mvc.perform(get("/api/projects/test-project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("test-project"))
                .andExpect(jsonPath("$.description").value("A description"))
                .andExpect(jsonPath("$.mainContractor").value("Contractor"))
                .andExpect(jsonPath("$.scope", hasSize(2)))
                .andExpect(jsonPath("$.scope[0]").value("rebar"));
    }

    @Test
    void getProject_unknown_slug_returns_404() throws Exception {
        when(service.getProjectBySlug("nonexistent"))
                .thenThrow(new ProjectNotFoundException("nonexistent"));

        mvc.perform(get("/api/projects/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Project Not Found"));
    }

    @Test
    void getFilters_returns_vocabularies_and_countries() throws Exception {
        var filters = new ProjectFiltersDto(
                List.of("airport", "mall_retail"),
                List.of("ongoing", "completed"),
                List.of("Qatar", "UAE"));
        when(service.getFilters()).thenReturn(filters);

        mvc.perform(get("/api/projects/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectors", hasSize(2)))
                .andExpect(jsonPath("$.sectors[0]").value("airport"))
                .andExpect(jsonPath("$.statuses", hasSize(2)))
                .andExpect(jsonPath("$.countries", hasSize(2)));
    }
}
