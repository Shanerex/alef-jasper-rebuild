# Handoff: Admin Content Management (architecture amendment for requirements v1.1)

| Field | Value |
|---|---|
| Feature # | 012 |
| From / To | Architect -> Dev |
| Status | ready (pending human approval of proposed ARCHITECTURE.md / DECISIONS.md changes) |
| Spec | specs/012-admin-content-management/architecture.md (v0.2), specs/012-admin-content-management/design.md (v0.2) |
| Supersedes | specs/012-admin-content-management/handoffs/2-architect-to-dev.md |

## Context

The human stakeholder revised requirements to **v1.1 (approved)** before approving the draft architecture, directing two settled scope changes plus one forward-looking constraint (see handoffs/3-pm-to-architect.md). architecture.md and design.md are now **v0.2** (still `status: draft`, awaiting human approval of the proposed knowledge-layer changes). **This handoff supersedes handoff 2** — build from v0.2.

## What changed since handoff 2 (delta only — everything else in handoff 2 still stands)

1. **Persisted, seeded, runtime-mutable admin credential (DEC-021 amended, F12-AC28/AC29).** The credential is **no longer** an immutable env var read on every boot. It now lives in a **new one-row table `admin_credential`** (`id`, `username`, `password_hash`, `updated_at`; `CHECK id=1` singleton). It is **seeded from `ADMIN_USERNAME` + `ADMIN_PASSWORD_HASH` on first boot only** (app-level seeder — insert only if the table is empty; **never overwrite** an existing row) and is **authoritative thereafter**. The `UserDetailsService` reads the persisted store, not env. Only a BCrypt hash is ever stored — no plaintext (F12-AC29, engineering-standards #8). Architecture §2.1.1, §3.6.
2. **New endpoint `POST /api/admin/password` (F12-AC28).** Authenticated + CSRF-protected. Validates `currentPassword` against the stored hash (reject → 400, session untouched), enforces the new-password policy (**non-blank, ≥ 12 chars, must differ from current**), BCrypt-rehashes, `UPDATE admin_credential ... WHERE id=1`, returns 204; takes effect on next login. Never logs plaintext. Design §A.7 (contract) + §B.9 (UI).
3. **New Flyway migration — `V14__create_admin_credential.sql`.** DDL only (empty table); the hash is seeded at app startup, **not** in Flyway (the hash comes from env at runtime). See "Flyway versions" below.
4. **Fully responsive admin (DEC-026 amended, F12-AC27).** Reverses the old desktop-only stance. Lists collapse **table → stacked cards** below `md`; forms go **single-column** below `md`; the shell sidebar collapses to a **hamburger drawer** below `lg`; the upload flow uses the **mobile OS file picker / camera roll**. Breakpoints match the public app (`sm` 640 / `md` 768 / `lg` 1024). **UI-only — no API/DTO change.** Design §B.10 (and per-screen notes).
5. **Themeable design tokens, no hard-coded hex (DEC-026 amended, F12-AC30).** Every admin color **must** go through the named tokens already in `web/tailwind.config.ts` (`bg-page`, `bg-surface-2`, `text-text-primary`, `text-text-muted`, `text-gold`, `border-gold-subtle`, …). **No raw hex, no arbitrary-value color (`bg-[#0E2035]`), no inline `style` color anywhere in admin components.** This is the precondition for the sibling **spec 013** light/dark toggle. Design "Aesthetic" section + §B.10.
6. **Spec 013 (light/dark toggle) is a sibling, NON-BLOCKING dependency.** 012 does **not** design or build the toggle or any CSS-variable indirection. 012 ships one dark theme via tokens; 013 adds the switch behind those same tokens. Do not build 013 here.
7. **Two new tables now, not one.** `sample` (V12, + optional V13 seed) **and** `admin_credential` (V14).

Unchanged from handoff 2 and still in force: DEC-022 (local upload volume `/uploads/**`), DEC-023 (materialize `sample`), DEC-024 (`kb.changed` AFTER_COMMIT, projects only, minimal payload), DEC-025 (trust derived facts edited via Projects), the Spring Security + Redis-session + `ALEFADMIN` cookie + CSRF core of DEC-021, and the `force-dynamic` freshness story.

## Flyway versions (existing history runs through V11)

- `V12__create_sample.sql` — `sample` DDL (architecture §3.4).
- `V13` — **reserved** for the optional idempotent `sample` seed (`ON CONFLICT (slug) DO NOTHING`); may seed metadata-only rows or be omitted (Dev/content call). Keep it reserved so later versions stay stable.
- `V14__create_admin_credential.sql` — `admin_credential` DDL, empty table (architecture §3.6). **Seeding is app-level, not in this migration.**

## What the receiving role (Dev) must do

1. **Do not start any task until it is selected from the active plan and cites an acceptance id** (F12-ACn) — invariant #4. Implementation phase produces `implementation.md`, which **Dev owns** (not the Architect). Do not write requirements.md or testing.md.
2. **api — security & account:** add `spring-boot-starter-security` + `spring-session-data-redis` (`pom.xml`). Build `com.alef.api.admin.security`: security config (lock down `/api/admin/**`, permit public reads + `/uploads/**`, CSRF on, CORS `WEB_ORIGIN` + `allowCredentials`), a `UserDetailsService` reading `admin_credential`, the **first-boot seeder** (`ApplicationRunner`: insert row `id=1` from env only if the table is empty — never overwrite), and the **`POST /api/admin/password`** endpoint/service (validate current, policy-check new, BCrypt-rehash, UPDATE, never log plaintext). `BCryptPasswordEncoder` ships with the security starter — **no extra dependency** for hashing.
3. **api — CRUD & uploads:** the `/api/admin/**` CRUD controllers/services (reuse existing entities/repositories in `portfolio`, `team`, `trust` — do not duplicate them), the `sample` module (entity/repo/service/controller), the multipart upload endpoint + `/uploads/**` resource handler, and the `kb.changed` `AFTER_COMMIT` producer on the project write path.
4. **Flyway:** `V12` (sample DDL), reserved `V13` (optional sample seed), `V14` (`admin_credential` DDL) — per the versions above.
5. **application.yml:** multipart size caps, Spring Session store = redis, env-driven admin-seed / CORS / upload-dir values.
6. **web:** `/admin/**` route tree incl. `/admin/account` (change password), middleware guard, responsive admin shell (drawer < `lg`), list/form/upload/delete-confirmation UI per design.md Part B, the **table→stacked-card** responsive lists and **single-column** responsive forms per §B.10. Typed API clients under `web/src/lib/api/admin*` mirroring the existing client pattern; call the API with `credentials:'include'` and send `X-XSRF-TOKEN` on mutations (including the password change). **All colors via named tokens — zero hard-coded hex** (F12-AC30).
7. **Tests (risk-based, engineering-standards #3):** auth is heaviest — login success/failure, 401 on unauthenticated mutation, 403/CSRF reject, logout invalidation, public endpoints stay open, login rate-limit. **New:** first-boot seeding (seeds when empty; **does not overwrite a changed hash across a restart** — this protects F12-AC28); password change (accept, wrong-current-password → 400 with session intact, policy reject, no plaintext logged). Then CRUD validation (required fields, slug/item_key uniqueness 409, vocab 400), upload validation (type/size/magic-byte), and the `kb.changed` emission (after-commit, correct payload). Full suite must pass before close (gate-tests hook).
8. **Doc comment every function** (intent + gotcha), per engineering-standards #2.

## PROPOSED ARCHITECTURE.md / DECISIONS.md changes awaiting human approval (route to the human — do NOT apply)

These files are human-approval-only / append-only. Nothing below is applied. Full text in architecture.md §9 and §10.

**A. PROPOSED ARCHITECTURE.md changes (§9) — human-approval-only file, do not touch:**
- **A1 (§9.1)** Materialize the `sample` data-model row: `{ id, slug, title, category, preview, file, display_order, created_at, updated_at }`.
- **A2 (§9.2) — NEW from v1.1:** Add a new data-model entry `admin_credential { id, username, password_hash, updated_at }` (single row, BCrypt hash only). This is a new **system-level data-model addition** — flagged separately so you can route it.
- **A3 (§9.3)** Note under Components/Constraints: the admin auth layer with a **persisted, env-seeded-on-first-boot, runtime-mutable** single credential (`POST /api/admin/password`) + Redis sessions; and the local upload-storage volume served under `/uploads/**`.
- **A4 (§9.4)** Update the "Redis serves three roles" wording to include the **admin session store**.
- **A5 (§9.5)** Optionally record the `kb.changed` payload (or point to DEC-024).
- No ARCHITECTURE.md **constraint** is weakened — all additive; env-seed-only upholds "secrets in env, not repo."

**B. PROPOSED DECISIONS (§10) — append-only DECISIONS.md, do not write it. Highest committed is DEC-020; DEC-021..DEC-026 are still PROPOSED (not yet appended):**
- **DEC-021 (AMENDED for v1.1):** admin auth = Spring Security + single credential **persisted in one-row `admin_credential`, seeded from env first-boot-only, mutable at runtime via `POST /api/admin/password`** + Redis sessions; standalone from 006. (Reverses the v0.1 "immutable env-only credential" wording.)
- **DEC-022, DEC-023, DEC-024, DEC-025:** unchanged from the v0.1 proposal.
- **DEC-026 (AMENDED for v1.1):** functional dark admin theme that is **fully responsive** (reverses desktop-only) and built entirely on **themeable design tokens, no hard-coded hex** (enables sibling spec 013's toggle).
- Since none of DEC-021..DEC-026 is committed yet, treat these as **proposal revisions**, not edits of committed records. If the keeper has already appended any of them, do **not** edit in place (append-only) — propose a superseding record and flag it.

**C. Infra (docker-compose) — infra's to apply, not Dev's, not mine:**
- Add the `alef-uploads` named volume mounted at `ALEF_UPLOAD_DIR` in the `api` service, and set the four env vars (`ADMIN_USERNAME`, `ADMIN_PASSWORD_HASH`, `WEB_ORIGIN`, `ALEF_UPLOAD_DIR`). Required for F12-AC26 (uploads persist locally; the credential seeds from env on first boot). Note `ADMIN_USERNAME`/`ADMIN_PASSWORD_HASH` are now **first-boot seed values**, not the live credential.

**D. Trust AC reconciliation (DEC-025) — PM to confirm:** F12-AC12/AC14 literally list "marquee project count" and "client/contractor names" as editable trust content, but per DEC-014 they are derived and edited via Projects. The design satisfies the ACs through that mechanism (read-only display + edit-via-Projects). PM should confirm this reading is acceptable.

**E. New env vars** (`ADMIN_USERNAME`, `ADMIN_PASSWORD_HASH`, `WEB_ORIGIN`, `ALEF_UPLOAD_DIR`) — the orchestrator updates CLAUDE.md's env table after this handoff (not me); note the amended semantics of the two admin vars (first-boot seed).

## Read first

- specs/012-admin-content-management/architecture.md (v0.2) and design.md (v0.2) — especially architecture §0/§2.1.1/§3.6/§10 and design §A.7/§B.9/§B.10.
- specs/012-admin-content-management/requirements.md (v1.1 — F12-AC27..AC30 are the new/revised ids).
- specs/012-admin-content-management/handoffs/3-pm-to-architect.md (the settled scope changes).
- specs/003-portfolio-filtering/architecture.md, specs/005-trust-layer/architecture.md (tables + conventions the admin write path reuses).
- docs/knowledge/DECISIONS.md (DEC-004 events, DEC-009 vocab, DEC-011 projections, DEC-014 derived facts, DEC-020 team-for-012).
- docs/knowledge/LEARNINGS.md (PITFALL-007 force-dynamic; PITFALL-006 Ollama autoconfig exclusion in tests).
- Existing conventions to match: api/.../lead/ (ProblemDetail handler, Redis rate-limit), .../office/entity/OfficeEntity.java (TEXT[] idiom), .../portfolio/ (controller/service/repository shape); web/tailwind.config.ts (the named color tokens to use); web/src/components/projects/project-grid.tsx (the `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3` responsive idiom).

## Do not touch

- docs/knowledge/ARCHITECTURE.md (human-approval-only; propose via architecture.md §9 — done).
- docs/knowledge/DECISIONS.md, LEARNINGS.md (append-only; keeper appends DEC-021..DEC-026).
- CLAUDE.md (orchestrator updates it after handoff).
- requirements.md, implementation.md, testing.md (not the Architect's to write; Dev owns implementation.md).
- The settled scope decisions (responsive, single-admin-with-in-app-known-password-change, 013-is-a-separate-sibling) and settled patterns (local-first, Java + Spring AI backend, event-driven side effects via Redis Streams, provider-swap). Do not revisit.
