# Handoff: Trust Layer

| Field | Value |
|---|---|
| Feature # | 005 |
| From / To | Dev -> QA |
| Status | ready |
| Spec | specs/005-trust-layer/implementation.md |

## What was built

Full backend + frontend implementation of the Trust Layer feature. The feature surfaces credibility signals (headline stats, client/contractor names, software/standards badges) as a portable, self-contained section for the Home page.

### Backend
- Flyway V4: `trust_content` table (key/value store for editorial facts)
- Flyway V5: 9 seed rows (3 stats, 3 software, 3 standards)
- `com.alef.api.trust.*` package: entity, repository, vocabulary, DTOs, service, controller
- `GET /api/trust/overview` endpoint returning the composed trust payload
- Three new query methods on `ProjectRepository` for deriving client/contractor names and marquee count

### Frontend
- `TrustStat` and `TrustOverview` TypeScript types
- `getTrustOverview()` API client with ISR revalidate
- 5 components: `TrustStats`, `ClientStrip`, `CapabilityBadges`, `TrustLayer`, `TrustLayerSection`
- All server components, no client state

## How to test

### Prerequisites
- Postgres running with Flyway migrations applied (V1-V5)
- API running on port 8080
- Project seed data in place (V2) for client/contractor derivation

### API endpoint verification

1. **Happy path**: `GET http://localhost:8080/api/trust/overview`
   - Verify 200 response
   - Verify `stats` array has 4 items: years_in_business, staff_count, monthly_steel_capacity_tonnes, marquee_projects
   - Verify `stats[0].value` is "18+" (from seed)
   - Verify `stats[2].unit` is "tonnes / month"
   - Verify `stats[3].key` is "marquee_projects" and value matches `SELECT count(*) FROM project WHERE featurable = TRUE`
   - Verify `clients` is a non-empty array of distinct client names from the project table
   - Verify `contractors` is a non-empty array of distinct main_contractor names
   - Verify `software` is `["AutoCAD", "CADS RC", "SteelPac RC"]`
   - Verify `standards` is `["BS 8666", "ACI 318", "BS EN ISO 3766"]`

2. **Empty data**: If trust_content table is emptied and no projects exist, verify all arrays are empty (not null) and response is still 200.

3. **Ordering**: Verify stats, software, and standards respect `display_order` from trust_content.

### Frontend component verification

Since F011 (Home page) is not yet built, the components cannot be visually tested in-browser yet. Verification is via:
- TypeScript type-check: `cd web && npx tsc --noEmit` (confirmed green)
- Code review against the design spec templates (design.md section 5)

### Acceptance criteria checklist

- [ ] F5-AC1: Stats array contains years in business, staff count, monthly steel capacity, marquee project count
- [ ] F5-AC2: Clients and contractors arrays are derived from the project table (not hardcoded)
- [ ] F5-AC3: Software and standards arrays contain the seeded capability items

### Graceful degradation checklist

- [ ] Each band self-hides when its arrays are empty
- [ ] Entire section hides when everything is empty
- [ ] No double gaps when a band is hidden (margins are on wrapper conditionals)
- [ ] API returns empty arrays, never nulls

## Test results

| Suite | Tests | Status |
|-------|-------|--------|
| TrustControllerTest | 2 | Green |
| TrustServiceTest | 6 | Green |
| ItemTypeTest | 3 | Green |
| Frontend tsc --noEmit | N/A | Green |
| Full suite | 29/29 | Green |

## Known issues

- **No visual testing yet**: Components are built but not mounted on any page. F011 (Home) will mount the `TrustLayerSection` wrapper or call `getTrustOverview()` directly and pass to `<TrustLayer data={...} />`.
- **Marquee count depends on seed data**: The synthesized marquee_projects stat counts `featurable = TRUE` projects. With the current 15-project seed, this should be non-zero. If projects change, the count updates automatically.
- **DEC-008 name clearance**: Real client/contractor names from the project table are displayed. Cleared for local demo only; revisit before public deploy.
