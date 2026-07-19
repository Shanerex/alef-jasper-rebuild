package com.alef.api.admin.trust.dto;

import java.util.List;

/**
 * Read-only derived trust facts shown for context on the Trust editor
 * (design.md §A.4, DEC-025). Never writable -- marquee count and client/
 * contractor names come from the project table (DEC-014) and are edited via
 * Projects CRUD, not here.
 */
public record DerivedTrustFactsDto(
        long marqueeProjectCount,
        List<String> clients,
        List<String> contractors,
        String note
) {

    private static final String NOTE =
            "These values are derived live from Projects and are edited via the Projects section.";

    public static DerivedTrustFactsDto of(long marqueeProjectCount, List<String> clients, List<String> contractors) {
        return new DerivedTrustFactsDto(marqueeProjectCount, clients, contractors, NOTE);
    }
}
