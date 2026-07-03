-- V6: Create the team table (architecture §4.1, feature 011).
--
-- Holds leadership profiles for the About page. Designed for admin CRUD from
-- day one (feature 012) so new profiles are addable via a single INSERT with
-- no code change or migration. The active flag and display_order support
-- soft hide/show and reordering without schema changes (DEC-020).
--
-- UNIQUE(name, role) provides a natural conflict key for idempotent seeding
-- (V7 ON CONFLICT DO NOTHING) since there is no slug/item_key business key.

CREATE TABLE team (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          TEXT        NOT NULL,
    role          TEXT        NOT NULL,                 -- display title, e.g. 'Managing Director'
    company       TEXT,                                 -- 'ALEF', 'JASPER', 'ALEF & JASPER'; nullable
    email         TEXT,                                 -- primary public address; nullable
    photo         TEXT,                                 -- public-relative path; nullable until assets exist
    display_order INTEGER     NOT NULL DEFAULT 0,       -- ordering on About page; 012 can resequence
    active        BOOLEAN     NOT NULL DEFAULT TRUE,    -- soft show/hide; 011 reads active rows only
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (name, role)                                 -- natural key for idempotent seed
);
