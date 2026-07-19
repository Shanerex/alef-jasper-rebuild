-- V14: Create the admin_credential singleton table (architecture §3.6, feature 012,
-- DEC-021 amended).
--
-- Holds the one persisted, runtime-mutable admin credential (F12-AC28, F12-AC29).
-- DDL only -- the row is seeded at application startup from ADMIN_USERNAME /
-- ADMIN_PASSWORD_HASH (only if the table is empty), never in this migration,
-- because the hash value comes from env at runtime and Flyway must stay
-- deterministic and secret-free (architecture §2.1.1).
--
-- V13 is intentionally not used here: it is reserved (per architecture §3.4) for
-- an optional sample seed that was not needed for this build (see implementation.md).
--
-- The PRIMARY KEY(id) + CHECK(id = 1) pair makes it structurally impossible to
-- hold more than one credential row, documenting the single-admin scope
-- (F12-AC3) at the DB level. v2 (multiple admins) would relax this additively.

CREATE TABLE admin_credential (
    id            SMALLINT    PRIMARY KEY DEFAULT 1,
    username      TEXT        NOT NULL,
    password_hash TEXT        NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT admin_credential_singleton CHECK (id = 1)
);
