package com.alef.api.portfolio.vocabulary;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Sector vocabulary enum contract used by filter validation
 * and the /filters endpoint (DEC-009, F3-AC1).
 */
class SectorTest {

    @Test
    void wireValues_returns_all_sectors_in_order() {
        List<String> values = Sector.wireValues();
        assertThat(values).containsExactly(
                "airport", "mall_retail", "hotel_hospitality",
                "residential", "infrastructure_rail", "leisure_museum"
        );
    }

    @Test
    void isValid_accepts_known_values() {
        assertThat(Sector.isValid("airport")).isTrue();
        assertThat(Sector.isValid("mall_retail")).isTrue();
        assertThat(Sector.isValid("leisure_museum")).isTrue();
    }

    @Test
    void isValid_rejects_unknown_values() {
        assertThat(Sector.isValid("unknown")).isFalse();
        assertThat(Sector.isValid("")).isFalse();
        assertThat(Sector.isValid("AIRPORT")).isFalse();
    }
}
