---
feature: 011-core-marketing-pages
spec_id: "011"
phase: architecture
owner: Architect
status: approved
version: "2.1"
entry_criteria:
  - requirements approved (v1.1, updated with prequalification doc findings)
exit_criteria:
  - Architect has produced this file and a human has approved it
---

# Architecture: Core Marketing Pages (Home, About, Services, Contact)

> **Round 2.** Round 1 was rejected after the ALEF Prequalification Document (39-page PDF, sourced from the Dubai DED trade license and company org chart) surfaced material content corrections. The structural design of round 1 was sound and is largely retained; the changes are: 10 team profiles (not 8) on a table explicitly designed for admin CRUD; two offices on Contact promoted to structured content; richer specializations (8) and software (7) lists; a flagged cross-feature update to the 005 trust stats; a richer India-arm story on About; and "Cadding" confirmed as the legal company name (no longer an open question). Sections changed materially since v1.0 are marked **[v2.0]**.

## 1. Overview

This feature builds the four foundational content pages that frame the entire site: Home (`/`), About (`/about`), Services (`/services`), and Contact (`/contact`). It is deliberately the lowest-novelty, highest-coverage feature in the build — content migration and layout — but it is structurally important because it is the shell every differentiating feature (001 concierge, 003 portfolio, 005 trust layer, 008 bilingual, 009 WhatsApp) sits inside.

It fits the existing topology unchanged. `web` (Next.js) renders the four pages and calls `api` (Spring Boot) over HTTP; `api` reads from `postgres`. Three new data needs are introduced:

- **Team profiles** for About — a new `team` table and a `GET /api/team` endpoint, mirroring the `trust_content` pattern from 005. **[v2.0]** The table holds **10** profiles and is designed for admin CRUD (012) from day one — 011 exposes only the read endpoint, but the schema and module must not need restructuring when 012 adds write endpoints. A KEY requirement is that new profiles are addable without a code change or migration; the design below makes that literal (a row insert, nothing more).
- **Office locations** for Contact — **[v2.0]** the two offices (Dubai ALEF + India Jasper) are promoted from hardcoded frontend copy to a small `office` table served by `GET /api/offices`, for the same reason team is a table: 012 will edit these, and `ARCHITECTURE.md` names `api` as the content-read service. Rationale and the alternative (frontend constant) are in §6.2 / DEC-019.
- **Contact form submission** for Contact — the first *write* path in the system, persisting a `lead` row via `POST /api/leads`. This is the first concrete consumer of the `lead` entity sketched in `ARCHITECTURE.md`, introduced here in a deliberately minimal form (see §3.4 and §4.3).

The Home page introduces **no new data of its own**: it composes existing sources of truth — featured projects from 003 (`GET /api/projects?featurable=true`) and trust stats from 005 (`GET /api/trust/overview`) — per F11-AC2.

**[v2.0] Services content** (§6.1, DEC-016) stays static frontend content, but the list is now the **8 reconciled specializations** and **7 software tools** from the prequalification doc, not the smaller round-1 lists. The software badges still come from 005's endpoint where shown, which forces a cross-feature update to the 005 seed (§9, flagged — not built here).

What this feature does NOT build, by scope (requirements "Out of scope"):
- No Arabic localization of this content — that is 008. This ships the English baseline.
- No CMS / admin editing — that is 012 (Phase 3). The `team` and `office` tables are designed so 012 edits them with zero schema churn, exactly as `trust_content` was.
- No live concierge — that is 001. Home gets a *placeholder entry-point slot* (F11-AC1), a styled CTA that 001 later wires to the streaming chat UI.
- No event emission / worker consumer for the contact lead — deferred to 001 (§3.4, DEC-018).

## 2. Where each page's data comes from **[v2.0]**

| Page | Data source | New? | Rendering |
|------|-------------|------|-----------|
| Home `/` | `GET /api/projects?featurable=true` (003), `GET /api/trust/overview` (005) | No — composes existing | Server component, request-time fetch (force-dynamic), see §7 |
| About `/about` | `GET /api/team` (new), static company-history copy | `team` table + endpoint | Server component, request-time fetch |
| Services `/services` | Static frontend content (8 specializations); 005 badges for software | No backend (uses 005 for software) | Static |
| Contact `/contact` | `GET /api/offices` (new), `POST /api/leads` (new write path) | `office` + `lead` tables + endpoints | Static shell + client form; office block server-fetched |

## 3. API Contracts

### 3.1 `GET /api/team` — leadership/team profiles (About, F11-AC3) **[v2.0]**

Public, read-only. Returns the leadership profiles for the About page, ordered for display. Mirrors the 005 trust pattern: backend is the single content source, `web` is pure presentation.

No parameters. Response shape:

```json
{
  "members": [
    {
      "name": "K. Jeyaraman",
      "role": "Managing Director",
      "company": "ALEF & JASPER",
      "email": "jeyaraman@alef-jasper.com",
      "photo": "/img/team/k-jeyaraman.jpg"
    }
  ]
}
```

Notes:
- The list is the **10 core profiles** from the prequalification doc (§4.2 lists them). It is explicitly **expanding** — the API and table must accommodate growth (see below).
- `email`, `photo`, `company` are **nullable** in the response — a profile may lack a photo or carry a secondary contact, and the card must degrade gracefully (render only present fields), consistent with the 003 nullable-credit-field stance.
- **`email` returns a single primary address.** Several people in the doc have two emails (a corporate `@alef-jasper.com` and a personal one). The public marketing card shows one; the model captures the primary (see §4.2). Storing multiple addresses is not warranted for a public card and is not a 011 concern.
- The list is returned **already ordered** by a `display_order` column (same device as `trust_content`), so the frontend never owns ordering.
- A single composed envelope (`{ "members": [...] }`) rather than a bare array, matching the 005 convention, so future fields (e.g. a section heading, or a `country` for the India arm) attach without a breaking change.
- Returns `200` with an empty `members` array if the table is empty (graceful degradation), never null.

**No pagination.** [v2.0] The team is expanding but bounded — a consultancy leadership list grows by ones, not thousands. A `GET`-all ordered by `display_order` is correct and stays correct: even at 30–40 profiles the payload is a few KB read in full for one page. A paginated envelope (as 003's project list uses) would be theatre here — there is no list page, no filter, and no scroll surface. If the team ever grew past what one About page can show (not foreseeable for a leadership roster), 012 could add `?active=true` filtering; that is an additive query param, not a contract break. The "addable without a code change" requirement is satisfied by the table being the source of truth (insert a row → it appears), independent of pagination.

### 3.2 `GET /api/offices` — office locations (Contact, F11-AC7) **[v2.0, new]**

Public, read-only. Returns both office locations for the Contact page, ordered for display. Same pattern as `GET /api/team`.

No parameters. Response shape:

```json
{
  "offices": [
    {
      "key": "dubai",
      "name": "ALEF — Dubai (Head Office)",
      "addressLines": [
        "404 Sheikh Maktoum Building",
        "Damascus Street, Al Qusais",
        "PO Box 65825, Dubai, UAE"
      ],
      "phones": ["+971 4 2513840", "+971 4 3434440"],
      "email": "alefllc@eim.ae",
      "mapQuery": "Sheikh Maktoum Building, Damascus Street, Al Qusais, Dubai"
    },
    {
      "key": "india",
      "name": "Jasper — India (Delivery Centre)",
      "addressLines": [
        "Plot No. 2/286, Near Dharani Sugars",
        "Vasudevanallur, Tirunelveli Dist.",
        "Tamil Nadu 627 758, India"
      ],
      "phones": ["0091 4636 293166"],
      "email": "jasperiec@gmail.com",
      "mapQuery": "Vasudevanallur, Tirunelveli District, Tamil Nadu 627758"
    }
  ]
}
```

Notes:
- **`phones` is an array** because the Dubai office has two numbers (Operations `+971 4 2513840`, HO `+971 4 3434440`); the India office has one. An array models both without a nullable second-phone column.
- **`addressLines` is an ordered array** of presentation lines, so the frontend renders a multi-line address without parsing a single blob and without owning line-break logic.
- **`mapQuery`** is a plain text location string the frontend feeds to a keyless map embed (§5.3, §10.2) — no lat/long, no geocoding, no API key. Storing a query string keeps the zero-account constraint (constraint 1) and lets 012 fix an address typo in one place.
- `email` is a single address per office (each office has one in the doc).
- Returns `200` with an empty `offices` array if the table is empty; the page renders only what is present.

### 3.3 Services content — no dedicated endpoint (DEC-016); software via 005 **[v2.0]**

The **8 specializations** (§6.1) are **static frontend content**, not an API. The **7 software tools** shown on Services are **not duplicated**: they are fetched from `GET /api/trust/overview` (005) `software` array, or the page links to where 005 renders them — never a second copy (requirements: "do not duplicate content"). **This means the 005 seed must be expanded from 3 software rows to 7 — a cross-feature change flagged in §9, not built here.**

### 3.4 `POST /api/leads` — contact form submission (Contact, F11-AC6)

Public, write. The first mutation endpoint in the system. Persists a `lead` row from the contact form and returns a confirmation. F11-AC6 requires "creates a lead record, same path as other lead sources" — so the endpoint and table are named generically (`lead`, not `contact_message`) and 001's concierge `capture_rfq` tool will write to the **same table** with a different `source`.

Request body (JSON):

```json
{
  "name": "…",
  "email": "…",
  "phone": "…",
  "company": "…",
  "message": "…",
  "website": ""
}
```

(`website` is the honeypot field — a real human leaves it blank; see abuse mitigation below.)

Validation (application layer, Jakarta Bean Validation):
- `name` — required, non-blank, max length (e.g. 200).
- `email` — required, valid email format, max length.
- `message` — required, non-blank, max length (e.g. 5000).
- `phone`, `company` — optional, max length.
- `website` (honeypot) — expected empty; if non-empty, see below.
- Unknown/extra fields ignored.
- Invalid body → `400` with an RFC 9457 `ProblemDetail`, consistent with the portfolio `PortfolioExceptionHandler` pattern (validation errors mapped by a `lead`-scoped `@RestControllerAdvice`).

Server behavior:
- Sets `source = 'contact_form'` server-side (never trusted from the client).
- Sets `created_at = now()` server-side.
- Returns `201 Created` with a minimal confirmation body:

```json
{ "id": 42, "status": "received" }
```

- The frontend uses the `201`/`4xx`/`5xx` distinction to drive the success/error state F11-AC6 requires. The `id` is informational only (not a public URL key).

**Abuse mitigation (production-readiness bar in `ARCHITECTURE.md`):** the form carries a honeypot field (`website`) plus Redis per-IP rate limiting — **both ship in 011** (§10 Decision 2). The honeypot is a hidden field the API silently accepts-without-writing when non-empty (bot gets `201`, no row created; check runs before the rate-limit increment so bots don't consume quota). Rate limit: **5 submissions per IP per 60 minutes**; `LeadService` checks Redis key `rate:lead:<ip>` (INCR + EXPIRE 3600) before persisting; on threshold returns `429 Too Many Requests` with `Retry-After: 3600`. IP extracted from `X-Forwarded-For` (single-hop). The contract above is unchanged regardless.

**Event emission deferred.** `ARCHITECTURE.md` constraint 4 and DEC-004 say lead side effects (email notification, etc.) go through a `lead.created` Redis Stream consumed by the `worker`. **011 does NOT build the event emission or the worker consumer.** 011 only persists the row (the durable source of truth) and returns. Wiring `lead.created` emission + the retry/dead-letter consumer is owned by 001 / a dedicated lead-delivery feature, where the reliability bar lives. This is called out explicitly so Dev does not over-build here and so the gap is visible: a contact-form lead in 011 is *persisted* but not yet *notified* (a human reads new `lead` rows directly until 001 lands). The row is never lost; delivery is a later, separately-tested concern. (See §10 and DEC-018.)

## 4. Data Model

### 4.1 The `team` table (new) — designed for CRUD from day one **[v2.0]**

A read-only-for-011 table holding the leadership profiles named in `ARCHITECTURE.md`'s data model (`team { name, role, company, email, photo }`), plus a surrogate id, an ordering column, an `active` flag, and audit timestamps. **The KEY requirement is that 012 manages this via CRUD and new profiles are addable without a code change or migration. The design makes that literal — adding a profile is a single `INSERT`; the public read picks it up on next request.** Nothing in the schema, entity, or module needs restructuring when 012 adds write endpoints.

Pseudo-DDL (Postgres):

```sql
CREATE TABLE team (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          TEXT        NOT NULL,
    role          TEXT        NOT NULL,                 -- e.g. 'Managing Director'
    company       TEXT,                                 -- 'ALEF', 'JASPER', 'ALEF & JASPER'; nullable
    email         TEXT,                                 -- primary public address; nullable
    photo         TEXT,                                 -- public-relative path; nullable
    display_order INTEGER     NOT NULL DEFAULT 0,       -- ordering on the About page
    active        BOOLEAN     NOT NULL DEFAULT TRUE,    -- soft show/hide for 012; 011 reads active only
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (name, role)                                 -- natural key for idempotent seed
);
```

Column decisions:
- **`id` is `BIGINT IDENTITY`**, internal, never in a URL — consistent with `project` and `trust_content` (LEARNING-001).
- **`role` and `company` are free `TEXT`**, not a vocabulary. Team roles are descriptive titles with no filtering, no enum, no UI vocabulary to keep in sync. No `/filters`-style endpoint is warranted. `company` carries the three observed values (`ALEF`, `JASPER`, `ALEF & JASPER`) as plain text — the prequalification doc shows people split across both arms, and a free-text column lets 012 add a new affiliation without a migration.
- **`email`, `photo`, `company` are nullable** for graceful degradation of incomplete profiles; only `name` and `role` are required (a profile with neither is meaningless).
- **`display_order`** lets 012 reorder profiles without a schema change, exactly as `trust_content.display_order` does. The API returns rows already sorted. **This is core to the "expanding team" requirement** — a new senior hire can be slotted into the order via the column, not a code change.
- **`active` (new since v1.0)** is the soft show/hide flag 012 needs to retire a profile without deleting history, and to stage a new profile before publishing. 011's read endpoint returns only `active = TRUE` rows. Adding it now (rather than letting 012 add it) avoids a later migration on a table 012 owns — the requirement explicitly demands no migration for team changes, and a hide is a team change. This is the one schema addition over round 1 driven directly by the "admin-manageable / expanding" requirement.
- **`created_at` / `updated_at`** mirror the other tables and underpin the 012 edit path.
- **`UNIQUE (name, role)`** gives the seed a natural conflict key so it stays idempotent (`ON CONFLICT (name, role) DO NOTHING`) on a long-lived local volume, since `team` has no `slug`/`item_key`-style business key. Matches DEC-012's idempotent-seed discipline. (Two people sharing a role — e.g. two "Project Manager" entries — are distinguished by name, so the composite key holds.)
- **No index beyond the PK and the unique constraint.** A small leadership roster, always read in full, ordered via `ORDER BY display_order`. Filter indexes would be theatre at this scale.

### 4.2 Team seed content (V7) **[v2.0]** — the 10 profiles

The prequalification doc gives the 10 core profiles below. The actual seed `INSERT`s are a content-migration task for the implementation phase (DEC-008 clears them for the local demo); they are listed here so Dev seeds the correct set and so the "10, not 8" correction is unambiguous. Where a person has two emails, the **primary** (corporate `@alef-jasper.com` where present) is seeded; the secondary is dropped for the public card.

| display_order | name | role | company | primary email |
|---|------|------|---------|---------------|
| 1 | Ali Bin Beyat | Sponsor / Chairman | ALEF | alibeyat@alef-jasper.com |
| 2 | K. Jeyaraman | Managing Director | ALEF & JASPER | jeyaraman@alef-jasper.com |
| 3 | J. Sunitha | Executive Director | JASPER | sunitha@alef-jasper.com |
| 4 | M.D. Dinu | Operations Manager | ALEF | dinu@alef-jasper.com |
| 5 | Lakshmipathy Rao | Senior Technical Manager | ALEF | pathy.alef@gmail.com |
| 6 | Namasivayam | Administration Manager | JASPER | siva@alef-jasper.com |
| 7 | Tamilmani | Projects Manager | JASPER | tamil@alef-jasper.com |
| 8 | Syed Oli Masood | Project Manager | JASPER | masood@alef-jasper.com |
| 9 | Sankar | Technical Manager | JASPER | sankar@alef-jasper.com |
| 10 | Varun Pillai | Business Development Manager | ALEF | varun.sales@alef-jasper.com |

`display_order` is a sensible default (sponsor → MD → directors → managers); the Designer/PM may re-sequence, which is exactly what `display_order` is for. `photo` is left null until photo assets exist (the card degrades gracefully).

### 4.3 The `office` table (new) **[v2.0]**

A small table holding the two office locations. Same calculus as `team`/`trust_content`: 012 will edit office details, `api` is the content-read service, and a frontend constant would force a redeploy to fix an address and split content ownership.

Pseudo-DDL (Postgres):

```sql
CREATE TABLE office (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    office_key    TEXT        NOT NULL UNIQUE,          -- stable key, 'dubai' | 'india'
    name          TEXT        NOT NULL,                 -- display name
    address_lines TEXT[]      NOT NULL DEFAULT '{}',    -- ordered presentation lines
    phones        TEXT[]      NOT NULL DEFAULT '{}',    -- one or more numbers
    email         TEXT,                                 -- nullable
    map_query     TEXT,                                 -- plaintext location for keyless embed; nullable
    display_order INTEGER     NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Column decisions:
- **`office_key` is the stable, machine-readable identity** (`dubai`, `india`), mirroring `trust_content.item_key`. It gives the idempotent seed a conflict key (`ON CONFLICT (office_key) DO NOTHING`) and lets 012/API address one office without coupling to display text.
- **`address_lines` and `phones` are `TEXT[]`** — Dubai has two phones; both offices have multi-line addresses. Arrays model the variable cardinality without nullable second-column sprawl. This is the same `TEXT[]` calculus as `project.scope` (DEC-010): small, fixed, never independently queried.
- **`map_query` is plaintext, nullable** — a location string for a keyless map embed, not coordinates (§5.3, §10.2). Keeps the zero-account constraint.
- **`display_order`** keeps Dubai first (head office) without frontend ordering logic.
- **No `active` flag** — there are exactly two offices and both always show; the soft-hide need that `team` has (retiring a person) does not apply to two fixed offices. If a third office is ever added it is just another row.

### 4.4 The `lead` table (new, minimal subset)

`ARCHITECTURE.md` defines a broad `lead` entity covering RFQ/concierge capture (project_name, sector, scope[], tonnage, drawing_count, attachment_ref, conversation_ref, etc.). **011 introduces the table but only the columns the contact form needs**, plus the columns that make the table forward-compatible with the full RFQ path so 001 does not have to rename or restructure it.

**Decision (DEC-017): create the `lead` table now with the minimal contact-form columns required, designed so 001 extends it additively (new nullable columns) rather than replacing it.** The full RFQ-specific columns (tonnage, scope[], attachment_ref, conversation_ref, …) are **not** created here — they have no consumer until 001 and adding unused columns now would be speculative. They are added by 001's migration as nullable columns when that feature lands.

Pseudo-DDL (Postgres) — the 011 subset:

```sql
CREATE TABLE lead (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    source      TEXT        NOT NULL,                  -- 'contact_form' (011); 'concierge' etc. later
    name        TEXT        NOT NULL,
    email       TEXT        NOT NULL,
    phone       TEXT,
    company     TEXT,
    message     TEXT                                   -- free-text enquiry from the contact form
);
```

Column decisions:
- **`source` is a mandatory `TEXT` discriminator**, set server-side. It is what makes "same path as other lead sources" (F11-AC6) literal: every lead origin (contact form now, concierge later, WhatsApp later) writes the same table with a distinct `source`. Validated/owned in the app layer, consistent with DEC-009.
- **`message` is the one field beyond the `ARCHITECTURE.md` core list** that the contact form needs (a free-text enquiry). Proposed as an additive field — see the proposed `ARCHITECTURE.md` change in §11.
- **`email` is `NOT NULL` here** (the contact form requires it). If 001 needs to insert a partial lead before email is known, it relaxes this with a migration; not a concern for the contact form, which validates email as required.
- **No `updated_at`.** A contact-form lead is write-once, never edited by the public path. 012/CRM concerns may add it later.

### 4.5 Migration plan (Flyway) **[v2.0]**

Existing migrations run V1–V5 (V1 create project, V2 seed projects, V3 image fix, V4 create trust_content, V5 seed trust_content). **The next available version is V6.** New migrations for 011, in order:

- **`V6__create_team.sql`** — the `team` DDL (§4.1), including `active` and the `UNIQUE (name, role)` constraint.
- **`V7__seed_team.sql`** — the **10** leadership profiles (§4.2), idempotent via `INSERT ... ON CONFLICT (name, role) DO NOTHING`, matching DEC-012.
- **`V8__create_office.sql`** — the `office` DDL (§4.3).
- **`V9__seed_office.sql`** — the two offices (§3.2 values), idempotent via `ON CONFLICT (office_key) DO NOTHING`.
- **`V10__create_lead.sql`** — the `lead` DDL (§4.4). No seed — leads are runtime data, not demo content.

**Cross-feature note (NOT a 011 migration):** the 005 trust seed (`V5__seed_trust_content.sql`) carries only 3 software rows and stale stat values. Correcting it to the 7-tool list and the prequalification stats is a **change to feature 005's content**, owned by 005/012, not 011. 011 must not edit V5 (Flyway checksums an applied migration; editing it breaks the local volume). The correction lands as a **new** forward migration. See §9 for the full flag and the proposed approach.

The actual profile/office content and the corrected typos (F11-AC4: "Intenational" → "International", "Concorse" → "Concourse"; **"Cadding" is the legal company name per the trade license — DO NOT change it**, this is now settled, no longer an open question) are a content-migration task for the implementation phase, cleared for demo by DEC-008.

## 5. Frontend Architecture

### 5.1 Routing (Next.js app router)

Four routes under `web/src/app/`:

```
web/src/app/
  page.tsx              # / (Home) — replaces the current placeholder
  about/page.tsx        # /about
  services/page.tsx     # /services
  contact/page.tsx      # /contact
```

The existing `web/src/app/page.tsx` is a placeholder (its own doc comment says 011 will replace it) — 011 replaces it with the full Home page.

### 5.2 Shared layout (nav + footer) — already in place, needs wiring

`web/src/app/layout.tsx` **already provides** the sticky `SiteNav` and `SiteFooter` per the design system, so F11-AC8 (consistent nav/header/footer across all four pages) is mostly satisfied by the existing root layout. Two corrections (proposed; Dev implements):
- The nav links currently point `About`, `Services`, `Samples`, `Contact` at `href="#"`. Wire `About → /about`, `Services → /services`, `Contact → /contact` (Samples stays `#`, that is feature 004). The "Enquire Now" CTA and the home hero CTAs point at the concierge slot / `/contact` (§5.3).
- The footer copyright must render the current year dynamically (`new Date().getFullYear()`) so it never goes stale again (live-site bug was 2014). F11-AC9.

**[v2.0] Legal name check:** wherever the nav/footer/logo render the company name, it must read "ALEF Architectural & Cadding Services" — "Cadding" is correct (legal name on the DED trade license). Dev must not "fix" it. Full legal entity name "ALEF ARCHITECTURAL & CADDING SERVICES L.L.C" is appropriate in the footer.

No new layout component is needed.

### 5.3 Component hierarchy per page **[v2.0]**

Components follow the established convention. Reuse 003 and 005 components rather than re-implementing.

**Home (`/`)** — server component composing existing data:
- `HomeHero` — single hero (F11-AC1), replacing the three repeated banners. Contains the **concierge entry-point slot**: a prominent gold CTA that 001 later wires to the chat UI; until then it routes to `/contact` so it is never dead.
- `CapabilityStrip` — sector/capability strip (static, or derived from `GET /api/projects/filters` sectors).
- **Featured projects marquee** — reuses 003's `ProjectCard`/`ProjectGrid` fed by `getProjects({ featurable: true })`. No new card, no duplicated data (F11-AC2).
- **Trust stats** — reuses 005's `TrustLayer`/`TrustStats` fed by `getTrustOverview()`. No duplicated data (F11-AC2). **[v2.0] Note:** the numbers this renders are 005's seed values, which the prequalification doc corrects (23 years, 70,000 t/month, 250 workstations, 102+ projects, 40+ clients, 6 GCC countries). Updating them is a 005-seed change flagged in §9, not done in 011 — but Home will display whatever 005 serves, so the §9 update should land alongside 011 for the Home page to show the right figures.
- `HomeCTA` — primary CTA path into RFQ/concierge.

**About (`/about`)** — server component:
- `AboutHero` — page hero matching the Projects hero pattern.
- `CompanyStory` — **[v2.0] richer than v1.0.** ALEF (Dubai, founded **March 2003**, 23 years) + the **Jasper India delivery arm** told as a selling point: purpose-built Vasudevanallur campus with **capacity for 250 staff**, **in-house accommodation for 196 staff** with canteen, full departments (QC, Technical, Administration, Project Management), 2 project departments / 10 sections, checkers and detailers organized in teams. This is the scale/cost-advantage story. Static content with corrected typos (F11-AC4), "Cadding" preserved.
- `GccReachBlock` (new section, may be part of `CompanyStory`) — **[v2.0]** the GCC footprint with dedicated onsite coordinators per country (UAE, Qatar, Bahrain, Oman, Kuwait, Saudi Arabia + "other"). A geographic-reach selling point from the org chart. Static content.
- `LandmarkProjectsStrip` (optional, new) — **[v2.0]** a named-marquee of landmark projects from the company history (Ski Dubai/Snow Centre, Mall of the Emirates, Dubai Airport Concourses 3 & 4, Abu Dhabi Midfield Terminal, New Doha International Airport, Route 2020 Metro, Dubai Hills Estate Mall, Bluewaters Island, etc.). **Subject to DEC-008 name-clearance** (same gate as the portfolio marquee). If shown, prefer pulling from the 003 `project` table where these exist (single source of truth) and only listing the rest as static text; the Designer/PM decide whether this is a static prestige strip or pulled from 003. Flagged in §10.
- `TeamGrid` + `TeamCard` (new, in `web/src/components/team/`) — the **10** profiles as designed cards (not a flat list), fed by `getTeam()`. Card styled per the design-system card patterns; the Designer specifies exact treatment. **[v2.0] The grid must lay out cleanly for a variable, growing count** (10 today, more later) — no hardcoded 8-card or 10-card layout; a responsive grid that flows for N cards (the team is expanding).

**Services (`/services`)** — static server component:
- `ServicesHero`.
- `ServicesList` + `ServiceCard` (new, in `web/src/components/services/`) — **[v2.0] all 8 specializations** (F11-AC5), reframed as outcomes, styled per the design system's "Service Card" pattern. Static content (DEC-016). The 8: rebar shop drawings & BBS; general arrangement drawings; concrete works drawings; global co-ordinates / setting-out drawings; road setting out / profiles / cross sections / road marking / signage & utility services; estimation & quantity surveying; MEP drawings; as-built drawings.
- `SoftwareBadges` — **[v2.0]** if software is shown on Services, it is fed by 005's `getTrustOverview()` `software` array (the **7-tool** list once §9's 005-seed update lands: AutoCAD, Steel PAC RCD, Steel PAC RCS, CADS RC, CADMATE, LOHA, Multi-Rc), not a static re-list. Never a second copy.

**Contact (`/contact`)** — static shell + a client form + a server-fetched office block:
- `ContactHero`.
- `ContactForm` (new, **client component**, in `web/src/components/contact/`) — the working form (F11-AC6). Posts to `/api/leads` via a new typed client; manages submit/success/error state; includes the hidden honeypot field (`website`). Styled per the design-system "Forms" spec.
- `OfficeLocations` + `OfficeCard` (new) — **[v2.0]** renders **both** offices (Dubai + India) from `getOffices()`: name, multi-line address, phone(s), email, and a map (F11-AC7). Two cards side-by-side per the design system.
- `ContactMap` — **[v2.0]** one map per office, fed by `office.mapQuery`. A keyless map embed (e.g. a keyless Google Maps `<iframe>` using the query string, or a static map image + "open in maps" link) keeps the local-first, zero-account constraint (constraint 1) — **no API key, no paid maps SDK**. Designer/Dev pick the lightest embed; flagged in §10.
- `DirectChannels` — the AI-concierge + WhatsApp (009) entry-point slots *alongside* direct phone/email (F11-AC7), not instead of them.

### 5.4 Typed clients + types (proposed; Dev implements) **[v2.0]**

Mirror the 003/005 convention:
- `web/src/lib/api/team.ts` + `web/src/lib/types/team.ts` — `getTeam()` returning the `{ members }` envelope; `next: { revalidate: 60 }`.
- `web/src/lib/api/offices.ts` + `web/src/lib/types/office.ts` — `getOffices()` returning the `{ offices }` envelope; same caching. **[v2.0, new]**
- `web/src/lib/api/leads.ts` + `web/src/lib/types/lead.ts` — `submitLead(payload)` POSTing to `/api/leads`, returning `{ id, status }` or throwing on non-2xx so the form can render the error state. No cache (it is a mutation).
- Home reuses the existing `web/src/lib/api/projects.ts` and `web/src/lib/api/trust.ts` — no new client for its data (F11-AC2).
- Services specializations live as a typed constant (`web/src/lib/content/services.ts`); software comes from the trust client.

## 6. Static-vs-table content decisions

### 6.1 Services / specializations stay static (DEC-016) **[v2.0]**

The **8 specializations** are fixed marketing copy: never filtered, never aggregated, never edited by a non-technical user in 011's scope, no per-row identity worth addressing. A table+migration+endpoint+fetch would be heavier than the code it describes. That is the inverse of the `trust_content` calculus (which earned a table because 012 edits it and stats recur). So Services content lives as a typed constant in the frontend. **If 012 later wants to edit specializations, it can promote them to a `trust_content`-style key/value table** — the constant is the cheap, reversible choice now. (The richer 8-item list does not change this calculus; it is still copy, not queryable data.)

### 6.2 Offices ARE a table (DEC-019) **[v2.0, new]**

Why offices are a table when specializations are not: office details (addresses, phones, emails) are exactly the kind of content **012 will edit** (a phone number changes, a PO box moves) and the prequalification doc shows they carry structured, multi-valued data (two phones, multi-line addresses). `ARCHITECTURE.md` names `api` as the content-read service; a frontend constant would force a redeploy to fix a phone number and split content ownership across two services — the same argument that put `trust_content` in a table (DEC-013). Two rows is a small table, but the *reason* for a table is editability and content-ownership, not row count. Alternatives rejected: a frontend constant (redeploy to edit, ownership split); folding offices into `trust_content` (different shape — arrays for phones/address-lines don't fit the key/value stat schema).

## 7. Rendering Strategy

Consistent with 003 and the live `web/src/app/projects/page.tsx`: the architecture *prescribes ISR* (`revalidate=60`) for the read pages, but the **current Docker-Compose reality forces `force-dynamic`** because the `web` image builds before the `api` container is up (PITFALL-007, CLAUDE.md TODOs). 011 follows the same stance as the shipped projects page:

- **Home, About, Contact (office block)** — server components fetching at request time; mark `export const dynamic = "force-dynamic"` for parity, *or* `export const revalidate = 60` once the deploy model makes the API reachable at build time. The API clients already pass `next: { revalidate: 60 }`, so flipping to ISR later is a one-line page change. All must wrap fetches in the same try/catch graceful-degradation pattern the projects page uses, so a cold API never produces a hard error page.
- **Services** — fully static (no data fetch) **except** the optional software badges. If software badges are rendered server-side from 005, Services becomes a server component with the same force-dynamic/try-catch treatment; if software is omitted or linked rather than embedded, Services stays pure SSG. Designer/Dev choose; either keeps the SEO budget.
- **Contact** — static shell (SSG) with a client-side `ContactForm`; the office block is server-fetched (above). The form POST is a runtime request to `/api/leads`, independent of page rendering.

This keeps F11-AC8's Lighthouse/SEO budget intact: all four pages serve fully-rendered HTML to crawlers, no client-side data-loading waterfall for primary content.

## 8. Integration Points **[v2.0]**

- **Home → 003 (featured projects).** `getProjects({ featurable: true, size: 50 })`. 003's summary projection already carries `featurable` and card fields (DEC-011); no new endpoint (003 §6 anticipates this). Single source of truth — F11-AC2.
- **Home → 005 (trust stats).** `getTrustOverview()`, mounting 005's `TrustLayer`. 005 §4/§7 states 011 mounts this block. **[v2.0] Numbers are 005's seed — see §9 cross-feature update.** Single source of truth — F11-AC2.
- **Services → 005 (software badges).** **[v2.0]** Software comes from `getTrustOverview()` `software`, not a static re-list — requires the 005 seed to grow to 7 tools (§9). "Do not duplicate content."
- **Contact → office data.** `getOffices()` → `GET /api/offices` → `office` rows. **[v2.0, new]**
- **Contact → lead creation.** `ContactForm` (client) → `submitLead()` → `POST /api/leads` → `lead` row (`source='contact_form'`). Same table the concierge (001) will write with `source='concierge'`. Event emission deferred to 001 (§3.4, DEC-018).
- **Navigation.** All four pages inherit `SiteNav`/`SiteFooter`; the nav-link `href` corrections (§5.2) connect the routes. Single nav definition — F11-AC8.
- **Concierge slot (001).** Home hero + Contact expose a styled concierge entry-point CTA routing to `/contact`; 001 rewires it.
- **WhatsApp slot (009).** Contact reserves a direct-channel slot alongside phone/email; 009 wires the link.

## 9. Cross-feature concern: the 005 trust seed is now stale **[v2.0, IMPORTANT]**

The prequalification doc supersedes several values currently seeded by 005's `V5__seed_trust_content.sql`. Because Home (and optionally Services) renders 005's data verbatim, **the numbers users see on the new Home page depend on the 005 seed being corrected.** This is a change to **feature 005's content**, not 011's — I am flagging it, not building it. 011 must not edit V5 (Flyway checksums applied migrations; editing breaks the local volume) and must not own 005's content.

Discrepancies (shipped 005 seed → prequalification doc):

| Item | 005 seed today | Prequalification doc | Type |
|------|---------------|----------------------|------|
| Years in business | `18+` | `23` (founded March 2003) | stat — UPDATE |
| Monthly steel capacity | `5,000` t/month | `70,000` tons/month | stat — UPDATE |
| Staff / workstations | `120+` engineers | `250` workstations | stat — review/UPDATE |
| Projects completed | (none) | `102+` across GCC | stat — ADD |
| Major clients | (none) | `40+` contractor clients | stat — ADD |
| GCC countries | (none) | `6` (UAE, Qatar, Bahrain, Oman, Kuwait, KSA) | stat — ADD |
| Drawings/month | (none) | `6,000`/month | stat — ADD (optional) |
| Software list | 3 (AutoCAD, CADS RC, SteelPac RC) | 7 (AutoCAD, Steel PAC RCD, Steel PAC RCS, CADS RC, CADMATE, LOHA, Multi-Rc) | software — UPDATE |

**Resolution (v2.1): `V11__update_trust_content_from_prequalification.sql` is written and ships alongside 011.** It `UPDATE`s the three stale stat rows and `INSERT ... ON CONFLICT DO NOTHING`s three new stat rows and four new software rows. V5 is not touched (Flyway checksums). The `trust_content` key/value shape (DEC-013) means this needs no DDL — pure data changes. Decision on hardcode-vs-derive is recorded in §10 Decision 1. The V11 migration also carries a code comment marking the three headline stats (`102+`, `40+`, `6`) as candidates for live derivation from the `project` table once the catalogue is fully seeded.

## 10. Architectural Decisions — All Resolved **[v2.1]**

All four open decisions from v2.0 have been ruled on. No open questions remain.

**Decision 1 — 005 trust-seed stats: hardcode now, derive later.**
Hardcode headline figures ("102+", "40+", "6") as `stat` rows in `V11__update_trust_content_from_prequalification.sql` (written alongside 011). When the project catalogue is fully seeded (CLAUDE.md TODO: ~38 total, 15 seeded), replace those three rows with live aggregation from the `project` table — the same mechanism DEC-014 already uses for marquee counts and client/contractor lists. V11 includes a comment marking this swap point so Dev knows it is intentional and not permanent.

**Decision 2 — Rate limiting on `POST /api/leads`: ship in 011 with Redis per-IP limit.**
Rationale: Redis is already in the stack; `ARCHITECTURE.md` explicitly mandates "honeypot plus rate limit" for forms — shipping the honeypot alone would be non-compliant with the architecture mandate on the first public write path. The implementation is minimal and establishes the pattern 001 will reuse. Ruling: **both honeypot AND Redis per-IP rate limit ship in 011.**
- Rate: **5 contact form submissions per IP per 60 minutes.** B2B enquiry forms have low legitimate volume; 5/hour gives real users plenty of headroom while stopping bot floods.
- Implementation: `LeadService` checks a Redis key `rate:lead:<ip>` (INCR + EXPIRE 3600) before persisting; on threshold, returns `429 Too Many Requests` with a `Retry-After` header. The honeypot check runs first (before the rate-limit increment) so bot submissions don't consume quota slots. The IP is extracted from `X-Forwarded-For` with a single-hop policy (the real IP behind any reverse proxy, not the proxy's IP). No separate Spring module needed — a few lines in `LeadService`, injecting `RedisTemplate<String, String>`.

**Decision 3 — Contact map: keyless Google Maps `<iframe>`.**
`office.mapQuery` is passed as the `q=` parameter to the Google Maps embed URL (`https://maps.google.com/maps?q=<mapQuery>&output=embed`). No API key required for this embed. One `<iframe>` per office card; the `src` is assembled from `mapQuery` in the component. If a map fails to load (cold demo, no internet), the card still shows the address and phone — the `<iframe>` is decorative, not structural (F11-AC7 is satisfied by the address text, not the embed).

**Decision 4 — Landmark-projects strip on About: included, sourced from 003.**
`LandmarkProjectsStrip` pulls `getProjects({ featurable: true })` from 003 (the same call Home makes) and renders a horizontal prestige marquee of those project names/images. The landmark names from the prequalification doc (Ski Dubai, Mall of the Emirates, Dubai Airport Concourses, Abu Dhabi Midfield Terminal, New Doha International Airport, Route 2020 Metro, Bluewaters Island, etc.) are already seeded or will be seeded in the 003 project catalogue — they do not appear as a parallel static list. DEC-008 clearance applies (demo use only until the full catalogue is confirmed for public deploy). Designer specifies the strip layout.

**Settled since round 1 (no longer open):** "Cadding" is the legal company name (DED trade license) — it is **not** a typo and must never be auto-corrected.

## 11. ARCHITECTURE.md Changes (PROPOSED, not applied) **[v2.0]**

I do not edit `ARCHITECTURE.md` (hook-blocked, human-approval only). The following changes are proposed for a human to review and apply.

### PROPOSED ARCHITECTURE.md CHANGE 1 — `team` table fields

`ARCHITECTURE.md` "Data model" currently lists:
```
team     { name, role, company, email, photo }
```
Proposed (field additions only, no constraint change):
```
team     { id, name, role, company, email, photo, display_order, active,
           created_at, updated_at }
```
Rationale: `id` surrogate key; `display_order` for 012 reordering without a migration; **`active` soft show/hide so 012 can retire/stage a profile without a delete or a migration** (directly serves the "admin-manageable / expanding team" requirement); `created_at`/`updated_at` for the 012 edit path. No constraint changes.

### PROPOSED ARCHITECTURE.md CHANGE 2 — new `office` table **[v2.0, new]**

`ARCHITECTURE.md` "Data model" has no office/contact-location store; office addresses were implicitly hardcoded. Proposed addition:
```
office   { id, office_key, name, address_lines[], phones[], email,
           map_query, display_order, created_at, updated_at }
```
Rationale: two offices (Dubai ALEF + India Jasper) must appear on Contact (F11-AC7) with multi-valued phones/address lines; 012 will edit them; `api` is the content-read service (DEC-013/DEC-019 reasoning). Data-model addition only — no constraint changes.

### PROPOSED ARCHITECTURE.md CHANGE 3 — `lead` table gets a `message` field

`ARCHITECTURE.md` `lead` lists structured RFQ fields but no general free-text field. Proposed addition (one field, `message`):
```
lead     { id, created_at, source, name, email, company, phone, message,
           project_name, sector, country, scope[], tonnage,
           drawing_count, software_standard, deadline, attachment_ref,
           conversation_ref }
```
Rationale: the contact form is unstructured; it needs a free-text `message` the current entity lacks. Additive, nullable for RFQ-sourced leads. 011 physically creates only the subset it uses (§4.4, DEC-017); the full set is the documented target, filled in additively by 001.

### PROPOSED ARCHITECTURE.md CHANGE 4 — resolve two open architectural questions **[v2.0]**

`ARCHITECTURE.md` "Open architectural questions" can have one item closed and one informed by the prequalification doc:
- Close: there is no longer ambiguity that "Cadding" is the legal trade-license name (relevant to content, not the question list per se, but worth recording in DECISIONS).
- The doc confirms a real two-office, multi-country operating model, which the new `office` table now represents structurally.

No constraint in `ARCHITECTURE.md` is changed by any proposal — all are data-model additions and a question-list update.

## 12. Risks and Mitigations **[v2.0]**

- **Home/Services show stale 005 numbers.** The single biggest round-2 risk: the modernized Home page renders 005's seed, which the prequalification doc supersedes (§9). Mitigated by flagging the 005-seed update as a cross-feature dependency to land alongside 011 (a new forward migration, never editing V5). If it does not land, Home shows "18+ years / 5,000 t" instead of "23 years / 70,000 t".
- **Cold API at web build/request time** (PITFALL-007). Mitigated by `force-dynamic` + the try/catch graceful-degradation pattern already shipped on the projects page; reused on Home/About/Contact.
- **Contact-form lead persisted but not delivered** (event emission deferred to 001). Mitigated by making the row the durable source of truth — the enquiry is never lost, only the *notification* is later. Until 001, a human reads new `lead` rows directly.
- **Form abuse.** Mitigated by honeypot in 011 + recommended Redis rate limit (open decision §10.2) + Bean Validation.
- **Content drift between Home and 003/005.** Mitigated structurally: Home composes the live endpoints and reuses 003/005 components; stores no copy (F11-AC2).
- **Team grid hardcoded to a fixed count.** [v2.0] The team is expanding; mitigated by requiring a responsive grid that flows for N cards (§5.3) and an `active`-flagged, `display_order`-sorted read so additions/removals/reorders are pure data changes (the core requirement).
- **"Cadding" auto-corrected by a well-meaning typo pass.** [v2.0] Mitigated by stating in §4.5/§5.2/F11-AC4 that it is the legal name and must not be changed; settled in §10.
- **Name/typo/content clearance for public deploy.** DEC-008 cleared demo content for local use only; team names, office details, and any landmark-project strip inherit the same pre-public-deploy gate.
- **Scope creep into 008/012/001/005.** Mitigated by the explicit "what this does NOT build" (§1) and the deferred slots being placeholders. The 005-seed update is explicitly *flagged, not built* here (§9).

## 13. Acceptance Criteria Traceability **[v2.0]**

| AC | Architectural decision(s) that satisfy it |
|----|---------------------------------------------|
| **F11-AC1** Single hero + concierge entry point above fold | §5.3 `HomeHero` single hero replacing three banners + prominent gold concierge-slot CTA (routes to `/contact` until 001). |
| **F11-AC2** Featured projects (003) + trust stats (005), no duplication | §8 Home composes `getProjects({featurable:true})` (003) and `getTrustOverview()` (005), reusing components; stores no copy. |
| **F11-AC3** Company history + all 10 team profiles, API-served and admin-manageable | §3.1 `GET /api/team` + §4.1 `team` table designed for CRUD (`active`, `display_order`, audit) + §4.2 the 10 seed rows (V6/V7) + §5.3 `CompanyStory`/`GccReachBlock`/`TeamGrid`. New profiles addable via a row insert, no code change (§4.1). |
| **F11-AC4** Correct live-site typos; preserve "Cadding" | §4.5 + §10 content-migration corrects "Intenational"→"International", "Concorse"→"Concourse"; "Cadding" is the legal name and is NOT changed (settled). |
| **F11-AC5** Every specialization, reframed as outcomes, nothing dropped | §5.3 `ServicesList`/`ServiceCard` + §6.1 static content carrying all **8** specializations. |
| **F11-AC6** Contact form submits, creates a lead, success/error state | §3.4 `POST /api/leads` → `lead` row (`source='contact_form'`), `201`/`4xx` drive form state; §5.3 `ContactForm`; §4.4 `lead` table (V10). Same table/path as future concierge leads. |
| **F11-AC7** Both offices + working map + direct channels | §3.2 `GET /api/offices` + §4.3 `office` table (V8/V9, two rows) + §5.3 `OfficeLocations`/`OfficeCard`/`ContactMap` (keyless embed) + `DirectChannels` (phone/email/concierge/WhatsApp). Both Dubai and India shown. |
| **F11-AC8** Shared nav/header/footer + same mobile/Lighthouse budgets | §5.2 all four pages inherit `SiteNav`/`SiteFooter`; §7 static/server rendering keeps the SEO/performance budget. |
| **F11-AC9** Correct stale copyright/dead content | §5.2 footer year dynamic (`new Date().getFullYear()`). |

---

## Proposed Decision Records (for the keeper to append to DECISIONS.md) **[v2.0]**

These arise from this feature. Per the append-only rule I do not write to DECISIONS.md directly. Next id after DEC-015 is DEC-016.

- **DEC-016: Services/specializations content is static frontend content, not an API/table.** Why: fixed marketing copy (8 items) with no filter/aggregate/query surface and no 011-scope edit need; a table+endpoint+fetch would be heavier than the code it describes. Contrast `trust_content` (012 edits it, stats recur) and `office` (012 edits it, structured multi-valued data). Reversal path: 012 can promote specializations to a `trust_content`-style table later. Alternatives rejected: a `services` table (no write/query consumer today); CMS now (that is 012).
- **DEC-017: The `lead` table is created in 011 with only the contact-form column subset, extended additively by 001.** Why: the full `ARCHITECTURE.md` `lead` entity has RFQ-specific columns with no consumer until 001; creating them now is speculative. 011 creates `{id, created_at, source, name, email, phone, company, message}`; 001 adds the rest as nullable columns. `source` makes it the shared lead path (F11-AC6). Alternatives rejected: build the full wide table now (unused, speculative shape); a separate `contact_message` table (violates F11-AC6's "same path", forces a later merge).
- **DEC-018: 011 persists contact leads but does NOT emit `lead.created` or build the worker consumer.** Why: `ARCHITECTURE.md` constraint 4 / DEC-004 route lead side effects through Redis Streams to the worker with a retry/dead-letter bar; that belongs with 001/lead-delivery, not this low-risk content feature. 011's minimal correct slice is a durable persisted row (never lost). Alternatives rejected: build emission+consumer in 011 (over-builds, duplicates hardening 001 owns); fire-and-forget email from the request thread (violates constraint 4).
- **DEC-019: Office locations stored in an `office` table, not a frontend constant. [v2.0]** Why: two offices (Dubai ALEF + India Jasper) carry structured, multi-valued data (two phones, multi-line addresses) and are exactly what 012 will edit (a phone/PO box changes); `ARCHITECTURE.md` names `api` as the content-read service, so a frontend constant would force a redeploy to fix a phone number and split content ownership — the same argument as DEC-013 for `trust_content`. The reason for a table is editability and content-ownership, not row count. Alternatives rejected: a frontend constant (redeploy to edit, ownership split); folding offices into `trust_content` (arrays for phones/address lines don't fit the key/value stat schema).
- **DEC-020: Team table designed for admin CRUD from day one, with an `active` soft-hide flag; 011 exposes read-only. [v2.0]** Why: a KEY requirement is that the leadership team is expanding and must be admin-manageable (012) with new profiles addable without a code change or migration. The `team` table therefore carries `display_order` (reorder without a migration), `active` (retire/stage a profile without a delete or migration), and audit timestamps from the start, so 012 adds write endpoints with zero schema churn. 011 ships only `GET /api/team` (active rows, ordered). Adding a profile is a single `INSERT`. Alternatives rejected: seed-and-forget table with no `active`/ordering (would need a migration when 012 lands — violates the no-migration requirement); a frontend constant for the team (redeploy per hire, ownership split). Also settled here: 10 profiles (not 8), and "Cadding" is the legal trade-license name (never auto-correct it).
