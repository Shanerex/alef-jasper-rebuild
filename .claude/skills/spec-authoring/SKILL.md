---
name: spec-authoring
description: How to write any spec file under specs/NNN-feature/, including shared frontmatter, per-file content, and the Open Questions Gate. Use when creating or updating a feature spec.
---
# Spec authoring

Every spec file uses this frontmatter:
```yaml
---
feature: human-readable name
spec_id: "NNN"
phase: requirements | architecture | design | implementation | testing
owner: PM | Architect | Dev | QA
status: draft | approved
version: "1.0"
entry_criteria: [what must be true before this phase starts]
exit_criteria: [what must be true before the next phase starts]
---
```

What goes in each file:
- requirements.md (PM): problem, user stories, acceptance criteria as checkboxes with ids F<n>-AC<m>, out of scope.
- architecture.md (Architect): how it fits the system, component responsibilities, key structural decisions.
- design.md (Architect): API contracts, data models, edge cases, limitations.
- implementation.md (Dev): files changed, endpoints, migrations, key decisions, where tests live.
- testing.md (QA): case table (id / test / expected / result), regression notes, gaps.

Open Questions Gate: when a role hits ambiguity it cannot resolve from specs, code, or the knowledge layer, it raises an Open Questions Gate BEFORE the approve/reject gate. An unknown external schema always triggers it. No approve/reject gate is shown while open questions remain.
