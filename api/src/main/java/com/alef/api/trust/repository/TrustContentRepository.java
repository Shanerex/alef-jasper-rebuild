package com.alef.api.trust.repository;

import com.alef.api.trust.entity.TrustContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Read-only repository for the trust_content table (feature 005).
 *
 * Provides finder methods by item_type, ordered by display_order, so the
 * service can group editorial facts for the composed trust overview payload.
 * The write path is deferred to feature 012 (Admin Content Management).
 */
public interface TrustContentRepository extends JpaRepository<TrustContentEntity, Long> {

    /**
     * Finds all trust content rows matching the given item type, ordered by display_order.
     *
     * Used to retrieve stat, software, or standard groups for the composed overview.
     * The ordering ensures F012 can reorder items without a schema change.
     */
    List<TrustContentEntity> findByItemTypeOrderByDisplayOrderAsc(String itemType);
}
