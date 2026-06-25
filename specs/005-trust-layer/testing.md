---
feature: 005-trust-layer
spec_id: "005"
phase: testing
owner: QA
status: approved
version: "1.0"
entry_criteria:
  - upstream phase approved
exit_criteria:
  - QA has verified all acceptance criteria and the full suite passes
---

# Testing: Trust Layer

## 1. Test Run Summary

| Suite | Tests | Status |
|-------|-------|--------|
| ProjectControllerTest | 5 | PASS |
| ProjectServiceTest | 7 | PASS |
| SectorTest | 3 | PASS |
| ProjectStatusTest | 3 | PASS |
| TrustControllerTest | 2 | PASS |
| TrustServiceTest | 6 | PASS |
| ItemTypeTest | 3 | PASS |
| **Total (API)** | **29** | **ALL PASS** |
| Frontend tsc --noEmit | N/A | **PASS (clean)** |

No regressions. Existing 18 portfolio tests still pass. 11 new trust tests all pass.

## 2. Acceptance Criteria Verification Matrix

| AC ID | Criterion | Verified | Evidence |
|-------|-----------|----------|----------|
| F5-AC1 | Home shows years in business, staff count, monthly steel capacity, marquee project count | YES | V5 seed has 3 stat rows (years_in_business="18+", staff_count="120+", monthly_steel_capacity_tonnes="5,000" with unit "tonnes / month"). TrustService.assembleStats() appends a synthesized marquee_projects stat from `projectRepository.countByFeaturableTrue()`. TrustControllerTest verifies the JSON shape has 4 stats with correct keys/values. TrustStats component renders the stats array as gold-inverted Stat Display cards with label, value, and optional unit. Values come from API, never hardcoded in frontend. |
| F5-AC2 | Client/contractor names shown as a logo or named strip | YES | TrustService.getOverview() derives clients from `projectRepository.findDistinctClients()` and contractors from `projectRepository.findDistinctMainContractors()`. Both use `SELECT DISTINCT ... WHERE ... IS NOT NULL ORDER BY ...`. ClientStrip component renders names as a text strip with gold middot separators. TrustServiceTest.getOverview_derives_clients_and_contractors_from_project_table() verifies derivation. TrustControllerTest verifies JSON arrays are present. Design resolution (section 7.1) explicitly chose text strip over logo grid for v1. |
| F5-AC3 | Software and standards (AutoCAD, CADS RC, SteelPac RC, international detailing standards) shown as capability badges | YES | V5 seed has 3 software rows (AutoCAD, CADS RC, SteelPac RC) and 3 standard rows (BS 8666, ACI 318, BS EN ISO 3766). TrustService.extractLabels() retrieves labels by item_type from trust_content. CapabilityBadges component uses the existing Badge component (variant="outline") per design spec. TrustServiceTest.getOverview_extracts_software_and_standards_labels() verifies extraction. TrustControllerTest verifies JSON arrays. |

## 3. Bug List

No bugs found.

## 4. Invariant Compliance Checklist

| Invariant | Status | Notes |
|-----------|--------|-------|
| Doc comment on every function (CLAUDE.md #6) | PASS | All 9 backend classes/methods and all 7 frontend functions have intent-focused doc comments. |
| No "use client" in trust components (architecture constraint: no client state) | PASS | Verified via grep -- zero occurrences. All components are server-renderable. |
| No hardcoded stat values in frontend (values come from API) | PASS | Verified via grep -- stat values only appear in doc comments, never in rendered output. |
| Knowledge layer updated (DECISIONS.md) | PASS | DEC-013, DEC-014, DEC-015 appended, covering the three architectural decisions. |
| ARCHITECTURE.md updated (trust_content in data model) | PASS | trust_content record added to the Data model section. |
| CLAUDE.md updated (spec status, endpoint, table, key files) | PASS | Feature 005 listed as "implemented (pending QA)", GET /api/trust/overview in endpoints, trust_content in tables, trust component/lib files in key files. |
| No external schema assumed (invariant #5) | PASS | trust_content schema derived from architecture spec 2.1. Project-derived queries use existing project table fields. |
| No confidential upload path (invariant #7) | N/A | Feature 005 is read-only, no uploads, no LLM path. |
| DECISIONS.md and LEARNINGS.md are append-only (invariant #8) | PASS | Only appended, not edited. |

## 5. Data Layer Verification

| Check | Status | Notes |
|-------|--------|-------|
| V4 migration creates trust_content with correct schema | PASS | Matches architecture 2.1: id (BIGINT GENERATED ALWAYS AS IDENTITY), item_key (TEXT NOT NULL UNIQUE), item_type (TEXT NOT NULL), label (TEXT NOT NULL), value (TEXT nullable), unit (TEXT nullable), display_order (INTEGER NOT NULL DEFAULT 0), created_at (TIMESTAMPTZ), updated_at (TIMESTAMPTZ). Index idx_trust_content_type created. |
| V5 seed has 9 rows: 3 stats, 3 software, 3 standards | PASS | All 9 rows present with correct item_key, item_type, label, value, unit, and display_order values. |
| V5 ON CONFLICT idempotency clause | PASS | Every INSERT uses `ON CONFLICT (item_key) DO NOTHING`. |
| display_order set for proper ordering | PASS | Stats: 1, 2, 3. Software: 1, 2, 3. Standards: 1, 2, 3. |

## 6. API Contract Verification

| Check | Status | Notes |
|-------|--------|-------|
| GET /api/trust/overview returns correct shape | PASS | Response has stats[], clients[], contractors[], software[], standards[]. Verified by TrustControllerTest. |
| Stats include 3 stored + 1 synthesized marquee_projects | PASS | assembleStats() appends marquee_projects after stored stat rows. TrustControllerTest verifies 4 stats. |
| Marquee count derived live from project.featurable | PASS | Uses projectRepository.countByFeaturableTrue(), never stored in trust_content. |
| Clients/contractors derived from DISTINCT queries on project | PASS | findDistinctClients() and findDistinctMainContractors() with IS NOT NULL and ORDER BY. |
| Software/standards from trust_content filtered by item_type | PASS | extractLabels() filters by ItemType.SOFTWARE.wireValue() and ItemType.STANDARD.wireValue(). |
| Empty arrays returned, not null, when no data | PASS | TrustControllerTest.getOverview_returns_empty_arrays_when_no_data() verifies all 5 arrays are empty (hasSize(0)), HTTP 200. |

## 7. Frontend Component Verification

| Component | Check | Status | Notes |
|-----------|-------|--------|-------|
| trust-stats.tsx | Renders stats as gold-inverted Stat Display | PASS | bg-gold, Cormorant serif font for numbers, Montserrat labels. |
| trust-stats.tsx | Handles empty array | PASS | Returns null when stats.length === 0. |
| trust-stats.tsx | Shows unit when present | PASS | Conditional render of unit sub-line when stat.unit is truthy. |
| client-strip.tsx | Renders clients/contractors as text strip | PASS | Uses NameGroup with gold middot separators, ul/li for accessibility. |
| client-strip.tsx | Handles empty arrays | PASS | Returns null when both empty; each group renders only if non-empty. |
| client-strip.tsx | No unused Fragment import | PASS | Design spec note about removing Fragment import was followed. |
| capability-badges.tsx | Renders software/standards as badges | PASS | Uses Badge variant="outline" from existing ui/badge.tsx. |
| capability-badges.tsx | Each group renders only if non-empty | PASS | Conditional rendering for both groups. |
| trust-layer.tsx | Composes all three bands | PASS | Heading block + TrustStats + ClientStrip + CapabilityBadges. |
| trust-layer.tsx | Graceful degradation (hides when all empty) | PASS | hasAny check returns null when all arrays empty. |
| trust-layer.tsx | No double gaps when band hidden | PASS | Inter-band margins on wrapper conditionals. |
| trust-layer-section.tsx | Self-fetching wrapper | PASS | Async server component, calls getTrustOverview(). |
| trust-layer-section.tsx | Fails soft (returns null on error) | PASS | try/catch returns null on fetch failure. |
| types/trust.ts | Matches API DTO shape | PASS | TrustStat (key, label, value, unit) and TrustOverview (stats, clients, contractors, software, standards). |
| api/trust.ts | ISR revalidate | PASS | next: { revalidate: 60 } on fetch call. |

## 8. Concierge Mandatory Tests

The mandatory concierge tests (grounding, tool-contract, event) are **not applicable** to feature 005. The Trust Layer is a pure read-only, content-driven feature with no LLM path, no RFQ capture, no Redis events, and no user interaction beyond viewing. These mandatory tests apply to feature 001 (AI RFQ Concierge) and will be verified when that feature is implemented.

## 9. Documented Deviations

| Deviation | Documented In | Impact | Verdict |
|-----------|---------------|--------|---------|
| TrustStats border approach simplified from Tailwind className mix to inline styles | implementation.md section 4 | Visually identical, avoids Tailwind specificity conflict | Acceptable -- documented, no AC impact |
| No TrustExceptionHandler created | implementation.md section 4 | Not needed -- endpoint always returns 200 with empty arrays, no user input, no 404 path | Acceptable -- documented, correct design |

## 10. Regression Notes

- All 18 existing portfolio tests (F003) pass unchanged.
- Three new query methods added to ProjectRepository (findDistinctClients, findDistinctMainContractors, countByFeaturableTrue) do not interfere with existing queries.
- No Flyway migration version conflict (V4/V5 follow V1/V2/V3 sequence).
- No changes to existing components, types, or API clients.

## 11. Gaps and Observations

- **Visual testing gap**: Components are not mounted on any page yet (F011 owns page placement). Full visual verification will occur when F011 (Home) is implemented. This is by design and documented in both the architecture and implementation specs.
- **Integration testing gap**: No integration test with a real database (H2 or Testcontainers) exists for the trust queries. The existing test suite uses mocks. This is consistent with the portfolio feature's test approach and acceptable under risk-based coverage (DEC-006) -- the trust path is read-only with no user input.
- **Marquee count of zero**: If all projects have featurable=false, the marquee stat shows "0". This is correct behavior (live derivation) but could look odd on the marketing site. This is a content concern, not a bug.

## 12. Verdict

**PASS.** All three acceptance criteria (F5-AC1, F5-AC2, F5-AC3) are verified. Full test suite (29/29) passes with zero failures and zero regressions. All project invariants are satisfied. Knowledge layer is updated. No bugs found. Implementation matches the approved architecture and design specs, with two minor deviations that are documented and do not affect acceptance criteria.
