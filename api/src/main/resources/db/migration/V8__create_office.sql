-- V8: Create the office table (architecture §4.3, feature 011).
--
-- Holds structured office location data for the Contact page. Promoted from
-- a frontend constant to a table because 012 will edit addresses/phones, and
-- ARCHITECTURE.md names api as the content-read service (DEC-019).
--
-- address_lines and phones are TEXT[] because Dubai has two phone numbers and
-- both offices have multi-line addresses -- the same TEXT[] calculus as
-- project.scope (DEC-010). map_query is a plaintext location string for the
-- keyless Google Maps iframe (architecture §10 Decision 3), not coordinates.

CREATE TABLE office (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    office_key    TEXT        NOT NULL UNIQUE,          -- stable machine key: 'dubai' | 'india'
    name          TEXT        NOT NULL,                 -- display name
    address_lines TEXT[]      NOT NULL DEFAULT '{}',    -- ordered presentation lines for rendering
    phones        TEXT[]      NOT NULL DEFAULT '{}',    -- one or more phone numbers
    email         TEXT,                                 -- single contact address; nullable
    map_query     TEXT,                                 -- plaintext query for keyless embed; nullable
    display_order INTEGER     NOT NULL DEFAULT 0,       -- Dubai (0) before India (1) by default
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
