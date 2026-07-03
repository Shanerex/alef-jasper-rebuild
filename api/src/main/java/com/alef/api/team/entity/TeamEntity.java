package com.alef.api.team.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for the team table (architecture §4.1, feature 011).
 *
 * Maps the leadership profile schema designed for admin CRUD from day one
 * (DEC-020). The active flag and display_order enable feature 012 to
 * soft-hide/show and reorder profiles via row updates, with no schema
 * migration required. Only active rows are returned by the public read endpoint.
 *
 * email, photo, and company are nullable — cards degrade gracefully when a
 * field is absent, matching the 003 nullable-credit-field stance.
 */
@Entity
@Table(name = "team")
public class TeamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Full display name, e.g. "K. Jeyaraman". Required. */
    @Column(nullable = false)
    private String name;

    /** Display title, e.g. "Managing Director". Required; free TEXT per DEC-009. */
    @Column(nullable = false)
    private String role;

    /** Affiliation label: 'ALEF', 'JASPER', or 'ALEF & JASPER'. Nullable for profiles
     *  that may not map cleanly to one arm. */
    @Column
    private String company;

    /** Primary public contact address. Nullable; one address per card. */
    @Column
    private String email;

    /** Public-relative image path, e.g. "/img/team/k-jeyaraman.jpg". Null until
     *  photo assets exist; TeamCard renders a serif monogram placeholder when null. */
    @Column
    private String photo;

    /** Ordering on the About page. Feature 012 updates this column to resequence
     *  without any code change (same device as trust_content.display_order). */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** Soft show/hide flag. Feature 011 reads only active=true rows; feature 012
     *  flips this to retire or stage a profile with no delete and no migration. */
    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Default constructor required by JPA. Public for test setup in other packages. */
    public TeamEntity() {
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getCompany() {
        return company;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoto() {
        return photo;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // --- Setters (needed for test data setup; write path is feature 012) ---

    public void setName(String name) {
        this.name = name;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
