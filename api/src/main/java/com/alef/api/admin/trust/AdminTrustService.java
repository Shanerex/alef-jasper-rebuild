package com.alef.api.admin.trust;

import com.alef.api.admin.error.AdminDuplicateKeyException;
import com.alef.api.admin.error.AdminInvalidVocabularyException;
import com.alef.api.admin.error.AdminResourceNotFoundException;
import com.alef.api.admin.trust.dto.AdminTrustOverviewDto;
import com.alef.api.admin.trust.dto.AdminTrustRequest;
import com.alef.api.admin.trust.dto.AdminTrustRowDto;
import com.alef.api.admin.trust.dto.DerivedTrustFactsDto;
import com.alef.api.portfolio.repository.ProjectRepository;
import com.alef.api.trust.entity.TrustContentEntity;
import com.alef.api.trust.repository.TrustContentRepository;
import com.alef.api.trust.vocabulary.ItemType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Admin write path for trust_content rows (design.md §A.4, F12-AC12..AC15, DEC-025).
 *
 * Reuses TrustContentEntity/TrustContentRepository from feature 005. Derived
 * trust facts (marquee count, client/contractor names) are read live from the
 * project table for context but are never accepted as writable input here --
 * they are edited through AdminProjectService instead (DEC-025, architecture
 * §2.2's single-source-of-truth reconciliation).
 */
@Service
public class AdminTrustService {

    private static final String RESOURCE_TYPE = "Trust content row";

    private final TrustContentRepository trustRepository;
    private final ProjectRepository projectRepository;

    public AdminTrustService(TrustContentRepository trustRepository, ProjectRepository projectRepository) {
        this.trustRepository = trustRepository;
        this.projectRepository = projectRepository;
    }

    /** GET /api/admin/trust -- rows grouped by item_type plus the read-only derived panel. */
    @Transactional(readOnly = true)
    public AdminTrustOverviewDto getOverview() {
        List<AdminTrustRowDto> stats = mapGroup(ItemType.STAT.wireValue());
        List<AdminTrustRowDto> software = mapGroup(ItemType.SOFTWARE.wireValue());
        List<AdminTrustRowDto> standards = mapGroup(ItemType.STANDARD.wireValue());

        var derived = DerivedTrustFactsDto.of(
                projectRepository.countByFeaturableTrue(),
                projectRepository.findDistinctClients(),
                projectRepository.findDistinctMainContractors());

        return new AdminTrustOverviewDto(stats, software, standards, derived);
    }

    /** POST /api/admin/trust -- create a stat/software/standard row (F12-AC12..AC14). */
    @Transactional
    public AdminTrustRowDto create(AdminTrustRequest request) {
        validate(request);
        if (trustRepository.existsByItemKey(request.itemKey())) {
            throw new AdminDuplicateKeyException("itemKey", request.itemKey());
        }

        TrustContentEntity entity = new TrustContentEntity();
        applyRequest(entity, request);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toDto(trustRepository.save(entity));
    }

    /** PUT /api/admin/trust/{id} -- update label/value/unit/displayOrder (and itemKey/itemType if needed). */
    @Transactional
    public AdminTrustRowDto update(Long id, AdminTrustRequest request) {
        TrustContentEntity entity = findOrThrow(id);
        validate(request);
        if (trustRepository.existsByItemKeyAndIdNot(request.itemKey(), id)) {
            throw new AdminDuplicateKeyException("itemKey", request.itemKey());
        }

        applyRequest(entity, request);
        entity.setUpdatedAt(Instant.now());
        return toDto(trustRepository.save(entity));
    }

    /** DELETE /api/admin/trust/{id} -- delete a row. */
    @Transactional
    public void delete(Long id) {
        TrustContentEntity entity = findOrThrow(id);
        trustRepository.delete(entity);
    }

    private TrustContentEntity findOrThrow(Long id) {
        return trustRepository.findById(id)
                .orElseThrow(() -> new AdminResourceNotFoundException(RESOURCE_TYPE, id));
    }

    private void applyRequest(TrustContentEntity entity, AdminTrustRequest request) {
        entity.setItemKey(request.itemKey());
        entity.setItemType(request.itemType());
        entity.setLabel(request.label());
        entity.setValue(request.value());
        entity.setUnit(request.unit());
        entity.setDisplayOrder(request.displayOrder());
    }

    /**
     * Validates itemType against the ItemType vocabulary, and enforces the
     * conditional rule from design §A.4: value is required for 'stat' rows,
     * may be null for 'software'/'standard' badge rows.
     */
    private void validate(AdminTrustRequest request) {
        if (!ItemType.isValid(request.itemType())) {
            throw new AdminInvalidVocabularyException("itemType", request.itemType());
        }
        boolean isStat = ItemType.STAT.wireValue().equals(request.itemType());
        if (isStat && (request.value() == null || request.value().isBlank())) {
            throw new AdminInvalidVocabularyException("value", "value is required for item_type=stat");
        }
    }

    private List<AdminTrustRowDto> mapGroup(String itemType) {
        return trustRepository.findByItemTypeOrderByDisplayOrderAsc(itemType).stream()
                .map(AdminTrustService::toDto)
                .toList();
    }

    private static AdminTrustRowDto toDto(TrustContentEntity entity) {
        return new AdminTrustRowDto(
                entity.getId(),
                entity.getItemKey(),
                entity.getItemType(),
                entity.getLabel(),
                entity.getValue(),
                entity.getUnit(),
                entity.getDisplayOrder()
        );
    }
}
