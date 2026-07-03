package com.alef.api.trust.service;

import com.alef.api.portfolio.repository.ProjectRepository;
import com.alef.api.trust.dto.TrustOverviewDto;
import com.alef.api.trust.dto.TrustStatDto;
import com.alef.api.trust.entity.TrustContentEntity;
import com.alef.api.trust.repository.TrustContentRepository;
import com.alef.api.trust.vocabulary.ItemType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Composes the trust overview payload from two sources (architecture 3.1).
 *
 * Mixes stored editorial facts from trust_content (stats, software, standards)
 * with live project-derived aggregates (marquee count, distinct client/contractor
 * names) from the project table. The marquee count is synthesized on read, never
 * stored, so it can never disagree with the portfolio.
 *
 * Returns empty arrays (not nulls) for any group with no data, so the frontend
 * can degrade gracefully by hiding empty bands.
 */
@Service
@Transactional(readOnly = true)
public class TrustService {

    private final TrustContentRepository trustContentRepository;
    private final ProjectRepository projectRepository;

    public TrustService(TrustContentRepository trustContentRepository,
                        ProjectRepository projectRepository) {
        this.trustContentRepository = trustContentRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Assembles the complete trust overview payload for GET /api/trust/overview.
     *
     * Composition rules (architecture 3.1):
     * - stats: trust_content rows of item_type='stat' ordered by display_order,
     *   PLUS a synthesized 'marquee_projects' stat derived live from project.
     * - clients: distinct non-null client names from the project table.
     * - contractors: distinct non-null main_contractor names from the project table.
     * - software: labels from trust_content where item_type='software'.
     * - standards: labels from trust_content where item_type='standard'.
     */
    public TrustOverviewDto getOverview() {
        List<TrustStatDto> stats = assembleStats();
        List<String> clients = projectRepository.findDistinctClients();
        List<String> contractors = projectRepository.findDistinctMainContractors();
        List<String> software = extractLabels(ItemType.SOFTWARE.wireValue());
        List<String> standards = extractLabels(ItemType.STANDARD.wireValue());

        return new TrustOverviewDto(stats, clients, contractors, software, standards);
    }

    /**
     * Assembles the stats array: stored editorial stats plus a synthesized marquee count.
     *
     * The marquee count is computed live from project.featurable so it tracks the
     * portfolio automatically without manual synchronization (architecture 2).
     */
    private List<TrustStatDto> assembleStats() {
        List<TrustContentEntity> statRows =
                trustContentRepository.findByItemTypeOrderByDisplayOrderAsc(ItemType.STAT.wireValue());

        List<TrustStatDto> stats = new ArrayList<>(statRows.size() + 1);
        for (TrustContentEntity row : statRows) {
            stats.add(new TrustStatDto(row.getItemKey(), row.getLabel(), row.getValue(), row.getUnit()));
        }

        // Synthesize the marquee project count from the project table (architecture 3.1).
        long marqueeCount = projectRepository.countByFeaturableTrue();
        stats.add(new TrustStatDto(
                "marquee_projects",
                "Marquee Projects",
                String.valueOf(marqueeCount),
                null
        ));

        return stats;
    }

    /**
     * Extracts the label values from trust_content rows of the given item type.
     *
     * Used for software and standards groups where only the label is displayed
     * (value and unit are null for badge-type rows).
     */
    private List<String> extractLabels(String itemType) {
        return trustContentRepository.findByItemTypeOrderByDisplayOrderAsc(itemType)
                .stream()
                .map(TrustContentEntity::getLabel)
                .toList();
    }
}
