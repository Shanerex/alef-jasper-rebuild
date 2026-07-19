# Handoff: Admin Content Management — Dev -> QA

| Field | Value |
|---|---|
| Feature # | 012 |
| From / To | Dev -> QA |
| Branch | (per orchestrator's branch convention for this feature) |
| API test result | 154 tests, 0 failures, 0 errors — BUILD SUCCESS (full suite, incl. pre-existing 003/005/011 tests) |
| TypeScript check | `npx tsc --noEmit` exits 0, no output |
| Spec | specs/012-admin-content-management/requirements.md (v1.1), architecture.md (v0.2), design.md (v0.2) |
| Acceptance IDs | F12-AC1 through F12-AC30 |
| Implementation doc | specs/012-admin-content-management/implementation.md |
| Testing spec | specs/012-admin-content-management/testing.md (QA's to write) |

---

## What was shipped

### Backend (Spring Boot) — `/api/admin/**`, all behind Spring Security

**Auth** (`com.alef.api.admin.security`)
- `POST /api/admin/session` — login, sets `ALEFADMIN` session cookie, rate-limited (10 attempts / 15 min / IP).
- `GET /api/admin/session` — current session check (401 if not authenticated).
- `DELETE /api/admin/session` — logout.
- `GET /api/admin/csrf` — **new, not in design.md** — primes the `XSRF-TOKEN` cookie the web client needs before its first mutating call (see implementation.md "Deviations"). Public.
- `POST /api/admin/password` — self-service password change; requires current password; CSRF-protected.
- Single admin credential persisted in `admin_credential` (seeded once from `ADMIN_USERNAME`/`ADMIN_PASSWORD_HASH` on first boot, never re-seeded after).

**Content CRUD** (all require login; all mutations require the `X-XSRF-TOKEN` header)
- `com.alef.api.admin.project` — full CRUD on `project`, `/api/admin/projects[/…]`; emits a `kb.changed` Redis Stream event on create/update/delete.
- `com.alef.api.admin.team` — full CRUD on `team`, `/api/admin/team[/…]`; includes inactive rows; hard delete supported (soft-hide via `active` is the UI-recommended path).
- `com.alef.api.admin.trust` — CRUD on `trust_content`, `/api/admin/trust[/…]`; `GET /api/admin/trust` also returns a **read-only** derived-facts block (marquee project count, distinct clients, distinct contractors) — these are never editable here, only via Projects.
- `com.alef.api.admin.sample` — full CRUD on the **new** `sample` table, `/api/admin/samples[/…]`.

**Uploads**
- `POST /api/admin/uploads` — multipart; `category` ∈ `project-image | team-photo | sample-preview | sample-file`; validates content-type, extension, and magic bytes; images ≤5 MB, PDFs ≤25 MB; returns a public `/uploads/...` URL.
- `GET /uploads/**` — public, unauthenticated, serves the stored files.

### Database (Flyway)

| Migration | Table |
|---|---|
| V12 | `sample` (DDL only — no seed; **ships empty**) |
| V14 | `admin_credential` (DDL only — seeded at app startup, not by Flyway) |

### Frontend (Next.js) — `/admin/**`

| Route | Notes |
|---|---|
| `/admin/login` | Public login form |
| `/admin` | Dashboard — live counts per resource |
| `/admin/account` | Change password |
| `/admin/projects`, `/new`, `/[id]` | List / create / edit |
| `/admin/team`, `/new`, `/[id]` | List / create / edit; inline Active toggle |
| `/admin/trust`, `/new`, `/[id]` | Grouped list (stats/software/standards) + read-only derived panel; create / edit |
| `/admin/samples`, `/new`, `/[id]` | List / create / edit |

- `web/src/middleware.ts` — redirects to `/admin/login` when the `ALEFADMIN` cookie is absent (UX signal only; the API's 401 is authoritative — `AdminShell` calls `GET /api/admin/session` on every protected page load and redirects on failure).
- Fully responsive per design §B.10: sidebar → hamburger drawer below `lg` (1024px); list tables → stacked cards below `md` (768px); forms → single column below `md`; delete confirmation → stacked full-width buttons on phone; upload widget uses a native file input (works with a phone's camera roll/file picker with no code branch).
- Every color in `web/src/app/admin/**` and `web/src/components/admin/**` is a named Tailwind token — verified by grep, zero raw hex / arbitrary-value colors / inline style colors (F12-AC30).

---

## Acceptance criteria to verify

| ID | Criterion | How to verify |
|---|---|---|
| F12-AC1 | Admin surface requires auth; unauthenticated page loads redirect to login | Visit `/admin/projects` in a fresh browser (no cookie) → redirected to `/admin/login?next=/admin/projects` |
| F12-AC2 | Unauthenticated mutating API calls are rejected, not silently accepted | `curl -X POST http://localhost:8080/api/admin/projects` with no cookie → 401 or 403 (CSRF check runs first on unsafe methods — both are explicit rejections, see implementation.md) |
| F12-AC3 | Admin auth is standalone, no client/tenant concept introduced | Inspect `admin_credential` schema — single row, `CHECK(id=1)`, no `client`/tenant column anywhere |
| F12-AC4–7 | Project create/edit/delete works and reflects on the public site immediately | Create a project in `/admin/projects/new`; visit `/projects` on the public site — appears without a redeploy (force-dynamic, PITFALL-007) |
| F12-AC8–11 | Team create/edit/delete works; inactive rows hidden publicly | Toggle a member inactive; confirm they disappear from `/about`'s team grid but still list in `/admin/team` |
| F12-AC12–15 | Trust stats/badges editable; changes reflect on `/` (home) trust section | Edit a stat's `value` in `/admin/trust`; reload `/` and confirm the new value |
| F12-AC16–19 | Sample create/edit/delete works | Create a sample in `/admin/samples/new`; confirm it appears in the admin list (no public read path yet — feature 004) |
| F12-AC20–22 | Upload validates type/size; invalid uploads show a clear error | Try uploading a `.txt` renamed to `.png` as a project image → rejected with an inline error (magic-byte check); try a >5MB image → rejected |
| F12-AC23 | Required fields enforced client- and server-side | Submit the project form with `slug` blank → blocked client-side; if you bypass the client (e.g. dev tools) the server still 400s with a `fields` map |
| F12-AC24 | Delete requires explicit confirmation | Click Delete on any list row → modal appears; only confirms on the second click |
| F12-AC25 | Project writes emit `kb.changed` | `redis-cli XRANGE kb.changed - +` after creating/editing/deleting a project — a new stream entry appears with `entityType=project`, correct `changeType` |
| F12-AC26 | Whole admin surface runs under `docker compose up`, no external accounts | Fresh `docker compose up`; log in with the seed credential (`admin` / `changeme123` per docker-compose.yml); no external API keys needed for any admin flow |
| F12-AC27 | Admin usable at phone/tablet widths | Resize browser to ~375px width on any list/form screen — table becomes stacked cards, sidebar becomes a drawer, forms go single-column |
| F12-AC28 | Password change with a known current password works, no redeploy, session stays valid | Log in, go to `/admin/account`, change password, confirm you are NOT logged out; log out and log back in with the new password |
| F12-AC29 | Password never stored/logged in plaintext | Inspect `admin_credential.password_hash` in Postgres — a BCrypt hash (`$2a$...` / `$2b$...`); grep API logs after a password change for the plaintext value — absent |
| F12-AC30 | No hard-coded colors anywhere in the admin UI | `grep -rnE "#[0-9a-fA-F]{3,8}|style={{" web/src/app/admin web/src/components/admin` → no matches (Dev already ran this; re-verify) |

---

## Edge cases to test

### Auth / session
| Scenario | Expected result |
|---|---|
| Log in with wrong password | 401, generic "Username or password is incorrect" (does not say which field) |
| 11 login attempts from the same IP within 15 minutes | 11th attempt → 429 with `Retry-After: 900` |
| Log in, then restart the `api` container without touching env, log in again with the OLD password | Still works (env only seeds once — the persisted row is authoritative; this is the F12-AC28 guarantee) |
| Change password, then restart `api` with the ORIGINAL `ADMIN_PASSWORD_HASH` still set in the environment | New password still works; old seed password does NOT come back (proves the seeder never re-applies env over an existing row) |
| Submit the account-change form with a new password identical to the current one | Client-side blocks it; if bypassed, server 400s with `fields.newPassword` |
| Submit a mutating admin request with a stale/missing CSRF cookie | 403 |

### Content CRUD
| Scenario | Expected result |
|---|---|
| Create a project with a slug that already exists | 409, "That slug is already used" inline on the slug field |
| Create a project with an unrecognized `sector` value (bypass the dropdown via dev tools) | 400 with `fields.sector` |
| Edit a project's slug to something new | Public URL changes; a `kb.changed` "updated" event carries the new slug; the OLD slug 404s on the public site (no redirect in v1 — expected, not a bug) |
| Delete a `featurable=true` project | Marquee count on the trust layer / home page drops immediately (derived live, DEC-014/DEC-025) |
| Try to edit a trust row's derived facts (marquee count, clients, contractors) | Not possible — the Trust editor shows them read-only with a note; there is no input for them |
| Team: click "Hide" instead of "Delete" | Member's `active` flips to false; they vanish from the public About page but remain editable in the admin list |

### Uploads
| Scenario | Expected result |
|---|---|
| Upload a valid JPEG as a project image | 201, thumbnail preview appears, URL stored on Save |
| Upload a 6 MB PNG (over the 5 MB image cap) | Rejected with a size error (413-style) |
| Upload a 26 MB PDF as a sample file (over both the global 25MB multipart cap and category cap) | Rejected — Spring's multipart limit fires first |
| Rename a `.exe` to `.png` and upload as a project image | Rejected — magic-byte sniff catches the mismatch even though the extension/content-type header look correct |

### Responsive
| Scenario | Expected result |
|---|---|
| Load `/admin/projects` at 375px width | Rows render as stacked cards, not a squeezed table |
| Load any admin screen at 375px and open the sidebar | Hamburger button opens a slide-over drawer; selecting a link closes it |
| Trigger a delete confirmation at 375px width | Modal renders as a centered sheet with full-width stacked Cancel/Delete buttons |

---

## What is NOT in scope for this QA pass

- Feature 004's public Sample Explorer (the `sample` table exists and is admin-CRUD-able, but there is no public read/download page yet).
- `kb.changed` consumption / RAG re-embedding — feature 001 is unbuilt; only confirm the event is produced.
- Forgotten-password self-service recovery — v1 has none by design; recovery is an ops action (documented, not a UI flow to test).
- Audit trail, multiple admin accounts, role hierarchies, bilingual content editing, draft/publish workflow, content versioning/undo — all explicitly out of scope for v1 per requirements.md.
- The spec 013 light/dark toggle itself — 012 only guarantees the admin is built on tokens; there is no switch to test.

---

## Environment setup

```bash
make infra-up   # postgres, qdrant, redis, ollama
cd api && ./mvnw spring-boot:run   # API on :8080
cd web && npm run dev              # Web on :3000
```

Seed admin credential for local dev (already set in docker-compose.yml for the containerized path): `ADMIN_USERNAME=admin`, `ADMIN_PASSWORD_HASH` = a BCrypt hash of `changeme123`. If running `api` outside Docker Compose, set these two env vars before first boot or `admin_credential` stays empty and login will always 401 with "Admin credential not seeded" (server-side log only, not exposed to the client).

Admin sessions require Redis (spring-session-data-redis) — without it, login will fail. Ensure `make infra-up` has started Redis.

---

## Test files (backend)

| File | Coverage |
|---|---|
| `api/src/test/java/com/alef/api/admin/security/AdminCredentialSeederTest.java` | First-boot seeding; the F12-AC28 non-overwrite guard |
| `api/src/test/java/com/alef/api/admin/security/AdminUserDetailsServiceTest.java` | Persisted-row lookup, ROLE_ADMIN mapping |
| `api/src/test/java/com/alef/api/admin/security/PasswordChangeServiceTest.java` | Accept / wrong current / policy violations / order of checks |
| `api/src/test/java/com/alef/api/admin/security/AdminSessionServiceTest.java` | Rate limit math, login delegation, logout |
| `api/src/test/java/com/alef/api/admin/security/AdminSecurityWebTest.java` | **The auth boundary itself**, over the real filter chain: login, 401/403/CSRF, public-stays-open |
| `api/src/test/java/com/alef/api/admin/project/AdminProjectServiceTest.java` + `AdminProjectControllerTest.java` | Slug uniqueness, vocab validation, kb.changed emission, HTTP contract |
| `api/src/test/java/com/alef/api/admin/team/AdminTeamServiceTest.java` + `AdminTeamControllerTest.java` | CRUD, soft-hide |
| `api/src/test/java/com/alef/api/admin/trust/AdminTrustServiceTest.java` + `AdminTrustControllerTest.java` | Value-required-for-stat rule, derived facts, uniqueness |
| `api/src/test/java/com/alef/api/admin/sample/AdminSampleServiceTest.java` + `AdminSampleControllerTest.java` | CRUD, slug uniqueness, category vocab |
| `api/src/test/java/com/alef/api/admin/upload/UploadServiceTest.java` + `UploadControllerTest.java` | Type/size/magic-byte validation |
| `api/src/test/java/com/alef/api/admin/kb/KbChangedPublisherTest.java` | Event payload shape, failure isolation |

Run: `cd api && ./mvnw test`

---

## Known gaps (not regressions)

- `sample` table is empty on a fresh `docker compose up` (no V13 seed) — expected; create rows through the admin UI to test.
- Team photos remain null unless uploaded through the new admin flow (011's seed data is unaffected).
- `next build` was not verified locally by Dev (Node version mismatch on the dev machine, not a code issue — `tsc --noEmit` is clean and the Docker build stage uses a compliant Node version). QA should run a full `docker compose up --build` to confirm the production build succeeds end-to-end.
- No frontend automated test runner exists for `web/` in this repo (pre-existing, not introduced by 012) — QA's testing.md should specify manual/browser verification steps for the UI acceptance criteria.
