package com.alef.api.office.dto;

import java.util.List;

/**
 * Envelope for GET /api/offices (architecture §3.2, feature 011).
 *
 * Matches the { offices } envelope shape from the architecture spec. A single
 * envelope rather than a bare array keeps the contract extensible (future fields
 * can be added without a breaking change), consistent with the 005 TrustOverviewDto
 * and the team TeamOverviewDto conventions.
 *
 * offices is never null; returns an empty list when the table is empty.
 */
public record OfficesDto(List<OfficeDto> offices) {
}
