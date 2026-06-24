package com.alef.api.portfolio.error;

/**
 * Thrown when a project slug does not match any record in the database.
 *
 * Translates to a 404 response. The slug is the public routing key (F3-AC2),
 * so this is the expected error for a mistyped or stale URL.
 */
public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(String slug) {
        super("Project not found: '%s'".formatted(slug));
    }
}
