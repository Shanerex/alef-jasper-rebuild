package com.alef.api.admin.error;

/**
 * Thrown when an admin GET/PUT/DELETE by internal id does not match any row
 * (design.md "Common error shapes": unknown {id} -> 404). Shared across the
 * admin project/team/trust/sample resources so they don't each define their
 * own 404 exception type.
 */
public class AdminResourceNotFoundException extends RuntimeException {

    public AdminResourceNotFoundException(String resourceType, Object id) {
        super("%s not found: %s".formatted(resourceType, id));
    }
}
