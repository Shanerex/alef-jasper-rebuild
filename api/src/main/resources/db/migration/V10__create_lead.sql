-- V10: Create the lead table — minimal contact-form subset (architecture §4.4, feature 011).
--
-- DEC-017: Only the columns the contact form needs are created now. Feature 001
-- (AI concierge) will extend this table additively with nullable columns
-- (project_name, sector, scope[], tonnage, attachment_ref, conversation_ref, etc.)
-- when it lands. This avoids speculative columns with no consumer.
--
-- The 'source' discriminator is set server-side to 'contact_form' by LeadService.
-- Feature 001 will write the same table with source='concierge', satisfying
-- F11-AC6's "same path as other lead sources" requirement.
--
-- No seed: leads are runtime data produced by real form submissions.

CREATE TABLE lead (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    source      TEXT        NOT NULL,                  -- 'contact_form' (011); 'concierge' etc. (001+)
    name        TEXT        NOT NULL,
    email       TEXT        NOT NULL,
    phone       TEXT,                                  -- optional; contact form field
    company     TEXT,                                  -- optional; contact form field
    message     TEXT                                   -- free-text enquiry; proposed ARCHITECTURE.md addition
);
