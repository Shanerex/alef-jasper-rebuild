---
feature: 012-admin-content-management
spec_id: "012"
phase: architecture
owner: Architect
status: approved
version: "0.2"
entry_criteria:
  - requirements approved (requirements.md v1.1)
  - human stakeholder directed two settled scope changes (responsive admin; single-admin with in-app known-password change) plus a forward-looking theme-readiness constraint — see handoffs/3-pm-to-architect.md
  - knowledge layer (ARCHITECTURE.md, DECISIONS.md, LEARNINGS.md) read
exit_criteria:
  - Architect has produced architecture.md and design.md; a human has approved this file and the proposed ARCHITECTURE.md / DECISIONS.md changes
---

# Architecture: Admin Content Management

## 0. Change log (v0.1 → v0.2)

Amended after the human stakeholder revised requirements to v1.1 (all AC-backed, not re-litigated here):

- **DEC-021 amended (§2.1):** the admin credential is no longer an immutable env var read on every boot. It is now a **persisted, single-row credential** (`admin_credential`) **seeded from env on first boot only** and **mutable at runtime**. A new **`POST /api/admin/password`** endpoint lets the one admin change a *known* password in-app with no redeploy (F12-AC28, F12-AC29). The `UserDetailsService` reads the persisted store, not env. Everything else in DEC-021 (Spring Security, single admin, standalone from 006, Redis session, HttpOnly `ALEFADMIN` cookie, CSRF on `/api/admin/**`) is **unchanged**.
- **DEC-026 flipped (§2.7):** the admin console is now **fully responsive** (phone / tablet / desktop), reversing the earlier desktop-only stance (F12-AC27). It also adds a **theme-readiness** constraint: all admin colors flow through **themeable design tokens, no hard-coded hex**, so the forthcoming **spec 013** light/dark toggle can layer on with no rework (F12-AC30). Spec 013 is a **sibling, non-blocking** dependency — 012 does not build the toggle.
- **New table:** `admin_credential` at **Flyway V14** (§3.6). This makes **two** new tables in 012 (`sample` + `admin_credential`); every other content type still edits an existing table.
- Env, component-boundary, risk, §9 (proposed ARCHITECTURE.md changes) and §10 (proposed DECISIONS) sections updated accordingly. **DEC-021..DEC-026 remain PROPOSED-only** (not yet in DECISIONS.md — highest committed is DEC-020) and are not applied here.

## 1. Overview

Feature 012 adds the first **authenticated, mutating** surface to the system: an internal admin console for non-technical ALEF staff to manage site content (projects, team, trust stats/badges, samples), upload the images/files those records reference, and **manage their own admin password**. Every content type it edits already has a public read path built by an earlier feature (003 projects, 005 trust, 011 team/office); 012 adds the **write** side behind auth and reuses those tables unchanged.

It fits the existing topology without new services:

- **web** (Next.js) gains an `/admin/**` route tree — a login screen, list views, forms, and an account screen for changing the password. It is **fully responsive** (F12-AC27) and styled entirely through **themeable design tokens** (F12-AC30). It talks to `api` over HTTP exactly as the public pages do, but with an authenticated session.
- **api** (Spring Boot) gains an `admin` security layer (Spring Security), a set of `/api/admin/**` mutating endpoints, a self-service password-change endpoint, a multipart upload endpoint, and — for projects only — emission of the `kb.changed` Redis Stream event on write.
- **postgres** gains **two** new tables (`sample` and `admin_credential`); every other content type edits an existing table.
- **redis** gains two roles beyond rate-limiting: the admin **session store** and the `kb.changed` **stream** producer.
- Uploaded files land on a **local filesystem volume** mounted into `api`, served back under `/uploads/**`.

No LLM path, vector store, or worker code is built here. The `kb.changed` **consumer** is feature 001's; 012 only **produces** the event so 001 can consume it later (approved: emit now, consume later — F12-AC25, DEC-004).

**Sibling dependency — spec 013 (light/dark theme toggle):** a separate feature staged to ship in v1 alongside 012. 012 does **not** design or build the toggle. 012 owes only **token-readiness** (F12-AC30): every admin color comes from a design token, so 013 can add the switch behind those same tokens without a rework pass. This is informational — 012 does **not** block on 013.

Every decision below traces to one or more `F12-ACn` ids.

## 2. Key structural decisions

The open architecture questions from the task, resolved. Full rationale and rejected alternatives are in the **Proposed Decision Records** section (DEC-021..DEC-026, of which DEC-021 and DEC-026 are amended for requirements v1.1).

### 2.1 Authentication & credential storage (F12-AC1, F12-AC2, F12-AC3, F12-AC26, F12-AC28, F12-AC29) — DEC-021

**Mechanism: Spring Security with a single admin credential in a persisted, runtime-mutable store (seeded from env on first boot) and a Redis-backed server-side session.**

- A **single admin identity** exists (v1 is explicitly single-admin, single-role — requirements "Out of scope": multiple accounts deferred to v2). A `UserDetailsService` supplies the one `ROLE_ADMIN` principal by reading it from a **persisted credential store**, not from env directly.
- **Login** is `POST /api/admin/session` (username + password). On success Spring Security creates a server-side **session**; the session id is returned in an **HttpOnly, SameSite=Lax, Secure(prod)** cookie named `ALEFADMIN`. **Logout** is `DELETE /api/admin/session`, which invalidates the session.
- **Sessions are stored in Redis** via Spring Session Data Redis. Redis is already a running dependency (rate limiting). This makes sessions survive an `api` restart and keeps auth stateless at the JVM level — no in-heap session map. It also keeps the door open to a horizontally-scaled `api` without a re-architecture (constraint 7: low-cost upgrade path).
- **Enforcement (the real security boundary):** all `/api/admin/**` endpoints require `ROLE_ADMIN`. Unauthenticated requests to mutating endpoints return **401** with an RFC 9457 `ProblemDetail`, never a silent success (F12-AC2). **CSRF protection is enabled** for all state-changing `/api/admin/**` requests (cookie-based auth is CSRF-exposed): Spring Security issues a CSRF token the admin client echoes in the `X-XSRF-TOKEN` header.
- **Public read endpoints stay open.** `/api/projects/**`, `/api/trust/overview`, `/api/team`, `/api/offices`, `/uploads/**`, and `POST /api/leads` remain unauthenticated — the security config permits them explicitly and locks down only `/api/admin/**`.
- **Web guard (F12-AC1, UX layer):** Next.js middleware on `/admin/**` (except `/admin/login`) checks for the presence of the session cookie and redirects unauthenticated browsers to `/admin/login`. Presence-check is UX only; **authoritative** enforcement is the API's 401/403. The admin browser calls the API directly with `credentials: "include"` (consistent with the existing `web/src/lib/api/*.ts` direct-to-API pattern); Spring Security CORS is configured to allow the web origin (`WEB_ORIGIN` env) with `allowCredentials=true`.
- **Standalone from 006 (F12-AC3):** this is an admin-only mechanism. It introduces **no** `client` user table, no per-client scoping, no tenant model. The `admin_credential` table holds exactly one admin identity, not a general user table. Feature 006 will build its own per-client auth; 012 deliberately does not lay a "shared foundation" that would presuppose 006's shape. The only thing 006 could reuse is the Spring Security setup itself — a framework, not a schema.
- **Local-first (F12-AC26):** no external IdP, OAuth provider, or managed auth service. The initial secret comes from env; sessions from the already-present Redis. Runs under `docker compose up` with zero external accounts.
- **Testability (production-readiness bar — auth is heavily tested):** login success/failure, 401-on-unauthenticated-mutation, 403/CSRF-reject, logout invalidation, public-endpoints-stay-open, **first-boot seeding**, and **password-change (accept / wrong-current-password / policy-reject)** are all deterministic tests. This is the highest-risk area of the feature and carries the heaviest coverage (engineering-standards #3).

#### 2.1.1 Persisted credential store & self-service password change (F12-AC28, F12-AC29) — DEC-021 (amended)

The credential cannot live as an immutable env var, because F12-AC28 requires the admin to change a *known* password in-app with **no redeploy** and F12-AC29 requires the change to **persist** across restarts. Resolution:

- **Storage:** a single-row table **`admin_credential`** (`id`, `username`, `password_hash`, `updated_at`) — schema in §3.6, created by **Flyway V14**. It stores only a **one-way BCrypt hash**, never plaintext, never a reversible form (F12-AC29, engineering-standards #8). The table is constrained to **exactly one row** (single-admin invariant enforced in the schema; see §3.6), so it is a credential singleton, not a general user table (upholds F12-AC3).
- **First-boot seeding (app-level, not Flyway):** on application startup an idempotent seeder runs: **if `admin_credential` is empty**, it inserts one row from `ADMIN_USERNAME` + `ADMIN_PASSWORD_HASH`. **If a row already exists, env does NOT overwrite it.** Seeding is app-level (an `ApplicationRunner`/startup listener), **not** a Flyway seed, because the hash value comes from **env at runtime** and a versioned SQL migration cannot read env — and because Flyway must stay deterministic and content-free of secrets. This keeps local-first intact (F12-AC26): the *initial* secret lives in env, never the repo.
- **First-boot-only, deliberately (env never clobbers a changed hash):** re-reading env on every boot would silently revert an admin's in-app change on the next redeploy — defeating F12-AC28. So env is a **one-time seed source**, and the persisted row is authoritative thereafter. Single-admin simplicity does not fight this: there is exactly one credential, so "seed once, then own it" is unambiguous.
- **Self-service change — `POST /api/admin/password`** (authenticated, `ROLE_ADMIN`, CSRF-protected). Body: current + new password. The service (a) validates the **current** password against the stored hash with `BCryptPasswordEncoder.matches` — **reject on mismatch** (400, does not touch the session); (b) validates the **new** password against policy (non-blank, minimum length, must differ from current); (c) re-hashes the new password with BCrypt and **UPDATEs the single row** (`updated_at = now()`); (d) **never logs plaintext** either password (F12-AC29). The change **takes effect on next login** (F12-AC28) — the current session stays valid; no forced re-login in v1. Contract detail in design.md §A.7.
- **Forgotten password is an ops action in v1 (accepted limitation).** Because env is first-boot-only, rotating `ADMIN_PASSWORD_HASH` alone will **not** change a persisted credential. If the single admin fully forgets the password, recovery is an **ops-level** action: update the `admin_credential` row directly with a freshly generated BCrypt hash (equivalently: clear the row and let the seeder repopulate from env on the next restart). v1 has no email infrastructure, so a self-service *forgotten-password* reset (e.g. an emailed link) is **deferred to v2** — requirements "Out of scope". The in-app change of a *known* password (F12-AC28) is the only self-service recovery in v1.

### 2.2 Trust stats & badges storage (F12-AC12, F12-AC13, F12-AC14, F12-AC15) — DEC-025

The `trust_content` table already exists (Flyway V4/V5, feature 005) with exactly the shape 012 needs: `item_key`, `item_type` (`stat` | `software` | `standard`), `label`, `value`, `unit`, `display_order`, audit timestamps. **012 edits this table directly** — CRUD on its rows plus reorder via `display_order`. No new storage is needed.

**Critical reconciliation with DEC-014.** The requirements (F12-AC12/AC14) list "marquee project count" and "client/contractor names" as trust content the admin edits. Per **DEC-014** these are **derived live from the `project` table** and are deliberately **not** stored in `trust_content`. They are therefore **not editable as trust fields**. They are edited through **Projects CRUD**:

| Requirement item | Where it lives | How the admin changes it |
|---|---|---|
| Years in business, staff count, monthly steel capacity (F12-AC12) | `trust_content` `item_type='stat'` | Trust editor (edit `value`/`label`/`unit`) |
| Software / standards badges (F12-AC13) | `trust_content` `item_type='software'\|'standard'` | Trust editor (create/edit/delete/reorder rows) |
| **Marquee project count** (F12-AC12) | Derived: `count(*) WHERE featurable=TRUE` | **Projects CRUD** — toggle a project's `featurable` flag |
| **Client / contractor names** (F12-AC14) | Derived: `DISTINCT project.client / main_contractor` | **Projects CRUD** — edit a project's `client` / `main_contractor` |

This upholds the single-source-of-truth invariant (a trust number can never disagree with the portfolio). The Trust editor UI **displays** the derived values read-only for context, with a note that they are edited via Projects. **This reconciliation is surfaced to the PM in the handoff** — it satisfies the ACs via the correct mechanism rather than adding a redundant, drift-prone editable copy.

### 2.3 Sample storage (F12-AC16..AC19) — DEC-023

Feature 004 (Sample Explorer) is **not implemented** — there is **no `sample` table** today. 012's Samples CRUD needs somewhere to write. **Decision: 012 materializes the `sample` table now** (schema + migration), and ships admin CRUD against it. 004's public, lead-gated preview/download read path attaches later with **zero schema churn**. This mirrors the established pattern where a table ships ahead of its full consumer (team shipped in 011 designed for 012's CRUD — DEC-020; `trust_content` shipped in 005 designed for 012 — DEC-013).

The table shape matches the `sample` entry already sketched in `ARCHITECTURE.md`'s data model (`slug, title, preview, file, category`), plus the same surrogate `id` / `display_order` / audit-timestamp scaffolding every other content table carries. 004's future lead-gating (F4-AC2/AC3: download requires name/email/company, creates a `lead`) is an endpoint concern that uses the existing `lead` table — it needs **no** change to the `sample` schema. See §3.4 for DDL.

### 2.4 File / image upload storage (F12-AC20, F12-AC21, F12-AC22) — DEC-022

**Local filesystem on a Docker named volume, served by `api`.**

- Uploaded bytes are written to a directory given by `ALEF_UPLOAD_DIR` (default `/var/alef/uploads`), backed by a Docker **named volume** (`alef-uploads`) mounted into the `api` container so uploads survive container rebuilds and a fresh `docker compose up` (as long as the volume exists). This is the local-first choice — no S3/object storage/external account (F12-AC26).
- `api` **serves** the files back statically under the public URL prefix **`/uploads/**`** via a Spring `ResourceHttpRequestHandler` mapped to `ALEF_UPLOAD_DIR`. Public pages reference an uploaded asset by its **public-relative path** (e.g. `/uploads/projects/9f3c1e2a.jpg`), stored verbatim in the record's `image` / `photo` / `preview` / `file` column — exactly the format those columns already hold for seeded assets (`/img/...`). No signed URLs, no CDN in v1.
- **Filenames are randomized** (UUID + validated extension). This prevents collisions, prevents path traversal via the client-supplied name, and avoids leaking original filenames. Files are foldered by `category` (`projects/`, `team/`, `samples/previews/`, `samples/files/`).
- **Validation (F12-AC22):** allowed content types and a max size, enforced server-side; invalid uploads are rejected with a clear RFC 9457 `ProblemDetail`.
  - Images (project image, team photo, sample preview): `image/jpeg`, `image/png`, `image/webp`; max **5 MB**.
  - Documents (sample downloadable file): `application/pdf`; max **25 MB**.
  - Validation checks the declared `Content-Type`, the extension, **and** a magic-byte sniff of the leading bytes (declared content type alone is client-controlled and spoofable). Spring's `spring.servlet.multipart.max-file-size` / `max-request-size` provide a hard first-line size cap (returns 413).
- The upload is a **separate step from the record save** (see §3.5): the admin uploads a file, gets back its URL, then that URL is submitted as a field of the project/team/sample form. This keeps the record write a plain JSON PUT/POST and the upload a focused multipart POST. On mobile (F12-AC27) the same multipart POST is driven by the OS file picker / camera roll — no desktop-only assumption in the API (see §2.7 and design.md §B.5/§B.10).

### 2.5 `kb.changed` event schema (F12-AC25) — DEC-024

On project **create / update / delete**, `api` emits a `kb.changed` event to a **Redis Stream** (stream key `kb.changed`) so feature 001's RAG indexer can re-embed later. Per **DEC-004** and constraint 4 this side effect must **not block the admin save**:

- Emission happens in a **`@TransactionalEventListener(phase = AFTER_COMMIT)`** handler — the admin's HTTP response returns as soon as the DB transaction commits; the `XADD` runs after commit, off the critical path. A publish failure is logged, never surfaced to the admin, and is recoverable (the worker can reconcile from `project.updated_at` — noted in 003 architecture §6).
- **Payload — defined now for 001 to consume later** (Redis Stream field/value map):

  | Field | Example | Meaning |
  |---|---|---|
  | `event` | `kb.changed` | event name (stream is single-purpose but explicit) |
  | `version` | `1` | schema version for forward-compat |
  | `entityType` | `project` | content type that changed |
  | `entityId` | `42` | internal `project.id` |
  | `slug` | `doha-metro-gold-line` | stable public key the worker fetches by |
  | `changeType` | `created` \| `updated` \| `deleted` | what happened |
  | `occurredAt` | `2026-07-12T10:15:30Z` | ISO-8601 emission time |

- **Deliberately a minimal reference, not the full project body.** The worker re-reads the current project from Postgres (source of truth) for `created`/`updated`, or removes vectors for `slug` on `deleted`. A fat event would risk staleness and duplicate the source of truth (constraint 6 spirit: the DB is authoritative).
- **Scope: projects only in v1.** F12-AC25 names projects; the concierge grounds on the project portfolio (001). Team, trust, and sample edits do **not** emit `kb.changed` in v1. If 001 later wants those in the KB, this event extends by `entityType` with no breaking change. 012 does **not** create a consumer group — 001 owns that.

### 2.6 Public-site freshness (F12-AC7, F12-AC11, F12-AC15, F12-AC19) — see PITFALL-007

"Reflected on the live public site without a code change or redeploy" is satisfied by the fact that **all content lives in Postgres and is read live**, plus the **current `force-dynamic` rendering** on the public pages (PITFALL-007: `force-dynamic` is correct under Docker Compose because the API is not available at web-build time):

- **v1 (compose):** public pages are `force-dynamic`, so every request re-fetches from `api`. An admin edit is visible on the **next page load** — effectively immediate. Uploaded files under `/uploads/**` are served the moment they land. **No cache-busting or revalidation machinery is needed.**
- **On deploy (if pages move to ISR `revalidate=60`, per the standing TODO / PITFALL-007):** freshness becomes **eventual** — an edit surfaces within the revalidate window (≤60s). This still meets "no code change or redeploy." No admin action is required to trigger it.
- There is no CDN cache layer in v1, so no purge step. If one is added at deploy, a targeted revalidation/purge on write becomes a future enhancement — explicitly **out of scope here** and noted as a downstream concern.

012 does **not** change the public pages' rendering mode; it depends only on the "read live from Postgres" property that already holds.

### 2.7 Admin UI — responsive, themeable dark tool (F12-AC27, F12-AC30) — DEC-026 (amended)

**The admin console uses a functional dark admin theme — NOT the full public "Dark Prestige" marketing system — and is fully responsive and built on themeable design tokens.** Stated explicitly so Dev does not default silently.

- **Why not the marketing system:** Dark Prestige is a conversion aesthetic tuned for buyers — 64px Cormorant display headlines, 120px section padding, hero grid overlays, diamond frames, sharp-cornered gold buttons. Those choices optimize *persuasion and scale*, not *dense data entry*. An internal CRUD tool needs legibility, form/table density, and clear affordances.
- **What it is instead:** a restrained, cohesive dark theme that **borrows the palette tokens** (Page BG `#07111C`, Surface `#0E2035`, Gold Primary `#C4973A` for primary actions and focus rings, Text Primary `#E8E0D0`, Text Muted `#7A8FA8`) and **Montserrat** for all UI text, but **drops** the marketing-specific patterns: standard form/table density, **border-radius allowed** on inputs/buttons/cards for usability, plain section spacing (not 80–120px), no hero patterns/diamonds, page titles in Montserrat or a light Cormorant at a modest size (not Display XL). It **reuses the shadcn/ui primitives already in the repo** (`Button`, `Select`, `Separator`, `Badge`).
- **Fully responsive (F12-AC27):** the console must be **usable on phone and tablet as well as desktop** — ALEF staff use desktop and mobile roughly equally. Lists, forms, and the upload flow all work at mobile widths, not just desktop. Concretely (full behavior in design.md §B.10): the admin shell's sidebar collapses to a hamburger drawer below the desktop breakpoint; record lists render as **dense tables on desktop and stacked cards on phone**; forms go **single-column** at narrow widths; the upload widget drives the mobile OS file picker / camera roll. Breakpoints match the public app's Tailwind conventions (`sm` 640, `md` 768, `lg` 1024 — e.g. the portfolio grid already uses `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`). **The responsive change is UI-only; it does not alter any API/DTO shape** — pagination and list payloads are the same at every width (the phone renders the same records as cards).
- **Themeable design tokens, no hard-coded hex (F12-AC30):** every admin color is referenced through the existing **named design tokens** exposed in `web/tailwind.config.ts` (`bg-page`, `bg-surface-2`, `text-text-primary`, `text-text-muted`, `text-gold`, `border-gold-subtle`, …) — **never** a raw hex literal, an arbitrary-value color (`bg-[#0E2035]`), or an inline `style` color. Today those tokens resolve to static hex; funneling every admin color through the token layer is the precondition that lets **spec 013** introduce the CSS-variable indirection and a light/dark switch behind the same token names **with no admin-component rework**. **012 does not build the toggle or the variable indirection — that is spec 013's deliverable (a sibling feature, non-blocking).** 012 ships a single (dark) theme via tokens; 013 adds the switch.
- **Result:** clearly "a backend tool that belongs to this brand," responsive, quick to build, no third-party admin template dependency, no second full design system to maintain, and ready for 013's toggle without a rework pass.

Full screen-by-screen UX, responsive breakpoints, and the token discipline are in **design.md**.

## 3. Data model

**Two** new tables (`sample`, `admin_credential`). Everything else is an **existing** table (`project`, `team`, `trust_content`) that 012 gains write access to — no schema change required, because 003/005/011 designed those tables for the 012 write path from day one (DEC-011/012, DEC-013, DEC-020).

### 3.1 `project` — existing (003, Flyway V1/V2), no change

012 writes the full row: `slug` (unique, editable, validated), `name`, `sector`/`status` (app-layer vocabulary validation per DEC-009 — the admin can extend `sector` without a migration by using a new value the app accepts; the Sector/ProjectStatus vocabularies remain the enforcement point), `country`, `image`, `description`, `main_contractor`, `client`, `consultant`, `location`, `scope[]`, `featurable`. `updated_at` is set on every write (the column exists for exactly this — 003 architecture §6). No column added.

### 3.2 `team` — existing (011, Flyway V6/V7), no change

012 writes `name`, `role`, `company`, `email`, `photo`, `display_order`, `active`. Per DEC-020 the recommended "remove from site" action is toggling `active=false` (soft-hide, preserves history); a hard `DELETE` is also supported (F12-AC10) behind confirmation. No column added.

### 3.3 `trust_content` — existing (005, Flyway V4/V5/V11), no change

012 does CRUD on rows keyed by `id`, editing `item_key`, `item_type` (validated against the `ItemType` vocabulary), `label`, `value`, `unit`, `display_order`. No column added. See §2.2 for the derived-facts reconciliation.

### 3.4 `sample` — NEW (Flyway V12 DDL, V13 seed), created by 012

Next free Flyway version for 012's first table is **V12** (existing run through **V11**). Shape matches the `ARCHITECTURE.md` `sample` sketch plus the standard scaffolding:

```sql
-- V12__create_sample.sql
CREATE TABLE sample (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug          TEXT        NOT NULL UNIQUE,        -- clean URL key (LEARNING-001), F12-AC16
    title         TEXT        NOT NULL,               -- display title
    category      TEXT        NOT NULL,               -- app-validated set (DEC-009): the five 004 books
    preview       TEXT,                               -- public-relative preview image path (/uploads/...), nullable
    file          TEXT,                               -- public-relative downloadable file path (/uploads/...), nullable
    display_order INTEGER     NOT NULL DEFAULT 0,     -- ordering on the future Sample Explorer
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Notes:
- `category` is `TEXT`, validated in the app layer against the five 004 sample types (Prequalification profile, BBS, Drawings, Bridge drawings, Roads & utility) — same stance as `project.sector` (DEC-009). This lets the admin/004 add a category without a migration.
- `preview` and `file` are nullable so a sample can be created and have its assets attached in a follow-up edit (graceful degradation, matching the `team.photo`-null pattern).
- **V13 seed is optional** — a small idempotent `INSERT ... ON CONFLICT (slug) DO NOTHING` for the five known books (DEC-012 discipline). The actual content is an implementation/content task; if 004's real files aren't ready, V13 can seed metadata-only rows with null `preview`/`file`, or be omitted. Flagged as a Dev/content decision, not designed here. **V13 is reserved for this seed** so downstream versions stay stable.

### 3.5 No new table for uploads

Uploaded files are filesystem objects (§2.4); their **references** are plain string columns on the records above. There is **no `upload`/`asset` table** in v1 — it would add a join and an orphan-tracking burden for no v1 benefit. (A future asset-library/garbage-collection feature could introduce one; out of scope.)

### 3.6 `admin_credential` — NEW (Flyway V14), created by 012 (F12-AC28, F12-AC29)

The persisted, runtime-mutable single admin credential (§2.1.1). **Next free Flyway version after V12 (sample DDL) + V13 (reserved sample seed) is `V14`.**

```sql
-- V14__create_admin_credential.sql
CREATE TABLE admin_credential (
    id            SMALLINT    PRIMARY KEY DEFAULT 1,      -- singleton row
    username      TEXT        NOT NULL,                   -- the one admin login name
    password_hash TEXT        NOT NULL,                   -- BCrypt hash ONLY; never plaintext (F12-AC29)
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),     -- set on every password change
    CONSTRAINT admin_credential_singleton CHECK (id = 1) -- v1 is single-admin: at most one row
);
```

Design notes:
- **Single-row invariant in the schema.** The `PRIMARY KEY(id)` + `CHECK (id = 1)` pair makes it structurally impossible to hold more than one credential, which documents the single-admin scope (F12-AC3) at the DB level. v2 (multiple admins) would drop the `CHECK`, relax the PK to identity, and add a `UNIQUE(username)` — an additive migration, not a rebuild.
- **No plaintext, ever** — `password_hash` stores a BCrypt hash only. There is no reversible column, no last-plaintext, no audit of the value (F12-AC29, engineering-standards #8).
- **Seeding is app-level, not a Flyway seed** (see §2.1.1): the migration creates the empty table; a startup seeder inserts row `id=1` from `ADMIN_USERNAME` + `ADMIN_PASSWORD_HASH` **only if the table is empty**. This keeps the secret out of the migration history and lets the in-app change survive restarts.
- **`updated_at`** gives a minimal "when did the password last change" signal without a full audit trail (audit trail is Out of scope for v1, revisit at v2 multi-admin — requirements Open Question #2).

## 4. API contract (admin surface)

All endpoints below are under **`/api/admin/**`**, require **`ROLE_ADMIN`**, and are guarded by Spring Security (401 unauthenticated, 403 authenticated-but-forbidden / CSRF fail). All responses are JSON; all errors are RFC 9457 `ProblemDetail` (matching `LeadExceptionHandler` / `PortfolioExceptionHandler`). Package layout follows the established convention: a new `com.alef.api.admin.*` security package, and admin controllers/services either extend the existing feature packages (`portfolio`, `team`, `trust`) with an `admin` sub-package or live in a cohesive `com.alef.api.admin.<resource>` — Dev's call, but reuse the existing entities/repositories (do not duplicate them).

Detailed request/response DTOs, field-level validation, and status codes are in **design.md §API**. Summary:

### 4.1 Session / auth & account
| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| POST | `/api/admin/session` | `{ username, password }` | 200 `{ authenticated:true, username }` + `ALEFADMIN` cookie | 401 bad credentials, 429 rate-limited |
| GET | `/api/admin/session` | — | 200 `{ authenticated:true, username }` | 401 no/invalid session |
| DELETE | `/api/admin/session` | — | 204, session invalidated | 401 |
| **POST** | **`/api/admin/password`** | **`{ currentPassword, newPassword }`** | **204, hash updated (takes effect next login)** | **400 wrong current password / policy fail, 401 no session, 403 CSRF fail** |

`POST /api/admin/password` (F12-AC28) is authenticated + CSRF-protected: it validates `currentPassword` against the stored hash (reject on mismatch), enforces the new-password policy, re-hashes with BCrypt, and UPDATEs the single `admin_credential` row. It never logs plaintext (F12-AC29). A fully-forgotten password is an ops action in v1 (update the row / re-seed via env — §2.1.1); self-service forgotten-password recovery is v2's.

### 4.2 Projects (F12-AC4..AC7) — emits `kb.changed`
| Method | Path | Notes |
|---|---|---|
| GET | `/api/admin/projects` | Full admin list (all fields, incl. non-featurable), paged |
| GET | `/api/admin/projects/{id}` | Full record by internal id (admin edits by id, since `slug` is editable) |
| POST | `/api/admin/projects` | Create; validates slug uniqueness + vocab; **emits `kb.changed` created** |
| PUT | `/api/admin/projects/{id}` | Full update; sets `updated_at`; **emits `kb.changed` updated** |
| DELETE | `/api/admin/projects/{id}` | Delete; **emits `kb.changed` deleted** |

### 4.3 Team (F12-AC8..AC11)
| Method | Path | Notes |
|---|---|---|
| GET | `/api/admin/team` | All rows incl. `active=false` |
| GET | `/api/admin/team/{id}` | Single |
| POST | `/api/admin/team` | Create |
| PUT | `/api/admin/team/{id}` | Update (incl. `active` toggle, `display_order`) |
| DELETE | `/api/admin/team/{id}` | Hard delete (UI recommends soft-hide via `active`) |

### 4.4 Trust content (F12-AC12..AC15)
| Method | Path | Notes |
|---|---|---|
| GET | `/api/admin/trust` | All `trust_content` rows grouped by `item_type`; plus read-only derived facts (marquee count, clients, contractors) for context |
| POST | `/api/admin/trust` | Create a stat/software/standard row |
| PUT | `/api/admin/trust/{id}` | Update (label/value/unit/display_order) |
| DELETE | `/api/admin/trust/{id}` | Delete a row |

### 4.5 Samples (F12-AC16..AC19)
| Method | Path | Notes |
|---|---|---|
| GET | `/api/admin/samples` | All rows |
| GET | `/api/admin/samples/{id}` | Single |
| POST | `/api/admin/samples` | Create; validates slug uniqueness + category vocab |
| PUT | `/api/admin/samples/{id}` | Update |
| DELETE | `/api/admin/samples/{id}` | Delete |

### 4.6 Uploads (F12-AC20..AC22)
| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| POST | `/api/admin/uploads` | multipart: `file`, `category` ∈ {`project-image`,`team-photo`,`sample-preview`,`sample-file`} | 201 `{ url, filename, contentType, size }` | 400 bad type / missing category, 413 too large |

Public read endpoints (`/api/projects/**`, `/api/trust/overview`, `/api/team`, `/api/offices`) are **unchanged and unauthenticated**; the admin list/detail endpoints are separate because they return the **full** internal record (all fields, `active=false` rows, internal `id`) that the public projections deliberately omit (DEC-011).

## 5. Component boundaries

| Concern | Lives in | Notes |
|---|---|---|
| Security config, `UserDetailsService`, session, CSRF, CORS | `api` `com.alef.api.admin.security` | Spring Security; `UserDetailsService` reads the persisted `admin_credential`, not env (DEC-021) |
| Credential seeder (first-boot, env → store) | `api` `com.alef.api.admin.security` (startup `ApplicationRunner`) | seeds `admin_credential` from env only if empty; never overwrites (§2.1.1) |
| Password-change endpoint/service | `api` `com.alef.api.admin.security` (or `.account`) | `POST /api/admin/password`; validate current, BCrypt-rehash, UPDATE row (DEC-021) |
| Admin CRUD controllers/services | `api` per-resource admin packages | reuse existing entities/repositories |
| `kb.changed` producer | `api` `com.alef.api.admin` (project write path) | AFTER_COMMIT event listener → Redis `XADD` (DEC-024) |
| Upload storage + static serving | `api` `com.alef.api.admin.upload` + resource handler | local volume, `/uploads/**` (DEC-022) |
| `sample` schema | `api` Flyway V12 (+ optional V13 seed) | new table (DEC-023) |
| `admin_credential` schema | `api` Flyway V14 | new singleton table (DEC-021 amended, §3.6) |
| Admin UI (login, lists, forms, account) | `web` `/admin/**` + middleware guard | responsive, themeable-token dark theme (DEC-026 amended) |
| Session cookie / CORS-credentials wiring | `api` security + `web` fetch `credentials:'include'` | direct-to-API, existing client pattern |

## 6. New environment variables

| Variable | Required | Default | Notes |
|---|---|---|---|
| `ADMIN_USERNAME` | yes (admin) | none | **first-boot seed** for the single admin login; persisted to `admin_credential` on first boot, authoritative thereafter (not re-read after seeding) |
| `ADMIN_PASSWORD_HASH` | yes (admin) | none | **first-boot seed** — a **BCrypt hash** of the admin password; never plaintext, never in repo. Seeds `admin_credential` only when the table is empty; does **not** override an in-app change (§2.1.1) |
| `WEB_ORIGIN` | yes | `http://localhost:3000` | CORS allowed origin for credentialed admin requests |
| `ALEF_UPLOAD_DIR` | no | `/var/alef/uploads` | filesystem dir for uploads; backed by the `alef-uploads` volume |

These are additions to the env table in CLAUDE.md (the orchestrator updates CLAUDE.md after the handoff, not me).

## 7. New dependencies & infra

- **api `pom.xml`:** add `spring-boot-starter-security` and `spring-session-data-redis`. (Redis, JPA, validation, Flyway already present.) **No new dependency is needed for password hashing or the change endpoint** — `BCryptPasswordEncoder` ships with `spring-security-crypto` (transitively via the security starter).
- **api `application.yml`:** add `spring.servlet.multipart.max-file-size` / `max-request-size`; Spring Session store type = redis; the admin/CORS/upload-dir values read from env.
- **Flyway:** `V12__create_sample.sql`; optional `V13` sample seed; **`V14__create_admin_credential.sql`** (§3.6). Existing history runs through V11.
- **docker-compose:** add a named volume `alef-uploads` mounted at `ALEF_UPLOAD_DIR` in the `api` service; set the four env vars for `api`. **This compose change is infra's to apply** — flagged in the handoff. It is required for F12-AC26 (uploads must persist locally, and the admin credential must seed from env on first boot).

## 8. Risks & mitigations

- **Auth is the highest-risk surface.** Mitigated by a minimal attack surface (single credential, one-row `admin_credential` table, no self-registration), Redis-backed sessions, CSRF on, and heavy risk-based tests on every auth path (engineering-standards #3). The BCrypt-hash-only rule keeps plaintext out of the store and the seed hash out of the repo.
- **Credential-store seeding correctness.** The seeder must be **idempotent and first-boot-only** — insert only when `admin_credential` is empty, never overwrite. Risk: a naive "upsert from env on every boot" would silently revert an in-app change on the next redeploy (breaks F12-AC28). Mitigated by the empty-check design (§2.1.1) and a dedicated test (env does not clobber a changed hash across a restart).
- **Password-change abuse / mistakes.** Requires the current password (reject on mismatch, 400), a new-password policy, CSRF, and an authenticated session. Never logs plaintext. Wrong current password does **not** invalidate the session (it is a request validation failure, not a session failure).
- **Forgotten password with no email in v1.** Accepted limitation: recovery is an ops action (update the `admin_credential` row / clear-and-re-seed via env). Documented in §2.1.1 and the handoff; self-service forgotten-password recovery is deferred to v2 (requirements Out of scope).
- **CSRF with cookie auth.** Mitigated by enabling Spring Security CSRF for `/api/admin/**` and requiring the `X-XSRF-TOKEN` header on mutating requests (including `POST /api/admin/password`); documented for Dev so the web client sends it.
- **Upload abuse (huge files, wrong types, path traversal, executable content).** Mitigated by hard multipart size caps (413), content-type + extension + magic-byte validation (400), UUID filenames (no client-controlled path), and category-scoped subfolders. `/uploads/**` serves static bytes only — no execution.
- **Orphaned uploads** (file uploaded, record never saved / asset replaced). Accepted for v1 — files are cheap and local; a cleanup/garbage-collection job is a future enhancement, not a v1 need. Noted, not built.
- **`kb.changed` published but never consumed** (001 not built). By design (approved emit-now/consume-later). The stream simply accumulates entries; when 001 creates the consumer group it can start from the beginning or the tail. No harm in the interim; log volume is trivial.
- **Trust derived-facts confusion** (admin expects to type a client name into a "trust" field). Mitigated by showing derived facts read-only in the Trust editor with an explicit "edited via Projects" note (§2.2), and surfaced to the PM in the handoff.
- **Slug edits break inbound links / RAG.** Editing a `project.slug` changes its public URL and its `kb.changed` key. Mitigated: `updated`/`deleted` events carry the (new) slug; a slug change is an `updated` event. Old-slug redirects are out of scope for v1 (no requirement) — noted.
- **Responsive regressions (F12-AC27).** The console must work at phone/tablet widths, not just desktop. Mitigated by matching the public app's Tailwind breakpoints (`sm`/`md`/`lg`) and a table→stacked-card pattern for lists (design.md §B.10); the upload flow uses the mobile OS file picker with the same API. Risk is UI-only — no DTO/API change — so it is covered by responsive layout review, not new API tests.
- **Theme-token leakage (F12-AC30).** Risk: a stray hard-coded hex or arbitrary-value color would not flip under spec 013's toggle. Mitigated by the "named tokens only, no raw hex" rule (design.md aesthetic section) — enforceable by review/lint. 012 does not build 013's variable indirection; it only guarantees no color bypasses the token layer.
- **Single admin, no audit trail** (Out of scope, Open Question #2). Accepted for v1; flagged for revisit before multiple admins are onboarded (in-app password change + eventual v2 multi-admin make an audit trail more warranted at that point — carried forward from the requirements' open question).

## 9. PROPOSED ARCHITECTURE.md CHANGES (not applied — human approval required)

I do not edit `ARCHITECTURE.md`. The following changes are proposed for a human to review and apply. (Additive throughout; no constraint is weakened.)

**9.1 Data model — materialize the `sample` entry with scaffolding.** Current:
```
sample   { slug, title, preview, file, category }
```
Proposed:
```
sample   { id, slug, title, category, preview, file, display_order,
           created_at, updated_at }
```
Rationale: 012 creates this table (DEC-023) with the same surrogate-id / display_order / audit scaffolding every other content table carries. Field addition only.

**9.2 Data model — add the `admin_credential` entry (NEW, from the v1.1 amendment).** Add:
```
admin_credential { id, username, password_hash, updated_at }   // single row; BCrypt hash only
```
Rationale: F12-AC28/AC29 require the admin credential to be persisted and runtime-mutable (seeded from env on first boot, changed via `POST /api/admin/password`). Single-row singleton (v1 single-admin); v2 multi-admin relaxes it additively. This is a new system-level data-model entry, so it is called out separately for the human.

**9.3 Components / Constraints — note the admin auth layer, credential store, and upload storage.** Add (human's placement choice):
- **admin auth**: `api` runs Spring Security with a **single admin credential persisted in `admin_credential`, seeded from env (`ADMIN_USERNAME` / `ADMIN_PASSWORD_HASH`) on first boot only and mutable at runtime** via `POST /api/admin/password`, plus Redis-backed sessions. Admin-only, standalone; does not presuppose feature 006's per-client auth.
- **upload storage**: uploaded images/files are stored on a local filesystem volume (`ALEF_UPLOAD_DIR`, Docker named volume `alef-uploads`) and served by `api` under `/uploads/**`. No object storage / external account (upholds constraint 1 & 7).

**9.4 Redis roles.** The existing note "Redis serves three roles (streams, cache, rate limiting)" now also covers the **admin session store**. Suggest updating the wording to include sessions.

**9.5 `kb.changed` event schema.** Optionally record the event payload (§2.5) in `ARCHITECTURE.md` (or leave it to this spec + DEC-024) so feature 001's consumer has a single canonical reference. Recommend at least a pointer to DEC-024.

No **constraint** in `ARCHITECTURE.md` is weakened — every change is additive and consistent with local-first (1), Java backend (2), event-driven side effects (4/DEC-004), and the low-cost upgrade path (7). The env-seed-only credential upholds "secrets in env not repo" (engineering-standards #8) while enabling runtime mutability.

## 10. PROPOSED DECISIONS (for the keeper to append to DECISIONS.md — I do not write it)

Next free id is **DEC-021** (highest committed in DECISIONS.md is DEC-020). **DEC-021..DEC-026 are still PROPOSED here and NOT yet appended to DECISIONS.md.** DEC-021 and DEC-026 below are the **amended** proposals for requirements v1.1 (replacing the v0.1 wording in-place — nothing has been committed to the append-only file yet, so this is a proposal revision, not an edit of a committed record).

- **DEC-021 (amended for v1.1): Admin auth is Spring Security with a single admin credential persisted in a one-row `admin_credential` table — seeded from env (`ADMIN_USERNAME` + `ADMIN_PASSWORD_HASH` BCrypt) on first boot only, mutable at runtime via `POST /api/admin/password` — plus Redis-backed sessions; standalone from feature 006.**
  Why: satisfies F12-AC1/AC2/AC3/AC26 **and** F12-AC28/AC29 with the smallest attack surface and full local-first compliance. The credential must be persisted and runtime-mutable so the single admin can change a *known* password with no redeploy (F12-AC28) and have it survive restarts (F12-AC29); env therefore **seeds once** (first-boot, when the store is empty) rather than being re-read every boot, so an in-app change is never silently reverted. `UserDetailsService` reads the store, not env. Only a BCrypt hash is ever stored or seeded — no plaintext (engineering-standards #8). Single-admin stays in scope; multiple accounts and self-service *forgotten*-password recovery are deferred to v2 (v1 has no email; a fully forgotten password is an ops-level row update / env re-seed). The one-row table is a credential singleton (`CHECK id=1`), not a general user table, so it does not presuppose 006's per-client shape (F12-AC3). Redis-backed sessions survive restart and keep a horizontal-scale door open (constraint 7). Alternatives rejected: **immutable env-only credential re-read on every boot** (cannot satisfy F12-AC28 — a password change can't persist without editing env/redeploying; the original v0.1 stance, reversed by the v1.1 requirement); **stateless JWT** (no easy revocation/logout without a denylist, secret sprawl, no benefit for one admin); **a general `user` table / shared 006 foundation** (presupposes 006's unspecified per-client shape — F12-AC3 forbids the dependency; the reusable part is the framework, not a schema); **external IdP / managed auth** (external account, violates local-first).

- **DEC-022: Uploaded files are stored on a local filesystem Docker volume and served by `api` under `/uploads/**`; references are plain string columns, no asset table.**
  Why: F12-AC20/AC21/AC26 — local-first with zero external accounts; `api` is the content service, so it owns both storage and serving; UUID filenames + type/size/magic-byte validation harden it. The same multipart endpoint serves the mobile file-picker/camera-roll path (F12-AC27) with no change. Alternatives rejected: **bytes in Postgres** (row bloat, backup/restore cost, no benefit at this scale); **S3/object storage** (external account, violates local-first); **serving from `web/public`** (splits content ownership, needs a redeploy or a shared mount); **an `upload`/`asset` table** (join + orphan-tracking burden with no v1 payoff).

- **DEC-023: Feature 012 materializes the `sample` table ahead of feature 004's public Sample Explorer.**
  Why: F12-AC16..AC19 need a write target and 004 is unbuilt; shipping the table now (matching the `ARCHITECTURE.md` shape) lets 004's public gated read attach later with zero schema churn — the same table-ahead-of-consumer pattern as DEC-020 (team) and DEC-013 (`trust_content`). Alternative rejected: **block Samples CRUD on 004 shipping first** (needlessly couples a content-management feature to an unscheduled read feature; the schema is already defined in `ARCHITECTURE.md`).

- **DEC-024: `kb.changed` is emitted on project create/update/delete via a Redis Stream after DB commit, with a minimal reference payload; the consumer is feature 001's.**
  Why: F12-AC25 + DEC-004 + constraint 4 — the side effect must not block the admin save, so emission is `AFTER_COMMIT` and fire-and-forget; a minimal payload (entityType/id/slug/changeType/occurredAt/version) keeps the DB the source of truth and lets the future worker re-read fresh data. Emit-now/consume-later is approved. v1 emits for projects only. Alternatives rejected: **synchronous re-embed** (blocks save, violates constraint 4); **fat event carrying the full project** (staleness, duplicates the source of truth); **emitting for all content types now** (no v1 consumer; extends cleanly by `entityType` when 001 needs it).

- **DEC-025: Admin trust editing operates directly on `trust_content` rows; derived trust facts (marquee count, client/contractor names) are edited through Projects CRUD, never as trust fields.**
  Why: reconciles F12-AC12/AC14 with DEC-014's single-source-of-truth rule — the marquee count and client/contractor names are derived live from `project`, so making them editable trust fields would create drift. The Trust editor shows them read-only with an "edited via Projects" note. Alternative rejected: **a redundant editable copy in `trust_content`** (reintroduces the exact drift DEC-014 prevents).

- **DEC-026 (amended for v1.1): The admin console uses a functional dark admin theme (palette tokens + Montserrat, marketing patterns dropped) that is fully responsive and built entirely on themeable design tokens.**
  Why: F12-AC27 + F12-AC30. The admin is an internal data-entry tool, so Dark Prestige's persuasion-tuned display type, hero patterns, and sharp-corner buttons hurt CRUD legibility and density; borrowing the palette + Montserrat and reusing existing shadcn/ui primitives keeps it on-brand and cheap without a second design system. It must be **fully responsive** — ALEF staff use desktop and mobile roughly equally, so lists (table→stacked-card), forms (single-column at narrow widths) and the upload flow (mobile file picker / camera roll) all work at phone/tablet widths, matching the public app's `sm`/`md`/`lg` Tailwind breakpoints (this reverses the v0.1 "desktop-only" stance per the stakeholder direction). Every admin color flows through the **named design tokens, no hard-coded hex**, so the sibling **spec 013** light/dark toggle can layer on behind the same tokens with no rework (012 builds the tokens, 013 builds the switch). Alternatives rejected: **full marketing system** (wrong density/affordances for forms and tables); **desktop-only** (reversed by F12-AC27 — staff use mobile); **hard-coded palette hex in admin components** (would not flip under 013's toggle, breaks F12-AC30); **a third-party admin template** (extra dependency, visually incoherent with the brand, and not token-compatible with 013).
