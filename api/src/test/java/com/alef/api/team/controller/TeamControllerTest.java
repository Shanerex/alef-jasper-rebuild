package com.alef.api.team.controller;

import com.alef.api.team.dto.TeamMemberDto;
import com.alef.api.team.dto.TeamOverviewDto;
import com.alef.api.team.service.TeamService;
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
 * HTTP-level tests for GET /api/team (architecture §3.1, feature 011).
 *
 * Verifies the REST contract (status code, JSON shape, nullable fields) with a
 * mocked service, mirroring the TrustControllerTest pattern from feature 005.
 *
 * addFilters = false (feature 012): see OfficeControllerTest's class doc.
 */
@WebMvcTest(TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeamControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TeamService service;

    @Test
    void getTeam_returns_members_envelope_with_correct_fields() throws Exception {
        var overview = new TeamOverviewDto(List.of(
                new TeamMemberDto("K. Jeyaraman", "Managing Director",
                        "ALEF & JASPER", "jeyaraman@alef-jasper.com", null),
                new TeamMemberDto("J. Sunitha", "Executive Director",
                        "JASPER", "sunitha@alef-jasper.com", "/img/team/sunitha.jpg")
        ));
        when(service.getTeam()).thenReturn(overview);

        mvc.perform(get("/api/team"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(2)))
                .andExpect(jsonPath("$.members[0].name").value("K. Jeyaraman"))
                .andExpect(jsonPath("$.members[0].role").value("Managing Director"))
                .andExpect(jsonPath("$.members[0].company").value("ALEF & JASPER"))
                .andExpect(jsonPath("$.members[0].email").value("jeyaraman@alef-jasper.com"))
                .andExpect(jsonPath("$.members[0].photo").doesNotExist())
                .andExpect(jsonPath("$.members[1].photo").value("/img/team/sunitha.jpg"));
    }

    @Test
    void getTeam_returns_empty_members_array_when_no_profiles() throws Exception {
        when(service.getTeam()).thenReturn(new TeamOverviewDto(List.of()));

        mvc.perform(get("/api/team"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(0)));
    }
}
