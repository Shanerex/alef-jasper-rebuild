# Handoff: Core Marketing Pages — Dev -> QA

| Field | Value |
|---|---|
| Feature # | 011 |
| From / To | Dev -> QA |
| Date | 2026-07-01 |
| Branch | feature/011-core-marketing-pages |
| API test result | 58 tests, 0 failures, 0 errors — BUILD SUCCESS |
| TypeScript check | `npx tsc --noEmit` exits 0, no output |
| Spec | specs/011-core-marketing-pages/requirements.md, architecture.md, design.md |
| Acceptance IDs | F11-AC1 through F11-AC9 |
| Testing spec | specs/011-core-marketing-pages/testing.md |

---

## What was shipped

### Backend (Spring Boot)

Three new Spring modules under `com.alef.api`:

**`team` — `GET /api/team`**
- Returns `{ members: [{ name, role, company, email, photo }] }`.
- Only active members, ordered by `display_order ASC`.
- 10 profiles seeded (V7 migration). `photo` is null for all — avatar fallback in the UI.

**`office` — `GET /api/offices`**
- Returns `{ offices: [{ key, name, addressLines, phones, email, mapQuery }] }`.
- 2 offices seeded (V9): Dubai and India.
- `address_lines` and `phones` are Postgres `TEXT[]` mapped as `List<String>`.

**`lead` — `POST /api/leads`**
- Request body: `{ name, email, phone?, company?, message?, website? }`.
- `website` is the honeypot — must be absent or blank from real users.
- Returns `{ id, status }` on success (HTTP 201).
- Validation: name required, email required + valid format, message ≤ 2000 chars.
- Rate limit: 5 submissions per IP per hour (Redis `INCR` + 3600s `EXPIRE`).
- Error shape: RFC 9457 `ProblemDetail` with a `fields` extension map for validation errors.

### Database (Flyway)

| Migration | Table |
|-----------|-------|
| V6 | team (DDL) |
| V7 | team (seed — 10 profiles) |
| V8 | office (DDL) |
| V9 | office (seed — 2 offices) |
| V10 | lead (DDL — no seed) |

### Frontend (Next.js)

**New pages:**

| Route | Notes |
|-------|-------|
| `/` | Full home page: HomeHero, CapabilityStrip, FeaturedProjects (003), TrustLayer (005), HomeCTA |
| `/about` | CompanyStory, GccReachBlock, LandmarkProjectsStrip (client marquee), TeamGrid, HomeCTA |
| `/services` | ServicesList (8 static entries), SoftwareBadges (005 data), HomeCTA |
| `/contact` | ContactForm (client), OfficeLocations (server-fetched), DirectChannels |

All pages use `export const dynamic = "force-dynamic"` (PITFALL-007 guard).

**New components (abbreviated):**
- `PageHero` — shared breadcrumb / heading / Arabic subtitle block
- `HomeHero`, `CapabilityStrip`, `FeaturedProjects`, `HomeCTA`
- `CompanyStory`, `GccReachBlock`, `LandmarkProjectsStrip`
- `TeamGrid`, `TeamMemberCard`
- `ServicesList`, `SoftwareBadges`
- `ContactForm` (client component), `OfficeLocations`, `DirectChannels`

**Layout changes:**
- `NAV_LINKS` in `web/src/app/layout.tsx`: `/about`, `/services`, `/projects`, `#` (Samples pending 004), `/contact`.
- "Enquire Now" CTA → `/contact`.
- Footer year: `{new Date().getFullYear()}`.

---

## Acceptance criteria to verify

| ID | Criterion | How to verify |
|----|-----------|---------------|
| F11-AC1 | Home page replaces placeholder — renders hero, featured projects, trust stats, CTA | `GET /` renders each section visually |
| F11-AC2 | Home reuses 003's ProjectCard and 005's TrustLayer — no copy/paste | Inspect source; no duplicate card or stat component |
| F11-AC3 | About page renders company story, GCC reach, marquee strip, team grid | `GET /about` |
| F11-AC4 | TeamGrid renders active team members from `GET /api/team` | Confirm 10 members appear; initials avatar shown (photo null) |
| F11-AC5 | Services page renders 8 service cards and software badges | `GET /services`; confirm 8 cards |
| F11-AC6 | Contact form POSTs to `POST /api/leads` and shows success state | Fill form, submit; expect success banner; check Postgres `lead` table |
| F11-AC7 | Contact page shows both office locations with maps | `GET /contact`; Dubai and India cards with iframe |
| F11-AC8 | "Enquire Now" nav CTA links to `/contact` | Click button in nav; verify destination |
| F11-AC9 | Footer shows current year dynamically | Footer text includes `2026` (or current year); no hardcoded year |

---

## Edge cases to test

### Contact form
| Scenario | Expected result |
|----------|----------------|
| Submit with name blank | HTTP 400; `fields.name` in response; form highlights name field |
| Submit with invalid email | HTTP 400; `fields.email` in response |
| Submit with message > 2000 chars | HTTP 400; `fields.message` in response |
| Submit with website (honeypot) filled | HTTP 201 (fake); no row in `lead` table; no Redis increment |
| Submit 11 times from same IP within an hour | 11th request returns HTTP 429; form shows rate-limit message |
| Submit with cold API (api container down) | Form renders; submit attempt shows server-error state gracefully |

### Graceful degradation (cold API)
| Page | Section affected | Expected |
|------|-----------------|---------|
| Home | FeaturedProjects + TrustLayer | Static sections render; muted "loading" message in data area |
| About | LandmarkProjectsStrip + TeamGrid | Static sections render; "Team and project data are loading" message |
| Services | SoftwareBadges | SoftwareBadges self-hides; 8 service cards always show |
| Contact | OfficeLocations | Empty-state copy in office section; ContactForm always renders |

### Marquee (LandmarkProjectsStrip)
| Scenario | Expected |
|----------|---------|
| OS has `prefers-reduced-motion: reduce` | Static 3-column grid instead of scrolling marquee |
| Fewer than 3 featured projects | Still renders without errors (doubled array has 0–2 items each half) |

---

## What is NOT in scope for this QA pass

- `lead.created` Redis Stream event — not emitted (DEC-018). No consumer test needed.
- Team photo uploads — all photos are null; initials avatar is the expected state.
- `Samples` nav link routes to `#` — not wired until feature 004.
- Lead delivery / email notification — deferred to a future feature.
- Admin CMS for teams, offices, or services — feature 012.

---

## Environment setup

```bash
# Ensure all services are running
make infra-up   # postgres, qdrant, redis, ollama

# Start the API (Spring Boot on :8080)
cd api && ./mvnw spring-boot:run

# Start the web (Next.js on :3000)
cd web && npm run dev
```

The `lead` rate-limit check requires Redis to be running. Without Redis, `POST /api/leads` will throw a 500 on the first submission. This is expected behaviour for the local demo without infra up.

---

## Test files (backend)

| File | Coverage |
|------|---------|
| `api/src/test/java/com/alef/api/team/controller/TeamControllerTest.java` | GET /api/team 200 shape |
| `api/src/test/java/com/alef/api/office/controller/OfficeControllerTest.java` | GET /api/offices 200 shape |
| `api/src/test/java/com/alef/api/lead/controller/LeadControllerTest.java` | 201 success, 400 validation, 422 honeypot, 429 rate limit |
| `api/src/test/java/com/alef/api/lead/service/LeadServiceTest.java` | Unit: honeypot short-circuit, rate OK, rate breach, persist path |

Run: `cd api && ./mvnw test`

---

## Known gaps (not regressions)

- `Samples` nav link is `#` — expected until feature 004.
- Team member photos all null — initials avatar is intentional.
- No ISR caching on any marketing page — `force-dynamic` is intentional until the deploy model makes the API available at Next.js build time (flip is a one-line change, see PITFALL-007).
