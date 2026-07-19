package com.alef.api.admin.team;

import com.alef.api.admin.team.dto.AdminTeamDto;
import com.alef.api.admin.team.dto.AdminTeamRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin CRUD endpoints for team members (design.md §A.3, F12-AC8..AC11).
 */
@RestController
@RequestMapping("/api/admin/team")
public class AdminTeamController {

    private final AdminTeamService service;

    public AdminTeamController(AdminTeamService service) {
        this.service = service;
    }

    /** GET /api/admin/team -- all rows including inactive, ordered. */
    @GetMapping
    public List<AdminTeamDto> list() {
        return service.listAll();
    }

    /** GET /api/admin/team/{id} -- single record; 404 if unknown. */
    @GetMapping("/{id}")
    public AdminTeamDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /** POST /api/admin/team -- create (F12-AC8). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminTeamDto create(@Valid @RequestBody AdminTeamRequest request) {
        return service.create(request);
    }

    /** PUT /api/admin/team/{id} -- update, incl. active toggle and displayOrder (F12-AC9). */
    @PutMapping("/{id}")
    public AdminTeamDto update(@PathVariable Long id, @Valid @RequestBody AdminTeamRequest request) {
        return service.update(id, request);
    }

    /** DELETE /api/admin/team/{id} -- hard delete (F12-AC10). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
