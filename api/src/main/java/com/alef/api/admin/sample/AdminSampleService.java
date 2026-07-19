package com.alef.api.admin.sample;

import com.alef.api.admin.error.AdminDuplicateKeyException;
import com.alef.api.admin.error.AdminInvalidVocabularyException;
import com.alef.api.admin.error.AdminResourceNotFoundException;
import com.alef.api.admin.sample.dto.AdminSampleDto;
import com.alef.api.admin.sample.dto.AdminSampleRequest;
import com.alef.api.sample.entity.SampleEntity;
import com.alef.api.sample.repository.SampleRepository;
import com.alef.api.sample.vocabulary.SampleCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Admin write path for samples (design.md §A.5, F12-AC16..AC19, DEC-023).
 *
 * The only write path samples have in v1 -- feature 004's public gated read
 * attaches later with no schema or entity change. No kb.changed emission
 * (samples are not in v1's RAG scope, architecture §2.5).
 */
@Service
public class AdminSampleService {

    private static final String RESOURCE_TYPE = "Sample";

    private final SampleRepository repository;

    public AdminSampleService(SampleRepository repository) {
        this.repository = repository;
    }

    /** GET /api/admin/samples -- all rows, ordered by displayOrder. */
    @Transactional(readOnly = true)
    public List<AdminSampleDto> listAll() {
        return repository.findAllByOrderByDisplayOrderAsc().stream()
                .map(AdminSampleService::toDto)
                .toList();
    }

    /** GET /api/admin/samples/{id} -- single record; 404 if unknown. */
    @Transactional(readOnly = true)
    public AdminSampleDto getById(Long id) {
        return toDto(findOrThrow(id));
    }

    /** POST /api/admin/samples -- create (F12-AC16). Validates slug uniqueness + category vocab. */
    @Transactional
    public AdminSampleDto create(AdminSampleRequest request) {
        validateCategory(request.category());
        if (repository.existsBySlug(request.slug())) {
            throw new AdminDuplicateKeyException("slug", request.slug());
        }

        SampleEntity entity = new SampleEntity();
        applyRequest(entity, request);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toDto(repository.save(entity));
    }

    /** PUT /api/admin/samples/{id} -- update (F12-AC17). */
    @Transactional
    public AdminSampleDto update(Long id, AdminSampleRequest request) {
        SampleEntity entity = findOrThrow(id);
        validateCategory(request.category());
        if (repository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new AdminDuplicateKeyException("slug", request.slug());
        }

        applyRequest(entity, request);
        entity.setUpdatedAt(Instant.now());
        return toDto(repository.save(entity));
    }

    /** DELETE /api/admin/samples/{id} -- delete (F12-AC18). */
    @Transactional
    public void delete(Long id) {
        SampleEntity entity = findOrThrow(id);
        repository.delete(entity);
    }

    private SampleEntity findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AdminResourceNotFoundException(RESOURCE_TYPE, id));
    }

    private void applyRequest(SampleEntity entity, AdminSampleRequest request) {
        entity.setSlug(request.slug());
        entity.setTitle(request.title());
        entity.setCategory(request.category());
        entity.setPreview(request.preview());
        entity.setFile(request.file());
        entity.setDisplayOrder(request.displayOrder());
    }

    private void validateCategory(String category) {
        if (!SampleCategory.isValid(category)) {
            throw new AdminInvalidVocabularyException("category", category);
        }
    }

    private static AdminSampleDto toDto(SampleEntity entity) {
        return new AdminSampleDto(
                entity.getId(),
                entity.getSlug(),
                entity.getTitle(),
                entity.getCategory(),
                entity.getPreview(),
                entity.getFile(),
                entity.getDisplayOrder()
        );
    }
}
