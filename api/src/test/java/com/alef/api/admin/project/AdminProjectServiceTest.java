package com.alef.api.admin.project;

import com.alef.api.admin.error.AdminDuplicateKeyException;
import com.alef.api.admin.error.AdminInvalidVocabularyException;
import com.alef.api.admin.error.AdminResourceNotFoundException;
import com.alef.api.admin.kb.KbChangedEvent;
import com.alef.api.admin.project.dto.AdminProjectRequest;
import com.alef.api.portfolio.entity.ProjectEntity;
import com.alef.api.portfolio.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AdminProjectService (design.md §A.2, F12-AC4..AC7, F12-AC25).
 *
 * Risk-weighted per the handoff test list: slug uniqueness -> 409, vocab -> 400,
 * and kb.changed emission on create/update/delete are the specific behaviours
 * called out for coverage.
 */
@ExtendWith(MockitoExtension.class)
class AdminProjectServiceTest {

    @Mock
    private ProjectRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AdminProjectService service;

    @BeforeEach
    void setUp() {
        service = new AdminProjectService(repository, eventPublisher);
    }

    @Test
    void create_persists_and_emits_kb_changed_created() {
        var request = validRequest("new-project");
        when(repository.existsBySlug("new-project")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> {
            ProjectEntity e = inv.getArgument(0);
            setId(e, 42L);
            return e;
        });

        var dto = service.create(request);

        assertThat(dto.id()).isEqualTo(42L);
        assertThat(dto.slug()).isEqualTo("new-project");
        ArgumentCaptor<KbChangedEvent> captor = ArgumentCaptor.forClass(KbChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().changeType()).isEqualTo("created");
        assertThat(captor.getValue().slug()).isEqualTo("new-project");
    }

    @Test
    void create_with_duplicate_slug_throws_409_and_never_emits_kb_changed() {
        var request = validRequest("existing-slug");
        when(repository.existsBySlug("existing-slug")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminDuplicateKeyException.class);

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void create_with_invalid_sector_throws_400_style_vocabulary_exception() {
        var request = new AdminProjectRequest("valid-slug", "Name", "not-a-real-sector", "UAE",
                "ongoing", null, null, null, null, null, null, List.of(), false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminInvalidVocabularyException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void create_with_invalid_status_throws_vocabulary_exception() {
        var request = new AdminProjectRequest("valid-slug", "Name", "airport", "UAE",
                "not-a-real-status", null, null, null, null, null, null, List.of(), false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminInvalidVocabularyException.class);
    }

    @Test
    void create_with_invalid_scope_item_throws_vocabulary_exception() {
        var request = new AdminProjectRequest("valid-slug", "Name", "airport", "UAE",
                "ongoing", null, null, null, null, null, null, List.of("not-a-scope"), false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminInvalidVocabularyException.class);
    }

    @Test
    void update_excludes_the_row_itself_from_the_slug_uniqueness_check() {
        var existing = entity(7L, "current-slug");
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.existsBySlugAndIdNot("current-slug", 7L)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = validRequest("current-slug");
        var dto = service.update(7L, request);

        assertThat(dto.slug()).isEqualTo("current-slug");
        verify(repository, never()).existsBySlug(any());
    }

    @Test
    void update_emits_kb_changed_updated() {
        var existing = entity(7L, "current-slug");
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.existsBySlugAndIdNot(any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(7L, validRequest("current-slug"));

        ArgumentCaptor<KbChangedEvent> captor = ArgumentCaptor.forClass(KbChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().changeType()).isEqualTo("updated");
    }

    @Test
    void update_unknown_id_throws_404_style_not_found_exception() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, validRequest("slug")))
                .isInstanceOf(AdminResourceNotFoundException.class);
    }

    @Test
    void delete_removes_the_row_and_emits_kb_changed_deleted() {
        var existing = entity(3L, "to-delete");
        when(repository.findById(3L)).thenReturn(Optional.of(existing));

        service.delete(3L);

        verify(repository).delete(existing);
        ArgumentCaptor<KbChangedEvent> captor = ArgumentCaptor.forClass(KbChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().changeType()).isEqualTo("deleted");
        assertThat(captor.getValue().slug()).isEqualTo("to-delete");
    }

    @Test
    void delete_unknown_id_throws_404_style_not_found_exception() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(AdminResourceNotFoundException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void listAll_maps_a_page_of_entities_to_dtos() {
        var page = new PageImpl<>(List.of(entity(1L, "a"), entity(2L, "b")));
        when(repository.findAll(any(PageRequest.class))).thenReturn(page);

        var result = service.listAll(PageRequest.of(0, 50));

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void getById_unknown_id_throws_404_style_not_found_exception() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(404L))
                .isInstanceOf(AdminResourceNotFoundException.class);
    }

    private AdminProjectRequest validRequest(String slug) {
        return new AdminProjectRequest(slug, "Name", "airport", "UAE", "ongoing",
                null, null, null, null, null, null, List.of("rebar"), false);
    }

    private ProjectEntity entity(long id, String slug) {
        ProjectEntity e = new ProjectEntity();
        setId(e, id);
        e.setSlug(slug);
        e.setName("Name");
        e.setSector("airport");
        e.setCountry("UAE");
        e.setStatus("ongoing");
        e.setScope(List.of());
        e.setFeaturable(false);
        e.setCreatedAt(java.time.Instant.now());
        e.setUpdatedAt(java.time.Instant.now());
        return e;
    }

    /**
     * ProjectEntity.id has no public setter (it is @GeneratedValue, normally
     * assigned by Hibernate) -- reflection is the pragmatic way to give a
     * mock-returned entity a deterministic id in these tests, mirroring the
     * anonymous-subclass workaround LeadServiceTest uses for the same reason.
     */
    private void setId(ProjectEntity entity, long id) {
        try {
            var field = ProjectEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
