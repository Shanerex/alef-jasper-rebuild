package com.alef.api.admin.sample;

import com.alef.api.admin.error.AdminDuplicateKeyException;
import com.alef.api.admin.error.AdminExceptionHandler;
import com.alef.api.admin.sample.dto.AdminSampleDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests for the admin sample CRUD contract (design.md §A.5, F12-AC16..AC19).
 * Security filters disabled -- see AdminProjectControllerTest's class doc for rationale.
 */
@WebMvcTest({AdminSampleController.class, AdminExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminSampleControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminSampleService service;

    @Test
    void create_valid_sample_returns_201() throws Exception {
        when(service.create(any())).thenReturn(
                new AdminSampleDto(1L, "prequalification-profile", "Prequalification Profile",
                        "prequalification", null, null, 0));

        mvc.perform(post("/api/admin/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"prequalification-profile","title":"Prequalification Profile",
                                 "category":"prequalification"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("prequalification-profile"));
    }

    @Test
    void create_missing_category_returns_400() throws Exception {
        mvc.perform(post("/api/admin/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"x","title":"X"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.category").isArray());
    }

    @Test
    void create_duplicate_slug_returns_409() throws Exception {
        when(service.create(any())).thenThrow(new AdminDuplicateKeyException("slug", "x"));

        mvc.perform(post("/api/admin/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"x","title":"X","category":"bbs"}
                                """))
                .andExpect(status().isConflict());
    }
}
