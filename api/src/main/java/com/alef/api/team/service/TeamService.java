package com.alef.api.team.service;

import com.alef.api.team.dto.TeamMemberDto;
import com.alef.api.team.dto.TeamOverviewDto;
import com.alef.api.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reads active, ordered team profiles for the About page (architecture §3.1, feature 011).
 *
 * Intentionally thin: all ordering and filtering is delegated to the repository
 * query so the service is just a mapping layer. Feature 012 adds write methods
 * to this service; the read path here stays unchanged.
 */
@Service
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository repository;

    public TeamService(TeamRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns all active team members in display_order sequence, mapped to DTOs.
     *
     * Returns an empty envelope rather than null when the table has no active
     * rows, so the API always returns a valid JSON object (graceful degradation).
     */
    public TeamOverviewDto getTeam() {
        List<TeamMemberDto> members = repository.findAllByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(e -> new TeamMemberDto(
                        e.getName(),
                        e.getRole(),
                        e.getCompany(),
                        e.getEmail(),
                        e.getPhoto()))
                .toList();
        return new TeamOverviewDto(members);
    }
}
