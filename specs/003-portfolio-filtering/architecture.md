---
feature: 003-portfolio-filtering
spec_id: "003"
phase: architecture
owner: Architect
status: approved
version: "1.0"
entry_criteria:
  - upstream phase approved
exit_criteria:
  - Architect has produced this file and a human has approved it
---

# Architecture: Portfolio with Filtering

## 1. Overview

This feature adds the public, read-only project portfolio to the system: a filterable list at `/projects` and an independently indexable detail page at `/projects/{slug}`. It is the first feature to introduce a real Postgres-backed read path through the `api` service and the first concrete consumer of the `project` entity already sketched in `ARCHITECTURE.md`. It fits the existing topology unchanged: `web` (Next.js) renders pages and calls `api` (Spring Boot) over HTTP; `api` reads the `project` table from `postgres`. No vector store, Redis, or LLM path is involved in this feature — the portfolio is a deterministic relational read. The schema and API are designed so that later features (005 trust aggregates, 011 home marquee, 012 admin write path, 001 RAG re-sync) attach without restructuring.

## 2. Data Model

### 2.1 The `project` table

The entity carries the fields named in `ARCHITECTURE.md` (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location) plus the fields the requirements demand: `scope` (F3-AC3, multi-valued), `featurable` (F3-AC4), a surrogate `id`, and audit timestamps.

Pseudo-DDL (Postgres):

```sql
CREATE TABLE project (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug            TEXT        NOT NULL UNIQUE,          -- clean URL key, F3-AC2
    name            TEXT        NOT NULL,
    sector          TEXT        NOT NULL,                 -- constrained set, see 2.2
    country         TEXT        NOT NULL,                 -- display name (e.g. "UAE", "Qatar")
    status          TEXT        NOT NULL,                 -- 'ongoing' | 'completed'
    image           TEXT,                                 -- public-relative path / URL, nullable
    description     TEXT,
    main_contractor TEXT,
    client          TEXT,
    consultant      TEXT,
    location        TEXT,
    scope           TEXT[]      NOT NULL DEFAULT '{}',    -- subset of rebar/BBS/GA/MEP/QS/as-built (F1-AC2 list)
    featurable      BOOLEAN     NOT NULL DEFAULT FALSE,   -- marquee on home page, F3-AC4
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_project_sector     ON project (sector);
CREATE INDEX idx_project_country    ON project (country);
CREATE INDEX idx_project_status     ON project (status);
CREATE INDEX idx_project_featurable ON project (featurable) WHERE featurable = TRUE;
```

### 2.2 Column decisions

- **`id` is `BIGINT IDENTITY`, not UUID.** This is an internal, server-generated key for a small, single-instance dataset. There is no client-supplied-id or distributed-merge requirement that would justify UUID overhead. The public-facing key is `slug`, never `id` — `id` does not appear in any URL or API path (LEARNING-001: clean slugs, no opaque ids in URLs).
- **`slug` is `NOT NULL UNIQUE`** and is the routing key for F3-AC2. Slug generation/validation is an admin (012) concern; the public read path treats it as a given, immutable key.
- **`sector` and `status` are stored as `TEXT`, with the allowed set enforced in the application layer, not via a Postgres `ENUM` or `CHECK`.** Rationale in the DEC list below. Allowed sectors (F3-AC1): `airport`, `mall_retail`, `hotel_hospitality`, `residential`, `infrastructure_rail`, `leisure_museum`. Allowed status: `ongoing`, `completed`.
- **`scope` is `TEXT[]`** because a project can carry several scopes from the F1-AC2 list (`rebar`, `BBS`, `GA`, `MEP`, `QS`, `as-built`). Postgres arrays are sufficient at ~38 rows; a join table would be over-engineering for a value list this small and never independently queried. If scope-based filtering is ever required at scale, a GIN index on `scope` is the migration path (not added now — no requirement filters on scope).
- **Indexing.** B-tree indexes on the three filter columns (`sector`, `country`, `status`) satisfy F3-AC1. A partial index on `featurable = TRUE` keeps the marquee lookup (F3-AC4 / 005 / 011) cheap. At 38 rows these indexes barely affect performance but are correct, cheap, and future-proof as the catalogue grows.
- **Nullability.** Only `slug`, `name`, `sector`, `country`, `status` are required. The narrative/credit fields (`image`, `description`, `main_contractor`, `client`, `consultant`, `location`) are nullable because legacy migrated data may be incomplete and the public detail page must degrade gracefully (render only the fields present).

## 3. API Contract

All endpoints are public, read-only, served by `api` under `/api`. Responses are JSON.

### 3.1 `GET /api/projects` — filterable list

Optional query parameters (all combinable, AND semantics):

| Param    | Allowed values                                                                                        | Notes |
|----------|-------------------------------------------------------------------------------------------------------|-------|
| `sector` | `airport`, `mall_retail`, `hotel_hospitality`, `residential`, `infrastructure_rail`, `leisure_museum` | unknown value -> 400 |
| `country`| any value present in the data (exact match)                                                           | unknown value -> empty result, not error |
| `status` | `ongoing`, `completed`                                                                                | unknown value -> 400 |
| `featurable` | `true` / `false`                                                                                  | optional; supports 011/005 marquee fetch without a separate endpoint |
| `page`   | integer, 0-based, default `0`                                                                         | see pagination stance |
| `size`   | integer, default `50` (>= full catalogue today), capped (e.g. 100)                                    | |

Response shape (paginated envelope, even though one page holds everything today):

```json
{
  "content": [
    {
      "slug": "doha-metro-gold-line",
      "name": "Doha Metro - Gold Line",
      "sector": "infrastructure_rail",
      "country": "Qatar",
      "status": "completed",
      "image": "/img/projects/doha-metro-gold-line.jpg",
      "location": "Doha, Qatar",
      "featurable": true
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 38,
  "totalPages": 1
}
```

The **list item is a summary projection** (card fields only: slug, name, sector, country, status, image, location, featurable). It deliberately omits `description`, the credit fields, and `scope` — those belong to the detail view and keep the list payload small. The summary fields are exactly what a project card and the marquee strip need.

**Pagination stance:** a paginated envelope is returned from day one even though `size=50` fits the whole 38-row catalogue on one page. Designing the envelope now means the contract does not change when the catalogue outgrows a single payload; the frontend simply starts requesting pages. This costs nothing today and avoids a breaking change later.

### 3.2 `GET /api/projects/{slug}` — detail

Path key is `slug`. Returns the full record (F3-AC3 fields plus scope and status):

```json
{
  "slug": "doha-metro-gold-line",
  "name": "Doha Metro - Gold Line",
  "sector": "infrastructure_rail",
  "country": "Qatar",
  "status": "completed",
  "image": "/img/projects/doha-metro-gold-line.jpg",
  "description": "Rebar detailing and BBS for ...",
  "mainContractor": "...",
  "client": "...",
  "consultant": "...",
  "location": "Doha, Qatar",
  "scope": ["rebar", "BBS", "GA"],
  "featurable": true
}
```

Unknown slug -> `404`. The response intentionally omits `id`, `created_at`, `updated_at` (internal concerns, not part of the public contract).

### 3.3 Filter-options discoverability

**Sector and status enum values are owned by the backend and exposed, not hardcoded in the frontend.** `sector` and `status` are a fixed, requirement-defined vocabulary; `country` is data-derived. Expose one lightweight endpoint:

`GET /api/projects/filters` ->
```json
{
  "sectors": ["airport","mall_retail","hotel_hospitality","residential","infrastructure_rail","leisure_museum"],
  "statuses": ["ongoing","completed"],
  "countries": ["UAE","Qatar","Saudi Arabia","Oman","Bahrain"]
}
```

Rationale: `country` is genuinely dynamic (it depends on which projects exist), so the frontend cannot hardcode it without drifting from the data. Serving all three filter dimensions from one endpoint keeps the filter UI in sync with reality and gives 012 a place to surface new sectors later without a web redeploy. `sectors`/`statuses` are returned as a stable list (human-readable display labels are a `web` concern); `countries` is computed as `SELECT DISTINCT country FROM project ORDER BY country`.

## 4. Web Integration

### 4.1 `/projects` — filterable list page

- **Rendering: Incremental Static Regeneration (ISR).** The page is fetched server-side at build/revalidate time from `GET /api/projects` and `GET /api/projects/filters`, rendered to static HTML, and revalidated on an interval (e.g. 60s) so admin edits (012) appear without a redeploy. This satisfies the SEO requirement (fully rendered HTML for crawlers) while staying fresh. Pure SSG would go stale after an admin edit; pure per-request SSR is unnecessary work for a 38-row, rarely-changing dataset.
- **Filtering strategy: load-all + client-side filtering, with URL query-param state.** With ~38 projects the full summary list is a few KB; the page loads the whole set once and filters in the browser for instant, no-round-trip interaction. The active filter set is reflected in the URL (e.g. `/projects?sector=airport&status=completed`) so filtered views are shareable and bookmarkable. This is a deliberate client-side choice for UX; it does **not** make the API filters redundant — `GET /api/projects` still supports server-side `sector`/`country`/`status` filtering so the architecture scales when the catalogue outgrows a single payload (at which point the page switches to per-filter server fetches with no API change).

### 4.2 `/projects/{slug}` — detail page

- **Rendering: SSG with ISR fallback** (`generateStaticParams` over known slugs, revalidate on interval, blocking fallback for slugs added after build). Each project is a clean, crawlable URL (F3-AC2), fully server-rendered.
- **SEO/meta:** per-project `<title>`, meta description (from `description`), and Open Graph image (from `image`) generated per slug. This is the direct fix for LEARNING-001 (base64 query-string ids -> indexable slugs).
- **Graceful degradation:** nullable credit fields render only when present.

## 5. Data Seeding

**Choice: a versioned Flyway migration for the schema DDL, plus a separate idempotent SQL seed for the ~38 hand-curated projects.**

- The schema lives in a versioned migration (e.g. `V<n>__create_project.sql`).
- The seed lives in a clearly separated seed migration carrying the demo dataset as `INSERT`s, so the demo data is reproducible on a fresh `docker compose up` against an empty volume.

Rationale:
- Flyway is the idiomatic Spring Boot migration tool, gives ordered, checksummed, reproducible schema evolution, and 012 will need this same migration discipline to evolve the table. Choosing it now avoids a later retrofit.
- A SQL seed (vs. a JSON fixture loaded on startup) keeps the seed in the same migration history as the schema, is transactional, and needs no bespoke startup loader. The data is hand-curated from the existing site and small enough that hand-written `INSERT`s are maintainable.
- Idempotency: the seed uses `INSERT ... ON CONFLICT (slug) DO NOTHING` so it never collides with admin-created rows in a long-lived local volume.

The actual 38-row content (curated project facts, scope clearance per DEC-008) is a content-migration task for the implementation phase, not designed here.

## 6. Downstream Dependencies

- **005 Trust Layer.** F5-AC1 needs marquee project count; F5-AC2 needs distinct client/contractor names. Supported directly: `SELECT count(*) FROM project WHERE featurable = TRUE` and `SELECT DISTINCT client / main_contractor FROM project`. No schema change required. 005 may add a dedicated aggregate endpoint (e.g. `GET /api/trust/stats`) that reads this table; that is 005's design, not this one's. Note: the non-project trust stats — years in business, staff count, steel capacity, badges — are not part of this entity and are 005/012's storage decision, flagged in 012's open questions.
- **011 Home / Marquee.** Featured projects for the home page are served by `GET /api/projects?featurable=true` (the summary projection already carries `featurable` and the card fields). No dedicated endpoint is required, though 011 may add one.
- **012 Admin Content Management.** This design is read-only and does **not** preclude the write path. The table, slug-keyed access, Flyway discipline, and application-layer enum validation are all reused by 012's CRUD endpoints (F12-AC4..F12-AC7). The `updated_at` column exists for the admin edit path. The summary/detail projection split means admin writes target the full row while the public read stays lean.
- **001 AI Concierge / RAG.** Project data feeds the RAG knowledge base. Per DEC-004, content changes emit `kb.changed` for re-embedding. This feature does **not** emit that event (the public read path never mutates data); emission is wired by 012 on create/edit/delete (F12-AC25). The design accommodates it: the `project` table is the single source of truth that the worker re-embeds from, and `updated_at` gives the worker a change signal if it ever reconciles rather than reacts.

## 7. Risks and Mitigations

- **Enum drift between frontend and backend.** Mitigated by serving the vocabulary from `GET /api/projects/filters` (3.3) rather than hardcoding it in `web`.
- **Stale public page after an admin edit.** Mitigated by ISR revalidation (4.1/4.2) rather than pure SSG; admin edits surface within the revalidate window without a redeploy.
- **Client-side filtering does not scale past a single payload.** Accepted for v1 (38 rows). Mitigated structurally: the API already supports server-side filtering and pagination, so the growth path is a frontend change only, no contract change.
- **Incomplete legacy data.** Mitigated by nullable narrative/credit columns and graceful field-level rendering.
- **Marquee/name clearance for public deploy.** DEC-008 cleared names for the local demo only. Risk is deferred to the pre-deploy gate noted in DEC-008; not a blocker here.
- **Array vs. join table for `scope`.** Low risk at this scale; documented migration path (GIN index / join table) if scope ever becomes a filter dimension.

## 8. ARCHITECTURE.md Changes (proposed, not applied)

The `project` entity in `ARCHITECTURE.md` currently reads:

```
project  { slug, name, sector, country, status, image, description,
           main_contractor, client, consultant, location }
```

Proposed update (to be reviewed and applied by a human — I do not edit `ARCHITECTURE.md`):

```
project  { id, slug, name, sector, country, status, image, description,
           main_contractor, client, consultant, location,
           scope[], featurable, created_at, updated_at }
```

Rationale for each addition:
- `id` — surrogate primary key (internal; slug remains the public key).
- `scope[]` — required by F3-AC3 (detail shows scope); multi-valued per the F1-AC2 scope vocabulary.
- `featurable` — required by F3-AC4 (marquee on home), consumed by 005 and 011.
- `created_at` / `updated_at` — audit timestamps; `updated_at` underpins the 012 edit path and any RAG reconciliation.

No constraint in `ARCHITECTURE.md` is changed — this is a data-model field addition only.

---

## Proposed Decision Records (for the keeper to append to DECISIONS.md)

These DECs arise from this feature. They are listed here for the handoff; per the append-only rule I do not write to DECISIONS.md directly.

- **Sector/status validated in the application layer, not as a Postgres ENUM/CHECK.** Why: the vocabulary is requirement-owned and expected to grow via admin (012); a Postgres `ENUM` requires a migration to extend and a `CHECK` couples the DB to a list the app already validates and serves via `/api/projects/filters`. Keeping the set in one place (app + the filters endpoint) avoids two sources of truth. Alternatives rejected: Postgres `ENUM` (rigid, migration-per-value); `CHECK` constraint (duplicate of app validation).
- **`scope` as `TEXT[]`, not a join table.** Why: small, fixed value list, never independently queried/filtered today. Alternative rejected: a `project_scope` join table (over-engineered at this scale; documented as the migration path if scope filtering ever arrives).
- **Public list is a summary projection; full record only on detail.** Why: keeps the list/marquee payload small and lets client-side filtering load the whole catalogue cheaply. Alternative rejected: returning full records in the list (larger payload, leaks detail-only fields).
- **Flyway migration plus an idempotent SQL seed for the 38 projects.** Why: idiomatic Spring discipline, reproducible on a fresh volume, reused by 012; transactional and in the same history as the schema. Alternative rejected: JSON fixture loaded at startup (bespoke loader, outside migration history).
