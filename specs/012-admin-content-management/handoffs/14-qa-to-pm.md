# Handoff: Admin Content Management — QA -> PM (ready to close, again)

| Field | Value |
|---|---|
| Feature # | 012 |
| From / To | QA -> PM |
| Status | **ready** — the live-browser re-verification of PITFALL-011's fix (uploaded images never rendering, anywhere) is complete, clean, and independently confirmed. testing.md status is `approved` (v1.5). |
| Spec | specs/012-admin-content-management/testing.md (v1.5, §10) |
| Prior | handoffs/12-qa-to-pm.md (QA's prior "ready to close" call, testing.md v1.4), handoffs/13-dev-to-qa.md (Dev's bug-fix pass 4, reopened this) |

## Summary

handoffs/12-qa-to-pm.md's v1.4 approval was reached without anyone having actually uploaded a real image and checked whether it rendered anywhere. A human doing exactly that afterward found: uploaded images (team photos, project images) never rendered — not in the admin form's own preview thumbnail, not on the public site. Root cause: `UploadService.store()` returns a root-relative `/uploads/...` URL, correct only as a path on the `api` container; every `<img src={...}>` rendering an uploaded value used that path verbatim, which resolves against the browser's *current page* origin (`web`, `:3000`), 404ing silently. `curl`ing the same path against `:8080` directly always worked, which is exactly why no prior API-level check ever caught this. Recorded as **PITFALL-011**.

Dev's fix (handoffs/13-dev-to-qa.md): `resolveUploadUrl()` in `web/src/lib/utils.ts`, prefixing only `/uploads/`-prefixed paths with `NEXT_PUBLIC_API_BASE_URL`, applied at all four render sites (`upload-field.tsx`, `team-card.tsx`, `project-card.tsx`, `project-detail.tsx`).

This pass independently re-verified that fix, live, in a real headless-Chrome session (not curl, not a code read alone):

- **Team photo (F12-AC11):** uploaded a real 200×200 PNG to an existing team member (`/admin/team/1`) through the actual admin form. Admin preview thumbnail rendered correctly (`naturalWidth: 200`, not a broken-image icon). Saved, then loaded the public `/about` page, scrolled it into view (per Dev's `loading="lazy"` note), and confirmed the photo rendered there too. Also independently confirmed a second, pre-existing real (~4MB) uploaded team photo (left over from the original human discovery, not created by this pass) still renders correctly (`naturalWidth: 3798`), proving the fix holds for large real-world files too.
- **Project image (spot-check, per handoffs/13-dev-to-qa.md item 3):** uploaded the same PNG to an existing project (`/admin/projects/1`). Admin preview, the public `/projects` list, and the public `/projects/doha-metro-gold-line` detail page all rendered the uploaded image correctly.
- **Origin check:** every `<img>` request for an uploaded value, across every page checked, resolved to `http://localhost:8080/uploads/...` (the API origin) — never `http://localhost:3000/uploads/...` (the web origin that would 404). No exceptions found.
- **Console/CORS:** zero CORS errors, zero other console errors attributable to the upload/render path, on any page checked. (One unrelated, pre-existing `favicon.ico` 404 on `/admin/login` was confirmed in isolation and is out of scope — not a regression, not related to this fix.)
- **Cleanup:** team id 1's photo reverted to `null` via the real UI; project id 1's image reverted to its exact original static-asset path via a direct authenticated API call (the UI's `UploadField` only supports "Choose file"/"Remove", with no way to type a URL back in, so a UI-only revert could not restore the exact original value — this was the only way to leave the record byte-identical to how it was found). Public project count (15) and team roster both confirmed unchanged from the pre-pass state.
- **Regression check, reproduced after the browser testing:** `./mvnw test` → 157/157, 0 failures/errors. `npx tsc --noEmit` → clean. `docker compose build --no-cache web` succeeded. All four `docker compose` services stayed `Up`/`healthy` throughout, zero restarts.

**Verdict: PITFALL-011's fix (bug-fix pass 4) is now genuinely, independently, live-verified. No blocking findings. No new bugs. No regressions.**

## Acceptance criteria

All F12-AC1 through F12-AC30 remain PASS by id (unchanged from testing.md v1.4, full history there). F12-AC20 and F12-AC11 (and by extension F12-AC7's project-image equivalent) were specifically re-strengthened this pass with a genuine render-level check, not just an upload-API-succeeds check.

## Definition-of-Done checklist (QA's view)

- [x] All acceptance criteria in requirements.md met, by id — 30/30 F12-ACn, no CONCERN, no FAIL.
- [x] Full test suite passes, no regressions — 157/157 backend, `tsc --noEmit` clean, both independently re-run this pass, after the live browser testing.
- [x] Doc comments — spot-checked across six passes, no gaps found; `resolveUploadUrl()`'s doc comment specifically checked this pass and matches its actual behavior.
- [x] No external schema assumed without confirmation — n/a to this pass, unchanged from prior passes.
- [x] LLM/lead-path hardening — n/a to 012 (no LLM path).
- [x] No confidential-upload path uses a free training tier — n/a to 012's upload feature (local filesystem, no model call).
- [x] No open questions remain — New Bug #3 (`worker` boot failure) is a closed, documented decision (DEC-032), not an open question; PITFALL-011, the gap that reopened approval, is now closed.
- [x] Knowledge layer updated — PITFALL-011 present, read, and its remedy verified accurate against both the code and the running application this pass.
- [x] CLAUDE.md updated — status line for 012 should move from "bug-fix pass 3 complete... pending QA re-verification" to reflect this approval (PM's call to make the edit). Note CLAUDE.md's current 012 status line predates bug-fix pass 4 entirely and needs updating regardless of this pass's outcome.
- [x] All five spec files are status: approved or explicitly deferred — requirements.md, architecture.md, design.md, implementation.md, testing.md (this file, now v1.5, status `approved`).

## What I did not do (and why — not QA's role)

- Did not fix anything — reporting only.
- Did not re-verify anything already confirmed in handoffs/12-qa-to-pm.md (project-CRUD CORS fix from bug-fix pass 3, worker boot failure/DEC-032, marketing nav/footer fix, team hide cache-latency finding) — explicitly out of scope for this pass, per handoffs/13-dev-to-qa.md's own instruction.
- Did not exercise `/admin/samples/[id]` against a real record — the `sample` table still ships empty (pre-existing, documented gap, not a 012 defect, and not this pass's focus — the shared `UploadField` component was already exercised twice, on team and project, which is sufficient evidence the fix generalizes).

## Recommendation

Feature 012 (Admin Content Management) is ready to close, again. testing.md is `approved` (v1.5). The only item that reopened it since the last PM handoff — PITFALL-011, uploaded images never rendering — is now closed, live-verified, not just code-reviewed. New Bug #3 (`worker` boot failure) remains a deliberately deferred, documented decision (DEC-032). Given this is the second time a human click-through has found a real bug *after* a QA approval (first PITFALL-010, now PITFALL-011), PM may want to flag to the team that "renders correctly in a real browser, for a genuinely-uploaded file, not a seeded/static one" deserves a permanent place in this feature's (and any future upload-handling feature's) regression checklist rather than being rediscovered by chance each time — that's a process observation, not a blocking condition on this handoff.

## Read first

- specs/012-admin-content-management/testing.md (v1.5, §10 — this pass's full evidence)
- specs/012-admin-content-management/handoffs/13-dev-to-qa.md (Dev's bug-fix pass 4)
- specs/012-admin-content-management/handoffs/12-qa-to-pm.md (the prior, since-reopened approval)
- docs/knowledge/LEARNINGS.md — PITFALL-011

## Do not touch

- Nothing flagged as blocking. No follow-on cleanup items identified this pass.
- Orphaned upload files from this pass's own test uploads — expected, pre-existing accepted gap (architecture §8), not a cleanup action available to an admin or to QA.
- Everything already confirmed in handoffs/12-qa-to-pm.md — out of scope, not re-litigated here.
