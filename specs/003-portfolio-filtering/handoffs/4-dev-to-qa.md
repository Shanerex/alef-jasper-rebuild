---
feature: 003-portfolio-filtering
handoff: dev-to-qa
from: Dev
to: QA
date: 2026-06-24
revision: 2
---

# Handoff: Dev to QA -- 003 Portfolio with Filtering (Revision 2)

## What changed since revision 1

This revision addresses QA findings from the 5-qa-to-dev handoff:

### BUG-1 (Major) -- FIXED: Sector and country filter controls added

**File changed:** `web/src/components/projects/project-filters.tsx`

The `ProjectFilters` component now renders three labeled chip rows:
- **Status:** "All" / Ongoing / Completed
- **Sector:** "All" / Airport / Mall-Retail / Hotel-Hospitality / Residential / Infrastructure-Rail / Leisure-Museum
- **Country:** "All" / (dynamic list from `filters.countries`)

Each row has a gold section label on the left (8px uppercase Montserrat, gold). Chips use the same Dark Prestige active/inactive styling as before. The "Showing N projects" counter is on the right of the Status row.

No changes to `ProjectBrowser`, types, or the API were needed -- the data plumbing was already complete.

### BUG-2/3 (Minor) -- DOCUMENTED: force-dynamic is deliberate

**Files changed:** `web/src/app/projects/page.tsx`, `web/src/app/projects/[slug]/page.tsx`

Added comprehensive doc comments explaining why `force-dynamic` is used instead of ISR. The Docker Compose build order means the web image is built before the api container starts, so build-time API fetches would fail. This is logged as PITFALL-007 in LEARNINGS.md.

### BUG-4 (Minor) -- NOT ADDRESSED

Sector/country badges on `ProjectCard` are a polish item. The filter controls (BUG-1) give users the discoverability that BUG-4 concerns. Deferring to a future polish pass.

## Re-test checklist (from QA handoff)

1. `cd web && npx tsc --noEmit` -- PASS (clean exit, no errors)
2. Visually confirm sector and country controls appear in the filter bar
3. Confirm that selecting a sector filters the grid to matching projects only
4. Confirm that selecting a country filters the grid to matching projects only
5. Confirm that combining sector + country + status narrows results (AND semantics)
6. Confirm that the URL updates with filter params (?sector=airport&country=UAE&status=completed)
7. Confirm that clearing filters restores the full list
8. `cd api && ./mvnw test` -- PASS (18/18, zero failures)

## Acceptance criteria to verify

| AC | What to test |
|----|-------------|
| **F3-AC1** | Filter by sector, country, status. All three filter dimensions now have UI controls. Verify AND semantics (combining filters narrows results). Verify URL state is preserved. |
| **F3-AC2** | Each project at `/projects/{slug}` is a clean URL. Verify unknown slug returns 404. Verify per-project metadata. |
| **F3-AC3** | Detail page shows image, description, main contractor, client, consultant, location, scope, status. |
| **F3-AC4** | `GET /api/projects?featurable=true` returns only featurable projects (8 of 15). |

## How to run

```bash
# Full stack (requires Docker)
make infra-up && make up
# API: http://localhost:8080/api/projects
# Web: http://localhost:3000/projects

# API tests only (requires JDK 17+)
cd api && ./mvnw test

# Web type-check only (requires Node 18+)
cd web && npx tsc --noEmit
```

## Test results

- 18 API tests: 5 controller, 7 service, 6 vocabulary. All green.
- Web: TypeScript compiles cleanly with `--noEmit`.

## Known gaps (unchanged from revision 1)

1. Seed data is 15 projects, not the full ~38. All sectors, countries, and statuses are represented.
2. No project images in `/public/img/projects/`. Placeholder paths in seed data.
3. BUG-4 (sector/country badges on cards) deferred to a polish pass.
