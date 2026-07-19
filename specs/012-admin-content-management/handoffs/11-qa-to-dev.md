# Handoff: Admin Content Management — QA -> Dev (blocked, environment failure, not a code finding)

| Field | Value |
|---|---|
| Feature # | 012 |
| From / To | QA -> Dev (cc: PM — this needs a decision on when/how to re-run, not a code fix) |
| Status | **blocked** — the mandated live-browser re-verification of bug-fix pass 3 (F12-AC4/F12-AC5) could not be completed this pass, due to a host-machine disk-space exhaustion and consequent Docker storage corruption hit mid-pass. This is **not** a code defect and **not** a regression finding — QA found nothing wrong with Dev's fix, but also could not confirm it live as explicitly required. testing.md moved to `blocked` (v1.3). v1.2's `approved` status is superseded and should not be treated as current. |
| Spec | specs/012-admin-content-management/testing.md (v1.3, §8) |
| Prior | handoffs/9-qa-to-pm.md (QA's premature approval, v1.2), handoffs/10-dev-to-qa.md (Dev's bug-fix pass 3, this pass's direct predecessor) |

## Summary

This is QA's fourth pass on feature 012, specifically launched to independently re-verify — live, in a real browser, not via curl/API calls or a code read — that Dev's bug-fix pass 3 (handoffs/10-dev-to-qa.md) actually fixed `/admin/projects/new` and `/admin/projects/[id]` hanging on "Loading…" forever (PITFALL-010: a client component calling a server-only, CORS-less API client).

**What was completed and is solid:**
- `./mvnw test` → 157/157, 0 failures/errors, reproduced independently.
- `npx tsc --noEmit` → clean, reproduced independently.
- A direct read of the actual changed files (`web/src/app/admin/(protected)/projects/new/page.tsx`, `.../projects/[id]/page.tsx`, `.../projects/[id]/edit-project-client.tsx`) confirms the fix is structurally exactly as described: both pages are now Server Components fetching `getProjectFilters()` server-side and passing it down as a prop; the project record itself is fetched client-side only via `getAdminProject()` (the CORS-enabled `admin-projects.ts` client), never via the CORS-unsafe helper. No trace of the original anti-pattern remains on either page.

**What could NOT be completed, and is the reason this is blocked rather than approved:**

Mid-pass, a `docker compose build --no-cache web` (issued to get a guaranteed-fresh image for the browser click-through — the same pattern this feature's own prior QA passes established) failed with `write /var/lib/docker/buildkit/metadata_v2.db: input/output error`. The host disk was, at that point, essentially full — `df -h /` showed free space fluctuating between **~125Mi and ~1.3Gi of a 228Gi volume** across the rest of the pass (91–100% capacity). This is a pre-existing low-headroom condition on this shared machine (`Docker.raw` alone is 34Gi; `docker ps -a` surfaced multiple unrelated, ~2-year-old containers from other projects, e.g. `demo_*`, `landoproxy*`) that the rebuild's fresh `npm ci` + layer copy pushed over the edge.

This left Docker's storage **corrupted**, not just full: `docker images` came back empty (the earlier valid cached image was gone), and `docker system df` / `docker system prune -af` / `docker builder prune -af` all failed with the same `input/output error`. The daemon process itself was still responsive (`docker ps -a`, `docker volume ls` worked) — only its build/image storage layer was broken. Repairing this needs either a Docker Desktop "Clean/Purge data" reset (GUI-only, and would also wipe unrelated containers/volumes belonging to other, unrecognized projects on this machine — not something QA should do unilaterally) or first freeing real host disk space. Neither is available to a QA subagent (no GUI, no `sudo`, and freeing arbitrary files on a shared machine is outside QA's mandate).

A non-Docker fallback was also checked and ruled out: local Node is `v18.12.0`, below Next.js 15's `^18.18.0` requirement (already documented in CLAUDE.md as the reason `docker compose build web` is the real gate on this machine), and no local Postgres is installed.

**As a direct result, none of the following, explicitly required by handoffs/10-dev-to-qa.md, could be performed:**
- Loading `/admin/projects/new` / `/admin/projects/[id]` in an actual browser to confirm the form renders (not "Loading…" forever), vocab dropdowns populate, and devtools shows no CORS/other errors.
- A full create-project round-trip (fill form, submit, confirm creation + redirect to list).
- A full edit-project round-trip (load pre-filled form, edit a field, save, confirm persistence).
- Spot-checking the upload field, scope checkboxes, validation-error display, and Cancel button on those two pages.
- Click-through of `/admin/trust` and `/admin/samples` new/edit screens for the same anti-pattern (Dev's own check there was a grep, not a click-through — this pass could not supply the stronger check either).

Full detail, including the exact commands and errors: testing.md v1.3 §8.

## Why this is not an approval, even though nothing was found broken

v1.2's `approved` status (routed via handoffs/9-qa-to-pm.md) was reached substantially through API-level and code-structural checks rather than a full browser click-through of every CRUD screen — and that is exactly the gap that let the F12-AC4/F12-AC5 bug reach a human tester after QA had already signed off (handoffs/10-dev-to-qa.md). Approving again on the strength of a clean test suite plus a code read, without the live check this specific pass exists to perform, would repeat that exact mistake. QA is treating "the environment stopped me" as equivalent in effect to "I did not check" — because it is, from the perspective of what's actually been verified.

## What the receiving role(s) must do

Nothing in *code*. This is not a request for a Dev fix — no defect was found in bug-fix pass 3. What's needed:

1. **Human/ops action** (outside Dev's and QA's tool access): free real disk space on the host machine and/or reset Docker Desktop's local data, so `docker compose build`/`up` can run reliably again.
2. Once the environment is healthy, **re-run this exact pass**: live headless-browser (or manual) click-through of `/admin/projects/new`, `/admin/projects/[id]` (full create + edit round-trips, not just "does it render"), plus `/admin/trust/*` and `/admin/samples/*` for the same anti-pattern.
3. Do not treat testing.md v1.2 (`approved`) or this handoff's absence-of-findings as a green light to close feature 012. Status is `blocked` until the live check actually completes.

## Read first

- specs/012-admin-content-management/testing.md (v1.3, §8 — this pass's full record, including §7's note that v1.2 is superseded)
- specs/012-admin-content-management/handoffs/10-dev-to-qa.md (the pass this was meant to close out)
- docs/knowledge/LEARNINGS.md — PITFALL-010

## Do not touch

- The actual fix in `web/src/app/admin/(protected)/projects/new/page.tsx`, `.../projects/[id]/page.tsx`, `.../projects/[id]/edit-project-client.tsx` — QA's code read found nothing wrong with it; there's no reason to touch it based on this pass.
- The team ISR-latency finding (handoffs/10-dev-to-qa.md) — still a documented, non-blocking known gap, not re-litigated here.
- `AdminSecurityConfig` / worker boot failure — still governed by DEC-032, out of scope, not re-litigated here.
