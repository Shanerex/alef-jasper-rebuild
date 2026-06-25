-- V4: Create the trust_content key/value table (architecture 2.1, feature 005).
-- Holds standalone editorial facts (stats, software, standards) that are not
-- derivable from any existing table. Project-derived trust facts (marquee count,
-- client/contractor names) are computed live from the project table and never
-- stored here.

CREATE TABLE trust_content (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_key      TEXT        NOT NULL UNIQUE,
    item_type     TEXT        NOT NULL,
    label         TEXT        NOT NULL,
    value         TEXT,
    unit          TEXT,
    display_order INTEGER     NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_trust_content_type ON trust_content (item_type);
