package com.alef.api.admin.project;

import com.alef.api.admin.project.dto.AdminProjectDto;
import com.alef.api.admin.project.dto.AdminProjectRequest;
import com.alef.api.portfolio.dto.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin CRUD endpoints for projects (design.md §A.2, F12-AC4..AC7, F12-AC25).
 *
 * Every request here requires ROLE_ADMIN (AdminSecurityConfig locks down
 * /api/admin/**); the controller owns no business logic, mirroring the
 * public ProjectController's thin-controller pattern.
 */
@RestController
@RequestMapping("/api/admin/projects")
public class AdminProjectController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminProjectService service;

    public AdminProjectController(AdminProjectService service) {
        this.service = service;
    }

    /** GET /api/admin/projects -- full admin list, paged. */
    @GetMapping
    public PagedResponse<AdminProjectDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int cappedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return service.listAll(PageRequest.of(page, cappedSize));
    }

    /** GET /api/admin/projects/{id} -- single record; 404 if unknown. */
    @GetMapping("/{id}")
    public AdminProjectDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /** POST /api/admin/projects -- create (F12-AC4); emits kb.changed created. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProjectDto create(@Valid @RequestBody AdminProjectRequest request) {
        return service.create(request);
    }

    /** PUT /api/admin/projects/{id} -- full update (F12-AC5); emits kb.changed updated. */
    @PutMapping("/{id}")
    public AdminProjectDto update(@PathVariable Long id, @Valid @RequestBody AdminProjectRequest request) {
        return service.update(id, request);
    }

    /** DELETE /api/admin/projects/{id} -- delete (F12-AC6); emits kb.changed deleted. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
