package com.alef.api.office.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/**
 * JPA entity for the office table (architecture §4.3, feature 011).
 *
 * address_lines and phones are Postgres TEXT[] columns mapped as List<String>
 * using the same Hibernate 6 JdbcTypeCode(SqlTypes.ARRAY) idiom as
 * ProjectEntity.scope (DEC-010). Dubai carries two phone numbers; both offices
 * have multi-line addresses -- the array models this without nullable second-
 * column sprawl.
 *
 * map_query is a plaintext location string for the keyless Google Maps iframe
 * (architecture §10 Decision 3). Nullable so a missing query omits the map
 * without breaking the card render.
 *
 * No active flag: both offices always display; the soft-hide need that team has
 * (retiring a profile) does not apply to two fixed offices (architecture §4.3).
 */
@Entity
@Table(name = "office")
public class OfficeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable machine-readable key, e.g. 'dubai' or 'india'. Unique per row. */
    @Column(name = "office_key", nullable = false, unique = true)
    private String officeKey;

    /** Display name shown in the card heading, e.g. "ALEF — Dubai (Head Office)". */
    @Column(nullable = false)
    private String name;

    /**
     * Ordered address presentation lines for the UI.
     * Uses Hibernate 6 native array support; same idiom as ProjectEntity.scope.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "address_lines", columnDefinition = "text[]", nullable = false)
    private List<String> addressLines;

    /**
     * One or more phone numbers. Dubai has two lines; India has one.
     * Uses Hibernate 6 native array support; same idiom as ProjectEntity.scope.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> phones;

    /** Single contact email address per office. Nullable. */
    @Column
    private String email;

    /** Plaintext location string fed to the keyless embed URL as the q= parameter.
     *  Nullable: if absent the ContactMap renders nothing (map is decorative). */
    @Column(name = "map_query")
    private String mapQuery;

    /** Ordering for the Contact page; Dubai (0) before India (1) by default. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Default constructor required by JPA. Public for test setup in other packages. */
    public OfficeEntity() {
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public String getOfficeKey() {
        return officeKey;
    }

    public String getName() {
        return name;
    }

    public List<String> getAddressLines() {
        return addressLines;
    }

    public List<String> getPhones() {
        return phones;
    }

    public String getEmail() {
        return email;
    }

    public String getMapQuery() {
        return mapQuery;
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

    public void setOfficeKey(String officeKey) {
        this.officeKey = officeKey;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddressLines(List<String> addressLines) {
        this.addressLines = addressLines;
    }

    public void setPhones(List<String> phones) {
        this.phones = phones;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setMapQuery(String mapQuery) {
        this.mapQuery = mapQuery;
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
