---
feature: 003-portfolio-filtering
handoff: qa-to-dev
from: QA
to: Dev
date: 2026-06-23
status: needs-revision
---

# Handoff: QA to Dev -- 003 Portfolio with Filtering (Needs Revision)

## Overall verdict

APPROVE WITH CAVEATS. One major bug blocks full AC satisfaction. Three minor bugs noted.

## What passed

- API: 18/18 tests green. All endpoints match the architecture contract. Vocabulary validation, error handling, pagination envelope, and DTO projections are correct.
- Web: TypeScript type-check clean. Detail page renders all F3-AC3 fields with graceful degradation. Per-project SEO metadata works. Dark Prestige design system applied correctly.
- F3-AC2: PASS. Clean slug URLs, 404 on unknown slug, generateMetadata for SEO.
- F3-AC3: PASS. All required fields rendered on detail page.
- F3-AC4: PASS. featurable column, partial index, API filtering, seed data with 8 featurable projects.
- F3-AC1 (API side): PASS. All three filter dimensions work at the API level.

## What must be fixed

### BUG-1 (Major): Missing sector and country filter controls in web UI

**File:** `web/src/components/projects/project-filters.tsx`

**Problem:** The component renders only status-based chips. There are no UI controls for sector or country filtering. F3-AC1 requires filtering by all three dimensions.

**Fix scope:** The data plumbing is complete -- `ProjectBrowser` accepts `filters.sectors` and `filters.countries`, and the client-side filtering logic already applies sector/country predicates. The fix is adding sector and country controls (selects or chip groups) to `ProjectFilters` that call `onChange` with the appropriate `FilterState` updates. The shadcn/ui `Select` component already exists in `web/src/components/ui/select.tsx`.

**No backend changes needed.**

## What should be fixed (minor, not blocking)

### BUG-2 (Minor): force-dynamic instead of ISR

Both page files use `export const dynamic = "force-dynamic"` instead of `export const revalidate = 60`. Switch to ISR per the architecture, or add a DEC record documenting why force-dynamic was chosen.

### BUG-3 (Minor): generateStaticParams not exported

The detail page mentions `generateStaticParams` in comments but does not export it. Add it if switching to ISR.

### BUG-4 (Minor): Sector/country badges missing from ProjectCard

Design spec says "Sector, country, and status shown as compact, muted badges" on cards. Only status is shown. Consider adding sector and/or country badges.

## Re-test plan

After BUG-1 is fixed:
1. `cd web && npx tsc --noEmit` must still pass.
2. Visually confirm sector and country controls appear in the filter bar.
3. Confirm that selecting a sector filters the grid to matching projects only.
4. Confirm that selecting a country filters the grid to matching projects only.
5. Confirm that combining sector + country + status narrows results (AND semantics).
6. Confirm that the URL updates with filter params (?sector=airport&country=UAE&status=completed).
7. Confirm that clearing filters restores the full list.
