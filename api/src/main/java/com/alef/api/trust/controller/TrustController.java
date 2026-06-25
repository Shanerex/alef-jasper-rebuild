package com.alef.api.trust.controller;

import com.alef.api.trust.dto.TrustOverviewDto;
import com.alef.api.trust.service.TrustService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only REST endpoint for the trust overview (architecture 3, feature 005).
 *
 * Serves the composed trust payload that the Trust Layer section consumes.
 * The single endpoint returns stats, client/contractor names, and capability
 * badges in one call so the frontend makes one fetch to render the whole block.
 * All logic lives in TrustService; the controller owns no business logic.
 */
@RestController
@RequestMapping("/api/trust")
public class TrustController {

    private final TrustService service;

    public TrustController(TrustService service) {
        this.service = service;
    }

    /**
     * GET /api/trust/overview -- composed trust payload (F5-AC1, F5-AC2, F5-AC3).
     *
     * Returns headline stats, client/contractor name lists, and software/standards
     * badges. Empty arrays for any group with no data -- never null.
     */
    @GetMapping("/overview")
    public TrustOverviewDto getOverview() {
        return service.getOverview();
    }
}
