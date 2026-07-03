# DECISIONS.md

> Append-only. Each DEC records an implementation choice not explicitly covered by the design, along with the rationale and the options that were considered and rejected.

---

## DEC-001 through DEC-015

Not recorded here. See earlier feature handoffs for context if needed.

---

## DEC-016 — Services content is a static frontend constant, not a database table

**Feature:** 011 — Core Marketing Pages
**Date:** 2026-07-01
**Author:** Dev

**Decision:** The 8 service specializations are stored as a TypeScript constant (`web/src/lib/content/services.ts`) rather than in a database table.

**Rationale:** Service names and outcomes are fixed marketing copy that does not change without a site release anyway. They require no admin UI, no translation in v1, and no per-request DB query. Adding a `service` table would add a migration, a Spring module, an endpoint, a typed client, and a type file — five artefacts — for content that changes at deploy frequency, not at runtime. A static constant is the correct abstraction. If feature 012 (Admin CMS) later wants to manage services, it can add the table at that point.

**Options considered:**
- Database table + GET /api/services endpoint — rejected: overhead exceeds benefit for immutable marketing copy.
- CMS (external) — out of scope for local demo.
- Static constant (chosen).

---

## DEC-017 — Lead table is a minimal subset of the full ARCHITECTURE.md schema

**Feature:** 011 — Core Marketing Pages
**Date:** 2026-07-01
**Author:** Dev

**Decision:** V10 creates a `lead` table with only: id, created_at, source, name, email, phone, company, message. The full schema from ARCHITECTURE.md (`project_name`, `sector`, `country`, `scope[]`, `tonnage`, `drawing_count`, `software_standard`, `deadline`, `attachment_ref`, `conversation_ref`) is not added.

**Rationale:** Those extra columns serve the AI RFQ concierge (feature 001) and drawing-aware phase (feature 010), neither of which is implemented yet. Adding them now would mean nullable columns with no writers, schema noise, and a wider DTO surface to maintain. The minimal table satisfies the contact form use-case (feature 011). Feature 001 will add a V1x migration to extend the table.

**Options considered:**
- Full schema immediately — rejected: premature; foreign-key columns cannot be populated yet.
- Separate `contact_submission` table for 011 vs `lead` for 001 — rejected: unnecessary split; the architecture uses one `lead` table.
- Minimal subset (chosen).

---

## DEC-018 — lead.created Redis Stream event not emitted by LeadService

**Feature:** 011 — Core Marketing Pages
**Date:** 2026-07-01
**Author:** Dev

**Decision:** `LeadService.submit()` persists the lead to Postgres and returns a confirmation DTO. It does NOT publish a `lead.created` event to the Redis Stream.

**Rationale:** The worker consumer that would process `lead.created` (send email, write to CRM) is not yet built. Publishing to a stream with no consumer results in an ever-growing unconsumed stream with no operational value and adds a Redis dependency to the write path. The ARCHITECTURE.md constraint is "the chat response never blocks on email" — there is no chat response here. Feature 001 (AI concierge) or a dedicated notification feature will add the Redis publish once a consumer exists.

**Options considered:**
- Publish event and accept unconsumed stream — rejected: operational noise, harder to debug.
- Publish event with synchronous email via Resend — rejected: requires `RESEND_API_KEY` which is deploy-only; breaks local demo.
- Persist only (chosen).

---

## DEC-019 — Office data lives in a Postgres table, not a frontend constant

**Feature:** 011 — Core Marketing Pages
**Date:** 2026-07-01
**Author:** Dev

**Decision:** Office locations (address lines, phones, email) are stored in the `office` database table and served via `GET /api/offices`. They are NOT a static TypeScript constant like `SERVICES`.

**Rationale:** Office details (phone number, physical address) change more frequently than service names, are needed by both the web front-end and any future worker or webhook, and are data that the Architect explicitly wanted in the database (architecture.md §4.3 defines the office table). An admin can update them without a code deploy once feature 012 ships.

**Options considered:**
- Static TypeScript constant — rejected: phone numbers and addresses are facts that change without a release cycle.
- Database table + GET /api/offices (chosen).

---

## DEC-020 — Team profiles are in a Postgres table with an active flag and display_order

**Feature:** 011 — Core Marketing Pages
**Date:** 2026-07-01
**Author:** Dev

**Decision:** Team profiles live in the `team` table with `active BOOLEAN` and `display_order INTEGER`. `GET /api/team` only returns active members, ordered by display_order. The seed has 10 profiles.

**Rationale:** The architecture explicitly called for a `team` table (ARCHITECTURE.md data model) and noted it should be designed for CRUD from day one. An `active` flag lets an admin hide a profile without deleting history. `display_order` gives deterministic rendering without a sort UI in the CMS. Both match the pattern of `trust_content` (005) which uses the same pattern for display control.

**Options considered:**
- Hardcoded JSON or TypeScript constant — rejected: same reason as DEC-019; team changes should not require a code deploy.
- Database without active/order columns — rejected: the Architect's spec included both.
- Database with active flag and display_order (chosen).
