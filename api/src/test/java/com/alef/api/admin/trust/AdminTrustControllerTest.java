package com.alef.api.admin.trust;

import com.alef.api.admin.error.AdminExceptionHandler;
import com.alef.api.admin.error.AdminDuplicateKeyException;
import com.alef.api.admin.trust.dto.AdminTrustOverviewDto;
import com.alef.api.admin.trust.dto.AdminTrustRowDto;
import com.alef.api.admin.trust.dto.DerivedTrustFactsDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests for the admin trust CRUD contract (design.md §A.4, F12-AC12..AC15).
 * Security filters disabled -- see AdminProjectControllerTest's class doc for rationale.
 */
@WebMvcTest({AdminTrustController.class, AdminExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminTrustControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminTrustService service;

    @Test
    void getOverview_returns_grouped_rows_and_derived_facts() throws Exception {
        var overview = new AdminTrustOverviewDto(
                List.of(new AdminTrustRowDto(1L, "years_in_business", "stat", "Years in Business", "18", null, 0)),
                List.of(),
                List.of(),
                DerivedTrustFactsDto.of(12, List.of("Six Construct"), List.of("ALEC")));
        when(service.getOverview()).thenReturn(overview);

        mvc.perform(get("/api/admin/trust"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats[0].itemKey").value("years_in_business"))
                .andExpect(jsonPath("$.derived.marqueeProjectCount").value(12))
                .andExpect(jsonPath("$.derived.note").exists());
    }

    @Test
    void create_missing_label_returns_400() throws Exception {
        mvc.perform(post("/api/admin/trust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemKey":"years_in_business","itemType":"stat","value":"18"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.label").isArray());
    }

    @Test
    void create_duplicate_item_key_returns_409() throws Exception {
        when(service.create(any())).thenThrow(new AdminDuplicateKeyException("itemKey", "years_in_business"));

        mvc.perform(post("/api/admin/trust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemKey":"years_in_business","itemType":"stat","label":"Years","value":"18"}
                                """))
                .andExpect(status().isConflict());
    }
}
