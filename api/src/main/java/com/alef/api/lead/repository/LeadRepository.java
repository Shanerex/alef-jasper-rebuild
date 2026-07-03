package com.alef.api.lead.repository;

import com.alef.api.lead.entity.LeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for the lead table (feature 011).
 *
 * Feature 011 only writes leads (contact form). Read access (for admin/CRM)
 * and additional query methods are owned by feature 012.
 *
 * JpaRepository provides save() which is all LeadService needs for the
 * contact form path. No custom query methods required in this feature.
 */
public interface LeadRepository extends JpaRepository<LeadEntity, Long> {
    // save() from JpaRepository is the only method used in 011.
    // Feature 012 adds finder methods when the admin read path is needed.
}
