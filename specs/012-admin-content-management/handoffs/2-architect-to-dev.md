# Handoff: Admin Content Management

| Field | Value |
|---|---|
| Feature # | 012 |
| From / To | Architect -> Dev |
| Status | ready (pending human approval of proposed ARCHITECTURE.md / DECISIONS.md changes) |
| Spec | specs/012-admin-content-management/architecture.md, specs/012-admin-content-management/design.md |

## Context

012 adds the first authenticated, mutating surface to the system: an internal admin console for non-technical ALEF staff to manage projects, team, trust stats/badges, and samples, plus the image/file uploads those records reference. Every content type already has a public read path (003 projects, 005 trust, 011 team/office); 012 adds the write side behind auth and reuses those tables unchanged. Requirements are PM-approved (v1.0), acceptance ids F12-AC1..AC27.

## What was done

Produced architecture.md and design.md. Key decisions (each traces to F12-ACn; full rationale + rejected alternatives in architecture.md §10):

- **Auth (DEC-021):** Spring Security, a single admin credential from env (`ADMIN_USERNAME` + `ADMIN_PASSWORD_HASH` BCrypt), Redis-backed server-side session, HttpOnly `ALEFADMIN` cookie, CSRF on for `/api/admin/**`. Standalone from 006 (no client user table). No JWT, no external IdP.
- **Trust storage (DEC-025):** admin edits the existing `trust_content` table directly. Derived facts — marquee count, client/contractor names — are **not** editable trust fields; they are changed via Projects CRUD (upholds DEC-014). Trust screen shows them read-only.
- **Samples (DEC-023):** 012 **creates** the `sample` table (Flyway **V12** DDL, optional V13 seed) ahead of feature 004; 004's public gated read attaches later with no schema churn.
- **Uploads (DEC-022):** local filesystem on a Docker named volume `alef-uploads` at `ALEF_UPLOAD_DIR` (default `/var/alef/uploads`), served by `api` under `/uploads/**`; UUID filenames; type + size + magic-byte validation (images ≤5 MB, PDF ≤25 MB). Upload is a separate step from record save.
- **kb.changed (DEC-024):** emitted on project create/update/delete only, via Redis Stream `kb.changed`, `AFTER_COMMIT`, minimal reference payload (`event, version, entityType, entityId, slug, changeType, occurredAt`). Consumer is 001's — emit now, consume later.
- **Freshness:** satisfied by force-dynamic + live Postgres reads; edits visible on next load. No cache machinery in v1 (PITFALL-007).
- **Aesthetic (DEC-026):** functional dark admin theme — public palette tokens + Montserrat, standard density, border-radius allowed, no marketing hero patterns/serif display. Reuse existing shadcn/ui primitives. Desktop-only.

## What the receiving role (Dev) must do

1. **Do not start any task until a task is selected from the active plan and cites an acceptance id** (F12-ACn) — invariant #4. Implementation phase must produce implementation.md (Dev owns it, not me).
2. **api:** add `spring-boot-starter-security` + `spring-session-data-redis` to `pom.xml`; add the security config (`com.alef.api.admin.security`), the `/api/admin/**` CRUD controllers/services (reuse existing entities/repositories in `portfolio`, `team`, `trust` — do not duplicate them), the multipart upload endpoint + `/uploads/**` resource handler, the `sample` module (entity/repo/service/controller), and the `kb.changed` `AFTER_COMMIT` producer on the project write path.
3. **Flyway:** `V12__create_sample.sql` per architecture §3.4; optional `V13` seed (idempotent `ON CONFLICT (slug) DO NOTHING`). Next free version is V12 (existing run through V11).
4. **application.yml:** multipart size caps, Spring Session store = redis, env-driven admin/CORS/upload-dir values.
5. **web:** `/admin/**` route tree, middleware guard, admin shell, list/form/upload/delete-confirmation UI per design.md Part B; typed API clients under `web/src/lib/api/admin*` mirroring the existing client pattern; call the API with `credentials:'include'` and send `X-XSRF-TOKEN` on mutations.
6. **Tests (risk-based, engineering-standards #3):** auth is the heaviest — login success/failure, 401 on unauthenticated mutation, 403/CSRF reject, logout invalidation, public endpoints stay open, login rate-limit. Then CRUD validation (required fields, slug/item_key uniqueness 409, vocab 400), upload validation (type/size/magic-byte), and the `kb.changed` emission (after-commit, correct payload). Full suite must pass before close (gate-tests hook).
7. **Doc comment every function** (intent + gotcha), per engineering-standards #2.

## Blocking / open questions for the human (surface before Dev closes)

1. **PROPOSED ARCHITECTURE.md changes (§9)** — human-approval-only file, I did not touch it. Needs approval: (a) materialize the `sample` data-model row with `id/display_order/created_at/updated_at`; (b) note the admin auth layer + upload-storage volume under Components/Constraints; (c) update the "Redis serves N roles" wording to include the admin **session store**; (d) optionally record the `kb.changed` payload (or point to DEC-024).
2. **PROPOSED DECISIONS DEC-021..DEC-026** (append-only DECISIONS.md, I did not write it) — listed verbatim in architecture.md §10 for the keeper to append.
3. **docker-compose change (infra):** add the `alef-uploads` named volume mounted at `ALEF_UPLOAD_DIR` in the `api` service, and set the four new env vars. Required for F12-AC26; it is an infra edit, not mine.
4. **Trust AC reconciliation (DEC-025):** F12-AC12/AC14 literally list "marquee project count" and "client/contractor names" as editable trust content, but per DEC-014 they are derived and edited via Projects. My design satisfies the ACs through that mechanism (read-only display + edit-via-Projects). **PM should confirm** this reading is acceptable rather than expecting a directly-editable trust field.
5. **New env vars** (`ADMIN_USERNAME`, `ADMIN_PASSWORD_HASH`, `WEB_ORIGIN`, `ALEF_UPLOAD_DIR`) — the orchestrator updates CLAUDE.md's env table after this handoff (not me).

## Read first

- specs/012-admin-content-management/architecture.md and design.md (this feature)
- specs/003-portfolio-filtering/architecture.md (project table + conventions the admin write path reuses)
- specs/005-trust-layer/architecture.md (trust_content shape; derived-facts rule)
- docs/knowledge/DECISIONS.md (DEC-004 events, DEC-009 vocab, DEC-011 projections, DEC-014 derived facts, DEC-020 team-for-012)
- docs/knowledge/LEARNINGS.md (PITFALL-007 force-dynamic; PITFALL-006 Ollama autoconfig exclusion in tests)
- Existing conventions to match: api/src/main/java/com/alef/api/lead/ (ProblemDetail handler, Redis rate-limit), .../office/entity/OfficeEntity.java (TEXT[] mapping idiom), .../portfolio/ (controller/service/repository shape)

## Do not touch

- docs/knowledge/ARCHITECTURE.md (human-approval-only; propose via architecture.md §9 — done).
- docs/knowledge/DECISIONS.md, LEARNINGS.md (append-only; keeper appends DEC-021..DEC-026).
- CLAUDE.md (orchestrator updates it after handoff).
- requirements.md, implementation.md, testing.md (not the Architect's to write).
- The settled patterns: local-first, Java + Spring AI backend, event-driven side effects via Redis Streams, provider-swap. Do not revisit.
