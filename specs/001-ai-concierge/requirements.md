---
feature: 001-ai-concierge
spec_id: "001"
phase: requirements
owner: PM
status: approved
version: "1.0"
entry_criteria:
  - business rationale captured in handoffs/1-business-to-pm.md
exit_criteria:
  - acceptance criteria are testable and id'd; architect can design from this
---

# Requirements: AI RFQ Concierge

## Problem
Buyers (GCC contractors and consultants) need fast answers about scope and a low-friction way to send a qualified enquiry. The current site offers a dead form. This is the hero feature: a grounded assistant that answers capability questions and captures a structured RFQ.

## User stories
- As a procurement lead, I can ask whether ALEF handles a specific scope and get an accurate, grounded answer.
- As a buyer, I can describe my project in plain language and have it turned into a structured RFQ.
- As a buyer, I can attach drawings or a tender document.
- As the ALEF team, I receive a clean structured lead, not freeform text.

## Acceptance criteria
- [ ] F1-AC1 Answers only from the known knowledge base; never invents capabilities the company does not offer.
- [ ] F1-AC2 On enquiry intent, collects: project name, sector, country, scope (rebar/BBS/GA/MEP/QS/as-built), approx tonnage or drawing count, software/standard, deadline, contact details.
- [ ] F1-AC3 Accepts file attachment (PDF, DWG, common images) up to a defined size limit.
- [ ] F1-AC4 On completion, creates a structured lead record and notifies the team.
- [ ] F1-AC5 When it cannot answer, offers a direct handoff rather than guessing.
- [ ] F1-AC6 Works on mobile.
- [ ] F1-AC7 Hardened: rate limited, max-token capped, resistant to prompt injection, degrades to the plain RFQ form when a model quota is hit.

## Out of scope
- Public CAD rendering. Multi-language (see 008). Drawing content extraction (see 010).
