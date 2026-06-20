---
name: handoff-protocol
description: How to write a handoff between roles and where it lives. Use at the end of any phase to pass work to the next role.
---
# Handoff protocol

Handoffs live at specs/NNN-feature/handoffs/N-fromrole-to-torole.md, ordered by pipeline position.

```markdown
# Handoff: [Feature]
| Field | Value |
|---|---|
| Feature # | NNN |
| From / To | [Role] -> [Role] |
| Status | ready / blocked |
| Spec | specs/NNN-feature/[phase].md |

## Context
Why this work exists.
## What was done
- decisions or work completed
## What the receiving role must do
1. specific, actionable steps
## Read first
- relevant files
## Do not touch
- explicit list
```
