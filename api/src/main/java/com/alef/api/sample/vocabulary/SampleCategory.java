package com.alef.api.sample.vocabulary;

import java.util.Arrays;
import java.util.List;

/**
 * Application-layer vocabulary for sample categories (DEC-009, architecture §3.4).
 *
 * The five 004 "books": Prequalification profile, BBS, Drawings, Bridge drawings,
 * Roads &amp; utility. Stored as TEXT in Postgres -- the DB does not enforce this
 * vocabulary, so a category can be added without a migration (same stance as
 * Sector/ItemType).
 */
public enum SampleCategory {

    PREQUALIFICATION("prequalification"),
    BBS("bbs"),
    DRAWINGS("drawings"),
    BRIDGE_DRAWINGS("bridge_drawings"),
    ROADS_UTILITY("roads_utility");

    private final String wireValue;

    SampleCategory(String wireValue) {
        this.wireValue = wireValue;
    }

    /** Returns the lowercase token used in the API and stored in the database. */
    public String wireValue() {
        return wireValue;
    }

    /** Checks whether a given string is a recognized category wire value. */
    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(c -> c.wireValue.equals(value));
    }

    /** Returns the ordered list of wire values. */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(SampleCategory::wireValue).toList();
    }
}
