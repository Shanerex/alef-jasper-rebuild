package com.alef.api.portfolio.vocabulary;

import java.util.Arrays;
import java.util.List;

/**
 * Application-layer vocabulary for project sectors (DEC-009).
 *
 * The allowed set is defined by F3-AC1 and served via GET /api/projects/filters.
 * Stored as TEXT in Postgres -- the DB does not enforce this vocabulary.
 * Adding a new sector requires only an enum constant here and an API redeploy,
 * not a database migration.
 */
public enum Sector {

    AIRPORT("airport"),
    MALL_RETAIL("mall_retail"),
    HOTEL_HOSPITALITY("hotel_hospitality"),
    RESIDENTIAL("residential"),
    INFRASTRUCTURE_RAIL("infrastructure_rail"),
    LEISURE_MUSEUM("leisure_museum");

    private final String wireValue;

    Sector(String wireValue) {
        this.wireValue = wireValue;
    }

    /** Returns the lowercase token used in the API and stored in the database. */
    public String wireValue() {
        return wireValue;
    }

    /** Checks whether a given string is a recognized sector wire value. */
    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(s -> s.wireValue.equals(value));
    }

    /** Returns the ordered list of wire values, used by the /filters endpoint. */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(Sector::wireValue).toList();
    }
}
