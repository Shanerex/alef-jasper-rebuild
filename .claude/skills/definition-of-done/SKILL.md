---
name: definition-of-done
description: The Definition of Done checklist QA verifies and PM confirms before loop-close. Use at the QA gate and before closing any feature.
---
# Definition of Done

A feature is not done, and PM does not close the loop, until every box is checked.

- [ ] All acceptance criteria in requirements.md are met, by id.
- [ ] Full test suite passes (no regressions), with risk-weighted coverage on the critical paths for this feature.
- [ ] Every function carries an intent-focused doc comment.
- [ ] No external schema was assumed; every unknown was confirmed with a human.
- [ ] LLM and lead-path hardening applied where the feature touches them.
- [ ] No confidential upload path sends data to a free training tier.
- [ ] No open questions remain.
- [ ] Knowledge layer updated (DECISIONS for decisions, LEARNINGS for pitfalls; any ARCHITECTURE change human-approved).
- [ ] CLAUDE.md updated (spec status, new endpoints/files).
- [ ] All five spec files are status: approved (or explicitly deferred for stretch features).

Coverage is risk-based per DEC-006, not a uniform percentage. Versioning is per-deploy, not per-feature.
