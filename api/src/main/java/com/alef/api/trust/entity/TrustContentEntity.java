package com.alef.api.trust.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for the trust_content table (architecture 2.1, feature 005).
 *
 * Maps the narrow key/value table that holds standalone editorial trust facts
 * (stats, software, standards). Project-derived facts (marquee count,
 * client/contractor names) are computed live from the project table and never
 * stored here.
 *
 * The item_type field is stored as TEXT, not a JPA enum, per DEC-009 -- this
 * lets values added by feature 012 read back without throwing even before a
 * redeploy adds them to the vocabulary enum.
 */
@Entity
@Table(name = "trust_content")
public class TrustContentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable machine key, e.g. 'years_in_business'. Used by F012 to address a specific item. */
    @Column(name = "item_key", nullable = false, unique = true)
    private String itemKey;

    /** Partitions items: 'stat', 'software', 'standard'. App-validated per DEC-009. */
    @Column(name = "item_type", nullable = false)
    private String itemType;

    /** Display label, e.g. 'Years in Business'. */
    @Column(nullable = false)
    private String label;

    /** Display value for stats (e.g. '18+', '5,000'); null for badge rows. */
    @Column
    private String value;

    /** Optional suffix/unit, e.g. 'tonnes / month'; null when absent. */
    @Column
    private String unit;

    /** Ordering within an item_type group; F012 can reorder without a schema change. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Default constructor required by JPA. Public for test setup in other packages. */
    public TrustContentEntity() {
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public String getItemKey() {
        return itemKey;
    }

    public String getItemType() {
        return itemType;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
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

    // --- Setters (needed for test data setup; write path is feature 012) ---

    public void setItemKey(String itemKey) {
        this.itemKey = itemKey;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setUnit(String unit) {
        this.unit = unit;
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
