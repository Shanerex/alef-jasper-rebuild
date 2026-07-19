package com.alef.api.admin.security.dto;

/**
 * Response body for POST/GET /api/admin/session (design.md §A.1).
 * `authenticated` is always true when this DTO is returned -- the false case
 * is a 401 with a ProblemDetail body, never this shape with authenticated=false.
 */
public record SessionResponseDto(boolean authenticated, String username) {
}
