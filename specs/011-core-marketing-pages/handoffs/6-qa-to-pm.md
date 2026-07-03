# Handoff: Core Marketing Pages — QA -> PM (Loop Close)

| Field | Value |
|---|---|
| Feature # | 011 |
| From / To | QA -> PM |
| Date | 2026-07-02 |
| Branch | feature/011-core-marketing-pages |
| QA status | APPROVED |
| Testing spec | specs/011-core-marketing-pages/testing.md (version 1.1) |

---

## Summary

Feature 011 (Core Marketing Pages — Home, About, Services, Contact) has passed QA. All nine acceptance criteria are satisfied. The one blocking bug found in the initial QA pass (Bug 2: missing `Retry-After: 3600` header on 429 responses from `POST /api/leads`) has been fixed by Dev and verified by QA. The fix was confirmed by code inspection and by a full test-suite run.

---

## What was verified

- All F11-AC1 through F11-AC9 — PASS (see `testing.md` section 1 case table).
- Bug 2 fix: `LeadExceptionHandler.handleRateLimit()` now returns `Retry-After: 3600` on 429 responses. The test `submit_rate_limit_breach_returns_429_with_retry_after_header` asserts the header and is green.
- Bug 1 fix: `handoffs/4-dev-to-qa.md` line 38 corrected from "10 submissions" to "5 submissions" per hour. Code was always correct; this was a documentation error only.
- Backend test suite: 58 tests, 0 failures, 0 errors — BUILD SUCCESS (re-QA run 2026-07-02).
- No regressions introduced by the fix. All other modules (portfolio, trust, team, office) pass unchanged.
- TypeScript check: `npx tsc --noEmit` exits 0.

---

## Open items (deferred, not blocking)

These are known gaps documented in `testing.md` section 8. None block feature 011 merge.

| Item | Deferred to |
|------|------------|
| `lead.created` Redis Stream event | Feature 001 (DEC-018) |
| Lead delivery / email notification | Future feature (human reads `lead` rows until then) |
| `Samples` nav link (`#`) | Feature 004 |
| WhatsApp channel link (`#`) | Feature 009 |
| Team member photos (all null) | When photo assets are available |
| No ISR caching on marketing pages (`force-dynamic`) | Deploy-time one-line flip (PITFALL-007) |
| Admin CMS for team/office/services | Feature 012 |
| "Concorse" → "Concourse" verifiability in static text (Bug 3) | If live-site copy is ever migrated verbatim |

---

## Recommendation

Merge `feature/011-core-marketing-pages` to `main`. Feature 011 is complete per its acceptance criteria.
