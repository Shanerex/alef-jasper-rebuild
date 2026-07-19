# Handoff: Admin Content Management — QA -> Dev (BLOCKED)

| Field | Value |
|---|---|
| Feature # | 012 |
| From / To | QA -> Dev |
| Status | **blocked** — 2 blocking bugs found, testing.md status remains `draft` |
| Spec | specs/012-admin-content-management/testing.md (this pass) |
| Prior | handoffs/4-dev-to-qa.md |

## Summary

Backend suite (154/154) and `tsc --noEmit` were independently re-verified — both green, confirmed. Every F12-ACn was then verified against the running system, not just the test suite: the Docker daemon was available in this environment, so I built and ran the real stack (`docker compose up -d --build postgres redis api`, `docker compose build web`) and drove it with `curl`/`psql`/`redis-cli`. 27 of 30 ACs pass cleanly (many verified live end-to-end, not just by test/code inspection — see testing.md §2 for the full table with evidence per id). Two **blocking** bugs surfaced from that live testing that the existing test/build gates do not catch. Full detail, repro steps, and rationale are in `specs/012-admin-content-management/testing.md` §3 — summarized here.

## Blocking bugs

### Bug #1 — global CSRF scoping breaks the pre-existing public `POST /api/leads` endpoint (feature 011 regression)

`AdminSecurityConfig`'s `SecurityFilterChain` has no `.securityMatcher(...)`, so it (and its CSRF filter) applies to the **entire app**, not just `/api/admin/**`. The CSRF ignore list only covers `/api/admin/session`. Result: every unsafe-method request anywhere, including the public contact-form `POST /api/leads` (feature 011, which architecture.md explicitly says stays unauthenticated and which 012 was never meant to touch), now requires an `X-XSRF-TOKEN` it has no way to obtain. Confirmed live against the built container:

```
curl -i -X POST http://localhost:8080/api/leads -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@example.com","message":"hi there this is a test message"}'
→ HTTP/1.1 403 {"title":"Forbidden", ...}
```
Expected 201 (or a genuine 400/429), never a CSRF-shaped 403 — `web/src/lib/api/leads.ts` never sends `X-XSRF-TOKEN` and was never meant to.

Not caught by the suite because `LeadControllerTest` (PITFALL-008 fix) runs with `addFilters = false`, and `AdminSecurityWebTest`'s "public endpoints stay open" test only drives a `GET` against a public endpoint, never a `POST`. Suggest: scope the filter chain / CSRF ignore rule to `/api/admin/**` (or explicitly add `/api/leads` to the CSRF ignore list), plus a new regression test that drives the real chain against a non-admin mutating endpoint.

### Bug #2 — `next build` fails; `docker compose up --build` cannot produce a `web` image

`web/src/app/admin/login/page.tsx` calls `useSearchParams()` with no `<Suspense>` boundary. `web/src/app/projects/page.tsx` already establishes the correct pattern for this (feature 003) — `/admin/login` doesn't follow it. Confirmed live:

```
docker compose build web
→ ⨯ useSearchParams() should be wrapped in a suspense boundary at page "/admin/login".
  Export encountered an error on /admin/login/page: /admin/login, exiting the build.
  ERROR: process "/bin/sh -c npm run build" did not complete successfully: exit code: 1
```
`tsc --noEmit` doesn't run Next's prerender/static-export validation, so it couldn't have caught this — which is exactly why implementation.md/handoffs/4-dev-to-qa.md flagged `next build` as unverified locally and asked QA to check via `docker compose up --build`. It fails. This blocks F12-AC26 outright and blocks the entire `web` service (not just admin routes) from being built fresh — 011's marketing pages are equally affected until fixed. Suggest: wrap the `useSearchParams()` call in `<Suspense>`, matching `projects/page.tsx`'s existing precedent.

## Non-blocking concerns (for the record, not required to close)

- **Concern #3:** `DELETE /api/admin/session` (logout) is CSRF-exempt (the whole `/api/admin/session` path is), which allows a forced cross-site logout of an authenticated admin. No data/state exposure — nuisance-level only. Design's error table for logout lists no 403, so this may be accepted-by-design; flagging so it's a conscious call, not an oversight.
- **Concern #4:** `DeleteConfirmDialog`'s destructive button uses the same gold-family tokens as the rest of the admin, not the "red-tinted, not primary gold" treatment design.md §B.6 specifies — `tailwind.config.ts` has no danger/destructive token to use. F12-AC24 still passes functionally (the confirmation gate is real); this is a design-fidelity nit worth a token addition at some point.

## What I verified live (not just code/test inspection) — for context on confidence level

Auth boundary (401/403/200 in all the documented combinations), first-boot seeding + non-overwrite across a real `docker restart`, full password-change round trip including the restart-durability guarantee, project/team/trust/sample CRUD (create, delete, uniqueness 409, vocab 400), upload magic-byte rejection + oversize 413 + valid accept + public re-serve, `kb.changed` Redis Stream payload for created/deleted, DEC-025 derived-facts recompute (marquee count tracking live project changes), CORS allow/deny by origin, BCrypt-only storage with no plaintext in logs, zero raw-hex/inline-style colors in the admin tree. All passed. Full table with evidence is in testing.md §2.

## What I did not verify live (and why) — see testing.md §5

Live browser check of responsive breakpoints and client-side form validation — blocked by Bug #2 (no running `web` container); verified by Tailwind-class inspection instead, which matches design §B.10 exactly. Team/trust live write→public-read round trip specifically — mechanism proven live for projects (F12-AC7) and both write paths proven live independently; the exact paired round trip for these two resources wasn't independently re-driven given the time budget.

## Next step

Fix Bug #1 and Bug #2 (both look small/contained), add regression coverage for each (a real-filter-chain test hitting a non-admin mutating endpoint; a `next build` check in CI or at minimum a documented manual gate), then hand back to QA for a re-pass. I did not fix either — that's explicitly not QA's role here.
