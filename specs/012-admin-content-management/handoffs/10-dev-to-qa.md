# Handoff: Admin Content Management — Dev -> QA (bug-fix pass 3, reopens QA approval)

| Field | Value |
|---|---|
| Feature # | 012 |
| From / To | Dev -> QA |
| Status | ready for re-verification — reopens handoffs/9-qa-to-pm.md's "ready to close" call; one real bug found post-approval and fixed |
| Prior | handoffs/9-qa-to-pm.md (QA approved, testing.md v1.2, routed to PM) |
| TypeScript check | `npx tsc --noEmit` exits 0, no output |
| Docker build | `docker compose build web` succeeds |
| Spec | specs/012-admin-content-management/requirements.md (v1.1), architecture.md (v0.2), design.md (v0.2) |
| Acceptance IDs | F12-AC4, F12-AC5 (project create/edit) — re-check these specifically |
| Implementation doc | specs/012-admin-content-management/implementation.md ("Bug-fix pass 3" section) |
| Testing spec | specs/012-admin-content-management/testing.md (QA owns; not touched by this pass) |

---

## Context

After testing.md moved to `approved` (v1.2) and handoffs/9-qa-to-pm.md routed the feature to PM for close-out, the project owner did a manual, human click-through of the running admin console before allowing that close-out to proceed. That surfaced a real, previously-uncaught, fully-blocking bug in core CRUD — and a second apparent bug that turned out to be expected behavior. **This means the QA-approved status was premature** — not a criticism of the process, but worth naming plainly: the browser-only defect below was invisible to API-level verification, which is how most of the prior CRUD re-checks were done.

## What was found and done

### Bug (real, fixed): `/admin/projects/new` and `/admin/projects/[id]` hung on "Loading…" forever

Both pages are client components that called `getProjectFilters()` (a server-fetch-only helper) directly in a `useEffect`. The API has no CORS policy outside `/api/admin/**`, so the browser silently blocked the cross-origin response and the page never got past "Loading…" — no error shown to the user, only a CORS message in devtools. This is a **complete block on creating or editing any project through the UI** — F12-AC4 and F12-AC5 were not actually exercisable in a browser, only via direct API calls.

**Fix:** converted both pages to Server Components that fetch the filter vocabulary server-side (mirroring the existing `(marketing)/projects/page.tsx` pattern) and pass it down as a prop; the project record itself stays client-fetched (it needs the browser's session cookie) in a new `edit-project-client.tsx`. Full root-cause trace and fix detail in implementation.md's "Bug-fix pass 3" section; the general gotcha is recorded as **PITFALL-010**.

**Re-verification done:** `tsc --noEmit` clean, `docker compose build web` succeeds, and a live headless-browser session against the rebuilt container confirmed both pages now render the full form (all fields, all vocab dropdowns populated) instead of hanging.

### Apparent bug (investigated, not a defect): team "hide" doesn't reflect immediately on the public site

Traced end-to-end (real UI toggle → PUT → backend persistence → public `GET /api/team`): the write and the filter both work correctly, instantly, at the API level. The actual behavior is that the public About page caches its team fetch for up to 60 seconds (pre-existing, deliberate ISR `revalidate: 60`, same as projects/trust), with no on-demand cache invalidation anywhere in the admin write paths. Confirmed live: the toggle takes effect within that window, never longer. Not a bug — but flagged in CLAUDE.md's known-gaps list as a real admin-UX limitation (an editor toggling "hide" reasonably expects it sooner than up to a minute). No fix applied — out of scope for a bug-fix pass, would need `revalidatePath`/`revalidateTag` wired into every admin write path.

---

## What the receiving role must do

1. Re-verify F12-AC4 and F12-AC5 **in an actual browser**, not via curl/API calls — load `/admin/projects/new`, fill and submit the form; load `/admin/projects/[id]` for an existing project, confirm it loads (not "Loading…" forever) and edits save correctly.
2. Spot-check that the Server Component conversion didn't regress anything else on those two pages (upload field, scope checkboxes, validation error display, cancel button, redirect-to-list on save).
3. Given this bug was specifically a *browser-only* defect invisible to API-level checks, consider whether any other admin screen deserves the same scrutiny — I already checked `trust`/`samples`/`team` for the same code pattern (a client component importing a non-`admin-*`, non-CORS API client) and found none, but an actual click-through is a different, stronger check than a grep.
4. Do not re-open New Bug #3 (worker) or re-verify Concern #6 (nav/footer) — both unaffected by this pass, still governed by DEC-032 and the prior handoff respectively.
5. If clean, testing.md can move back to `approved` and this should route to PM again (handoffs/9-qa-to-pm.md's content otherwise still stands).

## Read first

- docs/knowledge/LEARNINGS.md — PITFALL-010 (new)
- specs/012-admin-content-management/implementation.md — "Bug-fix pass 3" section
- web/src/app/admin/(protected)/projects/new/page.tsx, .../projects/[id]/page.tsx, .../projects/[id]/edit-project-client.tsx — the actual fix

## Do not touch

- The team ISR-latency finding — it's documented as a known gap, not something to fix in this pass.
- `AdminSecurityConfig` / worker boot failure — still governed by DEC-032, out of scope.

---

## Environment setup

Unchanged: `docker compose up --build` (or `make infra-up` + `./mvnw spring-boot:run` + `npm run dev`). Seed admin credential: `ADMIN_USERNAME=admin`, password `changeme123`.
