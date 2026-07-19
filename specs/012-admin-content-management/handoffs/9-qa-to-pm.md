# Handoff: Admin Content Management — QA -> PM (ready to close)

| Field | Value |
|---|---|
| Feature # | 012 |
| From / To | QA -> PM |
| Status | **ready** — New Concern #6 confirmed fixed, independently, live; New Bug #3 remains a deliberate, owner-approved deferral (DEC-032) with its rationale re-confirmed current; no new regressions found; testing.md status is now `approved` (v1.2) |
| Spec | specs/012-admin-content-management/testing.md (v1.2) |
| Prior | handoffs/7-qa-to-dev.md (blocked, v1.1), handoffs/8-dev-to-qa.md (Dev's bug-fix pass 2) |

## Summary

This is QA's third pass on feature 012. The first two passes (testing.md v1.0, v1.1) found and confirmed-fixed two critical bugs (CSRF scoping, `next build` failure) and two design-fidelity concerns (logout CSRF narrowing, delete-button styling), then surfaced two new findings from the first full `docker compose` + real-browser session: **New Bug #3** (the `worker` container fails to boot under `AdminSecurityConfig`) and **New Concern #6** (the admin route tree inherited the public marketing nav/footer, contradicting design.md §B.1, with a real overlap defect at 390px).

The project owner reviewed New Bug #3 and made a deliberate, documented call (**DEC-032**, **PITFALL-009**) that it does not block 012's close — the `worker` service isn't run in this environment and has zero live consumers today (feature 001 unbuilt), so a worker that fails to boot and one that boots-and-does-nothing are currently indistinguishable. This pass re-checked DEC-032's stated rationale (no worker-consumer code has appeared since the last pass — confirmed via a targeted grep of `api/src/main/java`) and did **not** re-test or re-block on the underlying boot-failure defect itself, per the owner's explicit instruction not to re-litigate a settled decision.

Dev's bug-fix pass 2 (handoffs/8-dev-to-qa.md) fixed New Concern #6: every public page was moved into a new `web/src/app/(marketing)/` route group, `SiteNav`/`SiteFooter` moved out of the root layout into `(marketing)/layout.tsx`, and the root layout (`web/src/app/layout.tsx`) now only provides `<html>`/`<body>`/fonts/metadata. This pass **independently re-verified that fix, live** — not by trusting Dev's self-reported curl output:

- **Fresh build/bring-up:** `docker compose down` → `docker compose build --no-cache web` → succeeds, route table identical to the prior pass's (same URLs, same static/dynamic split, `/admin/login` still prerendered). `docker compose up -d postgres redis api web` → all containers healthy.
- **Browser-driven (headless Chrome/Puppeteer, real DOM inspection), both 1440px and 390px:** `/admin/login` renders with **zero** `<nav>`/`<footer>` elements and no marketing text anywhere in the DOM, at both viewports — the specific 390px overlap this concern originally screenshotted is now fully absent, not just non-overlapping. Logged in via the real UI form and loaded `/admin/projects` (an authenticated `(protected)` screen): zero `<footer>` elements at both viewports; the one `<nav>` present is confirmed via screenshot to be `AdminShell`'s own sidebar (Dashboard/Projects/Team/Trust/Samples), structurally and visually distinct from the marketing `SiteNav` — no "Enquire Now", no marketing logo lockup, no About/Services/Contact links.
- **`AdminShell`'s own responsive behavior unaffected:** at 390px, the hamburger (`aria-label="Open menu"`) opens a drawer with the same nav items (screenshot confirmed); the projects list correctly collapses table→stacked-cards. No regression from the layout restructure.
- **Public marketing pages unaffected:** `/about` and `/projects` at 1440px render full `SiteNav`/`SiteFooter` and page content correctly (screenshot confirmed); one automated grep false-negatived on "Enquire Now" due to CSS `text-transform: uppercase` on the button (a test-script gotcha, not a product bug — resolved by cross-checking raw HTML and a screenshot).
- **Middleware redirect unaffected:** `/admin` (unauthenticated) still 307s to `/admin/login?next=%2Fadmin`.
- **Backend suite and `tsc --noEmit` reconfirmed unaffected:** `./mvnw test` → 157/157, unchanged from the prior two passes (expected — this bug-fix pass touched only frontend layout files). `npx tsc --noEmit` → clean.

**Verdict: New Concern #6 is CONFIRMED FIXED. No new regressions found anywhere this pass touched.**

## Acceptance criteria

All F12-AC1 through F12-AC30 remain PASS by id, carried across three QA passes with no regression (full detail: testing.md v1.2 §4, and the underlying evidence trail in testing.md v1.1 / handoffs/5-qa-to-dev.md / handoffs/7-qa-to-dev.md for ids not re-touched this pass). F12-AC1 and F12-AC27 are explicitly reconfirmed this pass (auth gating + the now-closed Concern #6 design-fidelity gap; responsive `AdminShell` behavior post-restructure).

## Definition-of-Done checklist (QA's view)

- [x] All acceptance criteria in requirements.md met, by id — 29/29 F12-ACn, no CONCERN, no FAIL.
- [x] Full test suite passes, no regressions — 157/157 backend, `tsc --noEmit` clean, all independently re-run this pass.
- [x] Doc comments — spot-checked across three passes, no gaps found (including the new `(marketing)/layout.tsx` and updated `layout.tsx`, both carry intent-focused doc comments explaining the Concern #6 fix).
- [x] No external schema assumed without confirmation — n/a to this pass, unchanged from prior passes.
- [x] LLM/lead-path hardening — n/a to 012 (no LLM path; lead-path hardening is 011's, unaffected here).
- [x] No confidential-upload path uses a free training tier — n/a to 012's upload feature (local filesystem, no model call).
- [x] No open questions remain — New Bug #3 is not an open question, it's a closed, documented decision (DEC-032); New Concern #6 is closed (fixed, verified).
- [x] Knowledge layer updated — DEC-032 and PITFALL-009 present, read, and their content verified accurate against the actual code this pass.
- [x] CLAUDE.md updated — verified accurate against the current repo state: the spec 012 status line, the `worker` known-gap note, and the `web/` "Key files" list (`web/src/app/layout.tsx`, `web/src/app/(marketing)/layout.tsx`, and every moved page path, e.g. `web/src/app/(marketing)/about/page.tsx`) all already reflect the route-group restructure — no stale paths found.
- [x] All five spec files are status: approved or explicitly deferred — requirements.md, architecture.md, design.md, implementation.md, testing.md (this file, now v1.2, status `approved`) — confirm final status roll-up is PM's call.

## What I did not do (and why — not QA's role)

- Did not fix anything — reporting only.
- Did not re-test or re-block on New Bug #3 (`worker` boot failure) beyond re-checking DEC-032's stated rationale — that is a settled, documented owner decision, not an open QA finding, and re-litigating it here would be QA overstepping.
- Did not re-drive full CRUD round-trips (team/trust/project create-read-delete) this pass — those were independently live-verified in the prior pass (v1.1) against backend code that did not change in this bug-fix pass (frontend-only). The unchanged, reconfirmed 157/157 backend suite is the relevant regression signal for backend behavior this pass.

## Recommendation

Feature 012 (Admin Content Management) is ready to close. testing.md is `approved` (v1.2). No blocking findings remain. The one open item (New Bug #3, `worker` boot failure) is a deliberately deferred, documented decision (DEC-032) with a clear trigger for when it must be revisited ("before feature 001 needs the `worker` process to actually run") — not a gap in 012's own deliverable.

## Read first

- specs/012-admin-content-management/testing.md (v1.2, this pass's full evidence)
- docs/knowledge/DECISIONS.md — DEC-032
- docs/knowledge/LEARNINGS.md — PITFALL-009
- specs/012-admin-content-management/handoffs/7-qa-to-dev.md, 8-dev-to-qa.md (this pass's immediate predecessors)

## Do not touch

- Nothing flagged as blocking. No follow-on cleanup items identified this pass.
