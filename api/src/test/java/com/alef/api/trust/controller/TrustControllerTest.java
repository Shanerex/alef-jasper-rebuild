package com.alef.api.trust.controller;

import com.alef.api.trust.dto.TrustOverviewDto;
import com.alef.api.trust.dto.TrustStatDto;
import com.alef.api.trust.service.TrustService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests for the trust overview endpoint.
 *
 * Verifies the REST contract (status code, JSON shape) with a mocked service
 * layer. Mirrors the portfolio controller test pattern -- no DB integration,
 * just the web slice.
 *
 * addFilters = false (feature 012): see OfficeControllerTest's class doc.
 */
@WebMvcTest(TrustController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrustControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TrustService service;

    @Test
    void getOverview_returns_composed_trust_payload() throws Exception {
        var stats = List.of(
                new TrustStatDto("years_in_business", "Years in Business", "18+", null),
                new TrustStatDto("staff_count", "Detailing Engineers", "120+", null),
                new TrustStatDto("monthly_steel_capacity_tonnes", "Monthly Steel Capacity",
                        "5,000", "tonnes / month"),
                new TrustStatDto("marquee_projects", "Marquee Projects", "12", null)
        );
        var overview = new TrustOverviewDto(
                stats,
                List.of("ALEC", "Habtoor", "Six Construct"),
                List.of("AECOM", "Bechtel"),
                List.of("AutoCAD", "CADS RC", "SteelPac RC"),
                List.of("BS 8666", "ACI 318", "BS EN ISO 3766")
        );
        when(service.getOverview()).thenReturn(overview);

        mvc.perform(get("/api/trust/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats", hasSize(4)))
                .andExpect(jsonPath("$.stats[0].key").value("years_in_business"))
                .andExpect(jsonPath("$.stats[0].label").value("Years in Business"))
                .andExpect(jsonPath("$.stats[0].value").value("18+"))
                .andExpect(jsonPath("$.stats[0].unit").doesNotExist())
                .andExpect(jsonPath("$.stats[2].unit").value("tonnes / month"))
                .andExpect(jsonPath("$.stats[3].key").value("marquee_projects"))
                .andExpect(jsonPath("$.stats[3].value").value("12"))
                .andExpect(jsonPath("$.clients", hasSize(3)))
                .andExpect(jsonPath("$.clients[0]").value("ALEC"))
                .andExpect(jsonPath("$.contractors", hasSize(2)))
                .andExpect(jsonPath("$.software", hasSize(3)))
                .andExpect(jsonPath("$.software[0]").value("AutoCAD"))
                .andExpect(jsonPath("$.standards", hasSize(3)))
                .andExpect(jsonPath("$.standards[0]").value("BS 8666"));
    }

    @Test
    void getOverview_returns_empty_arrays_when_no_data() throws Exception {
        var overview = new TrustOverviewDto(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        when(service.getOverview()).thenReturn(overview);

        mvc.perform(get("/api/trust/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats", hasSize(0)))
                .andExpect(jsonPath("$.clients", hasSize(0)))
                .andExpect(jsonPath("$.contractors", hasSize(0)))
                .andExpect(jsonPath("$.software", hasSize(0)))
                .andExpect(jsonPath("$.standards", hasSize(0)));
    }
}
