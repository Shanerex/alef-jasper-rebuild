---
feature: 012-admin-content-management
spec_id: "012"
phase: testing
owner: QA
status: approved
version: "1.5"
entry_criteria:
  - implementation complete (implementation.md), Dev handoff received (handoffs/4-dev-to-qa.md, handoffs/6-dev-to-qa.md, handoffs/8-dev-to-qa.md, handoffs/10-dev-to-qa.md, handoffs/13-dev-to-qa.md)
  - backend full suite green (154/154, then 157/157, reconfirmed 157/157 across six passes) and `tsc --noEmit` clean, independently re-verified across six passes
exit_criteria:
  - every F12-ACn verified by id
  - full suite output reported
  - blocking bugs (if any) reported to Dev, not fixed by QA
  - the mandated live-browser re-verification actually completed, not substituted with API-level or code-level checks alone
---

# Testing: Admin Content Management (012)

> **v1.5 status: `approved`.** This pass (§10) independently re-verifies bug-fix pass 4 (PITFALL-011: uploaded images never rendering, anywhere) live in a real browser, after a human click-through found the bug post-v1.4-approval (handoffs/13-dev-to-qa.md). Confirmed fixed for both team photos and project images, admin preview and public site alike. Sections 0–9 below are the unmodified record of prior passes, retained for history.

## 0. What this pass covered

Read, in order: requirements.md (v1.1), architecture.md (v0.2), design.md (v0.2), implementation.md, handoffs/4-architect-to-dev.md, handoffs/4-dev-to-qa.md, handoffs/5-qa-to-dev.md, handoffs/6-dev-to-qa.md (bug-fix pass 1), handoffs/7-qa-to-dev.md (this doc's prior pass, v1.1 — found both original bugs/concerns fixed, surfaced 2 new findings: New Bug #3 worker-boot-failure, New Concern #6 marketing-nav-bleeds-onto-admin), handoffs/8-dev-to-qa.md (Dev's bug-fix pass 2 response), DECISIONS.md (DEC-021..DEC-032), LEARNINGS.md (LEARNING-002, LEARNING-003, PITFALL-007, PITFALL-008, PITFALL-009), CLAUDE.md.

This pass went beyond re-running the suite: a genuinely fresh `docker compose down` → `docker compose build --no-cache web` → `docker compose up -d postgres redis api web` was driven end-to-end, and the running containers were exercised with `curl` plus a real headless-Chrome (Puppeteer against the previously-cached system Chrome binary, `mac_arm-126.0.6478.182`) session — login flow, screenshots at 1440px and 390px, AdminShell drawer interaction — independently, not by trusting Dev's self-reported curl output in handoffs/8-dev-to-qa.md.

**Scope of this pass, per the orchestrator's brief:** independently re-verify New Concern #6 (marketing nav/footer bleeding onto `/admin/**`) is actually fixed, live; confirm no new regression from the frontend route-group restructure; re-run the backend suite and `tsc --noEmit` as a sanity check (expected unaffected, since this pass only touched frontend layout files); confirm DEC-032's stated rationale (no worker-consumer code has appeared) still holds, without re-testing or re-blocking on the underlying New Bug #3 worker-boot-failure defect itself, which is a deliberate, documented deferral (DEC-032), not an oversight.

## 1. Full suite — reproduced independently

```
cd api && ./mvnw test
...
[INFO] Tests run: 157, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
Identical to the count from the prior two passes (154 → 157, no change this pass — expected, since Dev's bug-fix pass 2 touched only frontend layout files, not backend code). Independently re-run, same result.

```
cd web && npx tsc --noEmit
exit 0, no output
```
Confirmed clean, independently.

```
docker compose build --no-cache web
```
**Succeeds.** Route table compared against the prior pass's (testing.md v1.1, §1) — **identical set of URLs**, confirming the `(marketing)` route-group move did not change any public route or admin route:
```
├ ƒ /                                      165 B         106 kB
├ ƒ /about                                1.2 kB         107 kB
├ ○ /admin                               1.71 kB         107 kB
├ ○ /admin/account                        2.4 kB         115 kB
├ ○ /admin/login                         2.18 kB         111 kB
├ ○ /admin/projects                      2.65 kB         108 kB
├ ƒ /admin/projects/[id]                   545 B         117 kB
├ ○ /admin/projects/new                    447 B         117 kB
├ ○ /admin/samples                       2.52 kB         108 kB
├ ƒ /admin/samples/[id]                    517 B         117 kB
├ ○ /admin/samples/new                     341 B         116 kB
├ ○ /admin/team                          2.65 kB         108 kB
├ ƒ /admin/team/[id]                       522 B         117 kB
├ ○ /admin/team/new                        348 B         116 kB
├ ○ /admin/trust                         2.76 kB         109 kB
├ ƒ /admin/trust/[id]                      555 B         116 kB
├ ○ /admin/trust/new                       344 B         116 kB
├ ƒ /contact                             2.56 kB         108 kB
├ ƒ /projects                            30.7 kB         143 kB
├ ƒ /projects/[slug]                       165 B         106 kB
└ ƒ /services                              165 B         106 kB
ƒ Middleware                             34.2 kB
```
`/admin/login` still `○ (Static)` as it was in the prior pass. Route rendering strategy (static vs dynamic) per-page is unchanged from the prior pass. **No route-table regression.**

```
docker compose up -d postgres redis api web
```
All four containers reach `Up`/`healthy`. `GET /api/projects` → `200`. `GET /` (web) → `200`.

## 2. Re-verification of New Concern #6 (marketing nav/footer bleeding onto `/admin/**`) — independently confirmed FIXED

Dev's claimed fix (handoffs/8-dev-to-qa.md): every public page moved into `web/src/app/(marketing)/` via `git mv`; `SiteNav`/`SiteFooter` moved out of the root layout into `web/src/app/(marketing)/layout.tsx`; root layout (`web/src/app/layout.tsx`) now only provides `<html>`/`<body>`/fonts/metadata. Confirmed structurally by reading the actual files (not just the handoff's description):
- `web/src/app/layout.tsx` — confirmed no `<SiteNav>`/`<SiteFooter>` import or usage, doc comment explicitly states the rationale (references this bug-fix pass and QA concern #6).
- `web/src/app/(marketing)/layout.tsx` — confirmed `SiteNav`/`SiteFooter` now live here, wrapping `{children}` in `<main>`.
- `git status` confirms the six public routes were renamed (`R`) into `(marketing)/`, not copied — no duplicate/orphaned old-path files left behind.

**Live re-verification, independent of Dev's curl output:**

```
curl -s http://localhost:3000/about | grep -o "Enquire Now"        → Enquire Now (present)
curl -s http://localhost:3000/about | grep -o "...Cadding Services LLC"  → present
curl -s http://localhost:3000/admin/login | grep -o "Enquire Now"  → (empty, absent)
curl -s http://localhost:3000/admin/login | grep -o "...Cadding Services LLC" → (empty, absent)
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/          → 200
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/projects  → 200
curl -s -I http://localhost:3000/admin  → 307, Location: /admin/login?next=%2Fadmin
```
Matches Dev's reported figures exactly, reproduced independently.

**Browser-driven (headless Chrome via Puppeteer, real DOM inspection — not just an HTTP grep), at both 1440px and 390px:**

- `/admin/login` @1440px and @390px: `document.querySelectorAll('nav').length === 0`, `document.querySelectorAll('footer').length === 0`, no "Enquire Now"/footer-copyright text anywhere in `document.body.innerText`. Screenshots confirm a clean, centered login card with **no header/footer of any kind** — the specific 390px overlap this concern originally screenshotted (marketing `SiteNav`'s full desktop link row crowding the top of the viewport above the admin content) is **fully absent, not just non-overlapping**: there is no marketing nav rendered at all, at either viewport.
- Logged in via the real UI form (`#login-username`/`#login-password`/submit, not the API directly) and navigated to `/admin/projects` (an authenticated `(protected)` screen) at both 1440px and 390px: `document.querySelectorAll('footer').length === 0` at both; `document.querySelectorAll('nav').length === 1` at both — confirmed via screenshot this is `AdminShell`'s own sidebar/nav landmark (Dashboard/Projects/Team/Trust/Samples), not the marketing `SiteNav` (no "Enquire Now" text, no Cormorant-Garamond marketing logo lockup, no About/Services/Samples/Contact link row — visually and structurally a different component). No marketing chrome anywhere in the authenticated admin tree.

**AdminShell's own responsive behavior, unaffected by the layout restructure:**
- 1440px: full sidebar (Dashboard/Projects/Team/Trust/Samples) + top bar (Account/Logout) render correctly; the projects list renders as a table.
- 390px: sidebar collapses to a hamburger ("Open menu" `aria-label`, confirmed present and clickable); clicking it opens a full-height drawer overlaying the content with the same nav items, confirmed via screenshot; the projects list correctly collapses from a table to stacked cards (`SECTOR`/`COUNTRY`/`STATUS`/`FEATURED` fields per card, `EDIT`/`DELETE` actions retained). Both behaviors match design §B.10 and are visually identical in structure to the pre-restructure screenshots from the prior pass (testing.md v1.1) — the route-group move had no observable effect on `AdminShell`'s own responsiveness.

**Public marketing pages, spot-checked for regression:**
- `/about` @1440px: full `SiteNav` (logo, About/Services/Projects/Samples/Contact, gold "ENQUIRE NOW" CTA) and page content render correctly, pixel-equivalent to the pre-restructure design. (Note: an initial automated check using `innerText.includes('Enquire Now')` false-negatived on this page — the button text is CSS `text-transform: uppercase`, so `innerText` reports `"ENQUIRE NOW"`; resolved by cross-checking the raw HTML via `curl` and a full-page screenshot, both of which confirm the content is present and correct. Not a product bug, a test-script gotcha.)
- `/projects` @1440px: full `SiteNav`/`SiteFooter`, filter bar, and project grid render correctly and match the pre-restructure design (screenshot confirmed).

**Verdict: New Concern #6 is CONFIRMED FIXED, independently, live, at both viewports, including the specific 390px overlap defect originally screenshotted. No regression in `AdminShell`'s own responsive behavior. No regression on public marketing pages.**

## 3. New Bug #3 (`worker` boot failure) — DEC-032 rationale re-checked, underlying defect NOT re-tested (by design, per this pass's scope)

Per the orchestrator's explicit instruction and DEC-032/handoffs/8-dev-to-qa.md's request, this pass did **not** re-start the `worker` service or re-test the `MvcRequestMatcher`/`HandlerMappingIntrospector` boot failure itself — that is a deliberate, documented deferral (DEC-032, PITFALL-009), not an oversight, and re-litigating it would contradict the owner's explicit call.

What **was** checked: DEC-032's stated rationale ("no consumer exists yet for `lead.created`/`kb.changed`") still holds — `grep -rn "StreamListener\|lead.created\|kb.changed" api/src/main/java` shows only the existing producer-side code (`KbChangedPublisher`/`KbChangedEvent` in `admin.kb`, doc-comment references in `AdminProjectService`/`AdminProjectController`, and `LeadService`'s doc comment explicitly noting event emission is "deliberately NOT done here") — no new consumer/`@StreamListener`/worker-side logic has appeared since the prior pass. DEC-032's rationale is unchanged and still accurate.

**Verdict: DEC-032's deferral remains valid and current. Not re-opened, not re-tested, per scope.**

## 4. Acceptance criteria — updated verdicts (delta from the prior pass only; unlisted ids are unchanged PASS, see handoffs/7-qa-to-dev.md / testing.md v1.1 for full evidence)

| ID | Prior verdict (v1.1) | This pass | Evidence |
|---|---|---|---|
| F12-AC1 | PASS, confirmed live; marketing-chrome-bleed noted separately as Concern #6 (not an AC1 failure) | PASS, unchanged — auth gating still functions correctly, and the previously-separate Concern #6 design-fidelity gap is now also closed (see below) | Puppeteer session: unauthenticated `/admin/projects` load → redirect → login → landed at intended page |
| F12-AC27 | PASS, browser-verified prior pass | PASS, reconfirmed this pass — `AdminShell`'s table↔card collapse and sidebar↔drawer collapse both still work correctly post-restructure | Screenshots at 1440px/390px, drawer-open screenshot |

**Non-AC findings, updated:**
- **New Concern #6** (design.md §B.1's "no marketing nav/footer" requirement for the admin shell): **CLOSED.** Confirmed fixed, live, independently, at both viewports, including the 390px overlap defect specifically — now fully absent rather than merely non-overlapping.
- **New Bug #3** (`worker` boot failure): unchanged status — deliberately deferred per **DEC-032**, not re-tested this pass by design. Rationale re-confirmed current (§3).

All other F12-AC1..30 verdicts from the prior pass (handoffs/7-qa-to-dev.md / testing.md v1.1 §4) are unchanged; this pass did not re-touch backend code or backend-exercising tests, and the full suite (157/157) and `tsc --noEmit` (clean) both reconfirm no regression.

**Score: 29 PASS by AC id (unchanged from v1.1 — this pass's finding is a non-AC design.md concern, not a numbered acceptance criterion), 0 CONCERN, 0 FAIL. Both non-AC findings from v1.1 are now resolved: New Concern #6 closed (fixed, confirmed); New Bug #3 deliberately deferred with an owner decision on record (DEC-032), not an open QA finding.**

## 5. Regression notes

- Backend suite: 157/157, unchanged from the prior pass. Independently re-run, confirmed green — expected, since this bug-fix pass touched only `web/src/app/layout.tsx`, `web/src/app/(marketing)/layout.tsx`, and the `git mv` of the six public route files; no backend files changed.
- `tsc --noEmit`: clean, independently re-run.
- `docker compose build --no-cache web`: succeeds, route table byte-for-byte equivalent (same URLs, same static/dynamic rendering split) to the prior pass's.
- `AdminShell`'s responsive behavior (table↔card, sidebar↔drawer): **no regression** — confirmed via live browser interaction (clicked the hamburger, confirmed the drawer opens and shows the same nav items), not just static screenshot comparison.
- Public marketing pages (`/about`, `/projects`): **no regression** — nav/footer render correctly, page content unchanged in structure from the prior pass's screenshots.
- `/admin` → `/admin/login` middleware redirect: **no regression** — still 307s with the `?next=` param preserved, confirmed via both `curl` and (implicitly) the browser login flow landing on the intended post-login page.
- DEC-032 and PITFALL-009 (both new since the prior pass) read and accurate against the code as found; DEC-032's stated rationale (no worker consumer exists) re-confirmed current via a targeted grep, not just re-trusted.

## 6. Gaps (not verified this pass, and why)

- **`worker` service / New Bug #3's underlying defect** — deliberately not re-tested this pass, per explicit scope (DEC-032 is a settled owner decision; re-litigating it here would be QA overstepping a decision that isn't QA's to re-open). Only the *rationale* (no consumer code has appeared) was re-checked.
- **`qdrant`/`ollama`** — still correctly out of scope for 012 (no LLM/RAG path in this feature).
- **004's public Sample Explorer** — still correctly out of scope (no public read path yet, by design, DEC-023).
- **Full live CRUD round-trip re-drive** (team/trust/project create-read-delete cycles) — not repeated this pass; those were independently live-verified in the prior pass (v1.1 §2 item 5) against backend code that did not change in this bug-fix pass. The backend suite (157/157, unchanged) is the relevant regression signal for backend behavior this pass; browser/frontend verification effort this pass was concentrated on the actual thing that changed (the layout restructure).
- Did not attempt to fix anything — reporting only, per QA's role.

## 7. Overall verdict (v1.2, superseded — see §8, further superseded by §9)

**New Concern #6 (marketing nav/footer bleeding onto `/admin/**`, including the 390px overlap defect) is CONFIRMED FIXED, independently, live, at both viewports — not just by trusting Dev's self-reported curl output.** The root-cause fix (route-group restructure moving `SiteNav`/`SiteFooter` out of the root layout into a `(marketing)`-scoped layout) is structurally sound — `/admin/**` sits outside that route group and cannot inherit marketing chrome by construction, not by a per-route opt-out that could later be missed. `AdminShell`'s own responsive behavior (drawer, table→card collapse) is confirmed unaffected. Public marketing pages are confirmed unaffected. The full backend suite (157/157) and `tsc --noEmit` are both reconfirmed clean, as expected for a frontend-only layout change.

**New Bug #3 (`worker` boot failure)** remains a deliberately deferred, documented, owner-approved decision (DEC-032/PITFALL-009) and is correctly out of scope for re-testing in this pass. Its stated rationale (no `lead.created`/`kb.changed` consumer exists yet) was re-checked and still holds.

With both of this pass's items resolved — Concern #6 closed, and New Bug #3's deferral rationale confirmed still valid — **there are no open QA findings against feature 012.** Every F12-AC1..30 is PASS by id (carried across three QA passes with no regression), the full suite is green, `tsc --noEmit` is clean, and `docker compose up --build` produces a working `web`+`api`+`postgres`+`redis` stack with a verified admin console that is now also visually and structurally isolated from the public marketing site as design.md requires.

**Status at the time: approved (v1.2).** Routing to PM via handoffs/9-qa-to-pm.md. **This call turned out to be premature** — see handoffs/10-dev-to-qa.md and §8 below: a human click-through, after this approval, found `/admin/projects/new` and `/admin/projects/[id]` completely broken in the browser (F12-AC4/F12-AC5 never actually exercisable through the UI), invisible to this pass's API-level and code-level checks. That is the exact gap this v1.3 pass was launched to close, and (per §8) could not close, this time for an unrelated, external reason.

---

## 8. Pass 4 (2026-07-17): independent re-verification of the PITFALL-010 fix (project create/edit) — INCOMPLETE, blocked by a host-infrastructure failure

**Read first for this pass:** handoffs/9-qa-to-pm.md (the premature approval), handoffs/10-dev-to-qa.md (Dev's bug-fix pass 3, root cause and fix), LEARNINGS.md PITFALL-010, implementation.md's "Bug-fix pass 3" section.

**Scope, per handoffs/10-dev-to-qa.md:** independently confirm, live in a real browser (not curl, not a code read), that `/admin/projects/new` and `/admin/projects/[id]` (F12-AC4/F12-AC5) now render and function correctly post-fix; spot-check the two pages for regressions (upload field, scope checkboxes, validation errors, Cancel button, redirect-on-save); click through `/admin/trust` and `/admin/samples` new/edit screens for the same anti-pattern Dev already grepped for but did not click-through.

### 8.1 What was completed, independently, and is solid

- `cd api && ./mvnw test` → **157/157, 0 failures/errors.** Reproduced independently. Unchanged from the prior pass, as expected — this bug-fix pass touched only frontend files.
- `cd web && npx tsc --noEmit` → **exit 0, no output.** Reproduced independently.
- **Static/code-level review of the actual changed files** (in addition to, not instead of, the intended browser check — done because it was available before the environment failure hit):
  - `web/src/app/admin/(protected)/projects/new/page.tsx` — confirmed this is now a **Server Component** (no `"use client"`), calls `getProjectFilters()` inside an `async function` component body (server-side, not a `useEffect`), passes the result as a `filters` prop to `<ProjectForm mode="create" filters={filters} />`. `export const dynamic = "force-dynamic"` present, with a doc comment explaining the CORS rationale (matches PITFALL-010 exactly). No client-side call to `getProjectFilters()` remains.
  - `web/src/app/admin/(protected)/projects/[id]/page.tsx` — same Server Component pattern; fetches `filters` server-side, delegates the (session-cookie-dependent) project-record fetch to a new client component, `id` resolved via `await params` (Next 15 async params).
  - `web/src/app/admin/(protected)/projects/[id]/edit-project-client.tsx` (new file) — `"use client"`; its only network call is `getAdminProject(id)` from `web/src/lib/api/admin-projects.ts`, the CORS-enabled, `credentials: 'include'` admin client under `/api/admin/**` (the correct client for a browser-side call) — not the CORS-unsafe `getProjectFilters()` that caused the original bug. Renders `<ProjectForm mode="edit" initial={project} filters={filters} />` once loaded, a "Loading…" state before that resolves, and "Project not found." on fetch failure.
  - This is structurally consistent with the root cause and fix described in handoffs/10-dev-to-qa.md and PITFALL-010, and does not reintroduce the anti-pattern (a client component directly calling a non-`admin-*`, non-CORS-enabled API client) on either screen.
- `docker compose build web` (cached) succeeded once, early in this pass, producing a valid image — before a subsequent `docker compose build --no-cache web` (issued by QA to force a from-scratch image for the browser click-through, matching this feature's own established prior-pass verification pattern, e.g. testing.md v1.1/v1.2's `--no-cache` builds) triggered the environment failure in §8.2.

### 8.2 What could NOT be completed, and why — a host-infrastructure failure, not a code finding

Mid-pass, `docker compose build --no-cache web` failed with `write /var/lib/docker/buildkit/metadata_v2.db: input/output error`. Investigation found the host machine's disk was, at that moment, essentially full: `df -h /` reported free space fluctuating between **~125Mi and ~1.3Gi of a 228Gi volume, at 91–100% capacity**, across repeated checks over the rest of this pass. This is a pre-existing low-headroom condition on this shared machine (Docker Desktop's own virtual disk, `Docker.raw`, is independently **34Gi**; `docker ps -a`/`docker volume ls` surfaced multiple unrelated containers/volumes from other, older, unrelated projects on this machine — e.g. `demo_*`, `landoproxyhyperion5000gandalfedition_*`, dated ~2 years old) that the `--no-cache` rebuild's fresh `npm ci` + full layer copy pushed over the edge.

This left Docker's local storage in a **corrupted, not merely full**, state that persisted even after the OS reclaimed some space: `docker images` returned **empty** (the previously-built, valid cached image from earlier in this pass was gone), and `docker system df`, `docker system prune -af`, and `docker builder prune -af` **all failed with the same `input/output error`** writing to `overlay2`/`buildkit` metadata. Basic daemon operations (`docker ps -a`, `docker volume ls`) still worked, confirming the daemon process itself was up but its build/image storage layer was not.

Repairing this requires either a Docker Desktop "Clean / Purge data" / VM reset (GUI-only, and would also delete the unrelated containers/volumes belonging to other, unrecognized projects on this machine — not an action QA should take unilaterally without the machine owner's explicit consent) or first freeing substantial host disk space. Both are outside a QA subagent's tool access (no GUI, no `sudo`) and outside QA's mandate (this is not a codebase defect to fix). QA stopped attempting further Docker operations once free disk repeatedly measured under 1Gi, specifically to avoid destabilizing a shared machine that also runs unrelated projects any further.

Also checked and ruled out as a workaround: local (non-Docker) `next dev`/`next build` — CLAUDE.md already documents this machine's local Node as `18.12.0`, below Next.js 15's `^18.18.0` requirement; confirmed still the case (`node --version` → `v18.12.0`) and no local Postgres is installed (`pg_ctl` not found) — so a non-Docker verification path is not viable either, consistent with `docker compose build web` being the documented actual gate (CLAUDE.md TODOs).

**As a direct result, QA could NOT bring up the `docker compose` stack and could NOT independently drive a real (headless-Chrome/Puppeteer) browser session this pass.** None of the following, explicitly required by handoffs/10-dev-to-qa.md, could be completed:
- Loading `/admin/projects/new` and `/admin/projects/[id]` in an actual browser to confirm the form renders (not "Loading…" forever), all vocabulary dropdowns (sector/country/status/scope) are populated with real options, and the browser console/devtools network tab is free of CORS or other errors.
- Filling out and submitting the create form, confirming a project is actually created and the page redirects to the list.
- Editing an existing project's field, saving, and confirming the change persisted (via the list or a refetch).
- Spot-checking the upload field, scope checkboxes, validation-error display (submit with a required field empty), the Cancel button, and redirect-to-list behavior on both pages.
- Click-through of `/admin/trust` and `/admin/samples` new/edit screens for the same client-component-calling-a-non-CORS-client anti-pattern (Dev's own re-check here was a grep, not a click-through, per handoffs/10-dev-to-qa.md item 3 — this pass could not supply the stronger check either).

### 8.3 Verdict for this pass

**Not approved. Not "found broken" either — genuinely inconclusive.** The specific, explicit ask of this pass — independently confirm the fix *live in a browser*, not by trusting Dev's report and not by trusting a static code read — could not be carried out, for reasons entirely outside the application code (host disk exhaustion and consequent Docker storage corruption, encountered while QA was mid-way through exactly the kind of `--no-cache` rebuild this feature's own prior passes established as the correct verification pattern).

Declaring the feature ready to close on the strength of a clean backend suite, a clean `tsc --noEmit`, and a code-level read alone would repeat precisely the mistake that made this v1.3 pass necessary in the first place: v1.2's `approved` status (§7) was itself reached substantially via API-level and code-structural checks rather than a full, real browser click-through of every CRUD screen, and that is exactly the gap PITFALL-010 documents as invisible to non-browser checks. QA is not willing to make that same substitution twice, even under environment pressure. The §8.1 code review is genuine, positive evidence that the fix is structurally sound and matches PITFALL-010's documented root cause and remedy precisely — but it is evidence in favor, not a replacement for the live check this pass exists to perform, and is explicitly not being represented as equivalent to one.

No new regression or code defect was found in Dev's bug-fix pass 3 this pass — there is simply no completed verification either way for the live-browser behavior of `/admin/projects/new`, `/admin/projects/[id]`, `/admin/trust/*`, or `/admin/samples/*`.

**Status: `blocked` — on environment, not on code.**

**Recommendation:**
1. Free host disk space and/or restart/reset Docker Desktop on this machine (a human/ops action; outside QA's and Dev's tool access and outside this repo).
2. Re-run this exact check — live-browser click-through of `/admin/projects/new`, `/admin/projects/[id]` (both the render-and-populate check and a full create/edit/save round-trip), plus `/admin/trust/*` and `/admin/samples/*` for the same anti-pattern — once a working `docker compose up` stack can be brought up end to end.
3. Do not close feature 012, and do not treat v1.2's `approved` status as current, until that pass completes clean. This is the same discipline handoffs/10-dev-to-qa.md itself asked for; an infrastructure failure is not a reason to relax it.

---

## 9. Pass 5 (2026-07-17, this pass): live-browser re-verification completed — environment now healthy, feature 012 approved

**Read first for this pass:** handoffs/11-qa-to-dev.md (this pass's direct predecessor and the full blocked-pass detail, §8 above), handoffs/10-dev-to-qa.md (Dev's bug-fix pass 3), LEARNINGS.md PITFALL-010.

**Trigger:** the host machine's disk was freed (Docker Desktop restarted, 3 unrelated old Lando containers removed, `docker builder prune`/`docker image prune` reclaimed ~23GB) and re-confirmed healthy — `df -h /` now reports 28Gi free of 228Gi, 32% capacity (previously 91–100%). A sanity `docker compose build web` succeeded before this pass began. This is purely an environment fix; no application code changed since handoffs/10-dev-to-qa.md / handoffs/11-qa-to-dev.md.

### 9.1 Environment bring-up

```
docker compose up -d postgres redis api web
```
All four containers reached `Up`/`healthy` within ~25s. `GET /api/projects` → `200` (15 seed projects). `GET /` (web) → `200`. Containers remained stable (`Up About an hour`, zero restarts) for the full duration of this pass.

### 9.2 Tooling note

No interactive GUI browser was available in this environment, so this pass drove a real, independent headless-Chrome session via `puppeteer-core` (installed fresh into a QA scratchpad directory, no source-tree changes) pointed at the same cached Chrome-for-Testing binary (`mac_arm-126.0.6478.182`) used by prior QA passes — a genuine browser (full DOM, JS execution, real network stack, real CORS enforcement), not a curl/API-level substitute. `console.error`, uncaught `pageerror`, and failed network requests were captured on every navigation.

### 9.3 `/admin/projects/new` — F12-AC4

- Logged in via the real UI form (`#login-username` / `#login-password` / submit) — landed on `/admin`.
- Navigated to `/admin/projects/new`. **Form fully rendered, not stuck on "Loading…".** All fields present: SLUG*, NAME*, SECTOR* (dropdown, 6 real options: airport / mall_retail / hotel_hospitality / residential / infrastructure_rail / leisure_museum), COUNTRY* (free-text input), STATUS* (dropdown, 2 real options: ongoing / completed), LOCATION, SCOPE (6 checkboxes: rebar / BBS / GA / MEP / QS / as-built), "Featured on home marquee" checkbox, IMAGE (file upload with "Choose File" + JPG/PNG/WEBP-5MB hint), DESCRIPTION (textarea), MAIN CONTRACTOR, CLIENT, CONSULTANT. Screenshot confirms visual rendering matches design tokens (no unstyled/raw content).
- **Zero console errors, zero failed requests** on this page load (specifically checked for CORS — none found; the vocabulary dropdowns are populated with real data, confirming `getProjectFilters()` resolved server-side, exactly as PITFALL-010's fix describes).
- **Validation (F12-AC23):** submitted the form empty. Confirmed via `form.checkValidity()` → `false`, and via `input[required]` attributes present on `p-slug`, `p-name`, `p-sector`, `p-country`, `p-status` (the exact 5 asterisked fields). The browser's native HTML5 constraint validation blocked submission — the page stayed on `/admin/projects/new`, no navigation occurred. This is a legitimate, visible-to-the-user error mechanism (a native validation bubble), not a silent failure.
- **Full create round-trip:** filled slug/name/country/location/description/main-contractor via real DOM `input`/`change` events (sector/status left at their default first-option values, one scope checkbox ticked), clicked SAVE. **Result: navigated to `/admin/projects`, and the new "QA012 Test Project" row appeared in the list**, with zero console/request errors during submission. Confirmed independently via `GET /api/admin/projects` that the record was persisted with all submitted fields.
- **Cancel button:** verified separately — clicking Cancel on `/admin/projects/new` (without submitting) navigates back to `/admin/projects`, no record created.

**F12-AC4: PASS, confirmed live.**

### 9.4 `/admin/projects/[id]` — F12-AC5

- Fetched the real admin project list via `GET /api/admin/projects` (in-browser, session-cookie-authenticated) to get a real numeric id (id `1`, `doha-metro-gold-line`).
- Navigated to `/admin/projects/1`. **Form loaded pre-filled, not stuck on "Loading…"** — confirmed the NAME field's value was `"Doha Metro - Gold Line"` before any edit. Zero console/request errors.
- Edited the LOCATION field to a unique test value via real DOM events, clicked SAVE. Navigated to `/admin/projects`, zero errors during save.
- **Confirmed persisted** via a follow-up `GET /api/admin/projects/1`: the returned `location` matched the new value exactly.
- **Reverted** the edit back to the original value (`"Doha, Qatar"`) through a second real UI edit round-trip (not a raw API call) — confirmed via a third `GET` that the original value was restored, leaving no residue on this seed record.

**F12-AC5: PASS, confirmed live.**

### 9.5 Delete confirmation (F12-AC6, F12-AC24) — used for cleanup

- On `/admin/projects`, clicked DELETE on the "QA012 Test Project" row created in §9.3. A confirmation dialog (`role="dialog"`) appeared: "Delete this project? Delete project 'QA012 Test Project'? This removes it from the public site immediately (including the home marquee and RAG knowledge base) and cannot be undone." with CANCEL and a red-styled DELETE button (matches DEC-031's danger token, screenshot confirmed).
- Clicked the dialog's DELETE button. Confirmed via `GET /api/admin/projects` that the QA test project no longer appears in the list, and via `GET /api/projects` that the public project count returned to the original 15 (no orphaned test data left behind).

**F12-AC6, F12-AC24: PASS, reconfirmed live (delete + explicit confirmation both function correctly, and were used to leave the environment clean).**

### 9.6 `/admin/trust` and `/admin/samples` — spot-check for the same anti-pattern

Per scope, a quick load + console check (not a full CRUD cycle):

- `/admin/trust` (list): loads correctly, zero errors, shows grouped stat/software/standard rows.
- `/admin/trust/new`: form renders fully (TYPE dropdown with stat/software/standard, ITEM KEY*, LABEL*, VALUE*, UNIT, DISPLAY ORDER), zero errors.
- `/admin/trust/1` (edit, existing seed row): loads pre-filled (`Edit Trust Row`), not stuck loading, zero errors.
- `/admin/samples` (list): loads correctly, zero errors. Confirmed empty (0 rows) — consistent with the documented known gap (no V13 seed was written for `sample`, CLAUDE.md TODOs).
- `/admin/samples/new`: form renders fully (SLUG*, TITLE*, CATEGORY* dropdown, DISPLAY ORDER, PREVIEW IMAGE upload, DOWNLOADABLE FILE upload), zero errors.
- `/admin/samples/[id]`: could not be exercised against a real record because the `sample` table is empty (documented known gap, not a regression from this pass or from PITFALL-010's fix — no V13 seed was ever written, by Dev's own call per implementation.md). The `/admin/samples/new` check plus the identical-pattern confirmation on `/admin/trust/[id]` (which shares the exact same code pattern, per Dev's original grep in handoffs/10-dev-to-qa.md item 3) is sufficient evidence the anti-pattern was not reintroduced on the samples edit screen either.

**No CORS or other console errors found on any of these six screens. No regression of the PITFALL-010 anti-pattern found anywhere.**

### 9.7 Regression check

- `./mvnw test` → **157/157, 0 failures/errors.** Re-run independently *after* the browser CRUD testing, specifically to also confirm the live create/edit/delete round-trips didn't leave the backend in a bad state.
- `npx tsc --noEmit` → **exit 0, no output.**
- `docker compose ps`: all four containers `Up`/`healthy` throughout, zero restarts.
- Public project count (`GET /api/projects`) confirmed back at 15 (the original seed count) after this pass's cleanup — no orphaned test data.

### 9.8 Verdict for this pass

**All items required by handoffs/10-dev-to-qa.md and carried forward from the blocked handoffs/11-qa-to-dev.md are now complete, live, and clean:**

1. `/admin/projects/new` renders fully (all fields, all vocab dropdowns populated with real data), zero console/CORS errors, full create round-trip works (fill → submit → creates record → redirects to list). **PASS.**
2. `/admin/projects/[id]` loads pre-filled for a real existing project, zero console/CORS errors, full edit round-trip works (load → edit → save → persists, confirmed via refetch). **PASS.**
3. Spot-checks on both pages: upload field present and correctly typed/hinted, scope checkboxes present and functional, validation-error mechanism present and functional (native HTML5 required-field blocking, confirmed via `checkValidity()`), Cancel button returns to the list without saving, redirect-to-list on save confirmed. **No regressions found.**
4. `/admin/trust/*` and `/admin/samples/*` new/edit screens click-through: no instance of the PITFALL-010 anti-pattern (a client component calling a non-CORS API client) found; all screens render cleanly with zero console errors.
5. Test data cleanup: the one project created during this pass (`QA012 Test Project`) was deleted via the real UI delete-confirm flow; the one seed project edited for the edit-round-trip test (`Doha Metro - Gold Line`) was reverted to its original value via a second real UI edit. Public project count confirmed back at the original 15.
6. Backend suite (157/157) and `tsc --noEmit` (clean) both reconfirmed independently, after the browser testing, with no regression.

**No blocking findings. No new bugs. No regressions.** PITFALL-010's fix (bug-fix pass 3, handoffs/10-dev-to-qa.md) is now genuinely, independently, live-verified — closing the exact gap that made v1.3 (§8) and the prior blocked handoff necessary.

**Status: `approved` (v1.4).** Feature 012 has no open QA findings. New Bug #3 (`worker` boot failure) remains a deliberately deferred, owner-approved decision (DEC-032) and is not re-litigated here, per explicit scope. Routing to PM via handoffs/12-qa-to-pm.md.

## 10. Pass 6 (2026-07-19, this pass): independent re-verification of the PITFALL-011 fix (uploaded images never rendering) — CONFIRMED FIXED

**Read first for this pass:** handoffs/12-qa-to-pm.md (the v1.4 approval this pass reopens), handoffs/13-dev-to-qa.md (Dev's bug-fix pass 4, root cause and fix), LEARNINGS.md PITFALL-011, implementation.md's "Bug-fix pass 4" section.

**Trigger:** a human manually uploaded a real team photo through the running admin console after v1.4's approval and found it never rendered anywhere — not in the admin form's own preview, not on the public About page. Root cause: `UploadService.store()` returns a root-relative `/uploads/...` path, correct only as a path on the `api` container; every `<img src={...}>` rendering an uploaded value used that path verbatim, which resolves against the *page's own* origin (`web`, `:3000`) in the browser, 404ing silently. `curl`ing the same path against `:8080` always worked, which is exactly why no prior API-level check caught it. Dev's fix: `resolveUploadUrl()` in `web/src/lib/utils.ts`, applied at all four render sites (`upload-field.tsx`, `team-card.tsx`, `project-card.tsx`, `project-detail.tsx`).

**Scope, per handoffs/13-dev-to-qa.md:** independently re-verify live, in a real browser (not curl, not a code read) — upload a real image to a team member and confirm it renders in both the admin preview and the public About page; check the resolved `<img src>` hits the API origin, not the web origin; spot-check the project image upload+render path too; confirm no console/CORS errors; clean up test data; sanity-check the backend suite and `tsc --noEmit`.

### 10.1 Code review (supplementary, not a substitute for the live check below)

Read `web/src/lib/utils.ts`'s `resolveUploadUrl()` and all four call sites (`components/admin/upload-field.tsx`, `components/team/team-card.tsx`, `components/projects/project-card.tsx`, `components/projects/project-detail.tsx`). Confirmed structurally correct: the helper only rewrites paths that literally start with `/uploads/` (prefixing with `NEXT_PUBLIC_API_BASE_URL`, defaulting to `http://localhost:8080`), leaving static bundled assets (e.g. `/img/projects/*.svg`) untouched, and is applied at every render site that can hold an uploaded value. No missed call site found.

### 10.2 Environment bring-up

```
docker compose build --no-cache web   → succeeds, route table unchanged from v1.4's
docker compose up -d postgres redis api web   → all four containers Up/healthy
```
Disk space healthy this pass (25Gi free / 34% capacity) — the infra failure that blocked v1.3 (§8) was not a factor here.

### 10.3 Tooling note

Same approach as prior passes: a real, independent headless-Chrome session driven via `puppeteer-core` (freshly installed into a QA scratchpad directory, no source-tree changes) against the same cached Chrome-for-Testing binary (`mac_arm-126.0.6478.182`). A genuine browser — full DOM, JS execution, real network stack, real CORS enforcement — not a curl/API-level substitute. Console errors, page errors, and every `/uploads/`-matching network response were captured on every navigation. A real 200×200 PNG (`test-photo.png`, verified via `file` to be a genuine PNG, not a renamed file) was used as the upload payload throughout.

### 10.4 Team photo upload — admin preview (F12-AC20)

- Logged in via the real UI form. Navigated to `/admin/team/1` (Ali Bin Beyat, seed record, `photo: null` beforehand — confirmed via `GET /api/admin/team/1`).
- Uploaded `test-photo.png` via the real `<input type="file">`. The network response listener confirmed a `200` on `http://localhost:8080/uploads/team/<uuid>.png` — the correct API origin, not `localhost:3000`.
- **Admin preview thumbnail rendered correctly**, checked immediately after the upload settled and again 1.5s later: `{"src":"http://localhost:8080/uploads/team/8779b02e-...png","naturalWidth":200,"naturalHeight":200,"complete":true}` both times. Not a broken-image icon, not `naturalWidth: 0`.
- Saved the record. `GET /api/admin/team/1` confirmed the `photo` field persisted as `/uploads/team/8779b02e-...png`.

**PASS.**

### 10.5 Team photo — public About page (F12-AC11)

- Loaded `/about` in a fresh page, scrolled the full page height (triggering every `loading="lazy"` `<img>` along the way, per Dev's flagged gotcha), waited 2s after the scroll settled, then read every `<img>`'s resolved `src`/`naturalWidth`/`naturalHeight`.
- **Ali Bin Beyat's newly-uploaded photo:** `src: http://localhost:8080/uploads/team/8779b02e-...png`, `naturalWidth: 200`, `naturalHeight: 200`. Renders correctly.
- **Jeyamani Jeyaraman's pre-existing real (~4MB) uploaded photo** (present in the DB before this pass began, per handoffs/13-dev-to-qa.md's own re-verification note — not created by this pass, left untouched): `src: http://localhost:8080/uploads/team/3db12403-...jpg`, `naturalWidth: 3798`, `naturalHeight: 5316`. Renders correctly, confirming the fix also holds for a large real-world file, not just a small synthetic test PNG.
- Both `<img>` requests hit `http://localhost:8080/uploads/...` — the correct API origin — confirmed via the response listener (`200` on both).
- **Zero console errors, zero CORS errors** on this page load (the only console error observed across this entire pass, on any page, was an unrelated pre-existing `favicon.ico` 404 on `/admin/login`, confirmed in isolation and out of scope for this pass — not a regression, not related to PITFALL-011).

**PASS.**

### 10.6 Project image upload — admin preview + public list + public detail (F12-AC20, spot-check per handoffs/13-dev-to-qa.md item 3)

- Navigated to `/admin/projects/1` (Doha Metro - Gold Line, seed record, `image: /img/projects/doha-metro-gold-line.svg` beforehand — a static bundled asset, not an upload, confirmed via `GET /api/admin/projects/1`).
- Uploaded `test-photo.png`. First read of the preview `<img>` (taken immediately after the upload's own network response resolved) showed `naturalWidth: 0` — this is a script-timing artifact, not a defect: a follow-up, properly-awaited check (waiting explicitly for the `<img>`'s `load` event / `complete === true`, done via a separate throwaway `/admin/projects/new` form never submitted) confirmed the same preview mechanism reliably renders at `naturalWidth: 200, naturalHeight: 200, complete: true` once given a moment to decode — the same class of "checked before the image finished loading" gotcha Dev flagged for lazy-loading, just triggered by decode timing instead of viewport visibility. Not a rendering bug.
- Saved the record with the uploaded image. `GET /api/admin/projects/1` confirmed `image: /uploads/projects/<uuid>.png` persisted.
- **Public `/projects` list**, scrolled + waited (lazy-load): the Doha Metro card's `<img>` resolved to `http://localhost:8080/uploads/projects/<uuid>.png`, `naturalWidth: 200`, `naturalHeight: 200`. Renders correctly.
- **Public `/projects/doha-metro-gold-line` detail page**: hero `<img>` resolved to the same `http://localhost:8080/uploads/projects/<uuid>.png`, `naturalWidth: 200`, `naturalHeight: 200`. Renders correctly.
- All three `<img>` network requests (admin preview, list, detail) hit the API origin (`:8080`), never the web origin (`:3000`). Zero console/CORS errors on any of the three pages.

**PASS.**

### 10.7 Cleanup

- **Team id 1 (Ali Bin Beyat):** photo removed via the real UI ("Remove" button + Save), confirmed via `GET /api/admin/team/1` → `photo: null`, and via the public `GET /api/team` → `photo: null`. Left exactly as found.
- **Project id 1 (Doha Metro - Gold Line):** the UI's `UploadField` only supports "Choose file" / "Remove" (sets the field to `null`), with no way to type a URL back in — so a UI-only revert could not restore the exact original static-asset path (`/img/projects/doha-metro-gold-line.svg`). Reverted via a direct authenticated `PUT /api/admin/projects/1` reproducing the record's exact pre-pass field values (same session-cookie + CSRF-token flow the admin UI itself uses, not a bypass of the API contract). `GET /api/admin/projects/1` confirmed every field, including `image`, matches the pre-pass state exactly (`updatedAt` naturally advanced, all other fields byte-identical). Public project count confirmed still 15 (`GET /api/projects`, `totalElements: 15`).
- Orphaned upload files left on disk from this pass's test uploads (the team photo replaced by "Remove", the project photo replaced by the API revert, and one throwaway upload from the `/admin/projects/new` timing re-check that was never submitted) are not garbage-collected — this is a pre-existing, documented v1 gap (architecture §8, CLAUDE.md TODOs), not something this pass introduced or is expected to clean up; no admin-facing delete-raw-file affordance exists.

### 10.8 Regression check

```
cd api && ./mvnw test  → Tests run: 157, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.
cd web && npx tsc --noEmit  → exit 0, no output.
```
Both re-run independently *after* the live browser upload/save/revert activity, to also confirm it didn't leave the backend in a bad state. `docker compose ps` showed all four containers `Up`/`healthy` throughout, zero restarts.

### 10.9 Verdict for this pass

**PITFALL-011's fix (bug-fix pass 4, handoffs/13-dev-to-qa.md) is confirmed, live, independently, in a real browser — not by trusting Dev's report and not by a code read alone (§10.1 was supplementary, not a substitute).** Both required resource types were exercised end-to-end:

1. Team photo: admin preview renders (`naturalWidth: 200`), public About page renders both the newly-uploaded photo and a large (~4MB) pre-existing real photo, both after correctly handling `loading="lazy"`. **PASS.**
2. Project image: admin preview renders (once properly awaited), public projects list renders, public project detail page renders. **PASS.**
3. Every checked `<img>` request resolved against `http://localhost:8080/uploads/...` (the API origin) — never `http://localhost:3000/uploads/...` (the web origin that would 404). **Confirmed, no exceptions found.**
4. Zero CORS errors, zero other console errors attributable to the upload/render path, on any admin or public page checked this pass.
5. Test data cleanup: team id 1 reverted to `photo: null` via the real UI; project id 1 reverted to its exact original state via an authenticated API call (the UI itself has no affordance to type a URL back in, so this was the only way to restore the *exact* pre-pass value rather than merely clearing it). Public project count and team roster both confirmed unchanged from pre-pass state.
6. Backend suite (157/157) and `tsc --noEmit` (clean) both reconfirmed after the live activity, no regression.

**No blocking findings. No new bugs. No regressions.** This closes the exact gap that reopened QA approval after handoffs/12-qa-to-pm.md.

**Status: `approved` (v1.5).** Feature 012 has no open QA findings. New Bug #3 (`worker` boot failure, DEC-032) remains a deliberately deferred, owner-approved decision and is not re-litigated here, per explicit scope. Routing to PM via handoffs/14-qa-to-pm.md.

## Gaps (carried forward from all prior passes; not newly introduced this pass)

- **`worker` service / New Bug #3's underlying defect** — still deliberately deferred per DEC-032, not this pass's concern.
- **`/admin/samples/[id]`** could not be exercised against a real record because the `sample` table ships empty (documented known gap, not a 012 defect — no V13 seed was written, by Dev's own call per implementation.md).
- **`qdrant`/`ollama`** — out of scope for 012 (no LLM/RAG path in this feature).
- **004's public Sample Explorer** — out of scope (no public read path yet, DEC-023).
- **Orphaned uploads** (this pass's own test uploads included) are not garbage-collected — pre-existing, documented v1 gap (architecture §8), not newly introduced.
- **Public content cache latency** (up to 60s for admin writes to appear on the public site, `next: { revalidate: 60 }`, no on-demand invalidation) — pre-existing, documented gap (implementation.md, CLAUDE.md TODOs), not re-tested this pass, unaffected by PITFALL-011's fix.
- Did not attempt to fix anything — reporting only, per QA's role.
