package com.alef.api.admin.security.repository;

import com.alef.api.admin.security.entity.AdminCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for the admin_credential singleton table (DEC-021 amended).
 *
 * Every method operates on the fixed row id=1 or the table as a whole --
 * there is never more than one row in v1 (single-admin, enforced by the
 * DB CHECK(id = 1) constraint in V14).
 */
public interface AdminCredentialRepository extends JpaRepository<AdminCredentialEntity, Short> {
}
