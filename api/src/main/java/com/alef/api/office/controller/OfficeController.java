package com.alef.api.office.controller;

import com.alef.api.office.dto.OfficesDto;
import com.alef.api.office.service.OfficeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only REST endpoint for office locations (architecture §3.2, feature 011).
 *
 * Returns both offices for the Contact page in a single call. All logic lives
 * in OfficeService; this controller owns no business logic, mirroring the
 * TrustController and TeamController patterns.
 */
@RestController
@RequestMapping("/api/offices")
public class OfficeController {

    private final OfficeService service;

    public OfficeController(OfficeService service) {
        this.service = service;
    }

    /**
     * GET /api/offices -- all offices in display order (F11-AC7).
     *
     * Returns an { offices } envelope. Empty list when the table has no rows;
     * never null. No parameters -- the list is small and always returned in full,
     * ordered by display_order (Dubai first, India second).
     */
    @GetMapping
    public OfficesDto getOffices() {
        return service.getOffices();
    }
}
