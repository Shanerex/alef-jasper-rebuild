---
feature: 004-sample-explorer
spec_id: "004"
phase: requirements
owner: PM
status: approved
version: "1.0"
entry_criteria: [business rationale captured]
exit_criteria: [testable criteria]
---

# Requirements: Sample Explorer

## Problem
The five sample books sit on raw Dropbox links: no preview, no lead capture, and link rot. Host them in-site, preview them, and gate full downloads to capture leads.

## User stories
- As a buyer, I preview a sample without leaving the site.
- As a buyer, I download the full sample after giving my details.
- As the team, every download is a captured lead.

## Acceptance criteria
- [ ] F4-AC1 Each sample (Prequalification profile, BBS, Drawings, Bridge drawings, Roads and utility) has an in-site preview.
- [ ] F4-AC2 Full download requires name, email, company.
- [ ] F4-AC3 Each download creates a lead record.
- [ ] F4-AC4 No raw third-party storage links exposed.

## Out of scope
- DRM or watermarking (later if needed).
