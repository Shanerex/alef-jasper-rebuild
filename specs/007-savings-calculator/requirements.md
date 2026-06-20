---
feature: 007-savings-calculator
spec_id: "007"
phase: requirements
owner: PM
status: approved
version: "1.0"
entry_criteria: [business rationale captured]
exit_criteria: [testable criteria]
---

# Requirements: "Why Outsource" Savings Calculator

## Problem
The core pitch is cost: outsourcing detailing beats an in-house team. A calculator makes that argument concrete and self-serve, and it sells better than any paragraph.

## User stories
- As a buyer, I enter my in-house detailing cost assumptions and see the saving against ALEF's model.
- As a buyer, I move from the result straight into an enquiry.

## Acceptance criteria
- [ ] F7-AC1 Inputs: in-house team size or monthly cost, plus optional tonnage or drawing volume.
- [ ] F7-AC2 Output: indicative saving and capacity comparison against the outsourcing model.
- [ ] F7-AC3 Runs entirely client-side; no entered figures leave the browser.
- [ ] F7-AC4 Result links into the RFQ/contact CTA (001).
- [ ] F7-AC5 Shows an indicative-only disclaimer.

## Out of scope
- Saving or emailing the calculation. Real pricing.
