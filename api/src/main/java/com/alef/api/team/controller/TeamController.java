package com.alef.api.team.controller;

import com.alef.api.team.dto.TeamOverviewDto;
import com.alef.api.team.service.TeamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only REST endpoint for team profiles (architecture §3.1, feature 011).
 *
 * Returns the active leadership roster for the About page in a single call.
 * All logic lives in TeamService; this controller owns no business logic,
 * mirroring the TrustController pattern from feature 005.
 */
@RestController
@RequestMapping("/api/team")
public class TeamController {

    private final TeamService service;

    public TeamController(TeamService service) {
        this.service = service;
    }

    /**
     * GET /api/team -- active team profiles in display order (F11-AC3).
     *
     * Returns a { members } envelope. Empty list when no active profiles exist;
     * never null. No parameters -- the roster is returned in full, ordered by
     * display_order set in the seed (architecture §3.1 no-pagination rationale).
     */
    @GetMapping
    public TeamOverviewDto getTeam() {
        return service.getTeam();
    }
}
