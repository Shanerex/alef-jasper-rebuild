package com.alef.api.admin.error;

/**
 * Thrown on a unique-key conflict (slug / item_key already in use) during an
 * admin create or update (design.md "Common error shapes": 409 Conflict).
 * Shared across the admin project/trust/sample resources (team has no
 * unique business key).
 */
public class AdminDuplicateKeyException extends RuntimeException {

    public AdminDuplicateKeyException(String field, String value) {
        super("%s already in use: %s".formatted(field, value));
    }
}
