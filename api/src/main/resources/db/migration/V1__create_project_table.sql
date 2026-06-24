-- V1: Create the project table (architecture 2.1).
-- This is the first migration in the ALEF API module.

CREATE TABLE project (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug            TEXT        NOT NULL UNIQUE,
    name            TEXT        NOT NULL,
    sector          TEXT        NOT NULL,
    country         TEXT        NOT NULL,
    status          TEXT        NOT NULL,
    image           TEXT,
    description     TEXT,
    main_contractor TEXT,
    client          TEXT,
    consultant      TEXT,
    location        TEXT,
    scope           TEXT[]      NOT NULL DEFAULT '{}',
    featurable      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_project_sector     ON project (sector);
CREATE INDEX idx_project_country    ON project (country);
CREATE INDEX idx_project_status     ON project (status);
CREATE INDEX idx_project_featurable ON project (featurable) WHERE featurable = TRUE;
