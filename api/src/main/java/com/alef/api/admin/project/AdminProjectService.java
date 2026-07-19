package com.alef.api.admin.project;

import com.alef.api.admin.error.AdminDuplicateKeyException;
import com.alef.api.admin.error.AdminInvalidVocabularyException;
import com.alef.api.admin.error.AdminResourceNotFoundException;
import com.alef.api.admin.kb.KbChangedEvent;
import com.alef.api.admin.project.dto.AdminProjectDto;
import com.alef.api.admin.project.dto.AdminProjectRequest;
import com.alef.api.portfolio.dto.PagedResponse;
import com.alef.api.portfolio.entity.ProjectEntity;
import com.alef.api.portfolio.repository.ProjectRepository;
import com.alef.api.portfolio.vocabulary.ProjectStatus;
import com.alef.api.portfolio.vocabulary.ScopeItem;
import com.alef.api.portfolio.vocabulary.Sector;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Admin write path for projects (design.md §A.2, F12-AC4..AC7, F12-AC25).
 *
 * Reuses ProjectEntity/ProjectRepository from the portfolio package (feature 003)
 * rather than duplicating them, per the architect's component-boundary
 * instruction. Every create/update/delete publishes a KbChangedEvent; the
 * actual Redis XADD happens AFTER_COMMIT in KbChangedPublisher so the RAG
 * side effect never blocks or fails the admin's save (DEC-024).
 */
@Service
public class AdminProjectService {

    private static final String RESOURCE_TYPE = "Project";

    private final ProjectRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public AdminProjectService(ProjectRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    /** GET /api/admin/projects -- full admin list, all fields, incl. non-featurable (design §A.2). */
    @Transactional(readOnly = true)
    public PagedResponse<AdminProjectDto> listAll(Pageable pageable) {
        var page = repository.findAll(pageable).map(AdminProjectMapper::toDto);
        return PagedResponse.from(page);
    }

    /** GET /api/admin/projects/{id} -- single record by internal id; 404 if unknown. */
    @Transactional(readOnly = true)
    public AdminProjectDto getById(Long id) {
        return AdminProjectMapper.toDto(findOrThrow(id));
    }

    /**
     * POST /api/admin/projects -- create (F12-AC4). Validates vocab + slug
     * uniqueness before persisting, then emits a "created" kb.changed event.
     */
    @Transactional
    public AdminProjectDto create(AdminProjectRequest request) {
        validateVocabulary(request);
        if (repository.existsBySlug(request.slug())) {
            throw new AdminDuplicateKeyException("slug", request.slug());
        }

        ProjectEntity entity = new ProjectEntity();
        applyRequest(entity, request);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        ProjectEntity saved = repository.save(entity);
        eventPublisher.publishEvent(KbChangedEvent.created(saved.getId(), saved.getSlug()));
        return AdminProjectMapper.toDto(saved);
    }

    /**
     * PUT /api/admin/projects/{id} -- full replace (F12-AC5). Slug uniqueness
     * excludes the row itself, since editing a slug in place is allowed
     * (design §A.2 edge case: this is an "updated" kb.changed event carrying
     * the new slug; no old-slug redirect in v1).
     */
    @Transactional
    public AdminProjectDto update(Long id, AdminProjectRequest request) {
        ProjectEntity entity = findOrThrow(id);
        validateVocabulary(request);
        if (repository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new AdminDuplicateKeyException("slug", request.slug());
        }

        applyRequest(entity, request);
        entity.setUpdatedAt(Instant.now());

        ProjectEntity saved = repository.save(entity);
        eventPublisher.publishEvent(KbChangedEvent.updated(saved.getId(), saved.getSlug()));
        return AdminProjectMapper.toDto(saved);
    }

    /**
     * DELETE /api/admin/projects/{id} -- delete (F12-AC6). Silently reduces
     * the derived marquee count / client-contractor strip if the deleted
     * project was featurable (correct -- DEC-014 single source of truth).
     */
    @Transactional
    public void delete(Long id) {
        ProjectEntity entity = findOrThrow(id);
        String slug = entity.getSlug();
        repository.delete(entity);
        eventPublisher.publishEvent(KbChangedEvent.deleted(id, slug));
    }

    private ProjectEntity findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AdminResourceNotFoundException(RESOURCE_TYPE, id));
    }

    /** Copies every writable field from the request onto the entity (create and update share this). */
    private void applyRequest(ProjectEntity entity, AdminProjectRequest request) {
        entity.setSlug(request.slug());
        entity.setName(request.name());
        entity.setSector(request.sector());
        entity.setCountry(request.country());
        entity.setStatus(request.status());
        entity.setImage(request.image());
        entity.setDescription(request.description());
        entity.setMainContractor(request.mainContractor());
        entity.setClient(request.client());
        entity.setConsultant(request.consultant());
        entity.setLocation(request.location());
        entity.setScope(request.scope() != null ? List.copyOf(request.scope()) : List.of());
        entity.setFeaturable(request.featurable());
    }

    /**
     * Validates sector, status, and each scope entry against their DEC-009
     * app-layer vocabularies (design §A.2). Bean Validation on the request DTO
     * only checks "non-blank"; the actual allowed-value check happens here so
     * the 400 response can carry the same ProblemDetail 'fields' shape as
     * every other validation failure (AdminExceptionHandler).
     */
    private void validateVocabulary(AdminProjectRequest request) {
        if (!Sector.isValid(request.sector())) {
            throw new AdminInvalidVocabularyException("sector", request.sector());
        }
        if (!ProjectStatus.isValid(request.status())) {
            throw new AdminInvalidVocabularyException("status", request.status());
        }
        if (request.scope() != null) {
            for (String item : request.scope()) {
                if (!ScopeItem.isValid(item)) {
                    throw new AdminInvalidVocabularyException("scope", item);
                }
            }
        }
    }
}
