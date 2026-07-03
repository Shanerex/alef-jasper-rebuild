package com.alef.api.office.repository;

import com.alef.api.office.entity.OfficeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Read-only repository for the office table (feature 011).
 *
 * Returns all offices in display_order so Dubai (head office) always appears
 * before India (delivery centre) without any client-side ordering.
 * Feature 012 adds write operations; this read method stays unchanged.
 */
public interface OfficeRepository extends JpaRepository<OfficeEntity, Long> {

    /**
     * Returns all office rows ordered by display_order ascending.
     *
     * No filtering: both offices always display. If a third office is added
     * in the future it appears by inserting a row with the correct display_order.
     */
    List<OfficeEntity> findAllByOrderByDisplayOrderAsc();
}
