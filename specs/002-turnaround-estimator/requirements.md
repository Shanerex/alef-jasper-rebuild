---
feature: 002-turnaround-estimator
spec_id: "002"
phase: requirements
owner: PM
status: approved
version: "1.0"
entry_criteria:
  - business rationale captured
exit_criteria:
  - testable criteria; feeds 001
---

# Requirements: Turnaround / Capacity Estimator

## Problem
Buyers want a rough sense of turnaround before committing. A rules-driven estimate, explained in words, builds confidence and feeds the RFQ.

## User stories
- As a buyer, I enter project type and tonnage or drawing count and get an indicative turnaround.
- As a buyer, I understand the estimate is indicative, not a quote.

## Acceptance criteria
- [ ] F2-AC1 Inputs: scope, project type, tonnage or drawing count.
- [ ] F2-AC2 Output: indicative turnaround band plus a one-line explanation.
- [ ] F2-AC3 Always shows a disclaimer that final timelines are confirmed after review.
- [ ] F2-AC4 Feeds directly into the RFQ flow (001).

## Out of scope
- Binding quotes or pricing.
