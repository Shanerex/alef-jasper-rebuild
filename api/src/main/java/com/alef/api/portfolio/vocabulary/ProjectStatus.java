package com.alef.api.portfolio.vocabulary;

import java.util.Arrays;
import java.util.List;

/**
 * Application-layer vocabulary for project statuses (DEC-009).
 *
 * The allowed set is defined by F3-AC1 (ongoing/completed) and served via
 * GET /api/projects/filters. Stored as TEXT in Postgres -- the DB does not
 * enforce this vocabulary.
 */
public enum ProjectStatus {

    ONGOING("ongoing"),
    COMPLETED("completed");

    private final String wireValue;

    ProjectStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /** Returns the lowercase token used in the API and stored in the database. */
    public String wireValue() {
        return wireValue;
    }

    /** Checks whether a given string is a recognized status wire value. */
    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(s -> s.wireValue.equals(value));
    }

    /** Returns the ordered list of wire values, used by the /filters endpoint. */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(ProjectStatus::wireValue).toList();
    }
}
