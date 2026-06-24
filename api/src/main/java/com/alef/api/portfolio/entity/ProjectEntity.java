package com.alef.api.portfolio.entity;

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
 * JPA entity for the project table (architecture 2.1).
 *
 * Maps the Postgres DDL exactly. The sector and status fields are stored as TEXT,
 * not JPA enums, per DEC-009 -- this lets values added by feature 012 read back
 * without throwing even before a redeploy adds them to the vocabulary enum.
 *
 * The id, createdAt, and updatedAt fields are internal and never serialized
 * to a public DTO (DEC-011 summary/detail projection).
 */
@Entity
@Table(name = "project")
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    /** Stored as TEXT per DEC-009; validated by the Sector vocabulary in the app layer. */
    @Column(nullable = false)
    private String sector;

    @Column(nullable = false)
    private String country;

    /** Stored as TEXT per DEC-009; validated by the ProjectStatus vocabulary in the app layer. */
    @Column(nullable = false)
    private String status;

    private String image;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "main_contractor")
    private String mainContractor;

    private String client;

    private String consultant;

    private String location;

    /**
     * Multi-valued scope list stored as Postgres TEXT[] (DEC-010).
     * Uses Hibernate 6 native array support via JdbcTypeCode.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> scope;

    /** Marquee flag for F3-AC4: featurable projects appear on the home page. */
    @Column(nullable = false)
    private boolean featurable;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Default constructor required by JPA. Public for test setup in other packages. */
    public ProjectEntity() {
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getSector() {
        return sector;
    }

    public String getCountry() {
        return country;
    }

    public String getStatus() {
        return status;
    }

    public String getImage() {
        return image;
    }

    public String getDescription() {
        return description;
    }

    public String getMainContractor() {
        return mainContractor;
    }

    public String getClient() {
        return client;
    }

    public String getConsultant() {
        return consultant;
    }

    public String getLocation() {
        return location;
    }

    public List<String> getScope() {
        return scope;
    }

    public boolean isFeaturable() {
        return featurable;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // --- Setters (needed for test data setup; write path is feature 012) ---

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMainContractor(String mainContractor) {
        this.mainContractor = mainContractor;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public void setConsultant(String consultant) {
        this.consultant = consultant;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public void setFeaturable(boolean featurable) {
        this.featurable = featurable;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
