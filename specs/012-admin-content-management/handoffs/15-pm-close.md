# Handoff: Admin Content Management — PM close (feature 012, initial build)

| Field | Value |
|---|---|
| Feature # | 012 |
| From / To | PM -> (terminal; no receiving role — recorded for the project record and for whichever role next touches 012, e.g. the eventual `worker`-boot fix or 004's Sample Explorer) |
| Status | **closed** |
| Spec | specs/012-admin-content-management/requirements.md (v1.1, `approved`), architecture.md (v0.2, `approved`), design.md (v0.2, `approved`), implementation.md, testing.md (v1.5, `approved`) |
| Prior | handoffs/14-qa-to-pm.md (QA's final "ready to close" call) |

## Context

Feature 012 (Admin Content Management) is closing after an unusually long QA cycle: three prior "QA approved" calls (v1.2, v1.4, and an earlier informal one) were each walked back by a human manually clicking through the running admin console and finding a real bug invisible to API-level/code-level checks alone:

1. Bug-fix pass 1 (pre-dates this handoff chain): CSRF scoping breaking public `/api/leads`; `next build` failure on `/admin/login`.
2. Bug-fix pass 2: marketing nav/footer bleeding onto `/admin/**` (New Concern #6) — fixed via the `(marketing)` route-group restructure.
3. Bug-fix pass 3: `/admin/projects/new` and `/admin/projects/[id]` hung on "Loading…" forever — PITFALL-010 (a public API client written for server-side use hung when called client-side across origins with no CORS). Fixed by converting those two pages to Server Components.
4. Bug-fix pass 4: uploaded images never rendered anywhere — PITFALL-011 (a root-relative `/uploads/...` URL resolves against the wrong origin when rendered by a different container). Fixed via `resolveUploadUrl()` at all four `<img>` render sites.

A fifth item — a team "hide" (`active=false`) appearing not to take effect — was investigated and is **not a bug**: the public site's pre-existing 60-second ISR cache (`revalidate: 60`, no on-demand invalidation) explains the delay; confirmed working correctly within that window. Documented as a known UX gap, not fixed (see CLAUDE.md TODOs).

Since handoffs/14-qa-to-pm.md, the project owner independently ran their own full manual click-through of the entire admin console (projects, team, trust, samples, uploads, account, responsive) and confirmed it looks good, with no new issues raised.

## What was done (this close pass)

1. Read handoffs/14-qa-to-pm.md and testing.md v1.5 in full (§0–§10, all six QA passes).
2. Cross-checked every F12-AC1..30 in requirements.md against QA's verdicts. Confirmed the count independently: 30 acceptance criteria total (AC1–AC30, no gaps in the numbering — AC1–3, AC4–7, AC8–11, AC12–15, AC16–19, AC20–22, AC23–24, AC25, AC26, AC27–29 (account security ACs 28/29 sit under their own "Account Security" section, not sequentially), AC30). All 30 are accounted for as PASS across the QA pass history (testing.md v1.1 §4 baseline, carried and reconfirmed through v1.2/v1.4/v1.5, with F12-AC1/AC27 explicitly re-verified in the route-group-restructure pass, F12-AC4/AC5/AC6/AC24 explicitly re-verified in the PITFALL-010 pass, F12-AC11/AC20 explicitly re-verified in the PITFALL-011 pass). No AC has an open CONCERN or FAIL anywhere in the QA record.
3. Ran the Definition of Done checklist (`.claude/skills/definition-of-done/SKILL.md`) against the full spec chain, not just QA's self-report:
   - [x] All acceptance criteria in requirements.md met, by id — 30/30, confirmed independently against requirements.md's actual AC list (not just trusting the "30/30" figure quoted in handoffs).
   - [x] Full test suite passes, no regressions — 157/157 backend (`./mvnw test`), `tsc --noEmit` clean, both reproduced independently across multiple QA passes, most recently in testing.md §10.8 after live browser CRUD/upload activity.
   - [x] Doc comments — QA spot-checked across six passes; `resolveUploadUrl()`'s doc comment specifically verified against its actual behavior in the final pass.
   - [x] No external schema assumed without confirmation — n/a; requirements.md's Open Questions are resolved (trust-stats storage → `trust_content`/DEC-013/DEC-025; password-reset scope → DEC-021) and none of F12's ACs depend on an unconfirmed external schema.
   - [x] LLM/lead-path hardening — n/a to 012 (no LLM path in this feature; the `lead` path itself is 011's, untouched here).
   - [x] No confidential-upload path uses a free training tier — n/a; 012's uploads are local filesystem only, no model call.
   - [x] No open questions remain — requirements.md's three Open Questions are resolved (see requirements.md v1.1, updated at this close pass to record the resolutions explicitly rather than leaving them silently implied). New Bug #3 (`worker` boot failure) is a closed, owner-approved deferral (DEC-032), not an open question.
   - [x] Knowledge layer updated — DECISIONS.md carries DEC-021 through DEC-032 for this feature's implementation and bug-fix choices; LEARNINGS.md carries LEARNING-002, LEARNING-003, PITFALL-007, PITFALL-008, PITFALL-009, PITFALL-010, PITFALL-011, all read and cross-checked against the code this close pass.
   - [x] CLAUDE.md updated (this pass) — feature table row for 012, "Implemented specs" line reworded to reflect closure.
   - [x] All five spec files are status `approved` (or the equivalent terminal state) — requirements.md (v1.1, `approved`), architecture.md (v0.2, `approved`), design.md (v0.2, `approved`) all carry YAML frontmatter `status: approved`; implementation.md uses this repo's table-based status convention (consistent with every other feature's implementation.md, e.g. 011's) rather than YAML frontmatter, and its Status field ("bug-fix pass complete — 157 backend tests green, `tsc --noEmit` clean, both QA-blocking bugs resolved") is accurate as of the final bug-fix pass; testing.md is v1.5, `approved`.

   No item failed this checklist. No rubber-stamping was needed — the paper trail is genuinely complete.

4. Specifically confirmed the two items the orchestrator flagged:
   - **DEC-032 (worker boot failure deferred):** legitimate and non-blocking. `AdminSecurityConfig` (feature 012) crashes when loaded under the `worker` Spring profile because it has no profile/web-application guard (PITFALL-009). This is deferred, not fixed, because no Redis Stream consumer exists yet anywhere in the codebase for `lead.created` or `kb.changed` — feature 001 (the concierge/RAG consumer) is unbuilt. A worker that fails to boot and a worker that boots and does nothing are behaviorally indistinguishable today. QA independently re-confirmed the rationale still holds via a targeted grep (testing.md §3, §9) in a later pass, not just re-trusting the original decision. This is correctly out of 012's actual scope (012 is the admin console + auth + CRUD + uploads; it is not responsible for the `worker` process's own runtime health outside what it broke). It must be fixed before feature 001 lands a real consumer — already tracked in CLAUDE.md's TODOs and in PITFALL-009's remedy. Not a reason to hold this close.
   - **Team-hide ISR-latency:** correctly classified as a known gap, not a bug. Root cause (public site's `revalidate: 60` with no on-demand cache invalidation on any admin write path) is pre-existing to 012 (the ISR pattern itself predates 012; 012 only added the write paths that make the staleness window visible in practice). Confirmed working correctly within the 60s window. Documented in CLAUDE.md's TODOs as a real UX gap worth fixing later, not silently dropped.

5. Confirmed the requirements ↔ QA mapping has no drift: every F12-AC id in requirements.md v1.1 has a corresponding verdict in testing.md's history; no acceptance id was renumbered or reused across the six QA passes (checked, since a shipped id must never be renumbered per this repo's rules).

## Decision

**Feature 012 (Admin Content Management) is formally closed.** All 30 acceptance criteria pass by id. The full spec chain (requirements → architecture → design → implementation → testing) is internally consistent and each file is in its terminal state. Every Definition of Done item is satisfied. The two flagged items (DEC-032, team-hide latency) are legitimately non-blocking and correctly classified. No inconsistency was found in the paper trail that would justify reopening or holding this close.

This closes the **initial build** of 012. Two carried-forward, explicitly-not-blocking items remain live in CLAUDE.md's TODOs for future work: the `worker`/`AdminSecurityConfig` profile guard (needed before feature 001's consumer lands) and the public-site ISR cache latency (needed if urgent same-second admin edits become a real requirement).

## What was done (docs touched this pass)

- specs/012-admin-content-management/requirements.md — added a closure pointer note under the frontmatter; resolved the three Open Questions explicitly (they were already de facto resolved by architecture/implementation decisions but read as open in the doc text). `status: approved`, `version: "1.1"` unchanged — no acceptance criterion changed.
- CLAUDE.md — feature table row for 012 updated from "QA approved (testing.md v1.5), pending PM close-out" to a closed state; no other content changed.
- This handoff.

## Do not touch

- testing.md, architecture.md, design.md, implementation.md — QA/Architect/Dev-owned; not touched by this pass, and their content was independently confirmed consistent, not edited.
- DECISIONS.md / LEARNINGS.md — append-only, no new entries needed; nothing this pass found warrants a new DEC.
- The two carried-forward TODOs (`worker` profile guard, ISR cache latency) — correctly deferred, not fixed here; do not treat this close as resolving them.

## Read first (for anyone picking up 012-adjacent work later)

- specs/012-admin-content-management/testing.md (v1.5) — full six-pass QA history
- specs/012-admin-content-management/requirements.md (v1.1) — 30 acceptance criteria, this pass's closure note
- docs/knowledge/DECISIONS.md — DEC-021 through DEC-032
- docs/knowledge/LEARNINGS.md — PITFALL-009, PITFALL-010, PITFALL-011
- CLAUDE.md TODOs — `worker` boot failure, ISR cache latency
