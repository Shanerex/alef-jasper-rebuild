# Handoff: Core Marketing Pages — Architecture

| Field | Value |
|---|---|
| Feature # | 011 |
| From / To | PM -> Architect (received); Architect -> Designer/Dev (passing on) |
| Status | architecture v2.0 (round 2) ready for human approval |
| Spec | specs/011-core-marketing-pages/architecture.md (version 2.0) |

## Round 2 context

Round 1 architecture (v1.0) was **rejected** after the ALEF Prequalification Document (39-page PDF, sourced from the Dubai DED trade license + company org chart) surfaced material content corrections. Requirements were re-approved as **v1.1** with those corrections. This architecture is **v2.0**. The round-1 structure was sound and is retained; the changes below are layered on. Sections changed materially are marked `[v2.0]` in architecture.md.

## What I received

- Approved `requirements.md` **v1.1** with testable acceptance criteria F11-AC1..F11-AC9, updated for the prequalification findings (10 team profiles, 8 specializations, 7 software tools, two offices, India-arm story, "Cadding" is the legal name).
- The business handoff (`handoffs/1-business-to-pm.md`).
- The prequalification doc findings (10 leadership profiles with roles/companies/emails; 8 reconciled specializations; 7 software tools; both office addresses/phones/emails; India Jasper campus scale; capacity stats; GCC country coordinators; landmark project history; "Cadding" = legal trade-license name).
- Binding context: `docs/knowledge/ARCHITECTURE.md` (constraints + data model), `DESIGN_SYSTEM.md` (Dark Prestige), shipped 003 and 005 architectures and code (reuse patterns), `DECISIONS.md` (DEC-001..DEC-015 — not relitigated).

## Key architectural decisions made (v2.0)

1. **Three new backend modules** (was two in v1.0), mirroring the 005 `trust` pattern: `com.alef.api.team` (read-only `GET /api/team`), `com.alef.api.office` (read-only `GET /api/offices`, **new in v2.0**), and `com.alef.api.lead` (first write path, `POST /api/leads`).
2. **`team` table designed for admin CRUD from day one (DEC-020).** [v2.0] **10** profiles (not 8). Carries `display_order` + **`active` soft-hide flag** + audit timestamps so 012 adds write endpoints with zero schema churn and new profiles are addable via a single `INSERT` — no code change, no migration. This is the KEY requirement (expanding team). 011 ships only the read endpoint (active rows, ordered). Flyway V6 schema, V7 seed (10 rows).
3. **`office` table (DEC-019).** [v2.0, new] Both offices (Dubai ALEF + India Jasper) promoted from hardcoded copy to a table — `TEXT[]` phones/address-lines for multi-valued data, `map_query` for a keyless map embed. Same content-ownership argument as `trust_content`. Flyway V8 schema, V9 seed (2 rows).
4. **`lead` table (Flyway V10), minimal subset (DEC-017).** Created now with only contact-form columns (`id, created_at, source, name, email, phone, company, message`), designed for 001 to extend additively. `source='contact_form'` makes it the shared lead path (F11-AC6).
5. **Event emission deferred (DEC-018).** 011 persists the lead row (durable) but does NOT emit `lead.created` or build the worker consumer — that belongs with 001/lead-delivery.
6. **Services static content (DEC-016).** [v2.0] Now **8** specializations (not 7). Software badges (**7** tools) come from 005's endpoint, not a static re-list.
7. **Home composes existing sources of truth (F11-AC2).** Featured projects from 003, trust stats from 005. No copy stored.
8. **Nav/footer already exist;** 011 wires the four nav hrefs, makes the footer year dynamic (F11-AC9), and must preserve "Cadding" as the legal name.
9. **Rendering follows the shipped projects page:** `force-dynamic` + try/catch graceful degradation today (PITFALL-007), one-line flip to ISR later.

## CRITICAL cross-feature flag (architecture.md §9)

The shipped **005 trust seed (`V5__seed_trust_content.sql`) is now stale** against the prequalification doc, and Home renders 005's data verbatim. Discrepancies: years 18+ → 23 (founded March 2003); steel capacity 5,000 → 70,000 t/month; staff/workstations 120+ → 250; new stats (102+ projects, 40+ clients, 6 GCC countries, 6,000 drawings/month); software 3 tools → 7. **This is a change to feature 005's content, owned by 005/012 — I flagged it, did NOT build it.** Correction must be a **new forward Flyway migration** (do NOT edit V5 — Flyway checksums applied migrations). Recommended to land alongside 011 or Home shows the old wrong figures.

## Open decisions for the human (architecture.md §10)

1. **005-seed correction: hardcode "102+ projects / 40+ clients" as stat rows vs. derive live from `project` (DEC-014).** Recommend hardcode now (catalogue only partially seeded — CLAUDE.md TODO), derive later. 005-content change, not 011.
2. **Rate limiting on `POST /api/leads` in 011 vs. deferred to 001.** Honeypot is in scope; recommend a minimal Redis per-IP limit in 011 too, but acceptable to consolidate into 001.
3. **Contact map provider** — must be keyless/zero-account (constraint 1). Recommend keyless Google Maps `<iframe>` fed by `office.mapQuery`, or a static image + link.
4. **Landmark-projects strip on About** — source (003 table vs. static text) and DEC-008 clearance. PM/Designer call.

**Settled (no longer open):** "Cadding" is the legal company name (DED trade license) — NOT a typo, never auto-correct.

## Proposed ARCHITECTURE.md changes (need human ruling; I did NOT edit the file)

- **Change 1:** `team` gains `id, display_order, active, created_at, updated_at`.
- **Change 2 (new):** add an `office` table to the data model.
- **Change 3:** `lead` gains a free-text `message` field.
- **Change 4:** close the "Cadding" ambiguity; the new `office` table represents the confirmed two-office model.
All are data-model additions / question-list updates — no constraint changes.

## Proposed DECs for the keeper

DEC-016 (static services), DEC-017 (minimal lead table), DEC-018 (deferred event emission), **DEC-019 (office table) [new]**, **DEC-020 (team table for CRUD, `active` flag, 10 profiles, "Cadding" settled) [new]** — listed at the bottom of architecture.md.
