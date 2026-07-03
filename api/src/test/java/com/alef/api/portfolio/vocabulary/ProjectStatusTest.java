package com.alef.api.portfolio.vocabulary;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the ProjectStatus vocabulary enum contract used by filter validation
 * and the /filters endpoint (DEC-009, F3-AC1).
 */
class ProjectStatusTest {

    @Test
    void wireValues_returns_both_statuses_in_order() {
        List<String> values = ProjectStatus.wireValues();
        assertThat(values).containsExactly("ongoing", "completed");
    }

    @Test
    void isValid_accepts_known_values() {
        assertThat(ProjectStatus.isValid("ongoing")).isTrue();
        assertThat(ProjectStatus.isValid("completed")).isTrue();
    }

    @Test
    void isValid_rejects_unknown_values() {
        assertThat(ProjectStatus.isValid("cancelled")).isFalse();
        assertThat(ProjectStatus.isValid("ONGOING")).isFalse();
    }
}
