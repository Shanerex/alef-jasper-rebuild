package com.alef.api.admin.team;

import com.alef.api.admin.error.AdminResourceNotFoundException;
import com.alef.api.admin.team.dto.AdminTeamDto;
import com.alef.api.admin.team.dto.AdminTeamRequest;
import com.alef.api.team.entity.TeamEntity;
import com.alef.api.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Admin write path for team members (design.md §A.3, F12-AC8..AC11).
 *
 * Reuses TeamEntity/TeamRepository from feature 011 (DEC-020: the team table
 * was designed for this write path from day one). No kb.changed emission --
 * team is not in v1's RAG scope (architecture §2.5). Hard delete is supported
 * (F12-AC10) but the UI recommends the softer active=false toggle via PUT
 * (design §A.3 edge case) to preserve history.
 */
@Service
public class AdminTeamService {

    private static final String RESOURCE_TYPE = "Team member";

    private final TeamRepository repository;

    public AdminTeamService(TeamRepository repository) {
        this.repository = repository;
    }

    /** GET /api/admin/team -- all rows including active=false, ordered (design §A.3). */
    @Transactional(readOnly = true)
    public List<AdminTeamDto> listAll() {
        return repository.findAllByOrderByDisplayOrderAsc().stream()
                .map(AdminTeamService::toDto)
                .toList();
    }

    /** GET /api/admin/team/{id} -- single record; 404 if unknown. */
    @Transactional(readOnly = true)
    public AdminTeamDto getById(Long id) {
        return toDto(findOrThrow(id));
    }

    /** POST /api/admin/team -- create (F12-AC8). */
    @Transactional
    public AdminTeamDto create(AdminTeamRequest request) {
        TeamEntity entity = new TeamEntity();
        applyRequest(entity, request);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toDto(repository.save(entity));
    }

    /** PUT /api/admin/team/{id} -- update, including the active toggle and displayOrder (F12-AC9). */
    @Transactional
    public AdminTeamDto update(Long id, AdminTeamRequest request) {
        TeamEntity entity = findOrThrow(id);
        applyRequest(entity, request);
        entity.setUpdatedAt(Instant.now());
        return toDto(repository.save(entity));
    }

    /** DELETE /api/admin/team/{id} -- hard delete (F12-AC10). */
    @Transactional
    public void delete(Long id) {
        TeamEntity entity = findOrThrow(id);
        repository.delete(entity);
    }

    private TeamEntity findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AdminResourceNotFoundException(RESOURCE_TYPE, id));
    }

    private void applyRequest(TeamEntity entity, AdminTeamRequest request) {
        entity.setName(request.name());
        entity.setRole(request.role());
        entity.setCompany(request.company());
        entity.setEmail(request.email());
        entity.setPhoto(request.photo());
        entity.setDisplayOrder(request.displayOrder());
        entity.setActive(request.active());
    }

    private static AdminTeamDto toDto(TeamEntity entity) {
        return new AdminTeamDto(
                entity.getId(),
                entity.getName(),
                entity.getRole(),
                entity.getCompany(),
                entity.getEmail(),
                entity.getPhoto(),
                entity.getDisplayOrder(),
                entity.isActive()
        );
    }
}
