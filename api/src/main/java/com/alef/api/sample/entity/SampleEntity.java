package com.alef.api.sample.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for the sample table (architecture §3.4, feature 012, DEC-023).
 *
 * Materialized by feature 012 ahead of feature 004's public Sample Explorer so
 * Samples CRUD (F12-AC16..AC19) has a write target. preview and file are
 * nullable so a sample can be created before its assets are uploaded (matches
 * the team.photo nullable-until-attached pattern). category is stored as TEXT,
 * validated in the app layer per DEC-009 (see SampleCategory).
 */
@Entity
@Table(name = "sample")
public class SampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    /** Stored as TEXT per DEC-009; validated by the SampleCategory vocabulary in the app layer. */
    @Column(nullable = false)
    private String category;

    /** Public-relative preview image path (/uploads/...); nullable until attached. */
    @Column
    private String preview;

    /** Public-relative downloadable file path (/uploads/...); nullable until attached. */
    @Column
    private String file;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Default constructor required by JPA. */
    public SampleEntity() {
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getPreview() {
        return preview;
    }

    public String getFile() {
        return file;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // --- Setters ---

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
