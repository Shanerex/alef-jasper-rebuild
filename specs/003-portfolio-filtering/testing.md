---
feature: 003-portfolio-filtering
spec_id: "003"
phase: testing
owner: QA
status: approved
version: "2.0"
entry_criteria:
  - upstream phase approved
exit_criteria:
  - QA has verified all acceptance criteria and reported results
---

# Testing: 003 Portfolio with Filtering (Round 2 -- Re-verification)

Round 1 (v1.0, 2026-06-23) found 4 bugs: 1 major (BUG-1, missing sector/country filter controls), 3 minor (BUG-2/3 force-dynamic documentation, BUG-4 card badges). Dev applied fixes for BUG-1 and BUG-2/3. This round re-verifies those fixes and re-checks all acceptance criteria.

---

## Test Run Summary

| Suite | Runner | Result | Details |
|-------|--------|--------|---------|
| API unit tests | `cd api && ./mvnw test` | PASS (18/18) | 5 controller, 7 service, 3 sector vocab, 3 status vocab. Zero failures, zero skipped. BUILD SUCCESS. |
| Web type-check | `cd web && npx tsc --noEmit` | PASS | Clean exit, no errors, no output. |

---

## Bug Fix Verification

### BUG-1 (Major): Missing sector and country filter controls -- FIXED

**Round 1 finding:** `ProjectFilters` rendered only status chips. No UI for sector or country filtering.

**Verification:** The `ProjectFilters` component (`web/src/components/projects/project-filters.tsx`) has been completely rewritten. Evidence of fix:

1. **Three labeled filter rows.** The component now renders three `FilterRow` sections: Status (line 53-81), Sector (line 84-100), and Country (line 103-119). Each row has an "All" chip plus one chip per vocabulary option.
2. **SECTOR_LABELS used for human-readable names.** Line 4 imports `SECTOR_LABELS` and `STATUS_LABELS` from `@/lib/types/project`. Line 93 applies `SECTOR_LABELS[s] || s` to map wire tokens (e.g. "mall_retail") to display labels ("Mall / Retail").
3. **Country options from `filters.countries` prop.** Line 109-118 iterates `filters.countries` and renders raw country strings (e.g. "UAE", "Qatar") as chip labels.
4. **Dark Prestige styling.** The `FilterChip` component (lines 173-198) uses active style `background: "#C4973A"` / `color: "#07111C"` (gold bg, dark text) and inactive style `border: "1px solid rgba(196,151,58,0.2)"` / `color: "#7A8FA8"` (gold-subtle border, muted text). Sharp corners (no border-radius). Matches DESIGN_SYSTEM.md filter chip spec.
5. **Multi-dimension state management.** The `setFilter` function (line 39-41) updates a single dimension via spread operator `{ ...value, [dimension]: next }`, preserving other dimensions. Toggle-off is supported: clicking an active chip sets its value to `undefined`.
6. **ProjectBrowser wiring.** `project-browser.tsx` (line 71-76) passes `filters`, `value={activeFilters}`, and `onChange={handleFilterChange}` to `ProjectFilters`. The `activeFilters` memo (lines 35-39) reads `sector`, `country`, and `status` from URL search params. The `handleFilterChange` callback (lines 45-56) writes all three dimensions back to the URL. The `filteredProjects` memo (lines 59-66) applies AND semantics across all three dimensions.

**Verdict: FIXED.** All three filter dimensions are now fully controllable from the UI.

### BUG-2/3 (Minor): force-dynamic documentation -- FIXED

**Round 1 finding:** Both page files used `force-dynamic` without explaining why, deviating from the architecture's ISR prescription.

**Verification:**

1. **List page doc comment.** `web/src/app/projects/page.tsx` lines 20-31 contain a comprehensive doc comment explaining: the architecture spec prescribes ISR; the Docker Compose build order means the API is not available at build time; force-dynamic defers fetches to request time; this is correct for the local-first constraint; how to switch to ISR if the deployment model changes.
2. **Detail page doc comment.** `web/src/app/projects/[slug]/page.tsx` lines 11-23 contain an equivalent explanation, additionally noting generateStaticParams would also fail at build time.
3. **PITFALL-007 in LEARNINGS.md.** `docs/knowledge/LEARNINGS.md` lines 5-8 document PITFALL-007 ("ISR requires API at build time; force-dynamic is correct for Docker Compose") with the takeaway and how-to-apply guidance.
4. **CLAUDE.md deploy TODO.** `CLAUDE.md` line 90 contains the reminder: "On deploy: switch portfolio pages from force-dynamic to ISR (revalidate=60) if API is available at build time (see PITFALL-007)."

**Verdict: FIXED.** The deviation is now documented as a deliberate, reasoned decision with a clear migration path. PITFALL-007 exists. Deploy TODO exists.

### BUG-4 (Minor): Sector/country badges missing from ProjectCard -- NOT FIXED (accepted)

**Round 1 finding:** Design spec says "Sector, country, and status shown as compact, muted badges" on project cards, but only status badge is rendered.

**Verification:** `web/src/components/projects/project-card.tsx` still shows only a status badge overlay on the image (line 63, `StatusBadge`) and location text below the gold border (lines 69-76). No sector or country badge is rendered on the card. The description fallback text (lines 87-89) mentions sector and country inline, but not as badges.

**Verdict: NOT FIXED.** This was classified as minor in round 1 and was not listed as a required fix in the QA-to-Dev handoff. The design.md spec mentions sector/country/status badges on cards, but the DESIGN_SYSTEM.md (which is the authoritative visual reference) describes the ProjectCard pattern with location, title, description, and "Read More" link -- it does not explicitly require sector/country badges in the card content area. The detail page correctly shows all three badges. This remains a minor polish item, not a blocker.

---

## Acceptance Criteria Results

| AC ID | Criterion | Verdict | Evidence |
|-------|-----------|---------|----------|
| F3-AC1 | Filter by sector, country, and status | PASS | **API:** Controller accepts `sector`, `country`, `status` query params (ProjectController.java:42-48). Service validates sector/status against vocabulary enums; unknown sector/status returns 400. Unknown country returns empty result (by design). Specifications compose AND semantics (ProjectSpecifications.java). Tests confirm invalid-sector-400 and valid-filter-returns-results. `/api/projects/filters` returns all three vocabularies. **Web:** ProjectFilters now renders three filter rows -- Status, Sector, Country -- each with an "All" chip plus per-value chips (project-filters.tsx:53-119). SECTOR_LABELS provides human-readable names. Country chips use raw strings from filters.countries. FilterState is managed via URL search params with AND semantics (project-browser.tsx:59-66). All three dimensions are fully controllable. BUG-1 FIXED. |
| F3-AC2 | Each project has a clean slug URL and is independently indexable | PASS | Detail page at `web/src/app/projects/[slug]/page.tsx`. API serves by slug via `findBySlug` (ProjectRepository.java). Unknown slug returns 404 via ProjectNotFoundException. Page generates per-project metadata via `generateMetadata` (title, description, OG image). Slug is `NOT NULL UNIQUE` in V1 DDL. force-dynamic is now documented as a deliberate Docker Compose accommodation with migration path (PITFALL-007). Pages are fully server-rendered and SEO-indexable. |
| F3-AC3 | Detail shows image, description, main contractor, client, consultant, location, scope, status | PASS | ProjectDetailDto is a Java record with all required fields. ProjectDetail component renders: hero image (lines 50-66), description (lines 98-104), mainContractor/client/consultant/location as CreditField cards (lines 113-127), scope as badges (lines 129-145), status badge (line 87-89), sector badge (line 90-91), country badge (line 93). Nullable fields render conditionally with `&&` guards (graceful degradation). |
| F3-AC4 | Marquee projects are featurable on home page | PASS | Entity has `featurable boolean` column (ProjectEntity.java:73-74). V1 DDL includes `featurable BOOLEAN NOT NULL DEFAULT FALSE` and a partial index `WHERE featurable = TRUE`. API supports `?featurable=true` via ProjectSpecifications.isFeaturable (ProjectSpecifications.java:57-62). V2 seed has 8 projects marked TRUE and 7 FALSE. ProjectSummaryDto carries `featurable` for downstream consumers (features 005, 011). |

---

## Design System Compliance

| Check | Verdict | Evidence |
|-------|---------|----------|
| Dark Prestige palette (deep navy, gold accents) | PASS | Tailwind config: `page: "#07111C"`, `gold.DEFAULT: "#C4973A"`, surface layers `#0C1B2E`/`#0E2035`/`#101F32`/`#122339`, footer `#040C14` (tailwind.config.ts:19-36). Body background `#07111C`. |
| Fonts: Cormorant Garamond (serif), Montserrat (sans), Noto Serif Arabic | PASS | Tailwind fontFamily config (tailwind.config.ts:44-48). Google Fonts loaded in layout.tsx (line 46-48) with correct weights. h1-h6 use Cormorant Garamond. Arabic text uses Noto Serif Arabic (layout.tsx:89, projects/page.tsx:145). |
| Sharp corners (border-radius 0) | PASS | All borderRadius values set to "0" except `full` (tailwind.config.ts:50-60). FilterChip has no border-radius in inline styles. |
| Gold top border on project cards | PASS | ProjectCard uses `border-t-2 border-gold` (project-card.tsx:67). |
| Responsive grid: 3-col desktop, 2 tablet, 1 mobile | PASS | ProjectGrid: `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3` (project-grid.tsx:28). First card spans 2 columns with `sm:col-span-2`. |
| Filter chips: gold active, muted inactive | PASS | FilterChip active state: `background: "#C4973A"`, `color: "#07111C"` (project-filters.tsx:181-186). Inactive state: `border: "1px solid rgba(196,151,58,0.2)"`, `color: "#7A8FA8"` (lines 188-194). Matches DESIGN_SYSTEM.md button/chip spec. |
| Filter bar structure | PASS | Three labeled rows (Status, Sector, Country) with gold section labels (8px/bold/uppercase, letter-spacing 0.2em). Rows separated by subtle gold dividers. Project count displayed at right of status row. |

---

## Code Quality

| Check | Verdict | Evidence |
|-------|---------|----------|
| Doc comments on functions | PASS | All public classes and methods have Javadoc. Web components have JSDoc on each exported function. FilterRow, FilterChip, CreditField, StatusBadge helper components all have doc comments. Page files have comprehensive doc comments explaining rendering strategy. |
| Package structure matches design spec | PASS | All packages at `com.alef.api.portfolio.{controller,service,repository,entity,dto,vocabulary,error}`. |
| V1 DDL matches architecture 2.1 | PASS | CREATE TABLE project with all columns and four indexes. |
| V2 seed uses ON CONFLICT (slug) DO NOTHING | PASS | All 15 INSERT statements use idempotent upsert per DEC-012. |
| DTOs are Java records | PASS | ProjectSummaryDto, ProjectDetailDto, ProjectFiltersDto, PagedResponse, ProjectFilterQuery are all `record` types. |
| Vocabulary enums match requirements | PASS | Sector: airport, mall_retail, hotel_hospitality, residential, infrastructure_rail, leisure_museum. ProjectStatus: ongoing, completed. |
| LEARNINGS.md updated per protocol | PASS | PITFALL-007 appended at top (newest-first), correctly formatted. |

---

## New Bugs Found

None. No new issues discovered in round 2.

---

## Regression Notes

- The API test suite runs against H2 in-memory with PostgreSQL compatibility mode. Flyway is disabled in tests; schema is created by Hibernate `ddl-auto: create-drop`. Flyway migrations are NOT tested by unit tests. Integration testing with Postgres (via Testcontainers) would catch migration issues but is acceptable per DEC-006 (risk-based coverage).
- Spring AI Ollama auto-configuration is excluded in tests per PITFALL-006.
- The `web/` type-check validates type correctness but does not verify runtime behavior. All web-side verification beyond types requires a running API and browser.
- CLAUDE.md line 79 still describes the portfolio pages as "ISR" in the key files table. This is a minor documentation inconsistency now that force-dynamic is the actual strategy. Not a code bug.

---

## Gaps (carried from round 1, unchanged)

1. **No integration tests.** All API tests mock the repository. No tests run against a real Postgres instance. Acceptable per DEC-006 (risk-based coverage).
2. **No web runtime tests.** No Playwright/Cypress/Jest tests for Next.js pages or components. All web verification is type-level only.
3. **Seed data is 15 of ~38 projects.** Documented known gap. Not a blocker; all sectors, countries, and statuses are represented.
4. **No project images.** Placeholder paths in seed data; cards render "project photograph" fallback.
5. **BUG-4 (Minor) still open.** Sector/country badges not shown on project cards in the grid. Design.md mentions them but DESIGN_SYSTEM.md does not explicitly require them in the card content area. Polish item for a future pass.

---

## Recommendation

**APPROVE**

All four acceptance criteria (F3-AC1 through F3-AC4) now pass. The major bug from round 1 (BUG-1, missing sector and country filter controls) has been fixed with a thorough, well-structured implementation that includes all three filter dimensions, human-readable labels, Dark Prestige styling, and correct AND semantics via URL search params. The minor bugs (BUG-2/3) have been resolved by documenting the force-dynamic deviation as a deliberate Docker Compose accommodation with PITFALL-007 and a deploy TODO. BUG-4 (card badges) remains open as a minor polish item that does not affect any acceptance criterion.

The API test suite passes 18/18. The web type-check passes clean. Design system compliance is verified across palette, fonts, sharp corners, gold borders, filter chip styling, and responsive grid. Code quality checks pass including doc comments and package structure.

Feature 003 is approved for merge.
