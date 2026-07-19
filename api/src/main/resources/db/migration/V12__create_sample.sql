-- V12: Create the sample table (architecture §3.4, feature 012, DEC-023).
--
-- Materializes the sample table ahead of feature 004's public Sample Explorer
-- so feature 012's Samples CRUD (F12-AC16..AC19) has a write target. 004's
-- future public gated read attaches later with zero schema churn.
--
-- preview and file are nullable so a sample can be created and have its
-- assets attached in a follow-up edit (matches the team.photo-null pattern).
-- category is TEXT, validated in the app layer (DEC-009) against the five
-- 004 sample types.

CREATE TABLE sample (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug          TEXT        NOT NULL UNIQUE,
    title         TEXT        NOT NULL,
    category      TEXT        NOT NULL,
    preview       TEXT,
    file          TEXT,
    display_order INTEGER     NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
