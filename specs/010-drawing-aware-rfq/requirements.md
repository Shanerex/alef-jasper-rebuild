---
feature: 010-drawing-aware-rfq
spec_id: "010"
phase: requirements
owner: PM
status: draft
version: "0.1"
entry_criteria:
  - 001 concierge shipped
  - a paid or private multimodal model is available (privacy constraint)
exit_criteria: [testable criteria once the model dependency is met]
---

# Requirements: Drawing-Aware RFQ (phase 2)

## Problem
Buyers often have a tender or structural PDF in hand. If the assistant can read it and pre-fill the RFQ scope, the enquiry gets far richer with less effort.

## User stories
- As a buyer, I upload a tender or structural PDF and the assistant pre-fills likely scope (floors, structure type).
- As a buyer, I review and correct the extracted scope before submitting.

## Acceptance criteria
- [ ] F10-AC1 Accepts a tender or structural PDF upload.
- [ ] F10-AC2 Extracts likely scope signals to pre-fill the RFQ.
- [ ] F10-AC3 User confirms or edits every extracted field before submit.
- [ ] F10-AC4 Confidential documents are never sent to a free tier that trains on data; only a paid or private model processes them.
- [ ] F10-AC5 Falls back to manual entry if extraction fails or confidence is low.

## Out of scope
- Quantity takeoff or BBS generation from the drawing.

## Note
Phase 2. Depends on a paid/private multimodal model and the privacy constraint above. Do not start before 001 is green and that dependency is met.
