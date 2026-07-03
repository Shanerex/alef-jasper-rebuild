---
feature: 011-core-marketing-pages
spec_id: "011"
phase: testing
owner: QA
status: approved
version: "1.1"
entry_criteria:
  - implementation complete, 58 backend tests green, tsc clean
exit_criteria:
  - QA has verified all F11-AC1..AC9, documented findings, human approves
---

# Testing: Core Marketing Pages (Home, About, Services, Contact)

QA date: 2026-07-01 (initial pass) / 2026-07-02 (re-QA pass — Bug 2 fix verified)
Branch: feature/011-core-marketing-pages
Backend test result: 58 tests, 0 failures, 0 errors — BUILD SUCCESS
TypeScript check: `npx tsc --noEmit` exits 0, no output

---

## 1. Verification Summary

### Case table — Acceptance criteria

| ID | Test | Expected | Result |
|----|------|----------|--------|
| F11-AC1 | `web/src/app/page.tsx` replaces placeholder; `HomeHero` renders single hero with concierge entry-point CTA; no repeated banners; `export const dynamic = "force-dynamic"` present | Single hero, "Ask the Concierge" CTA linking to `/contact#contact-form`, gold bar after hero; `CapabilityStrip`, `FeaturedProjects`, `TrustLayer`, `HomeCTA` in order | PASS |
| F11-AC2 | Home reuses 003's `ProjectGrid`/`ProjectCard` via `FeaturedProjects` wrapper; reuses 005's `TrustLayer` directly; `Services` page reads software from `getTrustOverview()` via `SoftwareBadges`; no duplicate card or stat component | `FeaturedProjects` imports `ProjectGrid` from `@/components/projects/project-grid`; `TrustLayer` imported from `@/components/trust/trust-layer`; `SoftwareBadges` reads from the 005 prop, not a static list | PASS |
| F11-AC3 | About page: company history present; 10 profiles in V7 seed; `GET /api/team` returns `{ members: [...] }` only active rows; `TeamGrid` flows for N members without hardcoded count | V7 has 10 rows matching architecture §4.2 exactly; `TeamService` filters `active=true` ordered by `display_order`; `TeamGrid` uses CSS grid, no hardcoded count; `TeamCard` degrades gracefully on null photo/email/company | PASS |
| F11-AC4 | "Intenational" → "International" corrected; "Concorse" → "Concourse" corrected; "Cadding" preserved | "International" appears twice in `company-story.tsx` rendered text (lines 51, 70); "Cadding" preserved throughout all pages and layout; "Concorse" never present in rendered static copy (content rewritten, not copied verbatim) — see Bug 3 note | PARTIAL — see note |
| F11-AC5 | Services page presents all 8 specializations from the live site reframed as outcomes; nothing dropped | `services.ts` has exactly 8 entries; all 8 discipline names match architecture §5.3 list verbatim; each entry has `key`, `discipline`, `title`, `outcome` fields | PASS |
| F11-AC6 | Contact form posts to `POST /api/leads`, creates a lead record, shows success/error state; 4-state machine | `ContactForm` client component posts to `submitLead()` → `POST /api/leads`; `idle/submitting/success/error` states implemented; success panel replaces form on 201; error distinguishes 400/429/5xx; `Retry-After: 3600` header now present on 429 — Bug 2 FIXED | PASS |
| F11-AC7 | Both offices (Dubai + India) shown with maps; direct phone/email channels | V9 seeds both offices with correct data; `GET /api/offices` returns `{ offices }` envelope with `phones` and `addressLines` as arrays; `OfficeCard` maps over arrays; `ContactMap` uses keyless iframe with `?output=embed`, no API key; `DirectChannels` provides concierge/WhatsApp/call slots | PASS |
| F11-AC8 | Shared nav/footer across all four pages; nav hrefs correct; mobile/Lighthouse structure preserved | Root layout wraps all pages; `NAV_LINKS` has `/about`, `/services`, `/projects`, `#` (Samples), `/contact`; "Enquire Now" CTA → `/contact`; all pages set `metadata` with title template; server-rendered for SEO; `ContactForm` only client component | PASS |
| F11-AC9 | Footer copyright year dynamic; no hardcoded stale year | `SiteFooter` uses `{new Date().getFullYear()}`; footer reads `ALEF Architectural & Cadding Services LLC` (correct casing, "Cadding" preserved, "ALEF" not lowercased) | PASS |

---

## 2. Bugs Found

### Bug 1 — Rate limit threshold discrepancy: code implements 10, architecture mandates 5

**Severity:** Spec violation (architecture §3.4, §10 Decision 2)
**Status: FIXED — `specs/011-core-marketing-pages/handoffs/4-dev-to-qa.md` line 38 corrected from "10 submissions" to "5 submissions" per hour.**
**File:** `api/src/main/java/com/alef/api/lead/service/LeadService.java`, line 40
**What the code does:** `private static final int RATE_LIMIT = 5;` — the constant is correctly set to 5.
**What the dev handoff said:** The handoff document previously stated "Rate limit: 10 submissions per IP per hour". This contradicted the code, which implements 5. The architecture spec (§3.4 / §10 Decision 2) mandates 5/hour.
**Verdict (re-QA):** The handoff document has been corrected to "5 submissions per IP per hour". The code was always correct. No code change required or made. Confirmed fixed.

### Bug 2 — 429 response does not set the `Retry-After: 3600` header

**Severity:** Spec violation
**Status: FIXED — verified in re-QA pass (2026-07-02).**
**Files fixed:** `api/src/main/java/com/alef/api/lead/error/LeadExceptionHandler.java` and `api/src/test/java/com/alef/api/lead/controller/LeadControllerTest.java`.
**What the architecture spec requires:** Architecture §3.4 explicitly states "returns `429 Too Many Requests` with `Retry-After: 3600`". Architecture §10 Decision 2 repeats "returns `429 Too Many Requests` with a `Retry-After` header".
**Fix applied:**
- `LeadExceptionHandler` now has a `@ExceptionHandler(ResponseStatusException.class)` method (`handleRateLimit`) that intercepts 429-status exceptions specifically, builds a `ProblemDetail` response, and sets `HttpHeaders.RETRY_AFTER` to `"3600"` via `headers.set(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)`.
- The handler is scoped to `LeadController` via `@RestControllerAdvice(assignableTypes = LeadController.class)` — unchanged from the existing scope declaration.
- The existing `handleValidationFailure` (400, `MethodArgumentNotValidException`) is unmodified and still present.
- `LeadControllerTest.submit_rate_limit_breach_returns_429_with_retry_after_header()` now asserts `.andExpect(header().string("Retry-After", "3600"))`.
**Re-QA verification:** Test `submit_rate_limit_breach_returns_429_with_retry_after_header` is green in the 58-test suite. No regression on any other test file.

### Bug 3 — F11-AC4 "Concorse" → "Concourse" correction is claimed but untestable

**Severity:** Gap / documentation inconsistency (not a runtime bug)
**Status: Non-blocking gap — no change. No user-visible text is incorrect.**
**File:** `web/src/components/about/company-story.tsx`, lines 8-9 (code comment)
**What the spec requires:** F11-AC4 requires the live-site typo "Concorse" → "Concourse" to be corrected. The `CompanyStory` doc comment claims: `"Concorse" → "Concourse"`.
**What the code does:** The rendered static text content of `CompanyStory` contains neither "Concorse" nor "Concourse". The word was never introduced because the about-page content appears to have been written fresh rather than copied verbatim from the live site where the typo appeared. The landmark projects (Dubai Airport Concourses 3 & 4) that would naturally contain the word are rendered from the 003 project database (not static text), and the relevant project records were not seeded at the time of this QA pass.
**Impact:** The typo correction is not verifiable in the shipped code. If the live-site content containing "Concorse" is later migrated into static text, the intent is documented. Until then, the claim in the doc comment cannot be confirmed.
**This is a documentation gap, not a runtime defect.** No user-visible text is incorrect. Logged as a gap rather than a blocking bug.

---

## 3. Edge-Case Audit

### Honeypot

| Scenario | Expected | Result |
|----------|----------|--------|
| `website` field non-blank | `LeadService` returns fake 201 with `id=-1`; no `repository.save()` called; no Redis increment | PASS — `LeadServiceTest.submit_returns_fake_201_when_honeypot_is_filled_without_persisting()` asserts `repository` never called and `redisTemplate` never called |
| `website` field blank | Normal flow proceeds | PASS — `submit_proceeds_when_honeypot_is_blank()` |
| `website` field null | Normal flow proceeds | PASS — `submit_proceeds_when_honeypot_is_null()` |
| Honeypot check runs BEFORE rate-limit increment | Bot submissions do not consume rate-limit slots | PASS — service code evaluates honeypot at step 1, `enforceRateLimit()` at step 2; test asserts `redisTemplate` not called on honeypot hit |
| Honeypot field in frontend hidden from humans | `position: absolute; left: -9999px`; `tabIndex=-1`; `aria-hidden="true"` | PASS — `contact-form.tsx` lines 301-311 |

### Rate Limit

| Scenario | Expected | Result |
|----------|----------|--------|
| 5th submission from same IP | 201 success (limit is 5, 5th is still within) | PASS — `submit_allows_exactly_five_submissions_before_rejecting()` |
| 6th submission from same IP | 429 Too Many Requests | PASS — `submit_throws_429_when_rate_limit_exceeded()` (count=6) |
| Redis expiry set on first submission only | `EXPIRE` called once (count==1), not on subsequent submissions | PASS — `submit_sets_redis_expiry_on_first_submission_from_ip()` and `submit_does_not_reset_expiry_on_subsequent_submissions()` |
| 429 carries `Retry-After: 3600` header | Header present | PASS — Bug 2 FIXED; `submit_rate_limit_breach_returns_429_with_retry_after_header()` asserts `header().string("Retry-After", "3600")` |

### Graceful Degradation (cold API)

| Page | Section affected | Expected | Result |
|------|-----------------|---------|--------|
| Home | FeaturedProjects + TrustLayer | Static sections render; muted loading message | PASS — `page.tsx` catch block renders `HomeHero`, `CapabilityStrip`, loading message, `HomeCTA` |
| About | LandmarkProjectsStrip + TeamGrid | Static sections render; muted loading message | PASS — `about/page.tsx` catch block renders hero, `CompanyStory`, `GccReachBlock`, loading message, `HomeCTA` |
| Services | SoftwareBadges | `SoftwareBadges` self-hides; 8 service cards always render | PASS — `services/page.tsx` catches error, sets `software=[]`; `SoftwareBadges` returns null when `software.length === 0` |
| Contact | OfficeLocations | Form always renders; offices degrade to loading text | PASS — `contact/page.tsx` catches error, sets `offices=[]`; `OfficeLocations` renders fallback text when empty |

### Reduced-Motion (LandmarkProjectsStrip)

| Scenario | Expected | Result |
|----------|---------|--------|
| `prefers-reduced-motion: reduce` | Animation stops; track wraps statically with `justify-center` | PASS — `landmark-projects-strip.tsx` uses `motion-reduce:animate-none motion-reduce:flex-wrap motion-reduce:justify-center` Tailwind variants; inline `<style>` block with `@media (prefers-reduced-motion: reduce)` also present |
| Empty projects array | Section self-hides entirely (returns null) | PASS — `if (projects.length === 0) return null` at line 27 |
| Array doubled for seamless loop | `doubled = [...projects, ...projects]` before rendering | PASS — line 30 |

### ContactMap

| Scenario | Expected | Result |
|----------|---------|--------|
| `mapQuery` is null | Returns null (no iframe rendered) | PASS — `contact-map.tsx` line 21: `if (!mapQuery) return null` |
| `mapQuery` non-null | Keyless iframe with `?output=embed`, no API key | PASS — src assembled as `https://maps.google.com/maps?q=${encodeURIComponent(mapQuery)}&output=embed` with no API key |

### TeamCard Photo Degradation

| Scenario | Expected | Result |
|----------|---------|--------|
| `photo` is null (all current profiles) | Monogram placeholder renders (Surface 3 + gold-ghost tint + serif initials at 0.4 opacity) | PASS — `team-card.tsx` null branch renders div with `#101F32` bg + radial gold-ghost + serif monogram |
| `photo` non-null | `<img>` with lazy loading and hover scale | PASS — `team-card.tsx` photo branch renders img with `loading="lazy"` and `group-hover:scale-[1.02]` |
| `company` is null | No empty paragraph | PASS — wrapped in `{company && (...)}` |
| `email` is null | No empty link | PASS — wrapped in `{email && (...)}` |
| Monogram extraction | "K. Jeyaraman" → "KJ"; "Namasivayam" → "N" | PASS — `getMonogram()` splits on `[\s.]+`, takes first letter of first and last non-empty alpha tokens |

### SoftwareBadges

| Scenario | Expected | Result |
|----------|---------|--------|
| `software.length === 0` | Returns null, section self-hides | PASS — line 20: `if (software.length === 0) return null` |
| Non-empty array | Reads from prop, not a static re-list | PASS — maps over prop `software`, no static fallback list |

---

## 4. Backend: Migration Verification

### V6 — `create_team`
- All required columns present: `id`, `name`, `role`, `company`, `email`, `photo`, `display_order`, `active`, `created_at`, `updated_at` — PASS
- `UNIQUE (name, role)` constraint present — PASS
- `active BOOLEAN NOT NULL DEFAULT TRUE` — PASS
- `display_order INTEGER NOT NULL DEFAULT 0` — PASS

### V7 — `seed_team`
- Exactly 10 profiles — PASS (10 `VALUES` rows counted)
- `ON CONFLICT (name, role) DO NOTHING` — PASS
- `photo` is NULL for all 10 — PASS (matches architecture §4.2 note: null until assets exist)
- All 10 profiles match architecture §4.2 table exactly (name, role, company, email, display_order) — PASS

### V8 — `create_office`
- `office_key TEXT NOT NULL UNIQUE` — PASS
- `address_lines TEXT[]` — PASS
- `phones TEXT[]` — PASS
- `map_query TEXT` — PASS
- `display_order INTEGER NOT NULL DEFAULT 0` — PASS
- `created_at`, `updated_at` — PASS

### V9 — `seed_office`
- Exactly 2 offices (Dubai + India) — PASS
- Dubai has 2 phones: `['+971 4 2513840', '+971 4 3434440']` — PASS
- India has 1 phone: `['0091 4636 293166']` — PASS
- Both match architecture §3.2 values exactly (addresses, emails, mapQuery strings) — PASS
- `ON CONFLICT (office_key) DO NOTHING` — PASS

### V10 — `create_lead`
- Columns present: `id`, `created_at`, `source`, `name`, `email`, `phone`, `company`, `message` — PASS
- No RFQ-specific columns (no `tonnage`, `scope`, `attachment_ref`, `conversation_ref`, `project_name`, `sector`, etc.) — PASS (DEC-017 correctly implemented)
- No `updated_at` — PASS (write-once table per §4.4)

### V11 — `update_trust_content_from_prequalification`
- UPDATEs `years_in_business` → `'23'` — PASS
- UPDATEs `staff_count` label → `'Workstations'`, value → `'250+'` — PASS
- UPDATEs `monthly_steel_capacity_tonnes` → `'70,000'` — PASS
- INSERTs `projects_completed` stat — PASS
- INSERTs `contractor_clients` stat — PASS
- INSERTs `gcc_countries` stat — PASS
- UPDATEs `software_steelpac_rc` label → `'Steel PAC RCS'` — PASS
- INSERTs `software_steelpac_rcd`, `software_cadmate`, `software_loha`, `software_multi_rc` — PASS
- All INSERTs use `ON CONFLICT (item_key) DO NOTHING` — PASS
- V5 is NOT touched — PASS (V11 is a separate forward migration)
- Comment marking the three headline stats as candidates for live derivation from `project` table — PASS

---

## 5. Backend API Contract Verification

### `GET /api/team`
- Returns `{ "members": [...] }` envelope — PASS (`TeamOverviewDto` record wraps `List<TeamMemberDto>`)
- Only `active = true` rows via `findAllByActiveTrueOrderByDisplayOrderAsc()` — PASS
- Ordered by `display_order ASC` — PASS
- Nullable fields (`company`, `email`, `photo`) — PASS
- Returns 200 with empty `members` array if table empty — PASS (controller test: `getTeam_returns_empty_members_array_when_no_profiles`)

### `GET /api/offices`
- Returns `{ "offices": [...] }` envelope — PASS (`OfficesDto` record)
- `addressLines` is an array — PASS (mapped from `TEXT[]` Postgres column)
- `phones` is an array — PASS
- Dubai has 2 phones; India has 1 — PASS (seeded in V9; controller test verifies `hasSize(2)` and `hasSize(1)`)
- Returns 200 with empty `offices` array if table empty — PASS (controller test)

### `POST /api/leads`
- `name` required / max 200 — PASS (`@NotBlank`, `@Size(max=200)`)
- `email` required / valid format / max 200 — PASS (`@NotBlank`, `@Email`, `@Size(max=200)`)
- `message` required / max 5000 — PASS (`@NotBlank`, `@Size(max=5000)`)
- `phone`, `company` optional — PASS (no `@NotBlank`)
- `website` honeypot — no validation annotation, accepts any string — PASS
- Returns 201 `{ "id": n, "status": "received" }` — PASS
- RFC 9457 `ProblemDetail` 400 with `fields` extension on validation failure — PASS (`LeadExceptionHandler` maps `MethodArgumentNotValidException`)
- Returns 429 on rate-limit breach — PASS (throws `ResponseStatusException(TOO_MANY_REQUESTS, ...)`)
- 429 with `Retry-After: 3600` header — PASS (Bug 2 FIXED — `LeadExceptionHandler.handleRateLimit()` sets `HttpHeaders.RETRY_AFTER` to `"3600"`)
- `source` set server-side to `'contact_form'` — PASS (test: `submit_sets_source_to_contact_form_server_side`)
- `created_at` set server-side — PASS (test: `submit_sets_created_at_server_side`)
- `website` honeypot not persisted when filled — PASS (fake id=-1 returned, `repository.save()` never called)

### `LeadExceptionHandler`
- Scoped to `LeadController` via `assignableTypes` — PASS (mirrors `PortfolioExceptionHandler` pattern)
- `MethodArgumentNotValidException` → 400 ProblemDetail with `fields` map — PASS
- `title: "Validation Failed"` — PASS
- `ResponseStatusException(TOO_MANY_REQUESTS)` → 429 ProblemDetail with `Retry-After: 3600` header — PASS (new in re-QA pass)
- Other `ResponseStatusException` statuses fall through to `ResponseEntity.status(ex.getStatusCode()).build()` — PASS (non-429 path unaffected)

---

## 6. Frontend Code Review

### `web/src/app/page.tsx` (Home)
- Not a stub, full implementation — PASS
- `export const dynamic = "force-dynamic"` — PASS
- `Promise.all([getProjects({featurable:true, size:50}), getTrustOverview()])` inside try/catch — PASS
- Renders: `HomeHero` → 4px gold bar → `CapabilityStrip` → `FeaturedProjects` → `TrustLayer` → `HomeCTA` — PASS
- Catch fallback: static sections + muted loading message, no hard error — PASS
- Uses 003's `ProjectGrid` via `FeaturedProjects` wrapper — PASS
- Uses 005's `TrustLayer` directly — PASS

### `web/src/app/about/page.tsx`
- `export const dynamic = "force-dynamic"` — PASS
- `Promise.all([getTeam(), getProjects({featurable:true, size:50})])` inside try/catch — PASS
- Renders: `PageHero` → 4px bar → `CompanyStory` → `GccReachBlock` → `LandmarkProjectsStrip` → `TeamGrid` → `HomeCTA` — PASS
- "Cadding" preserved in metadata description — PASS

### `web/src/app/services/page.tsx`
- `export const dynamic = "force-dynamic"` — PASS
- Fetches `getTrustOverview()` for software only inside try/catch — PASS
- `ServicesList` receives static `SERVICES` constant (8 entries) — PASS
- `SoftwareBadges` self-hides when software empty — PASS
- No static software re-list — PASS

### `web/src/app/contact/page.tsx`
- `export const dynamic = "force-dynamic"` — PASS
- `ContactForm` is client component (`"use client"` at line 1 of `contact-form.tsx`) — PASS
- Form section wrapped in `id="contact-form"` so anchor `#contact-form` works — PASS
- `OfficeLocations` receives server-fetched offices — PASS
- Catch fallback: offices degrade to empty array, form still renders — PASS

### `web/src/app/layout.tsx`
- Nav hrefs: `/about`, `/services`, `/projects`, `#` (Samples), `/contact` — PASS
- "Enquire Now" CTA → `href="/contact"` — PASS
- Footer year: `{new Date().getFullYear()}` — PASS
- "Cadding" in footer legal name — PASS
- Footer "ALEF" in correct uppercase — PASS

---

## 7. Test Suite Confirmation

Test run command: `cd api && ./mvnw test`
Result: **58 tests, 0 failures, 0 errors — BUILD SUCCESS**

| Test file | Count | Result |
|---|---|---|
| `LeadControllerTest` | 7 | All green |
| `LeadServiceTest` | 10 | All green |
| `TeamControllerTest` | 2 | All green |
| `TeamServiceTest` | 4 | All green |
| `OfficeControllerTest` | 2 | All green |
| `OfficeServiceTest` | 4 | All green |
| `ProjectControllerTest` | 5 | All green |
| `ProjectServiceTest` | 7 | All green |
| `SectorTest` | 3 | All green |
| `ProjectStatusTest` | 3 | All green |
| `TrustControllerTest` | 2 | All green |
| `TrustServiceTest` | 6 | All green |
| `ItemTypeTest` | 3 | All green |
| **Total** | **58** | **0 failures** |

TypeScript check: `npx tsc --noEmit` exits 0, no output — PASS

---

## 8. Known Gaps (Not Regressions)

These are documented intentional omissions or deferred items, not bugs:

- **`Samples` nav link routes to `#`** — expected; feature 004 not yet built.
- **Team member photos all null** — expected; `TeamCard` degrades to serif monogram placeholder. Photo assets are not yet available.
- **No ISR caching on any marketing page** — `force-dynamic` is intentional per PITFALL-007 (web container builds before api container in Docker Compose). The API clients already set `next: { revalidate: 60 }`; switching to ISR is a one-line page change.
- **No `lead.created` Redis Stream event emitted** — deferred to feature 001 per DEC-018 and architecture constraint 4. Contact form leads are persisted (durable) but not yet notified. This is the intended 011 scope boundary.
- **Lead delivery / email notification** — deferred; a human reads new `lead` rows directly until 001 lands.
- **Admin CMS for teams, offices, services** — feature 012. The `team` and `office` tables are designed for CRUD from day one (DEC-020) but 011 only ships the read endpoints.
- **WhatsApp channel link routes to `#`** — placeholder slot; feature 009 wires the real link.
- **"Concorse" → "Concourse" correction not verifiable in rendered text** — see Bug 3 note. The content was rewritten fresh; the misspelling was never introduced into the new code.

---

## 9. Re-QA Pass — 2026-07-02

**Triggered by:** Bug 2 fix delivered by Dev.

### Fix verification

**Bug 2 fix — `LeadExceptionHandler.java`:**
- `@ExceptionHandler(ResponseStatusException.class)` method `handleRateLimit` is present at lines 69-82.
- Checks `ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS` before acting.
- Creates `ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ...)`.
- Sets `HttpHeaders.RETRY_AFTER` to the constant `RETRY_AFTER_SECONDS = "3600"` via `headers.set(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)`.
- Returns `ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(headers).body(detail)`.
- Non-429 `ResponseStatusException` fall through to `ResponseEntity.status(ex.getStatusCode()).build()` — no unintended interception of other error paths.
- Class-level annotation `@RestControllerAdvice(assignableTypes = LeadController.class)` is unchanged — scoping is correct.
- Existing `handleValidationFailure` (400, `MethodArgumentNotValidException`) is unmodified.

**Bug 2 fix — `LeadControllerTest.java`:**
- Test `submit_rate_limit_breach_returns_429_with_retry_after_header` (line 139) now asserts both `status().isTooManyRequests()` and `header().string("Retry-After", "3600")`.
- All 6 other `LeadControllerTest` methods are unchanged.

**Bug 1 fix — `handoffs/4-dev-to-qa.md`:**
- Line 38 now reads "Rate limit: 5 submissions per IP per hour" — matches the code and architecture spec.

### Test suite result (re-QA run)

58 tests, 0 failures, 0 errors — BUILD SUCCESS.
No regression in any module (portfolio, trust, team, office, lead).

### No new bugs found in this pass.

---

## 10. QA Status

**Status: APPROVED**

All F11-AC1 through F11-AC9 are satisfied. The only previously-blocking issue (Bug 2 — missing `Retry-After: 3600` header on 429 responses) has been fixed, verified by code inspection and confirmed green by the test suite. Bug 1 (documentation error in handoff) is corrected. Bug 3 (non-blocking documentation gap) remains logged for awareness but does not affect any runtime correctness.

Feature 011 is ready for PM loop-close and merge to main.

Approval gate: human review required per exit criteria.
