package com.alef.api.admin.security.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for the admin_credential singleton table (architecture §3.6, DEC-021 amended).
 *
 * Exactly one row exists, with id fixed at 1 (enforced by the DB CHECK constraint
 * in V14). password_hash is a BCrypt hash ONLY -- never plaintext, never a
 * reversible form (F12-AC29). This is a credential singleton, not a general user
 * table, so it deliberately does not presuppose feature 006's per-client shape
 * (F12-AC3).
 *
 * id is not @GeneratedValue: the seeder explicitly sets it to 1 on first insert
 * (there is exactly one row ever, so identity generation would add nothing and
 * would fight the CHECK(id = 1) constraint's intent).
 */
@Entity
@Table(name = "admin_credential")
public class AdminCredentialEntity {

    /** Always 1 -- see the DDL's CHECK(id = 1) singleton constraint. */
    @Id
    private Short id;

    /** The one admin login name. */
    @Column(nullable = false)
    private String username;

    /** BCrypt hash only -- F12-AC29. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Default constructor required by JPA. */
    public AdminCredentialEntity() {
    }

    public Short getId() {
        return id;
    }

    public void setId(Short id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
