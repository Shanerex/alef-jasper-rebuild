package com.alef.api.admin.project;

import com.alef.api.admin.error.AdminDuplicateKeyException;
import com.alef.api.admin.error.AdminExceptionHandler;
import com.alef.api.admin.error.AdminInvalidVocabularyException;
import com.alef.api.admin.error.AdminResourceNotFoundException;
import com.alef.api.admin.project.dto.AdminProjectDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests for the admin project CRUD contract (design.md §A.2,
 * F12-AC4..AC7, F12-AC23). Security filters are disabled here (addFilters =
 * false) -- auth/CSRF are covered exhaustively by AdminSecurityWebTest; this
 * class isolates the request/response/validation contract with the service
 * mocked, mirroring ProjectControllerTest's pattern.
 */
@WebMvcTest({AdminProjectController.class, AdminExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminProjectControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminProjectService service;

    private static final String VALID_BODY = """
            {
              "slug": "doha-metro-gold-line",
              "name": "Doha Metro - Gold Line",
              "sector": "infrastructure_rail",
              "country": "Qatar",
              "status": "completed",
              "scope": ["rebar", "BBS"],
              "featurable": true
            }
            """;

    @Test
    void create_valid_project_returns_201() throws Exception {
        var dto = dto(1L, "doha-metro-gold-line");
        when(service.create(any())).thenReturn(dto);

        mvc.perform(post("/api/admin/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("doha-metro-gold-line"));
    }

    @Test
    void create_missing_required_field_returns_400_with_field_map() throws Exception {
        String body = """
                {"slug": "", "name": "", "sector": "", "country": "", "status": ""}
                """;

        mvc.perform(post("/api/admin/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.fields.slug").isArray())
                .andExpect(jsonPath("$.fields.name").isArray());
    }

    @Test
    void create_bad_slug_pattern_returns_400() throws Exception {
        String body = """
                {"slug": "Not A Valid Slug!", "name": "Name", "sector": "airport",
                 "country": "UAE", "status": "ongoing"}
                """;

        mvc.perform(post("/api/admin/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.slug").isArray());
    }

    @Test
    void create_duplicate_slug_returns_409() throws Exception {
        when(service.create(any())).thenThrow(new AdminDuplicateKeyException("slug", "doha-metro-gold-line"));

        mvc.perform(post("/api/admin/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void create_invalid_sector_returns_400() throws Exception {
        when(service.create(any())).thenThrow(new AdminInvalidVocabularyException("sector", "bogus"));

        mvc.perform(post("/api/admin/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.fields.sector").isArray());
    }

    @Test
    void getById_unknown_id_returns_404() throws Exception {
        when(service.getById(999L)).thenThrow(new AdminResourceNotFoundException("Project", 999L));

        mvc.perform(get("/api/admin/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void update_returns_200_with_updated_dto() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(dto(1L, "doha-metro-gold-line"));

        mvc.perform(put("/api/admin/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void delete_returns_204() throws Exception {
        mvc.perform(delete("/api/admin/projects/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void list_returns_paged_envelope() throws Exception {
        var page = new com.alef.api.portfolio.dto.PagedResponse<>(List.of(dto(1L, "a")), 0, 50, 1, 1);
        when(service.listAll(any())).thenReturn(page);

        mvc.perform(get("/api/admin/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private AdminProjectDto dto(long id, String slug) {
        return new AdminProjectDto(id, slug, "Doha Metro - Gold Line", "infrastructure_rail", "Qatar",
                "completed", null, null, null, null, null, null, List.of("rebar", "BBS"), true,
                Instant.now(), Instant.now());
    }
}
