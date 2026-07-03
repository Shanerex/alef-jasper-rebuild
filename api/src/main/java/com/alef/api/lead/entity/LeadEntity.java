package com.alef.api.lead.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for the lead table — minimal contact-form subset (architecture §4.4, feature 011).
 *
 * DEC-017: Only the columns the contact form needs are mapped here. Feature 001
 * will extend this table additively with nullable columns (project_name, sector,
 * scope[], etc.) so it never restructures what 011 creates.
 *
 * The source column is the discriminator that makes this the "same path as other
 * lead sources" per F11-AC6: 011 writes 'contact_form'; 001 writes 'concierge'.
 * It is set server-side and never trusted from the client.
 *
 * No updatedAt: a contact-form lead is write-once; CRM/012 concerns may add it later.
 */
@Entity
@Table(name = "lead")
public class LeadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Server-set timestamp; never trusted from the client. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Lead origin: 'contact_form' for this feature; 'concierge' etc. for future features.
     *  Set server-side by LeadService using LeadSource constants. */
    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String name;

    /** Required for the contact form; future RFQ paths may relax this constraint via migration. */
    @Column(nullable = false)
    private String email;

    /** Optional telephone number from the contact form. */
    @Column
    private String phone;

    /** Optional company or organisation name. */
    @Column
    private String company;

    /** Free-text enquiry body from the contact form.
     *  Proposed addition to ARCHITECTURE.md lead entity (architecture §11). */
    @Column
    private String message;

    /** Default constructor required by JPA. */
    public LeadEntity() {
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getSource() {
        return source;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getCompany() {
        return company;
    }

    public String getMessage() {
        return message;
    }

    // --- Setters ---

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
