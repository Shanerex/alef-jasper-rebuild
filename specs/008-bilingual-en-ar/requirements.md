---
feature: 008-bilingual-en-ar
spec_id: "008"
phase: requirements
owner: PM
status: approved
version: "1.0"
entry_criteria: [business rationale captured]
exit_criteria: [testable criteria; RTL design noted for architect]
---

# Requirements: English / Arabic Bilingual

## Problem
The market is the GCC and most competitor sites are English-only. An Arabic option is a real regional edge.

## User stories
- As an Arabic-speaking buyer, I switch the site to Arabic, including layout direction.
- As a returning visitor, my language choice is remembered.

## Acceptance criteria
- [ ] F8-AC1 A visible EN/AR toggle.
- [ ] F8-AC2 All primary pages localized (home, services, projects, about, samples, contact).
- [ ] F8-AC3 Arabic renders right-to-left with a correct RTL layout.
- [ ] F8-AC4 Locale choice persists across visits.
- [ ] F8-AC5 hreflang tags so each locale is indexable.

## Out of scope
- Localizing individual project descriptions in v1 (UI and static copy first).
