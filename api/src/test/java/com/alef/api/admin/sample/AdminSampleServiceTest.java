package com.alef.api.admin.sample;

import com.alef.api.admin.error.AdminDuplicateKeyException;
import com.alef.api.admin.error.AdminInvalidVocabularyException;
import com.alef.api.admin.error.AdminResourceNotFoundException;
import com.alef.api.admin.sample.dto.AdminSampleRequest;
import com.alef.api.sample.entity.SampleEntity;
import com.alef.api.sample.repository.SampleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AdminSampleService (design.md §A.5, F12-AC16..AC19, DEC-023).
 */
@ExtendWith(MockitoExtension.class)
class AdminSampleServiceTest {

    @Mock
    private SampleRepository repository;

    private AdminSampleService service;

    @BeforeEach
    void setUp() {
        service = new AdminSampleService(repository);
    }

    @Test
    void create_persists_with_null_preview_and_file_allowed() {
        var request = new AdminSampleRequest("prequalification-profile", "Prequalification Profile",
                "prequalification", null, null, 0);
        when(repository.existsBySlug("prequalification-profile")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.create(request);

        assertThat(dto.slug()).isEqualTo("prequalification-profile");
        assertThat(dto.preview()).isNull();
        assertThat(dto.file()).isNull();
    }

    @Test
    void create_with_duplicate_slug_throws_409() {
        var request = new AdminSampleRequest("existing", "Title", "bbs", null, null, 0);
        when(repository.existsBySlug("existing")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminDuplicateKeyException.class);
    }

    @Test
    void create_with_invalid_category_throws_vocabulary_exception() {
        var request = new AdminSampleRequest("slug", "Title", "not-a-real-category", null, null, 0);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminInvalidVocabularyException.class);
    }

    @Test
    void update_excludes_the_row_itself_from_slug_uniqueness() {
        var existing = entity(1L, "current-slug");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsBySlugAndIdNot("current-slug", 1L)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.update(1L,
                new AdminSampleRequest("current-slug", "Title", "drawings", null, null, 0));

        assertThat(dto.slug()).isEqualTo("current-slug");
    }

    @Test
    void update_unknown_id_throws_404_style_not_found() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L,
                new AdminSampleRequest("slug", "Title", "drawings", null, null, 0)))
                .isInstanceOf(AdminResourceNotFoundException.class);
    }

    @Test
    void delete_removes_the_row() {
        var existing = entity(2L, "to-delete");
        when(repository.findById(2L)).thenReturn(Optional.of(existing));

        service.delete(2L);

        verify(repository).delete(existing);
    }

    private SampleEntity entity(long id, String slug) {
        SampleEntity e = new SampleEntity();
        e.setSlug(slug);
        e.setTitle("Title");
        e.setCategory("bbs");
        e.setDisplayOrder(0);
        e.setCreatedAt(java.time.Instant.now());
        e.setUpdatedAt(java.time.Instant.now());
        return e;
    }
}
