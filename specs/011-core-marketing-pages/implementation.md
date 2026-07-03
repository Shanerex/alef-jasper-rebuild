# Implementation: 011 — Core Marketing Pages (Home, About, Services, Contact)

| Field | Value |
|---|---|
| Feature | 011 |
| Branch | feature/011-core-marketing-pages |
| Status | done — 58 API tests green, tsc --noEmit clean |
| Acceptance IDs covered | F11-AC1 through F11-AC9 |
| Depends on | 003 (project table, ProjectCard), 005 (trust endpoint, TrustLayer) |

---

## What was built

### Phase 1 — Backend: Flyway migrations V6–V10

| Migration | What it does |
|-----------|--------------|
| V6__create_team.sql | Creates `team` table. BIGINT GENERATED ALWAYS AS IDENTITY PK, `active` flag, `display_order`, UNIQUE(name, role). |
| V7__seed_team.sql | 10 profiles from architecture.md §4.2. All `photo = NULL` until assets exist. ON CONFLICT DO NOTHING. |
| V8__create_office.sql | Creates `office` table. `address_lines TEXT[]`, `phones TEXT[]` — same Postgres array type as `project.scope`. |
| V9__seed_office.sql | Dubai (2 phones, 3 address lines, email alefllc@eim.ae) and India (1 phone, 3 address lines, email jasperiec@gmail.com). ON CONFLICT (office_key) DO NOTHING. |
| V10__create_lead.sql | Creates minimal `lead` table: id, created_at, source, name, email, phone, company, message. No seed. (DEC-017) |
| V11__update_trust_content_from_prequalification.sql | Pre-existing file (Architect authored). Not touched. |

### Phase 1 — Backend: Spring modules

Three new top-level packages under `com.alef.api`, each following the exact package layout of `portfolio` and `trust`.

#### `com.alef.api.team`
- `GET /api/team` — returns `{ members: [...] }` envelope.
- `TeamService` reads only `active = true` rows, ordered by `display_order ASC`.
- `TeamRepository.findAllByActiveTrueOrderByDisplayOrderAsc()` — Spring Data derived query.
- `TeamMemberDto` (name, role, company, email, photo — all nullable except name/role), `TeamOverviewDto` envelope.

#### `com.alef.api.office`
- `GET /api/offices` — returns `{ offices: [...] }` envelope.
- `OfficeEntity` maps `TEXT[]` columns via `@JdbcTypeCode(SqlTypes.ARRAY)` + `@Column(columnDefinition = "text[]")` — identical to `ProjectEntity.scope` (PITFALL-010).
- `OfficeRepository.findAllByOrderByDisplayOrderAsc()`.
- `OfficeDto` (key, name, addressLines, phones, email, mapQuery).

#### `com.alef.api.lead`
- `POST /api/leads` — receives `ContactLeadRequest` (name, email, phone, company, message, website honeypot), returns `LeadConfirmationDto` (id, status).
- Honeypot check runs FIRST — if `website` is non-blank the request returns a fake 201 with `id = -1` immediately; nothing is persisted and Redis is not touched (so bots get no timing signal).
- `enforceRateLimit(clientIp)` uses `RedisTemplate.opsForValue().increment()` + EXPIRE only on `count == 1`. Throws `ResponseStatusException(TOO_MANY_REQUESTS)` when `count > 10`.
- `@RestControllerAdvice(assignableTypes = LeadController.class)` scoped error handler returns RFC 9457 `ProblemDetail` with a `fields` extension map.
- `lead.created` event emission is intentionally NOT implemented (DEC-018). The lead is persisted; notification is a future concern.

### Phase 2 — Frontend: typed clients and types

| File | Purpose |
|------|---------|
| `web/src/lib/types/team.ts` | TypeScript types mirroring `TeamMemberDto` and `TeamOverviewDto` |
| `web/src/lib/types/office.ts` | TypeScript types mirroring `OfficeDto` and `OfficesDto` |
| `web/src/lib/api/team.ts` | `getTeam()` — typed fetch with `next: { revalidate: 60 }` and error carrying `.status` |
| `web/src/lib/api/offices.ts` | `getOffices()` — same pattern |
| `web/src/lib/api/leads.ts` | `submitLead()` — POST with `cache: "no-store"`, error carries `.status` for form-state discrimination |
| `web/src/lib/content/services.ts` | Static `SERVICES` array (8 entries from design.md §6) — DEC-016 |

### Phase 3 — Frontend: components

#### Shared marketing
- `web/src/components/marketing/page-hero.tsx` — PageHero reusable hero block (breadcrumb, section label, heading, Arabic subtitle, description). Server component. Props-driven.

#### Home page
- `web/src/components/home/home-hero.tsx` — HomeHero. Display XL headline, Arabic accent, two CTAs, grid-pattern overlay, diamond frame decorations.
- `web/src/components/home/capability-strip.tsx` — CapabilityStrip. Gold-dot separated marquee (no JS, CSS animation).
- `web/src/components/home/featured-projects.tsx` — FeaturedProjects. Renders up to 6 projects using the existing `ProjectCard` (003).
- `web/src/components/home/home-cta.tsx` — HomeCTA. Shared gold-background CTA used on Home, About, Services.

#### About page
- `web/src/components/about/company-story.tsx` — CompanyStory. Static founding narrative, numbers grid.
- `web/src/components/about/gcc-reach-block.tsx` — GccReachBlock. Static GCC coverage paragraph and country list.
- `web/src/components/about/landmark-projects-strip.tsx` — LandmarkProjectsStrip. "use client". Doubles the projects array for seamless CSS marquee (`@keyframes translateX(-50%)`). `prefers-reduced-motion` degrades to a static 3-column grid.

#### Team
- `web/src/components/team/team-grid.tsx` — TeamGrid. Server component. Renders a 3-column grid of TeamMemberCard. Empty-state copy when `members` is empty.
- `web/src/components/team/team-member-card.tsx` — TeamMemberCard. Styled card with initials avatar fallback (photo null in seed).

#### Services page
- `web/src/components/services/services-list.tsx` — ServicesList. Renders 8 service cards from the static `SERVICES` constant.
- `web/src/components/services/software-badges.tsx` — SoftwareBadges. Renders software names from 005's trust endpoint. Self-hides when array is empty.

#### Contact page
- `web/src/components/contact/contact-form.tsx` — ContactForm. "use client". 4-state machine: `idle | submitting | success | error` with `errorKind: validation | rate_limit | server`. Honeypot field at `position: absolute; left: -9999px`, `tabIndex={-1}`, `aria-hidden="true"`. Gold left-border highlights on per-field validation errors. Calls `submitLead()`.
- `web/src/components/contact/office-locations.tsx` — OfficeLocations. Server component. Renders one OfficeCard per office with keyless Google Maps iframe (`maps.google.com/maps?q=…&output=embed`).
- `web/src/components/contact/direct-channels.tsx` — DirectChannels. Static phone / WhatsApp / email links for the Dubai office direct channel.

### Phase 4 — Pages

| Route | File | Notes |
|-------|------|-------|
| `/` | `web/src/app/page.tsx` | Replaced placeholder. Composes HomeHero, CapabilityStrip, FeaturedProjects (003), TrustLayer (005), HomeCTA. `Promise.all` + graceful-degradation catch. |
| `/about` | `web/src/app/about/page.tsx` | Composes CompanyStory, GccReachBlock, LandmarkProjectsStrip, TeamGrid, HomeCTA. `Promise.all` for team + featurable projects. Two-branch catch: static sections always render. |
| `/services` | `web/src/app/services/page.tsx` | Composes ServicesList (static SERVICES), SoftwareBadges (trust/005), HomeCTA. Single try/catch for the 005 fetch. |
| `/contact` | `web/src/app/contact/page.tsx` | Composes ContactForm (client), OfficeLocations (server-fetched), DirectChannels (static). Independent: form always renders even when offices fetch fails. |

All four pages: `export const dynamic = "force-dynamic"` (PITFALL-007 — web container builds before api is up).

### Phase 5 — Layout wiring

- `NAV_LINKS` in `web/src/app/layout.tsx` updated: /about, /services, /projects, # (Samples, pending 004), /contact.
- "Enquire Now" CTA href changed from `/` to `/contact` (F11-AC8).
- Footer: `{new Date().getFullYear()} ALEF Architectural & Cadding Services LLC` — dynamic year, "ALEF" and "Cadding" correct.
- Logo tagline reads "Architectural & Cadding Services" (not changed — was already correct).

---

## pom.xml additions

Two new dependencies added before the Testing block:
- `spring-boot-starter-validation` — Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@Size`).
- `spring-boot-starter-data-redis` — `RedisTemplate<String, String>` for rate limiting.

## application.yml additions

Redis connection block under `spring.data.redis` with `SPRING_REDIS_HOST` / `SPRING_REDIS_PORT` env-var overrides defaulting to localhost:6379.

## application-test.yml additions

`RedisAutoConfiguration` and `RedisRepositoriesAutoConfiguration` excluded from the test autoconfigure list so unit and slice tests do not require a live Redis instance.

---

## Tests

Backend: 58 tests, 0 failures, 0 errors (BUILD SUCCESS).

New test files:
- `api/src/test/java/com/alef/api/team/controller/TeamControllerTest.java` — `@WebMvcTest` slice. GET /api/team 200 response structure.
- `api/src/test/java/com/alef/api/office/controller/OfficeControllerTest.java` — `@WebMvcTest` slice. GET /api/offices 200 response structure.
- `api/src/test/java/com/alef/api/lead/controller/LeadControllerTest.java` — `@WebMvcTest` slice. 201 success, 400 validation, 422 honeypot intercept, 429 rate limit.
- `api/src/test/java/com/alef/api/lead/service/LeadServiceTest.java` — Mockito-only unit test (no Spring context). Honeypot short-circuit, rate OK, rate breach, persist path.

Frontend: `npx tsc --noEmit` exits 0 with no output.

---

## Decisions recorded

See DECISIONS.md: DEC-016 through DEC-020.

## Pitfalls recorded

See LEARNINGS.md: PITFALL-008 through PITFALL-011.

---

## Known gaps / follow-ons

- Lead delivery (email notification on `lead.created`) — deferred to a later feature (DEC-018).
- 10 team `photo` fields are NULL — assets TBD; TeamMemberCard shows initials avatar.
- `Samples` nav link stays `#` until feature 004 ships.
- On deploy: flip all four pages from `force-dynamic` to ISR once api is available at build time (PITFALL-007).
- Remaining ~23 projects not yet seeded (V2 has 15 of ~38).
- Redis rate limit assumes Docker Compose's Redis is running. Cold start without Redis will throw on first lead POST — acceptable for local demo.
