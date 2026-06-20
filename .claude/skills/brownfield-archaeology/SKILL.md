---
name: brownfield-archaeology
description: How to backfill specs and the knowledge layer for a codebase that already exists. Use once when adopting this playbook on an existing project.
---
# Brownfield archaeology

Run once when adopting the playbook on an existing repo.
1. Identify every major feature already built.
2. For each, create specs/NNN-feature/ and write all five files describing what the code ACTUALLY does. Set status: approved (they document reality).
3. Seed the knowledge layer from the code: ARCHITECTURE (current design and constraints), DECISIONS (rationale you can infer), LEARNINGS (known pitfalls).
4. Update CLAUDE.md to reflect the current state.
5. Report: features found, specs created, knowledge seeded, gaps.
