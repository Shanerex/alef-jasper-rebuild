package com.alef.api.portfolio.service;

/**
 * Carrier record for the optional filter parameters on the project list endpoint.
 *
 * Avoids a long parameter list on the service method. All fields are nullable --
 * a null field means "no filter on this dimension." Sector and status are
 * vocabulary-validated by the service before querying; country is passed through
 * to the specification unvalidated (unknown country -> empty result, not error).
 */
public record ProjectFilterQuery(
        String sector,
        String country,
        String status,
        Boolean featurable
) {
}
