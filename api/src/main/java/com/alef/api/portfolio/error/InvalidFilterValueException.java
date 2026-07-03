package com.alef.api.portfolio.error;

/**
 * Thrown when a filter parameter contains an unrecognized vocabulary value.
 *
 * Sector and status are constrained sets validated in the app layer (DEC-009).
 * An unknown value in either filter produces a 400 response (architecture 3.1).
 * Country is data-derived and does not trigger this exception -- an unknown
 * country simply yields an empty result set.
 */
public class InvalidFilterValueException extends RuntimeException {

    public InvalidFilterValueException(String filterName, String value) {
        super("Invalid value '%s' for filter '%s'".formatted(value, filterName));
    }
}
