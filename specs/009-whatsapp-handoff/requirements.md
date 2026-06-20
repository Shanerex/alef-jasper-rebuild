---
feature: 009-whatsapp-handoff
spec_id: "009"
phase: requirements
owner: PM
status: approved
version: "1.0"
entry_criteria: [business rationale captured]
exit_criteria: [testable criteria]
---

# Requirements: WhatsApp Handoff

## Problem
GCC buyers live on WhatsApp. A one-tap handoff from the concierge and the contact page meets them where they are.

## User stories
- As a buyer, I tap to continue on WhatsApp from the assistant or the contact page.
- As a buyer, the chat opens with useful context pre-filled.

## Acceptance criteria
- [ ] F9-AC1 One-tap WhatsApp entry from the concierge handoff and the contact page.
- [ ] F9-AC2 The opening message is pre-filled with context the user has already shared.
- [ ] F9-AC3 The business number is configurable, not hard-coded.
- [ ] F9-AC4 No personal data beyond what the user has consented to is placed in the link.

## Out of scope
- Two-way WhatsApp Business API integration (link handoff only in v1).
