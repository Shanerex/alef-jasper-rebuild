package com.alef.api.trust.service;

import com.alef.api.portfolio.repository.ProjectRepository;
import com.alef.api.trust.dto.TrustOverviewDto;
import com.alef.api.trust.dto.TrustStatDto;
import com.alef.api.trust.entity.TrustContentEntity;
import com.alef.api.trust.repository.TrustContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TrustService composition logic (architecture 3.1).
 *
 * Verifies that the service correctly assembles stats from trust_content,
 * synthesizes the marquee count from the project table, and derives
 * client/contractor names and software/standards labels. Repositories are mocked.
 */
@ExtendWith(MockitoExtension.class)
class TrustServiceTest {

    @Mock
    private TrustContentRepository trustContentRepository;

    @Mock
    private ProjectRepository projectRepository;

    private TrustService service;

    @BeforeEach
    void setUp() {
        service = new TrustService(trustContentRepository, projectRepository);
    }

    @Test
    void getOverview_assembles_stats_plus_synthesized_marquee_count() {
        var statRow = statEntity("years_in_business", "Years in Business", "18+", null, 1);
        when(trustContentRepository.findByItemTypeOrderByDisplayOrderAsc("stat"))
                .thenReturn(List.of(statRow));
        when(projectRepository.countByFeaturableTrue()).thenReturn(12L);
        stubEmptyProjectDerived();
        stubEmptyBadges();

        TrustOverviewDto overview = service.getOverview();

        assertThat(overview.stats()).hasSize(2);
        // First stat from trust_content
        TrustStatDto first = overview.stats().get(0);
        assertThat(first.key()).isEqualTo("years_in_business");
        assertThat(first.label()).isEqualTo("Years in Business");
        assertThat(first.value()).isEqualTo("18+");
        assertThat(first.unit()).isNull();
        // Second stat is the synthesized marquee count
        TrustStatDto marquee = overview.stats().get(1);
        assertThat(marquee.key()).isEqualTo("marquee_projects");
        assertThat(marquee.label()).isEqualTo("Marquee Projects");
        assertThat(marquee.value()).isEqualTo("12");
        assertThat(marquee.unit()).isNull();
    }

    @Test
    void getOverview_includes_stat_unit_when_present() {
        var statRow = statEntity("monthly_steel_capacity_tonnes", "Monthly Steel Capacity",
                "5,000", "tonnes / month", 3);
        when(trustContentRepository.findByItemTypeOrderByDisplayOrderAsc("stat"))
                .thenReturn(List.of(statRow));
        when(projectRepository.countByFeaturableTrue()).thenReturn(0L);
        stubEmptyProjectDerived();
        stubEmptyBadges();

        TrustOverviewDto overview = service.getOverview();

        TrustStatDto capacity = overview.stats().get(0);
        assertThat(capacity.unit()).isEqualTo("tonnes / month");
    }

    @Test
    void getOverview_derives_clients_and_contractors_from_project_table() {
        stubEmptyStats();
        when(projectRepository.findDistinctClients())
                .thenReturn(List.of("ALEC", "Habtoor", "Six Construct"));
        when(projectRepository.findDistinctMainContractors())
                .thenReturn(List.of("AECOM", "Bechtel"));
        stubEmptyBadges();

        TrustOverviewDto overview = service.getOverview();

        assertThat(overview.clients()).containsExactly("ALEC", "Habtoor", "Six Construct");
        assertThat(overview.contractors()).containsExactly("AECOM", "Bechtel");
    }

    @Test
    void getOverview_extracts_software_and_standards_labels() {
        stubEmptyStats();
        stubEmptyProjectDerived();
        var autocad = badgeEntity("software_autocad", "software", "AutoCAD", 1);
        var cadsRc = badgeEntity("software_cads_rc", "software", "CADS RC", 2);
        when(trustContentRepository.findByItemTypeOrderByDisplayOrderAsc("software"))
                .thenReturn(List.of(autocad, cadsRc));
        var bs8666 = badgeEntity("standard_bs_8666", "standard", "BS 8666", 1);
        when(trustContentRepository.findByItemTypeOrderByDisplayOrderAsc("standard"))
                .thenReturn(List.of(bs8666));

        TrustOverviewDto overview = service.getOverview();

        assertThat(overview.software()).containsExactly("AutoCAD", "CADS RC");
        assertThat(overview.standards()).containsExactly("BS 8666");
    }

    @Test
    void getOverview_returns_empty_arrays_when_no_data_exists() {
        stubEmptyStats();
        stubEmptyProjectDerived();
        stubEmptyBadges();

        TrustOverviewDto overview = service.getOverview();

        // Stats still has the synthesized marquee count even with no trust_content rows
        assertThat(overview.stats()).hasSize(1);
        assertThat(overview.stats().get(0).key()).isEqualTo("marquee_projects");
        assertThat(overview.clients()).isEmpty();
        assertThat(overview.contractors()).isEmpty();
        assertThat(overview.software()).isEmpty();
        assertThat(overview.standards()).isEmpty();
    }

    @Test
    void getOverview_preserves_display_order_from_trust_content() {
        var second = statEntity("staff_count", "Detailing Engineers", "120+", null, 2);
        var first = statEntity("years_in_business", "Years in Business", "18+", null, 1);
        // Repository returns them in display_order; verify service preserves that order
        when(trustContentRepository.findByItemTypeOrderByDisplayOrderAsc("stat"))
                .thenReturn(List.of(first, second));
        when(projectRepository.countByFeaturableTrue()).thenReturn(5L);
        stubEmptyProjectDerived();
        stubEmptyBadges();

        TrustOverviewDto overview = service.getOverview();

        assertThat(overview.stats()).hasSize(3);
        assertThat(overview.stats().get(0).key()).isEqualTo("years_in_business");
        assertThat(overview.stats().get(1).key()).isEqualTo("staff_count");
        assertThat(overview.stats().get(2).key()).isEqualTo("marquee_projects");
    }

    // --- Test helpers ---

    /** Creates a stat-type TrustContentEntity for test assertions. */
    private TrustContentEntity statEntity(String key, String label, String value,
                                           String unit, int order) {
        var entity = new TrustContentEntity();
        entity.setItemKey(key);
        entity.setItemType("stat");
        entity.setLabel(label);
        entity.setValue(value);
        entity.setUnit(unit);
        entity.setDisplayOrder(order);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    /** Creates a badge-type TrustContentEntity (software or standard) for test assertions. */
    private TrustContentEntity badgeEntity(String key, String type, String label, int order) {
        var entity = new TrustContentEntity();
        entity.setItemKey(key);
        entity.setItemType(type);
        entity.setLabel(label);
        entity.setDisplayOrder(order);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    /** Stubs all stat-related repositories to return empty/zero values. */
    private void stubEmptyStats() {
        when(trustContentRepository.findByItemTypeOrderByDisplayOrderAsc("stat"))
                .thenReturn(List.of());
        when(projectRepository.countByFeaturableTrue()).thenReturn(0L);
    }

    /** Stubs the project-derived client/contractor queries to return empty lists. */
    private void stubEmptyProjectDerived() {
        when(projectRepository.findDistinctClients()).thenReturn(List.of());
        when(projectRepository.findDistinctMainContractors()).thenReturn(List.of());
    }

    /** Stubs the software/standards badge queries to return empty lists. */
    private void stubEmptyBadges() {
        when(trustContentRepository.findByItemTypeOrderByDisplayOrderAsc("software"))
                .thenReturn(List.of());
        when(trustContentRepository.findByItemTypeOrderByDisplayOrderAsc("standard"))
                .thenReturn(List.of());
    }
}
