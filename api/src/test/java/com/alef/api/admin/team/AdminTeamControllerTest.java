package com.alef.api.admin.team;

import com.alef.api.admin.error.AdminExceptionHandler;
import com.alef.api.admin.error.AdminResourceNotFoundException;
import com.alef.api.admin.team.dto.AdminTeamDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests for the admin team CRUD contract (design.md §A.3, F12-AC8..AC11).
 * Security filters disabled -- see AdminProjectControllerTest's class doc for rationale.
 */
@WebMvcTest({AdminTeamController.class, AdminExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminTeamControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminTeamService service;

    @Test
    void create_valid_member_returns_201() throws Exception {
        when(service.create(any())).thenReturn(new AdminTeamDto(1L, "K. Jeyaraman", "Managing Director",
                "ALEF", null, null, 0, true));

        mvc.perform(post("/api/admin/team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"K. Jeyaraman","role":"Managing Director","active":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("K. Jeyaraman"));
    }

    @Test
    void create_missing_name_returns_400() throws Exception {
        mvc.perform(post("/api/admin/team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"Managing Director"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").isArray());
    }

    @Test
    void create_invalid_email_returns_400() throws Exception {
        mvc.perform(post("/api/admin/team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"A","role":"B","email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").isArray());
    }

    @Test
    void getById_unknown_returns_404() throws Exception {
        when(service.getById(99L)).thenThrow(new AdminResourceNotFoundException("Team member", 99L));

        mvc.perform(get("/api/admin/team/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns_204() throws Exception {
        mvc.perform(delete("/api/admin/team/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void list_returns_array_including_inactive() throws Exception {
        when(service.listAll()).thenReturn(List.of(
                new AdminTeamDto(1L, "Active", "Role", null, null, null, 0, true),
                new AdminTeamDto(2L, "Inactive", "Role", null, null, null, 1, false)));

        mvc.perform(get("/api/admin/team"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[1].active").value(false));
    }
}
