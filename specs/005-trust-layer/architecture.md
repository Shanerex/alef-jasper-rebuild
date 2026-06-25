---
feature: 005-trust-layer
spec_id: "005"
phase: architecture
owner: Architect
status: approved
version: "0.1"
entry_criteria:
  - upstream phase approved
exit_criteria:
  - Architect has produced this file and a human has approved it
---

# Architecture: Trust Layer

## 1. Overview

The Trust Layer surfaces credibility signals — headline stats (years in business, staff count, monthly steel capacity, marquee project count), a client/contractor name strip, and software/standards capability badges — at the points where buyers decide (primarily Home, F011). It is a read-only, content-driven feature: there is no user interaction beyond viewing. It fits the existing topology unchanged — `web` (Next.js) renders presentational sections and calls `api` (Spring Boot) over HTTP; `api` reads a tiny new `trust_content` key/value table from `postgres` and derives the project-based aggregates from the existing `project` table (F003). No vector store, Redis, or LLM path is involved.

## 2. Data Model

The trust content splits cleanly into two kinds, and the architecture treats them differently:

- **Project-derived facts** — marquee project count, distinct client/contractor names. These already live in the `project` table (F003) and must not be duplicated; duplicating them would let the trust copy drift from the portfolio. They are computed on read.
- **Standalone editorial facts** — years in business, staff count, monthly steel capacity, software list, standards list. These are not derivable from any existing table. They change rarely (roughly yearly) and are short.

### 2.1 The `trust_content` table

A single, narrow key/value table holds the standalone editorial facts. This is deliberately not a richly-typed, one-column-per-stat schema: a key/value shape lets F012 (Admin Content Management) add, edit, or retire a trust item later without a migration per field, and keeps this feature to one small table.

Pseudo-DDL (Postgres):

```sql
CREATE TABLE trust_content (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_key    TEXT        NOT NULL UNIQUE,   -- stable machine key, e.g. 'years_in_business'
    item_type   TEXT        NOT NULL,          -- 'stat' | 'software' | 'standard'  (app-validated)
    label       TEXT        NOT NULL,          -- display label, e.g. 'Years in Business'
    value       TEXT,                          -- display value for stats, e.g. '18'; NULL for badge rows
    unit        TEXT,                          -- optional suffix/unit, e.g. 'tonnes / month'; nullable
    display_order INTEGER   NOT NULL DEFAULT 0,-- ordering within an item_type group
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_trust_content_type ON trust_content (item_type);
```

### 2.2 Column decisions

- **`item_key` is the stable, machine-readable identity** (`years_in_business`, `staff_count`, `monthly_steel_capacity_tonnes`, plus one row per software/standard). The frontend never hardcodes display strings for the numeric stats — it consumes `label` + `value` + `unit`. `item_key` lets F012 and the API address a specific item without coupling to display text.
- **`item_type`** partitions the three concerns the requirements call out: `stat` (F5-AC1 headline numbers), `software` and `standard` (F5-AC3 capability badges). The API groups by this column. Validated in the application layer, consistent with DEC-009 (vocabulary owned by the app, not a Postgres ENUM/CHECK).
- **`value` is nullable** because badge rows (software/standards) have only a `label`; only `stat` rows carry a numeric `value`.
- **`value` is `TEXT`, not numeric.** The stats are display strings ("18", "120+", "5,000"). Storing them as text avoids forcing format decisions (the "+", thousands separators, ranges) into the schema and keeps F012's edit form a plain text field. These are marketing facts shown verbatim, never summed or compared.
- **`display_order`** lets F012 reorder badges/stats without a schema change; the API returns each group already sorted.
- **`created_at`/`updated_at`** mirror the `project` table for the F012 edit path and consistency.

### 2.3 Why a table and not static JSON in `web`

The brief flags this as a real choice. A full DB table with admin CRUD would be overkill *if* this were the end state — but F012 (Admin Content Management, requirements approved) will need to edit exactly this content, and `ARCHITECTURE.md` names `api` as the content-read service with `web` as a pure presentation layer. Putting trust copy in a `web/` JSON file would force a code change + redeploy to fix a stat and would split content ownership across two services. A single narrow table keeps `api` the single source of truth, gives F012 a write target with zero schema churn, and costs almost nothing (one small migration, one read query). Alternatives are recorded in the Decisions section.

## 3. API Contract

One public, read-only endpoint composes both kinds of data into a single payload, so the Home page makes one call to render the whole Trust Layer.

### 3.1 `GET /api/trust/overview` — composed trust payload

Intent: return everything the Trust Layer renders — headline stats, the client/contractor name strip, and software/standards badges — in one read, mixing stored editorial content with live project-derived aggregates.

No parameters. Response shape:

```json
{
  "stats": [
    { "key": "years_in_business",          "label": "Years in Business",      "value": "18",   "unit": null },
    { "key": "staff_count",                "label": "Detailing Engineers",    "value": "120",  "unit": null },
    { "key": "monthly_steel_capacity_tonnes","label": "Monthly Steel Capacity","value": "5,000","unit": "tonnes / month" },
    { "key": "marquee_projects",           "label": "Marquee Projects",       "value": "12",   "unit": null }
  ],
  "clients": ["Six Construct", "ALEC", "Habtoor", "..."],
  "contractors": ["..."],
  "software": ["AutoCAD", "CADS RC", "SteelPac RC"],
  "standards": ["BS 8666", "ACI 318", "BS EN ISO 3766"]
}
```

Composition rules (server-side, in the service):
- `stats` is the `trust_content` rows of `item_type = 'stat'` (ordered by `display_order`), **plus** one synthesized `marquee_projects` stat whose `value` is `SELECT count(*) FROM project WHERE featurable = TRUE` (F003 marquee count). The marquee count is derived live, never stored, so it can never disagree with the portfolio.
- `clients` is `SELECT DISTINCT client FROM project WHERE client IS NOT NULL ORDER BY client`; `contractors` is the same over `main_contractor`. These satisfy F5-AC2 from existing data — no new storage.
- `software` and `standards` are the `label` values of `trust_content` rows of the matching `item_type`, ordered by `display_order` (F5-AC3).

Rationale for one composed endpoint rather than several:
- The Trust Layer is one logical block on the page; one fetch keeps the `web` integration and ISR revalidation simple.
- It does not preclude F011/F012 reuse — F011 (Home) consumes this directly; F012 edits the underlying `trust_content` rows and the change surfaces here on next read.
- The synthesized `marquee_projects` stat means the brief's F5-AC1 "marquee project count" requirement is satisfied without the content editor ever having to keep a number in sync by hand.

The endpoint returns `200` with empty arrays for any group that has no rows (graceful degradation; the page renders only the sections that have content).

## 4. Web Integration

- **One presentational block, three sub-components, no client state.** `web` adds a `TrustLayer` section composed of:
  - `TrustStats` — renders the `stats` array as the gold-inverted Stat Display cards (DESIGN_SYSTEM "Stat Display": gold bg, Cormorant number, Montserrat uppercase label). Label/value/unit come straight from the API.
  - `ClientStrip` — renders `clients` + `contractors` as a named logo/text strip (F5-AC2). v1 renders names as styled text per the design system; an optional logo asset path is a Design concern, not an architecture one (see open questions).
  - `CapabilityBadges` — renders `software` + `standards` as capability badges (F5-AC3), styled per the Service Card / gold-muted tag pattern.
- **Data fetch + rendering: ISR**, consistent with F003 (4.1). The block is server-rendered from `GET /api/trust/overview` at build/revalidate time so it is fully present for SEO crawlers and stays fresh after an F012 edit within the revalidate window. No per-request SSR (content is near-static) and no pure SSG (would go stale after an admin edit).
- **Typed client + types** mirror the F003 convention: a `web/src/lib/api/trust.ts` client and a `web/src/lib/types/trust.ts` type mirroring the DTO. (Proposed; Dev implements — Architect does not write code.)
- **Placement** (which page hosts the block) is owned by F011 (Home). This feature delivers the self-contained block and its data; F011 mounts it.

## 5. Component Boundaries

| Concern | Lives in | Notes |
|---|---|---|
| `trust_content` schema + seed | `api` (Flyway V4, V5) | editorial facts; source of truth |
| Project-derived aggregates (marquee count, client/contractor names) | `api` service, read from `project` table | never duplicated into trust storage |
| Composition into one payload | `api` `trust` service | mixes stored + derived |
| `GET /api/trust/overview` | `api` `trust` controller | public, read-only |
| `TrustLayer` / `TrustStats` / `ClientStrip` / `CapabilityBadges` | `web` components | pure presentation, no state |
| Page placement | `web` Home (F011) | out of scope here |

Backend package follows the established convention (`com.alef.api.portfolio.*` → new `com.alef.api.trust.*`): controller / service / dto, with `item_type` vocabulary validated in a small enum, matching the portfolio module's shape.

## 6. Data Seeding

Two new Flyway migrations, consistent with DEC-012 (schema migration + idempotent seed). **Next available version is V4** (existing: V1 create project, V2 seed projects, V3 image fix):

- **`V4__create_trust_content.sql`** — the DDL in 2.1.
- **`V5__seed_trust_content.sql`** — the editorial facts: the three `stat` rows, the `software` rows (AutoCAD, CADS RC, SteelPac RC), and the `standard` rows (the international detailing standards named in F5-AC3). Idempotent via `INSERT ... ON CONFLICT (item_key) DO NOTHING`, so it never collides with F012-created rows on a long-lived local volume.

The exact seed values (the real numbers for years/staff/capacity and the precise standards list) are a content task for the implementation phase, not designed here. They are demo content, cleared by DEC-008 for local use.

## 7. Downstream Dependencies

- **F011 Home / Marquee.** Consumes `GET /api/trust/overview` and mounts `TrustLayer`. The marquee project count it shows is the synthesized `marquee_projects` stat. No new endpoint needed for F011's trust content.
- **F012 Admin Content Management.** Edits `trust_content` rows (CRUD on a stable, key/value table designed for it). `updated_at` exists for the edit path; `display_order` exists for reordering; `item_key` is the stable address. No schema change is required when F012 lands. The marquee count, client, and contractor lists are *not* editable here because they are derived from `project` — F012 edits them through the portfolio, which is correct (single source of truth).
- **F003 Portfolio.** This feature is a read-only consumer of the `project` table; it does not alter F003's schema or contract.
- **F001 RAG.** Trust content is small marketing copy; whether it feeds the knowledge base is an F001/F012 decision (the `kb.changed` event on edit). This feature does not emit events (no mutation on the read path), consistent with F003.

## 8. Risks and Mitigations

- **Trust copy drifting from the portfolio.** Mitigated structurally: marquee count and client/contractor names are derived live from `project`, never stored in `trust_content`.
- **Stale block after an F012 edit.** Mitigated by ISR revalidation (consistent with F003), not pure SSG.
- **Name/logo clearance for public deploy.** F5-AC2 surfaces real client/contractor names. DEC-008 cleared these for the local demo only; the pre-public-deploy gate in DEC-008 covers this — not a blocker here, but explicitly inherited.
- **Over-engineering for a near-static feature.** Mitigated by keeping it to one narrow key/value table and one composed endpoint; no admin CRUD is built here (that is F012). Justified by F012's known need and the `api`-as-content-source constraint.
- **`item_type` vocabulary drift.** Mitigated by app-layer validation and the fact that the only consumer is this feature's own endpoint; consistent with DEC-009.

## 9. ARCHITECTURE.md Changes (proposed, not applied)

`ARCHITECTURE.md` "Data model" lists `project`, `team`, `sample`, `lead`, `kb_chunk` but no trust/editorial-content store. Proposed addition (for a human to review and apply — I do not edit `ARCHITECTURE.md`):

```
trust_content { id, item_key, item_type, label, value, unit, display_order,
                created_at, updated_at }
```

Rationale: the standalone trust facts (years in business, staff count, steel capacity, software, standards) have no home in the current data model; the marquee count and client/contractor names are intentionally *not* added because they remain derived from `project`. This is a data-model addition only — no constraint in `ARCHITECTURE.md` changes.

This also closes one of `ARCHITECTURE.md`'s open questions only partially: it does not touch the bilingual-storage (F008) question — trust `label`/`value` localization is deferred to F008 and noted as an open question for Design below.

---

## Proposed Decision Records (for the keeper to append to DECISIONS.md)

These DECs arise from this feature. Listed here for the handoff; per the append-only rule I do not write to DECISIONS.md directly.

- **Trust editorial facts stored in a narrow `trust_content` key/value table, not static JSON in `web`.** Why: F012 will edit this content and `ARCHITECTURE.md` names `api` as the content-read service; JSON in `web` would force a redeploy to fix a stat and split content ownership. A key/value shape lets F012 add/edit/retire items without a per-field migration. Alternatives rejected: static JSON in `web` (redeploy to edit, content ownership split); a wide one-column-per-stat table (migration per new stat, rigid for F012).
- **Project-derived trust facts (marquee count, client/contractor names) are computed live from `project`, never stored in `trust_content`.** Why: prevents the trust copy drifting from the portfolio; keeps a single source of truth. Alternative rejected: caching/duplicating these into trust storage (drift risk, redundant write path).
- **One composed `GET /api/trust/overview` endpoint mixes stored editorial content with live project aggregates.** Why: the Trust Layer is one logical page block; one fetch keeps `web` integration and ISR simple, and the synthesized marquee count means F5-AC1 is satisfied without hand-syncing a number. Alternative rejected: several granular endpoints (more round-trips, more wiring for a single block).
