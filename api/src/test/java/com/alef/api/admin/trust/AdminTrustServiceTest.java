package com.alef.api.admin.trust;

import com.alef.api.admin.error.AdminDuplicateKeyException;
import com.alef.api.admin.error.AdminInvalidVocabularyException;
import com.alef.api.admin.error.AdminResourceNotFoundException;
import com.alef.api.admin.trust.dto.AdminTrustRequest;
import com.alef.api.portfolio.repository.ProjectRepository;
import com.alef.api.trust.entity.TrustContentEntity;
import com.alef.api.trust.repository.TrustContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AdminTrustService (design.md §A.4, F12-AC12..AC15, DEC-025).
 *
 * Covers the DEC-025 reconciliation directly: derived facts (marquee count,
 * clients, contractors) appear read-only in getOverview() and are never
 * accepted as writable input (there is no field for them on AdminTrustRequest
 * at all -- the test asserting that shape is the API contract test, this
 * class focuses on the value-required-for-stat rule and uniqueness).
 */
@ExtendWith(MockitoExtension.class)
class AdminTrustServiceTest {

    @Mock
    private TrustContentRepository trustRepository;

    @Mock
    private ProjectRepository projectRepository;

    private AdminTrustService service;

    @BeforeEach
    void setUp() {
        service = new AdminTrustService(trustRepository, projectRepository);
    }

    @Test
    void getOverview_includes_derived_facts_from_the_project_table() {
        when(trustRepository.findByItemTypeOrderByDisplayOrderAsc(anyString())).thenReturn(List.of());
        when(projectRepository.countByFeaturableTrue()).thenReturn(12L);
        when(projectRepository.findDistinctClients()).thenReturn(List.of("Six Construct"));
        when(projectRepository.findDistinctMainContractors()).thenReturn(List.of("ALEC"));

        var overview = service.getOverview();

        assertThat(overview.derived().marqueeProjectCount()).isEqualTo(12L);
        assertThat(overview.derived().clients()).containsExactly("Six Construct");
        assertThat(overview.derived().contractors()).containsExactly("ALEC");
        assertThat(overview.derived().note()).contains("edited via the Projects section");
    }

    @Test
    void create_stat_without_value_throws_vocabulary_exception() {
        var request = new AdminTrustRequest("years_in_business", "stat", "Years in Business", null, null, 0);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminInvalidVocabularyException.class);
    }

    @Test
    void create_software_badge_without_value_is_allowed() {
        var request = new AdminTrustRequest("autocad", "software", "AutoCAD", null, null, 0);
        when(trustRepository.existsByItemKey("autocad")).thenReturn(false);
        when(trustRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.create(request);

        assertThat(dto.itemKey()).isEqualTo("autocad");
        assertThat(dto.value()).isNull();
    }

    @Test
    void create_with_invalid_item_type_throws_vocabulary_exception() {
        var request = new AdminTrustRequest("key", "not-a-real-type", "Label", "value", null, 0);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminInvalidVocabularyException.class);
    }

    @Test
    void create_with_duplicate_item_key_throws_409() {
        var request = new AdminTrustRequest("years_in_business", "stat", "Label", "18", null, 0);
        when(trustRepository.existsByItemKey("years_in_business")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminDuplicateKeyException.class);
    }

    @Test
    void update_unknown_id_throws_404_style_not_found() {
        when(trustRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L,
                new AdminTrustRequest("key", "stat", "Label", "1", null, 0)))
                .isInstanceOf(AdminResourceNotFoundException.class);
    }

    @Test
    void delete_removes_the_row() {
        var entity = new TrustContentEntity();
        entity.setItemKey("years_in_business");
        entity.setItemType("stat");
        entity.setLabel("Years in Business");
        when(trustRepository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L);

        org.mockito.Mockito.verify(trustRepository).delete(entity);
    }
}
