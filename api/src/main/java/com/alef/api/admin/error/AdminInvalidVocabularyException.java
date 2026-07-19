package com.alef.api.admin.error;

/**
 * Thrown when an admin create/update body supplies a value outside a
 * DEC-009 app-layer vocabulary (sector, status, scope item, trust item_type,
 * sample category) -- design.md "Common error shapes" folds this into the
 * generic 400 Validation Failed shape via AdminExceptionHandler.
 */
public class AdminInvalidVocabularyException extends RuntimeException {

    private final String field;

    public AdminInvalidVocabularyException(String field, String value) {
        super("Invalid value for %s: %s".formatted(field, value));
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
