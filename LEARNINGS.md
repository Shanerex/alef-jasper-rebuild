# LEARNINGS.md

> Append-only log of pitfalls and implementation choices discovered during development.

## PITFALL-008: Cross-package repository usage for derived trust data

**Feature**: 005 (Trust Layer)
**Date**: 2025-06-25

The trust service needs to derive client/contractor names and the marquee project count from the `project` table, which is owned by the portfolio package (`com.alef.api.portfolio`). Rather than duplicating the repository or creating a separate query service, we added three new query methods directly to `ProjectRepository` (the existing portfolio repository): `findDistinctClients()`, `findDistinctMainContractors()`, and `countByFeaturableTrue()`. The trust service then depends on the portfolio repository directly. This keeps a single source of truth for project data access but creates a cross-package dependency. If the portfolio package is ever restructured, the trust service's dependency must be updated too.
