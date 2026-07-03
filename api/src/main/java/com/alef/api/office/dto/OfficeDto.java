package com.alef.api.office.dto;

import java.util.List;

/**
 * Public DTO for one office location, returned inside GET /api/offices envelope.
 *
 * addressLines and phones are arrays to match the database columns: Dubai has
 * two phone numbers; both offices have multi-line addresses. The UI maps over
 * the arrays without parsing a blob.
 *
 * email and mapQuery are nullable; the card degrades by omitting the email link
 * and by not rendering the map iframe when mapQuery is absent.
 */
public record OfficeDto(
        String key,
        String name,
        List<String> addressLines,
        List<String> phones,
        String email,        // nullable
        String mapQuery      // nullable; plaintext for keyless embed q= parameter
) {
}
