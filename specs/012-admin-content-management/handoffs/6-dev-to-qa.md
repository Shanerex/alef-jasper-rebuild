# Handoff: Admin Content Management — Dev -> QA (bug-fix pass)

| Field | Value |
|---|---|
| Feature # | 012 |
| From / To | Dev -> QA |
| Status | bug-fix pass complete — both blocking bugs fixed and re-verified live; ready for QA re-pass |
| Prior | handoffs/5-qa-to-dev.md (blocked) |
| API test result | 157 tests, 0 failures, 0 errors — BUILD SUCCESS (up from 154; +3 regression tests) |
| TypeScript check | `npx tsc --noEmit` exits 0, no output |
| Docker build | `docker compose build web` succeeds (previously failed — this is the gate QA's prior pass caught and this pass fixes) |
| Spec | specs/012-admin-content-management/requirements.md (v1.1), architecture.md (v0.2), design.md (v0.2) |
| Acceptance IDs | F12-AC1 through F12-AC30 (unchanged from prior pass) |
| Implementation doc | specs/012-admin-content-management/implementation.md ("Bug-fix pass" section) |
| Testing spec | specs/012-admin-content-management/testing.md (QA owns; not touched by this pass) |

---

## What changed since handoffs/5-qa-to-dev.md

This pass fixes exactly the 2 blocking bugs QA found and makes a deliberate call on each of the 2 non-blocking concerns. No other scope was touched — nothing QA marked as passing (27/30 ACs) was modified.

### Bug #1 (critical) — fixed: CSRF filter now scoped to `/api/admin/**` only

**Root cause:** `AdminSecurityConfig`'s `SecurityFilterChain` had no `.securityMatcher(...)`. Being the only registered chain, it governed the whole app — CSRF filter included — even though `authorizeHttpRequests` correctly `permitAll()`'d public paths (that rule only governs authorization, not CSRF, which is a separate, earlier filter in the same chain). Live effect: `POST /api/leads` (feature 011, public) returned a CSRF-shaped 403 for every real submission.

**Fix:** added `.securityMatcher("/api/admin/**")` to `adminSecurityFilterChain()` in `api/src/main/java/com/alef/api/admin/security/AdminSecurityConfig.java`. Requests outside that prefix now never enter this chain at all — restoring exact pre-012 behavior for `/api/projects/**`, `/api/trust/overview`, `/api/team`, `/api/offices`, `POST /api/leads`, `/uploads/**`.

**Regression test added:** `AdminSecurityWebTest.public_lead_submission_endpoint_stays_open_without_authentication_or_csrf` — POSTs to `/api/leads` through the real filter chain, no session, no CSRF token, asserts 201. This is the test that would have caught the original bug (the prior "public endpoints stay open" test only drove a GET, which can never reveal a CSRF-scoping issue).

**Live re-verification** (built and ran `postgres` + `redis` + `api` via `docker compose up -d --build`, reproduced QA's exact repro):
```
curl -i -X POST http://localhost:8080/api/leads -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@example.com","message":"hi there this is a test message"}'
→ HTTP/1.1 201
{"id":1,"status":"received"}
```
Also spot-checked the admin boundary is unaffected: `GET /api/admin/session` unauth → 401; `POST /api/admin/projects` unauth/no-CSRF → 403; `GET /api/projects` → 200.

Recorded as **DEC-029** (docs/knowledge/DECISIONS.md) and **LEARNING-003** (docs/knowledge/LEARNINGS.md).

### Bug #2 (critical) — fixed: `next build` now succeeds

**Root cause:** `web/src/app/admin/login/page.tsx` called `useSearchParams()` directly in the page component, with no `<Suspense>` boundary — Next.js 15's static prerendering step rejects this outright, failing `docker compose build web` for the whole `web` service.

**Fix:** split the page into a thin `AdminLoginPage` default export (`<Suspense fallback={null}><LoginForm /></Suspense>`) and a `LoginForm` component holding the previous logic, matching the existing pattern in `web/src/app/projects/page.tsx`.

**Re-verification:** `docker compose build web` succeeds; the build's route table now shows `/admin/login` as `○ (Static)` (prerendered), confirming the fix:
```
├ ○ /admin/login                         2.18 kB         111 kB
```

### Non-blocking concern #3 — deliberate call made: logout is no longer CSRF-exempt

Narrowed the CSRF exemption from the whole `/api/admin/session` path to `POST /api/admin/session` only (login). `DELETE /api/admin/session` (logout) now requires CSRF like every other authenticated mutation, closing the forced-logout gap you flagged. Costs nothing functionally — the web client already primes the CSRF cookie before login (`admin-session.ts`'s `login()` calls `primeCsrfToken()` first), so it's already present by the time an authenticated admin calls `logout()`.

`AdminSecurityWebTest`'s logout coverage was updated: unauthenticated + no CSRF → 403 (not 401, since CSRF now runs first — same pattern as every other protected mutating endpoint); unauthenticated + valid CSRF → 401 (auth boundary still holds); authenticated + valid CSRF → 204 (happy path still works end-to-end).

Recorded as **DEC-030**.

### Non-blocking concern #4 — fixed: DeleteConfirmDialog now uses a danger token

Added a minimal `danger` color group (`DEFAULT` + `subtle`) to `web/tailwind.config.ts` and switched `DeleteConfirmDialog`'s confirm button from gold to danger tokens, matching design.md §B.6's "outline/red-tinted, not primary gold" spec. Still token-only (F12-AC30 compliant) — no raw hex introduced.

Recorded as **DEC-031**.

---

## Full verification results (re-run after every change in this pass)

- `cd api && ./mvnw test` → **157 tests, 0 failures, 0 errors, BUILD SUCCESS**.
- `cd web && npx tsc --noEmit` → clean, exit 0.
- `docker compose build web` → **succeeds** (previously failed).
- `docker compose build api` → succeeds (sanity check on the touched security config).
- `docker compose up -d postgres redis api` + live `curl` reproductions of both QA repro commands → both now return the expected result (see Bug #1/#2 above).

---

## What to re-verify

Everything QA already verified live and passed (27/30, per handoffs/5-qa-to-dev.md §"What I verified live") should be unaffected — none of that surface was touched. Please focus the re-pass on:

1. **Bug #1 regression:** re-run the exact repro from your prior handoff (`curl -X POST http://localhost:8080/api/leads ...` with no CSRF header) against a freshly built `docker compose up --build` stack, and confirm 201/400/429 as appropriate, never a CSRF-shaped 403. Also spot-check a couple of the other previously-affected-in-principle public endpoints (`GET /api/projects`, `GET /api/trust/overview`) still work with no auth/CSRF.
2. **Bug #2 regression:** `docker compose up --build` end-to-end (the exact command that failed last time) — confirm the `web` container starts and `/admin/login` renders and functions (submit with `?next=` present and absent).
3. **Concern #3:** log in through the UI, then log out — confirm logout still works (the CSRF-priming-at-login mechanism should make this invisible to a real user). If you want to specifically probe the tightened boundary, try a raw `curl -X DELETE http://localhost:8080/api/admin/session` with no cookies/token at all — expect 403 now (previously 401), which is the intended, documented change (DEC-030), not a regression.
4. **Concern #4:** visually confirm the Delete confirmation dialog's Delete button now reads as a distinct red/danger treatment, not gold, on both desktop and phone widths.
5. Anything you did **not** get to verify live last time (team/trust live write→public-read round trip, browser-driven responsive checks) is still open and welcome in this pass now that the `web` container builds.

---

## Environment setup

Unchanged from the prior handoff:

```bash
make infra-up   # postgres, qdrant, redis, ollama
cd api && ./mvnw spring-boot:run   # API on :8080
cd web && npm run dev              # Web on :3000
```

Or the full containerized path: `docker compose up --build` (this is the command that failed last time and now succeeds).

Seed admin credential (already set in docker-compose.yml): `ADMIN_USERNAME=admin`, `ADMIN_PASSWORD_HASH` = a BCrypt hash of `changeme123`.

---

## Test files changed in this pass (backend)

| File | Change |
|---|---|
| `api/src/main/java/com/alef/api/admin/security/AdminSecurityConfig.java` | Added `.securityMatcher("/api/admin/**")`; narrowed CSRF exemption to `POST /api/admin/session` only; doc comments updated |
| `api/src/test/java/com/alef/api/admin/security/AdminSecurityWebTest.java` | Added `LeadController`/`LeadExceptionHandler` to the test slice and a new regression test for Bug #1; updated/added 3 logout tests for the DEC-030 CSRF change |

## Files changed in this pass (frontend)

| File | Change |
|---|---|
| `web/src/app/admin/login/page.tsx` | Split into `AdminLoginPage` (Suspense wrapper) + `LoginForm` (the actual logic) |
| `web/tailwind.config.ts` | Added the `danger` token group |
| `web/src/components/admin/delete-confirm-dialog.tsx` | Confirm button switched from gold to danger tokens |

---

## Known gaps (unchanged, not regressions)

Same as handoffs/4-dev-to-qa.md's "Known gaps" section — `sample` table ships empty, team photos null until uploaded, no frontend test runner in this repo. Nothing new introduced by this pass.
