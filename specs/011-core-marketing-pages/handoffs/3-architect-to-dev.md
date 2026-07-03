# Handoff: Core Marketing Pages — Architecture -> Dev

| Field | Value |
|---|---|
| Feature # | 011 |
| From / To | Architect -> Dev (and Designer) |
| Status | architecture v2.1 approved — Dev may build |
| Spec | specs/011-core-marketing-pages/architecture.md (version 2.1) |
| Depends on | 003 (project table + ProjectCard), 005 (trust endpoint + TrustLayer), existing root layout (SiteNav/SiteFooter) |

This is round 2. Architecture was re-cut after the ALEF Prequalification Document corrected company facts. Build from architecture.md v2.0 and requirements.md v1.1. Do not build from round-1 assumptions (8 team profiles, 3 software tools, hardcoded offices). Sections changed are marked `[v2.0]` in architecture.md.

## Build order (suggested)

1. Backend: `team` module + V6/V7 migrations (10 profiles).
2. Backend: `office` module + V8/V9 migrations (2 offices).
3. Backend: `lead` module + V10 migration (write path, honeypot).
4. Frontend: typed clients/types for team, offices, leads.
5. Frontend: the four pages, reusing 003/005 components.
6. Coordinate the 005-seed correction (see "Cross-feature" below) — not your file to own, but Home depends on it.

## Backend — what to implement

Follow `com.alef.api.<feature>.{controller,service,dto,entity,repository,vocabulary,error}` exactly as `portfolio` and `trust` do. Doc comment every class and method, intent-focused (DEC-006).

### `com.alef.api.team` (read-only; mirror `trust`)
- `controller/TeamController` — `GET /api/team` → `TeamService.getTeam()`. No logic in controller.
- `service/TeamService` — `@Transactional(readOnly = true)`; reads **active** rows ordered by `display_order`, maps to DTO.
- `entity/TeamEntity` — JPA entity for `team` (§4.1 schema, incl. `active`).
- `repository/TeamRepository` — `findAllByActiveTrueOrderByDisplayOrderAsc()`.
- `dto/TeamMemberDto` (name, role, company, email, photo — nullable except name/role), `dto/TeamOverviewDto` (`{ members }` envelope).
- Seed the **10** profiles from architecture.md §4.2 table. Primary email only (drop secondary). `photo` null until assets exist.

### `com.alef.api.office` (read-only; new in v2.0)
- `controller/OfficeController` — `GET /api/offices` → `OfficeService.getOffices()`.
- `service/OfficeService` — `@Transactional(readOnly = true)`; reads all rows ordered by `display_order`.
- `entity/OfficeEntity` — JPA entity for `office` (§4.3). `address_lines TEXT[]`, `phones TEXT[]` — map as `List<String>` (see how `project.scope` TEXT[] is mapped in the portfolio entity for the idiom).
- `repository/OfficeRepository` — `findAllByOrderByDisplayOrderAsc()`.
- `dto/OfficeDto` (key, name, addressLines, phones, email, mapQuery), `dto/OfficesDto` (`{ offices }` envelope).
- Seed the 2 offices from architecture.md §3.2 (Dubai two phones; India one).

### `com.alef.api.lead` (first write path)
- `controller/LeadController` — `POST /api/leads`, `@Valid @RequestBody`, returns `201` + confirmation DTO.
- `service/LeadService` — `@Transactional`; sets `source`/`created_at` server-side, runs the honeypot check (filled `website` → return a `201` confirmation WITHOUT persisting), persists, returns id. Design it so 001 can call a richer overload without breaking this one.
- `entity/LeadEntity` — JPA entity for the `lead` subset (§4.4).
- `repository/LeadRepository` — `JpaRepository`.
- `dto/ContactLeadRequest` (validated; includes `website` honeypot), `dto/LeadConfirmationDto` (`{ id, status }`).
- `error/LeadExceptionHandler` — `@RestControllerAdvice` mapping `MethodArgumentNotValidException` → RFC 9457 `ProblemDetail` (400), mirroring `PortfolioExceptionHandler`.
- `vocabulary/LeadSource` — small enum/constant (`CONTACT_FORM`).

### Migrations (next free version is V6)
- `V6__create_team.sql`, `V7__seed_team.sql` (10 rows, `ON CONFLICT (name, role) DO NOTHING`).
- `V8__create_office.sql`, `V9__seed_office.sql` (2 rows, `ON CONFLICT (office_key) DO NOTHING`).
- `V10__create_lead.sql` (no seed).
- **Do NOT edit V5** (005's seed) — Flyway checksums applied migrations; editing breaks the local volume.

## Frontend — what to implement

Routes: `web/src/app/page.tsx` (replace placeholder), `about/page.tsx`, `services/page.tsx`, `contact/page.tsx`. All inherit `SiteNav`/`SiteFooter` from the existing root layout.

Reuse, don't reinvent:
- Home featured marquee → 003's `ProjectCard`/`ProjectGrid` via `getProjects({ featurable: true })`.
- Home trust band → 005's `TrustLayer`/`TrustStats` via `getTrustOverview()`.
- Mirror 003/005 client/type conventions for new clients.

New typed clients/types: `lib/api/team.ts` + `lib/types/team.ts`; `lib/api/offices.ts` + `lib/types/office.ts`; `lib/api/leads.ts` + `lib/types/lead.ts`. Read clients use `next: { revalidate: 60 }`; `submitLead` is a no-cache mutation that throws on non-2xx.

New components (Designer specifies exact treatment): `TeamCard`/`TeamGrid` (responsive grid that flows for N cards — team is growing, do NOT hardcode 10), `ServiceCard`/`ServicesList` (8 specializations as outcomes), `ContactForm` (client component, honeypot `website` field, explicit success/error states), `OfficeCard`/`OfficeLocations` (both offices), `ContactMap` (keyless embed fed by `mapQuery`), `DirectChannels`, plus Home/About/Services/Contact heroes and sections (`HomeHero`, `CompanyStory`, `GccReachBlock`, `HomeCTA`, etc.).

Layout corrections in `web/src/app/layout.tsx`: wire nav hrefs (About/Services/Contact), make footer year dynamic (`new Date().getFullYear()`, F11-AC9).

Rendering: `force-dynamic` + try/catch graceful degradation (PITFALL-007 parity with the shipped projects page) for pages that fetch. Services is static unless software badges are server-fetched. Clients already pass `revalidate:60` so the ISR flip is one line later.

## MUST NOT do — explicit guardrails

- **Do NOT change "Cadding"** anywhere. It is the legal trade-license name ("ALEF ARCHITECTURAL & CADDING SERVICES L.L.C"). F11-AC4 only fixes "Intenational"→"International" and "Concorse"→"Concourse".
- **Do NOT build the `lead.created` event emission or the worker consumer** (DEC-018). Persist the row and return. A human reads new `lead` rows until 001 lands.
- **Do NOT create the full wide `lead` table** — only the §4.4 subset (DEC-017).
- **Do NOT edit V5** or otherwise own 005's content. The 005-seed correction (below) is a separate, 005-owned forward migration.
- **Do NOT hardcode a fixed team-card count** — the grid must flow for a growing roster.
- **Do NOT introduce a keyed/paid maps SDK** (constraint 1) — keyless embed or static image only.
- **Do NOT write a frontend constant for offices or team** — both are tables (DEC-019/DEC-020). Services specializations ARE a frontend constant (DEC-016).

## Cross-feature dependency you must coordinate (architecture.md §9)

Home (and Services software badges) render 005's data verbatim, and the shipped 005 seed (V5) is stale vs. the prequalification doc (years 18+→23, steel 5,000→70,000 t/mo, +new stats, software 3→7 tools). **The correction is a NEW forward Flyway migration owned by 005/012, not 011.** Flag to the human/orchestrator that it should land alongside 011, or the modernized Home page will show the old wrong figures. See architecture.md §9 for the full discrepancy table and §10.1 for the hardcode-vs-derive open decision.

## Open decisions still pending a human ruling (architecture.md §10)

1. 005-seed: hardcode headline counts vs. derive from `project`.
2. Rate limiting in 011 vs. deferred to 001 (honeypot is in scope regardless).
3. Contact map provider (keyless).
4. Landmark-projects strip on About — exists? source (003 vs static)? DEC-008 clearance.

Do not proceed on these four until ruled; the rest of the build is unblocked once architecture v2.0 is approved.
