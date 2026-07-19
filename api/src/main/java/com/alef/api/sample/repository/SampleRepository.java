package com.alef.api.sample.repository;

import com.alef.api.sample.entity.SampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the sample table (feature 012, DEC-023).
 *
 * Only a write path exists in v1 (via the admin CRUD service); feature 004's
 * public gated read attaches later without repository changes.
 */
public interface SampleRepository extends JpaRepository<SampleEntity, Long> {

    /** Finds a sample by its unique slug; used for uniqueness checks on create/update. */
    Optional<SampleEntity> findBySlug(String slug);

    /** Returns all samples ordered for the admin list and the future public explorer. */
    List<SampleEntity> findAllByOrderByDisplayOrderAsc();

    /** Checks slug uniqueness on admin create (F12-AC16, design §A.5). */
    boolean existsBySlug(String slug);

    /** Checks slug uniqueness on admin update, excluding the row being updated. */
    boolean existsBySlugAndIdNot(String slug, Long id);
}
