package com.alef.api.trust.vocabulary;

import java.util.Arrays;
import java.util.List;

/**
 * Application-layer vocabulary for trust_content item types (DEC-009, feature 005).
 *
 * Partitions trust content into three groups: stat (F5-AC1 headline numbers),
 * software and standard (F5-AC3 capability badges). Stored as TEXT in Postgres --
 * the DB does not enforce this vocabulary. Matches the portfolio Sector/ProjectStatus
 * vocabulary pattern.
 */
public enum ItemType {

    STAT("stat"),
    SOFTWARE("software"),
    STANDARD("standard");

    private final String wireValue;

    ItemType(String wireValue) {
        this.wireValue = wireValue;
    }

    /** Returns the lowercase token stored in the database. */
    public String wireValue() {
        return wireValue;
    }

    /** Checks whether a given string is a recognized item type wire value. */
    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(t -> t.wireValue.equals(value));
    }

    /** Returns the ordered list of wire values. */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(ItemType::wireValue).toList();
    }
}
