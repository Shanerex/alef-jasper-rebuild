package com.alef.api.trust.vocabulary;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the ItemType vocabulary enum contract used by the trust overview
 * composition (DEC-009, F5-AC1, F5-AC3).
 */
class ItemTypeTest {

    @Test
    void wireValues_returns_all_types_in_order() {
        List<String> values = ItemType.wireValues();
        assertThat(values).containsExactly("stat", "software", "standard");
    }

    @Test
    void isValid_accepts_known_values() {
        assertThat(ItemType.isValid("stat")).isTrue();
        assertThat(ItemType.isValid("software")).isTrue();
        assertThat(ItemType.isValid("standard")).isTrue();
    }

    @Test
    void isValid_rejects_unknown_values() {
        assertThat(ItemType.isValid("unknown")).isFalse();
        assertThat(ItemType.isValid("")).isFalse();
        assertThat(ItemType.isValid("STAT")).isFalse();
    }
}
