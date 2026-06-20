---
name: engineering-standards
description: The enforced engineering rules for this repo (knowledge-layer-first, doc comment every function, risk-based coverage, no assumed external schemas, LLM and lead-path hardening). Use before any structural change, before writing code, and before closing a feature.
---
# Engineering standards

A stricter project rule wins. These are calibrated for a solo build that may become the primary production site, so rigor is risk-based, not uniform.

1. Knowledge layer first. Read ARCHITECTURE.md, DECISIONS.md, LEARNINGS.md before any structural change. ARCHITECTURE.md is human-approval only. [hook: guard-architecture]
2. Doc comment on every function. Focus on intent, the decision it encodes, and any gotcha. Do not paraphrase the signature. This is full strength on purpose: it is the owner's memory years later.
3. Risk-based test coverage, not a uniform floor. Heavy coverage on RFQ capture, the agent tool contract, the event consumer, and auth. Do not pad trivial code to hit a percentage. The full suite must pass before a feature closes. [hook: gate-tests]
4. Never assume external schemas. Do not invent the shape of an API response, message payload, or DB schema. If it is not derivable from existing code, STOP and raise an Open Question.
5. LLM hardening (concierge). Rate limit chat, cap max tokens, resist prompt injection, and degrade to the plain RFQ form when a model quota is hit.
6. Lead path is reliable. lead.created must have retry and a dead-letter path so a real enquiry is never lost.
7. Privacy. Confidential uploads never touch a free model tier that trains on data. Paid or private model only.
8. Hygiene. Secrets in env not repo. Honeypot plus rate limit on public forms. SEO, performance, and accessibility budgets are correctness for the marketing pages.

Versioning note: per DEC-006 we do not bump a version every feature. Tag releases on deploy.
Stack-specific lines (test runner, doc-comment tool) are the only things to edit when forking.
