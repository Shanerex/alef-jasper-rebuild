package com.alef.api.team.dto;

import java.util.List;

/**
 * Envelope for GET /api/team (architecture §3.1, feature 011).
 *
 * A single composed envelope rather than a bare array, matching the 005
 * convention, so future fields (e.g. a section heading or a team-level
 * descriptor) can be added without a breaking API change.
 *
 * members is never null — returns an empty list when the table is empty
 * so the frontend can degrade gracefully (hide the section) without a
 * null-check on the members field.
 */
public record TeamOverviewDto(List<TeamMemberDto> members) {
}
