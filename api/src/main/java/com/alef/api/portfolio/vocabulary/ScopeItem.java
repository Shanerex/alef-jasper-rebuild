package com.alef.api.portfolio.vocabulary;

import java.util.Arrays;
import java.util.List;

/**
 * Application-layer vocabulary for project scope entries (DEC-009, feature 012 design §A.2).
 *
 * project.scope is a Postgres TEXT[] (DEC-010); this enum is the admin-write-side
 * validation for each entry, matching the Sector/ProjectStatus vocabulary pattern.
 * The public read path (feature 003) never validated scope values because it only
 * ever read seeded data; feature 012 is the first write path, so validation is
 * introduced here rather than retrofitted onto ProjectEntity.
 */
public enum ScopeItem {

    REBAR("rebar"),
    BBS("BBS"),
    GA("GA"),
    MEP("MEP"),
    QS("QS"),
    AS_BUILT("as-built");

    private final String wireValue;

    ScopeItem(String wireValue) {
        this.wireValue = wireValue;
    }

    /** Returns the token used in the API and stored in the database. */
    public String wireValue() {
        return wireValue;
    }

    /** Checks whether a given string is a recognized scope wire value. */
    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(s -> s.wireValue.equals(value));
    }

    /** Returns the ordered list of wire values. */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(ScopeItem::wireValue).toList();
    }
}
