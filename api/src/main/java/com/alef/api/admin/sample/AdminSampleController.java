package com.alef.api.admin.sample;

import com.alef.api.admin.sample.dto.AdminSampleDto;
import com.alef.api.admin.sample.dto.AdminSampleRequest;
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
 * Admin CRUD endpoints for samples (design.md §A.5, F12-AC16..AC19).
 */
@RestController
@RequestMapping("/api/admin/samples")
public class AdminSampleController {

    private final AdminSampleService service;

    public AdminSampleController(AdminSampleService service) {
        this.service = service;
    }

    /** GET /api/admin/samples -- all rows, ordered. */
    @GetMapping
    public List<AdminSampleDto> list() {
        return service.listAll();
    }

    /** GET /api/admin/samples/{id} -- single record; 404 if unknown. */
    @GetMapping("/{id}")
    public AdminSampleDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /** POST /api/admin/samples -- create (F12-AC16). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminSampleDto create(@Valid @RequestBody AdminSampleRequest request) {
        return service.create(request);
    }

    /** PUT /api/admin/samples/{id} -- update (F12-AC17). */
    @PutMapping("/{id}")
    public AdminSampleDto update(@PathVariable Long id, @Valid @RequestBody AdminSampleRequest request) {
        return service.update(id, request);
    }

    /** DELETE /api/admin/samples/{id} -- delete (F12-AC18). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
