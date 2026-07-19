package com.alef.api.admin.trust;

import com.alef.api.admin.trust.dto.AdminTrustOverviewDto;
import com.alef.api.admin.trust.dto.AdminTrustRequest;
import com.alef.api.admin.trust.dto.AdminTrustRowDto;
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

/**
 * Admin CRUD endpoints for trust_content (design.md §A.4, F12-AC12..AC15).
 */
@RestController
@RequestMapping("/api/admin/trust")
public class AdminTrustController {

    private final AdminTrustService service;

    public AdminTrustController(AdminTrustService service) {
        this.service = service;
    }

    /** GET /api/admin/trust -- rows grouped by item_type plus the read-only derived panel. */
    @GetMapping
    public AdminTrustOverviewDto getOverview() {
        return service.getOverview();
    }

    /** POST /api/admin/trust -- create a stat/software/standard row. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminTrustRowDto create(@Valid @RequestBody AdminTrustRequest request) {
        return service.create(request);
    }

    /** PUT /api/admin/trust/{id} -- update. */
    @PutMapping("/{id}")
    public AdminTrustRowDto update(@PathVariable Long id, @Valid @RequestBody AdminTrustRequest request) {
        return service.update(id, request);
    }

    /** DELETE /api/admin/trust/{id} -- delete a row. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
