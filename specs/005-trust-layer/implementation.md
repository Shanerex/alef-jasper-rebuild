---
feature: 005-trust-layer
spec_id: "005"
phase: implementation
owner: Dev
status: approved
version: "1.0"
entry_criteria:
  - upstream phase approved
exit_criteria:
  - Dev has produced this file and a human has approved it
---

# Implementation: Trust Layer

## 1. What was built

Full backend + frontend implementation of the Trust Layer feature (F5-AC1, F5-AC2, F5-AC3) per the approved architecture and design specs. The feature surfaces credibility signals as a portable, self-contained section for the Home page (F011 to mount).

### Backend (api/)

| File | Purpose |
|------|---------|
| `db/migration/V4__create_trust_content.sql` | DDL for the `trust_content` key/value table (architecture 2.1) |
| `db/migration/V5__seed_trust_content.sql` | Idempotent seed of 9 editorial facts: 3 stats, 3 software, 3 standards |
| `com.alef.api.trust.entity.TrustContentEntity` | JPA entity mapping to `trust_content` table |
| `com.alef.api.trust.repository.TrustContentRepository` | Spring Data JPA repository with finder by item_type ordered by display_order |
| `com.alef.api.trust.vocabulary.ItemType` | App-layer vocabulary enum: stat, software, standard (DEC-009 pattern) |
| `com.alef.api.trust.dto.TrustStatDto` | Individual stat DTO (key, label, value, unit) |
| `com.alef.api.trust.dto.TrustOverviewDto` | Composed overview DTO (stats, clients, contractors, software, standards) |
| `com.alef.api.trust.service.TrustService` | Composition logic: mixes trust_content rows with project-derived aggregates |
| `com.alef.api.trust.controller.TrustController` | `GET /api/trust/overview` endpoint |

Additionally, three new query methods were added to `ProjectRepository` (portfolio package) since the trust service derives client/contractor names and the marquee count from the existing `project` table:
- `findDistinctClients()` -- distinct non-null client names, alphabetical
- `findDistinctMainContractors()` -- distinct non-null main_contractor names, alphabetical
- `countByFeaturableTrue()` -- count of marquee projects

### Frontend (web/)

| File | Purpose |
|------|---------|
| `web/src/lib/types/trust.ts` | TrustStat and TrustOverview interfaces |
| `web/src/lib/api/trust.ts` | `getTrustOverview()` typed client with ISR revalidate |
| `web/src/components/trust/trust-stats.tsx` | Band 1: gold-inverted Stat Display row (F5-AC1) |
| `web/src/components/trust/client-strip.tsx` | Band 2: named text strip (F5-AC2) |
| `web/src/components/trust/capability-badges.tsx` | Band 3: gold-muted capability badges (F5-AC3) |
| `web/src/components/trust/trust-layer.tsx` | Composition of heading + three bands |
| `web/src/components/trust/trust-layer-section.tsx` | Self-fetching async server wrapper for F011 drop-in |

## 2. Test coverage

| Test file | Tests | Status |
|-----------|-------|--------|
| `TrustControllerTest` | 2 (happy path, empty data) | Green |
| `TrustServiceTest` | 6 (stat assembly, marquee synthesis, unit presence, client/contractor derivation, software/standards extraction, display order, empty state) | Green |
| `ItemTypeTest` | 3 (wire values, valid, invalid) | Green |
| Frontend type-check (`tsc --noEmit`) | N/A | Green |

Full suite: 29/29 tests pass (18 existing + 11 new).

## 3. Acceptance criteria traceability

| AC | Implementation |
|----|----------------|
| F5-AC1 | `TrustStats` renders stats array from `GET /api/trust/overview` as gold-inverted Stat Display row. Stats include years_in_business, staff_count, monthly_steel_capacity_tonnes (from trust_content) + marquee_projects (synthesized live from project table). Values shown verbatim. |
| F5-AC2 | `ClientStrip` renders clients + contractors arrays derived from `SELECT DISTINCT client/main_contractor FROM project`. Named text strip with gold middot separators. |
| F5-AC3 | `CapabilityBadges` renders software + standards arrays from trust_content as gold-muted outline badges using the existing Badge component. |

## 4. Deviations from specs

- **TrustStats border approach**: The design template used `className="border-page/10 ... sm:border-l first:border-l-0 lg:border-l"` with an inline style override on the first cell. This was simplified to use inline styles for the left border on all cells (transparent for index 0, the rgba value for others), avoiding a Tailwind specificity conflict between `first:border-l-0` and `sm:border-l` / `lg:border-l`. Visually identical.
- **No error handler class**: The trust endpoint has no feature-specific exceptions (no user input, no 404 path), so no `TrustExceptionHandler` was created. The endpoint always returns 200 with empty arrays for missing data.

## 5. What is NOT built here

- **Page placement**: The section is not mounted on any page yet. F011 (Home) owns that. The `TrustLayerSection` wrapper is ready for drop-in use.
- **Admin CRUD**: No write path for trust_content. That is F012 (Admin Content Management).
- **Bilingual content**: F008 will localize labels/values. The structural eyebrows ("Trusted By", "Main Contractors", etc.) are isolated in the component files as the natural future i18n surface.
