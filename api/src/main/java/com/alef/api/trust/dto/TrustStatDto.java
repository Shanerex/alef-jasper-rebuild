package com.alef.api.trust.dto;

/**
 * One headline stat in the trust overview response (architecture 3.1, F5-AC1).
 *
 * Values are display strings shown verbatim ("18+", "120+", "5,000") --
 * they are never parsed, summed, or formatted by the API or frontend.
 * The unit is optional; null when absent (e.g. years has no suffix).
 */
public record TrustStatDto(
        String key,
        String label,
        String value,
        String unit
) {
}
