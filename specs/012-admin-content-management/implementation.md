# Implementation: 012 — Admin Content Management

| Field | Value |
|---|---|
| Feature | 012 |
| Status | bug-fix pass complete — 157 backend tests green (0 failures/errors), `tsc --noEmit` clean, `docker compose build web` succeeds; both QA-blocking bugs resolved |
| Acceptance IDs covered | F12-AC1 through F12-AC30 |
| Built from | architecture.md v0.2, design.md v0.2, handoffs/4-architect-to-dev.md |
| Depends on | 003 (project), 005 (trust_content), 011 (team, office pattern; lead ProblemDetail/rate-limit pattern) |

---

## Summary

Feature 012 adds the first authenticated, mutating surface to the system: a Spring Security-backed `/api/admin/**` API and a responsive Next.js `/admin/**` console, covering projects/team/trust/samples CRUD, image/file uploads, self-service password change, and a `kb.changed` producer on project writes. Every acceptance criterion in requirements.md v1.1 is implemented.

---

## Backend — what was built

### Flyway migrations

| Migration | What it does |
|---|---|
| `V12__create_sample.sql` | Creates `sample` (id, slug, title, category, preview, file, display_order, created_at, updated_at). DEC-023. |
| `V13` | **Not created.** Architecture flagged it as reserved/optional ("Dev's call... may be a no-op file or omitted"). No 004 content exists yet to seed meaningfully, so it is skipped entirely rather than shipping an empty placeholder migration. `sample` ships with zero rows; the admin can create them. Flagged in CLAUDE.md TODOs. |
| `V14__create_admin_credential.sql` | Creates `admin_credential` (id SMALLINT PK DEFAULT 1, username, password_hash, updated_at, `CHECK(id=1)` singleton). DEC-021 amended. DDL only — no seed row (seeding is app-level, §2.1.1). |

### `com.alef.api.admin.security` — auth (F12-AC1, AC2, AC3, AC26, AC28, AC29)

- **`AdminSecurityConfig`** — the `SecurityFilterChain`: CORS (`WEB_ORIGIN`, credentials allowed), CSRF (`CookieCsrfTokenRepository` + `CsrfTokenRequestAttributeHandler` — see gotchas below), authorization (`POST /api/admin/session` and `GET /api/admin/csrf` permitAll; everything else under `/api/admin/**` requires `ROLE_ADMIN`; everything outside it stays public, unchanged from pre-012), JSON 401/403 handlers (`ProblemDetail` shape matching the rest of the API).
- **`AdminUserDetailsService`** — reads the persisted `admin_credential` row (not env) on every login attempt, so an in-app password change takes effect immediately.
- **`AdminCredentialSeeder`** (`ApplicationRunner`) — inserts row `id=1` from `ADMIN_USERNAME`/`ADMIN_PASSWORD_HASH` **only if the table is empty**; never overwrites. This is the single most important guard in the feature (protects F12-AC28) and has a dedicated test proving it does not clobber a changed hash across a simulated restart.
- **`SessionController`/`AdminSessionService`** — `POST`/`GET`/`DELETE /api/admin/session`. Login is implemented manually (no `formLogin`): authenticates via `AuthenticationManager`, then explicitly saves the resulting `SecurityContext` into the session via `SecurityContextRepository` (the step `formLogin` would normally do). Redis-backed per-IP rate limiting (10/15min), mirroring `LeadService`'s pattern exactly.
- **`CsrfTokenController`** (`GET /api/admin/csrf`) — **not in design.md**, added to solve a real gap found while testing (see "Deviations from design" below). Public priming endpoint the web client calls once before its first mutation.
- **`PasswordChangeController`/`PasswordChangeService`** (`POST /api/admin/password`) — validates current password via `BCryptPasswordEncoder.matches` (400 on mismatch, session untouched), enforces policy (non-blank, ≥12 chars, must differ from current), re-hashes, updates the singleton row. Never logs plaintext.
- Dedicated `AdminAuthExceptionHandler` / `PasswordChangeExceptionHandler` map these to the documented `ProblemDetail` shapes.

### Admin CRUD (F12-AC4..AC19, AC23)

Four resource packages, each reusing the existing entity/repository from its owning feature (per the architect's explicit instruction — no duplication):

- **`com.alef.api.admin.project`** — `AdminProjectController`/`Service`/`Mapper`, `AdminProjectDto`/`Request`. Reuses `ProjectEntity`/`ProjectRepository` (003), adding `existsBySlug`/`existsBySlugAndIdNot`. Validates `sector`/`status`/`scope` against the existing `Sector`/`ProjectStatus` vocabularies plus a **new** `ScopeItem` vocabulary (design §A.2 names a scope vocabulary that had no enum yet — added in `com.alef.api.portfolio.vocabulary`, matching the existing Sector/ProjectStatus pattern). Publishes `KbChangedEvent` on create/update/delete.
- **`com.alef.api.admin.team`** — reuses `TeamEntity`/`TeamRepository` (011), adding `findAllByOrderByDisplayOrderAsc()`. Hard delete supported; UI defaults to the `active` soft-hide toggle.
- **`com.alef.api.admin.trust`** — reuses `TrustContentEntity`/`TrustContentRepository` (005), adding uniqueness-check methods. `GET /api/admin/trust` composes the grouped rows plus the **read-only derived facts** panel (marquee count, clients, contractors) computed live from `ProjectRepository` (DEC-025) — never writable, no field for them exists on `AdminTrustRequest` at all.
- **`com.alef.api.admin.sample`** — new `com.alef.api.sample` package (entity/repository/`SampleCategory` vocabulary, matching the team/trust/portfolio package-per-feature convention) plus the admin CRUD layer.
- **`com.alef.api.admin.error`** — one shared `AdminExceptionHandler` (404/409/400) scoped to all four CRUD controllers plus `UploadController`, instead of four near-identical handlers.

### Uploads (F12-AC20..AC22)

- **`com.alef.api.admin.upload`** — `UploadController` (`POST /api/admin/uploads`, multipart), `UploadService` (validates category → `MediaKind` → allowed content-types/extensions/max-size, then sniffs the leading bytes against the real magic number — JPEG/PNG/WEBP/PDF — before writing a UUID-named file to `${ALEF_UPLOAD_DIR}/<category-subfolder>/`), `UploadResourceConfig` (`WebMvcConfigurer` serving `/uploads/**` from the same directory, public/unauthenticated, read-only).
- `UploadCategory` enum: `project-image`, `team-photo`, `sample-preview` → `MediaKind.IMAGE` (jpeg/png/webp, 5 MB); `sample-file` → `MediaKind.DOCUMENT` (PDF, 25 MB).

### `kb.changed` (F12-AC25)

- **`com.alef.api.admin.kb`** — `KbChangedEvent` (record with the documented fields), `KbChangedPublisher` (`@TransactionalEventListener(phase = AFTER_COMMIT)` → `RedisTemplate.opsForStream().add(...)`). `AdminProjectService` publishes the event synchronously inside its write methods; the actual Redis `XADD` only fires after the transaction commits, and a publish failure is caught and logged, never propagated (constraint 4).

### `application.yml` / `application-test.yml`

- `spring.session.store-type: redis`; `server.servlet.session.cookie.name: ALEFADMIN` (+ `http-only`, `same-site: lax`).
- `spring.servlet.multipart.max-file-size: 25MB` / `max-request-size: 26MB` (the coarse first-line gate; `UploadService` re-checks the tighter per-category cap).
- `alef.admin.username`/`alef.admin.password-hash`/`alef.web-origin`/`alef.upload-dir` bound from `ADMIN_USERNAME`/`ADMIN_PASSWORD_HASH`/`WEB_ORIGIN`/`ALEF_UPLOAD_DIR`.
- Test profile: `spring.session.store-type: none` (no real Redis needed; `@WebMvcTest` slices never trigger `SessionAutoConfiguration` anyway, but this guards a stray full-context test) plus placeholder `alef.admin.*` test values.

### `pom.xml`

Added `spring-boot-starter-security`, `spring-session-data-redis` (runtime), and `spring-security-test` (test scope, DEC-027 — not explicitly named in design.md but required to exercise the real filter chain in `MockMvc`).

---

## Deviations from design.md (documented, with rationale)

1. **`GET /api/admin/csrf` priming endpoint — not in design.md.** While building `AdminSecurityWebTest`, empirical testing showed Spring Security 6's `CsrfFilter` only issues the `XSRF-TOKEN` cookie on requests that actually require CSRF protection (an unsafe method on a non-ignored path) — a plain `GET` never triggers it, and `/api/admin/session` is itself CSRF-exempt, so there was no request that naturally handed the web client a token before its first mutation. Added `CsrfTokenController` (`GET /api/admin/csrf`, public), which forces token resolution via Spring Security's own documented SPA-priming pattern (inject `CsrfToken`, call `.getToken()`). Recorded as **DEC-028** and **LEARNING-002**.
2. **CSRF exemption scoped to `/api/admin/session` only, not stated explicitly in design.md.** Design's error table for login/logout lists only 401/429, never 403 — read as intentional. `POST /api/admin/password` and every other mutation stay CSRF-protected. Recorded as part of **DEC-028**.
3. **`ScopeItem` vocabulary added.** Design §A.2 names a fixed scope vocabulary (`rebar, BBS, GA, MEP, QS, as-built`) for validation, but no `ScopeItem` enum existed anywhere in the codebase (003 never validated `scope` on write, since it had no write path). Added `com.alef.api.portfolio.vocabulary.ScopeItem`, matching the `Sector`/`ProjectStatus` pattern exactly.
4. **V13 sample seed omitted entirely**, per architecture's explicit "may be omitted" allowance — no placeholder file, no metadata-only rows. `sample` ships empty; the admin populates it.
5. **`spring-security-test` added as a test dependency** (DEC-027) — needed for `AdminSecurityWebTest`, not called out in architecture/design but a standard, minimal addition.
6. **CORS `allowedOrigins`/`allowedMethods`/`allowedHeaders`** are the minimal explicit set needed (`WEB_ORIGIN`; GET/POST/PUT/DELETE/OPTIONS; `Content-Type`/`X-XSRF-TOKEN`) — not spelled out to this level of detail in design.md, filled in as an implementation detail consistent with architecture §2.1's CORS requirement.

None of these weaken any architecture constraint; all are additive and documented per invariant #5 (no external schema assumed; every choice traced to a rationale).

---

## Frontend — what was built

### Route tree (`web/src/app/admin/**`)

- `login/page.tsx` — public login screen (design §B.2).
- `(protected)/layout.tsx` + `(protected)/page.tsx` (dashboard), `account/page.tsx` (design §B.9), `projects/`, `team/`, `trust/`, `samples/` (each with `page.tsx` list, `new/page.tsx`, `[id]/page.tsx`). The `(protected)` route group keeps the `AdminShell` chrome off the login screen without needing per-page conditionals.
- `web/src/middleware.ts` — cookie-presence guard on `/admin/:path*` except `/admin/login` (UX-only; `AdminShell`'s client-side `GET /api/admin/session` call is the authoritative check, redirecting on any 401).

### Shared components (`web/src/components/admin/`)

- **`AdminShell`** — responsive sidebar (persistent at `lg`+, hamburger drawer below), top bar (username, Account link, Logout), and the client-side session check.
- **`ResponsiveList`** — one generic component implementing the table→stacked-card responsive pattern (design §B.10) shared by all four resource list screens; same columns config renders both ways, no duplicate API calls.
- **`DeleteConfirmDialog`** — the single confirmation modal used by every delete flow (F12-AC24); responsive (centered modal → stacked full-width buttons).
- **`UploadField`** — the shared upload widget (design §B.5): native `<input type="file">` (works identically for a desktop file dialog or a phone's OS picker/camera roll), preview, Remove, inline error.
- **`AdminPageHeader`/`FormErrorBanner`** — shared header-row and top-of-form error banner.
- **`ui/admin-input.tsx`, `ui/admin-select.tsx`, `ui/admin-field.tsx`, `ui/admin-checkbox.tsx`** — token-only native form primitives. **Deliberately not reusing** `web/src/components/ui/select.tsx` / `ui/separator.tsx`: both bake in raw `rgba(...)` colors via inline `style` attributes (pre-existing, correct for the public marketing site, but would fail F12-AC30's "zero inline style colors" rule if rendered inside an admin screen). `ui/button.tsx` and `ui/badge.tsx` were checked and **are** safe to reuse conceptually (their only inline styles are `letterSpacing`, not colors) but the admin screens use plain `<button>` elements styled with the same token classes for consistency with the rest of the admin-native primitives, rather than mixing two button implementations.
- Per-resource form components: `projects/project-form.tsx`, `team/team-form.tsx`, `trust/trust-form.tsx`, `samples/sample-form.tsx`.

### Typed API layer

- `web/src/lib/types/admin.ts` — mirrors every `/api/admin/**` DTO, plus the fixed vocabularies (`SCOPE_VOCABULARY`, `PROJECT_STATUS_VOCABULARY`, `TRUST_ITEM_TYPES`, `SAMPLE_CATEGORY_VOCABULARY`) so forms never hardcode enum values inline.
- `web/src/lib/api/admin-client.ts` — shared `adminFetch()` wrapper: always `credentials: 'include'`; injects `X-XSRF-TOKEN` (read from the `XSRF-TOKEN` cookie) on mutating methods; parses `ProblemDetail` on error into a typed `AdminApiError`; `primeCsrfToken()` calls the new priming endpoint.
- `admin-session.ts`, `admin-account.ts`, `admin-projects.ts`, `admin-team.ts`, `admin-trust.ts`, `admin-samples.ts`, `admin-uploads.ts` — one typed client module per resource, mirroring the existing `web/src/lib/api/projects.ts` pattern.
- New env var `NEXT_PUBLIC_API_BASE_URL` (optional, defaults to `http://localhost:8080`) — admin calls run in the browser (need `credentials: 'include'` against a real origin), unlike the existing server-side fetches that use the Docker-internal `API_BASE_URL`. The default already matches `docker compose up`'s host-mapped `api` port, so no compose/Dockerfile change was needed.

### Responsive behavior (F12-AC27)

Implemented per design §B.10 exactly: `ResponsiveList` collapses table→card at `md` (768px); `AdminShell`'s sidebar collapses to a drawer below `lg` (1024px); all forms use `grid-cols-1 md:grid-cols-2`; `UploadField`'s file input is a native `<input>`, functionally identical on mobile (opens the OS picker/camera roll) with no desktop-only assumption; `DeleteConfirmDialog` stacks its buttons full-width below `sm`.

### Token-only colors (F12-AC30)

Verified by direct grep across `web/src/app/admin/**` and `web/src/components/admin/**`: zero raw hex literals, zero arbitrary-value color classes (`bg-[#...]`, `text-[rgb(...)]`), zero inline `style` color attributes. The only non-token-class styling anywhere in the admin tree is `letterSpacing`/`tracking-*` (typography, not color) via Tailwind's `tracking-[...]` utility, which is not a color and not restricted by F12-AC30.

---

## Tests

### Backend: 154 tests, 0 failures, 0 errors (`./mvnw test`, full suite including pre-existing 011/005/003/011 tests)

Risk-weighted per the handoff, heaviest on auth:

| Area | File(s) | Coverage |
|---|---|---|
| First-boot seeding | `AdminCredentialSeederTest` | Seeds when empty; **does not overwrite** a changed hash across a simulated restart (the specific F12-AC28 guard); no-op when env is missing. |
| UserDetailsService | `AdminUserDetailsServiceTest` | Maps persisted row → `ROLE_ADMIN`; unknown username / no row both throw `UsernameNotFoundException`. |
| Password change | `PasswordChangeServiceTest` | Accept; wrong current password (no repository write); policy violations (length, must-differ); current-password check runs before policy check. |
| Session/rate-limit (unit) | `AdminSessionServiceTest` | Login delegates to `AuthenticationManager` + saves context; 429 before auth attempt on rate-limit breach; Redis expiry set only on first attempt; logout invalidates session. |
| **Full security filter chain** | `AdminSecurityWebTest` | Login success/failure/rate-limit (real `AuthenticationManager` + BCrypt); unauthenticated GET/DELETE session → 401; unauthenticated mutating request → 403 (CSRF-first) *and* → 401 once CSRF alone is satisfied (proves auth is the real boundary); authenticated-without-CSRF → 403; authenticated-with-valid-CSRF → 201; **public endpoint stays open** under the real chain. 10 tests, all against the actual `AdminSecurityConfig` filter chain, not mocked. |
| Admin CRUD services | `AdminProjectServiceTest`, `AdminTeamServiceTest`, `AdminTrustServiceTest`, `AdminSampleServiceTest` | Slug/item-key uniqueness → 409; vocabulary validation → 400-style exception; 404 on unknown id; kb.changed emission (project only) with correct `changeType`; DEC-025 derived-facts composition. |
| Admin CRUD controllers | `AdminProjectControllerTest`, `AdminTeamControllerTest`, `AdminTrustControllerTest`, `AdminSampleControllerTest` | HTTP contract: 201/200/204/404/409/400 with `fields` map, filters disabled (`addFilters=false`) since these isolate the business/validation contract, not auth (covered above). |
| Uploads | `UploadServiceTest` (real temp-dir writes), `UploadControllerTest` | Valid PNG/JPEG/PDF accepted; wrong content-type, wrong extension, magic-byte mismatch, oversize (per-category cap) all rejected with the right exception type → right HTTP status. |
| `kb.changed` | `KbChangedPublisherTest` | Correct field set/values in the Redis Stream record for created/updated/deleted; a Redis failure is swallowed, never propagated. |

**Regression fix required:** adding `spring-boot-starter-security` made every *pre-existing* `@WebMvcTest` (ProjectControllerTest, TrustControllerTest, TeamControllerTest, OfficeControllerTest, LeadControllerTest) start failing with 401, because Spring Boot's test slice auto-configuration now applies a default "secure everything" chain. Added `@AutoConfigureMockMvc(addFilters = false)` to all five (documented as **PITFALL-008**) — these classes test HTTP/business contracts, not auth, and the public-endpoints-stay-open property is independently proven against the *real* chain in `AdminSecurityWebTest`.

### Frontend: `npx tsc --noEmit` — exit 0, zero errors

No test runner is configured for `web/` (consistent with 011's precedent — the gate for this project's frontend is the type-checker, not a Jest/RTL suite). Manually verified:
- Full route tree resolves under Next.js App Router route groups (`(protected)`).
- Token-only color audit (grep, see above) — clean.
- `next build` could **not** be run locally: this machine has Node 18.12.0, and Next.js 15 requires `^18.18.0 || >=20.0.0`. The Dockerfile's build stage uses `node:20-alpine`, which satisfies the requirement — this is a local-tooling-only gap, not a code defect (same category as PITFALL-005's JDK version note). Flagged in CLAUDE.md TODOs for whoever next runs `docker compose up --build`.

---

## Decisions and learnings recorded

- **DECISIONS.md:** DEC-027 (spring-security-test dependency), DEC-028 (CSRF exemption scope + `/api/admin/csrf` priming endpoint). Bug-fix pass adds DEC-029 (`.securityMatcher` scoping), DEC-030 (logout CSRF narrowing), DEC-031 (`danger` token). Bug-fix pass 2 adds DEC-032 (`worker` boot failure deferred, not blocking).
- **LEARNINGS.md:** LEARNING-002 (Spring Security 6 CSRF cookie issuance gotcha), PITFALL-008 (adding Security auto-secures pre-existing `@WebMvcTest` slices). Bug-fix pass adds LEARNING-003 (an un-scoped `SecurityFilterChain` governs the whole app, not just the intended paths). Bug-fix pass 2 adds PITFALL-009 (a web-only `@Configuration` crashes a non-web profile sharing the same jar). Bug-fix pass 3 adds PITFALL-010 (a server-only API client hangs forever when called from a client component, no CORS policy to answer it). Bug-fix pass 4 adds PITFALL-011 (a root-relative URL from one container 404s when rendered by a different container's origin).

(DEC-021 through DEC-026, proposed by the Architect for architecture v0.2, were already appended to DECISIONS.md before this implementation phase started — not re-added here.)

---

## Bug-fix pass (post-QA, 2026-07-15)

QA's first pass (handoffs/5-qa-to-dev.md) drove the actual built stack end-to-end and found 2 blocking bugs invisible to `mvn test`/`tsc --noEmit`, both independently reproduced by the orchestrator before this pass began. Both are now fixed and re-verified against a live `docker compose` stack, not just the test suite.

### Bug #1 (critical, fixed): CSRF filter applied app-wide instead of scoping to `/api/admin/**`

`AdminSecurityConfig`'s `SecurityFilterChain` had no `.securityMatcher(...)`. Being the only registered chain, Spring Security routed *every* request in the app through it, CSRF filter included — `authorizeHttpRequests`'s `.anyRequest().permitAll()` only governs the authorization decision, not CSRF (a separate filter earlier in the chain). Live effect: `POST /api/leads` (feature 011, public, no CSRF token ever sent by `web/src/lib/api/leads.ts`) returned a 403 for every real submission.

**Fix:** added `.securityMatcher("/api/admin/**")` as the first call in `adminSecurityFilterChain()`. Requests outside that prefix now never enter this chain at all (not CSRF, not auth, not this chain's CORS) — restoring exact pre-012 behavior. Recorded as **DEC-029** (a correction to DEC-021/DEC-028's implementation, not a new tradeoff) and **LEARNING-003**.

**Regression coverage:** `AdminSecurityWebTest` now also loads `LeadController`/`LeadExceptionHandler` and has a new test, `public_lead_submission_endpoint_stays_open_without_authentication_or_csrf`, that POSTs to `/api/leads` through the *real* filter chain with no session and no CSRF token, exactly reproducing QA's repro, and asserts 201. This is the test that would have caught the bug originally — its prior absence (not just the missing `.securityMatcher`) was the root cause per the QA handoff.

**Live re-verification** (not just the mocked test): built and ran `postgres` + `redis` + `api` via `docker compose up -d --build`, then:
```
curl -i -X POST http://localhost:8080/api/leads -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@example.com","message":"hi there this is a test message"}'
→ HTTP/1.1 201 {"id":1,"status":"received"}
```
Also confirmed the admin boundary is unaffected: `GET /api/admin/session` unauth → 401; `POST /api/admin/projects` unauth/no-CSRF → 403; `GET /api/projects` → 200.

### Bug #2 (critical, fixed): `next build` failed — `useSearchParams()` outside a `<Suspense>` boundary

`web/src/app/admin/login/page.tsx` called `useSearchParams()` directly in the page component with no `<Suspense>` wrapper, which Next.js 15's static-export prerendering step rejects outright, failing `docker compose build web` (and therefore `docker compose up --build`) for the whole `web` service, not just `/admin/login`.

**Fix:** split the page into a thin `AdminLoginPage` default export that renders `<Suspense fallback={null}><LoginForm /></Suspense>`, moving all the previous logic (including the `useSearchParams()` call) into the new `LoginForm` component — the same pattern already established correctly in `web/src/app/projects/page.tsx` for the same underlying Next.js requirement.

**Re-verification:** `docker compose build web` now succeeds end-to-end; the build's own route table shows `/admin/login` as `○ (Static)` (prerendered), confirming the fix. `tsc --noEmit` stayed clean throughout (as it did before the fix — this bug class is invisible to the type-checker, which is exactly why it shipped broken the first time; only a real `next build` catches it).

### Non-blocking concerns addressed

- **Concern #3 (logout CSRF exemption)** — deliberate call made: narrowed the CSRF exemption from the whole `/api/admin/session` path to `POST /api/admin/session` only. `DELETE /api/admin/session` (logout) now requires CSRF like every other authenticated mutation. Costs nothing in practice — the web client already primes the CSRF cookie before login, so it's already present by the time an authenticated admin logs out. Recorded as **DEC-030**. `AdminSecurityWebTest`'s logout tests were updated/added accordingly (403 without CSRF, 401 with CSRF but no auth, 204 on the full authenticated+CSRF happy path).
- **Concern #4 (DeleteConfirmDialog visual treatment)** — fixed. Added a minimal `danger` token (`DEFAULT` + `subtle`) to `web/tailwind.config.ts`, matching the existing token-group pattern (`gold`), and switched the confirm button in `web/src/components/admin/delete-confirm-dialog.tsx` from gold to danger, satisfying design.md §B.6's "outline/red-tinted, not primary gold" spec via a named token (F12-AC30 compliant — no raw hex). Recorded as **DEC-031**.

### Full re-verification results

- `./mvnw test`: **157/157 passing** (0 failures, 0 errors) — up from 154; +3 new tests (1 Bug #1 regression test, 2 logout CSRF tests replacing/extending the prior single logout test).
- `npx tsc --noEmit` (web): clean, exit 0.
- `docker compose build web`: **succeeds** (previously failed — this is the gate that was missing last pass).
- `docker compose build api`: succeeds (sanity check, not previously broken).
- Live stack (`docker compose up -d postgres redis api`): both QA repro commands re-run directly against the running container and confirmed fixed (see Bug #1 above).

## Bug-fix pass 2 (post-QA re-pass, 2026-07-17)

QA's second re-verification pass (handoffs/7-qa-to-dev.md) confirmed both original bugs and both non-blocking concerns from the first bug-fix pass fixed, live, independently. It surfaced two new findings via a full `docker compose` stack + real browser session that neither prior pass could exercise:

### New Bug #3 (`worker` container fails to boot) — deferred, not fixed, per owner call

`AdminSecurityConfig` has no profile/web-application guard, so it loads inside the `worker` profile's non-web (`web-application-type: none`) context too; its `MvcRequestMatcher`-backed rules need a `HandlerMappingIntrospector` bean that only exists under real Spring MVC, so the worker's context fails to start. Root cause fully traced by QA (handoffs/7-qa-to-dev.md, testing.md §3) — not fixed in this pass. The owner made the deliberate call that this does not block 012's close: the `worker` service isn't run in this environment and has zero live consumers today (`lead.created`/`kb.changed` — feature 001 unbuilt, DEC-018/DEC-024), so a worker that fails to boot and one that boots-and-does-nothing are currently indistinguishable. Recorded as **DEC-032**; the gotcha itself (a web-only `@Configuration` crashing a non-web profile sharing the same jar) recorded as **PITFALL-009**. Must be fixed before feature 001 needs the `worker` process to actually run.

### New Concern #6 (marketing nav/footer bleeding onto `/admin/**`) — fixed

`web/src/app/layout.tsx` (root layout) unconditionally wrapped `{children}` in the public `<SiteNav />`/`<SiteFooter />`, so every admin screen — login included — rendered the full public marketing header on top of the admin shell, contradicting design.md §B.1's explicit "no marketing nav/footer" requirement. At 390px this was worse than cosmetic: the marketing nav has no responsive treatment and visibly overlapped the (correctly responsive) admin content beneath it.

**Fix:** moved the public site (`/`, `/about`, `/contact`, `/projects`, `/projects/[slug]`, `/services`) into a new `web/src/app/(marketing)/` route group via `git mv` (route groups don't affect URLs, confirmed by an unchanged `next build` route table), and moved `SiteNav`/`SiteFooter` out of the root layout into `web/src/app/(marketing)/layout.tsx`. The root layout (`web/src/app/layout.tsx`) now only provides `<html>`/`<body>`/fonts/metadata — `/admin/**`, which sits outside the `(marketing)` group, no longer inherits any marketing chrome by construction (not by a per-route opt-out that could later be missed).

**Re-verification:** `npx tsc --noEmit` clean; `docker compose build web` succeeds with an unchanged route table (all URLs identical pre/post-move); live `docker compose up -d postgres redis api web` + curl confirmed `/about` still renders `SiteNav`/`SiteFooter` ("Enquire Now", the footer copyright line) while `/admin/login` renders neither; `/`, `/projects` return 200; `/admin` still 307-redirects to `/admin/login` when unauthenticated (middleware unaffected by the route-group move, since it matches on URL path, not file location).

## Bug-fix pass 3 (post-QA-approval, human click-through, 2026-07-17)

testing.md had already moved to `approved` (v1.2) when a manual, human-driven click-through of the running admin console (not a subagent QA pass) found one real, previously-uncaught bug and one apparent bug that turned out to be expected behavior. This is a reminder that prior QA passes verified most CRUD paths via direct API calls rather than by loading every screen in a browser — exactly the gap a human clicking through the actual UI catches.

### Bug — `/admin/projects/new` and `/admin/projects/[id]` hang on "Loading…" forever — fixed

**Root cause:** both pages are client components that called `getProjectFilters()` (`web/src/lib/api/projects.ts`) directly in a `useEffect`. That helper was written only for server-side fetches; called from the browser, the cross-origin request to `http://localhost:8080/api/projects/filters` has no CORS headers to answer it (CORS is only configured on `AdminSecurityConfig`'s chain, scoped to `/api/admin/**` — every public read endpoint was designed to be called server-side only, never from a browser). The browser silently blocks the response, the `fetch()` promise rejects with no `.catch()` at the call site, and `filters` state never gets set — the page shows "Loading…" indefinitely with no visible error except a CORS message in devtools. Confirmed via a headless-Chrome console capture: `Access to fetch at 'http://localhost:8080/api/projects/filters' from origin 'http://localhost:3000' has been blocked by CORS policy`.

**Fix:** converted both pages to Server Components (`force-dynamic`, for the same build-time-API-unavailable reason `(marketing)/projects/page.tsx` already documents) that fetch the filter vocabulary server-side and pass it down as a prop — mirroring the exact pattern the public `/projects` page already uses. `/admin/projects/[id]/page.tsx` now delegates the project-record fetch (which needs the browser's admin session cookie, so it must stay client-side) to a new `edit-project-client.tsx`. Recorded as **PITFALL-010** — checked every other admin screen (`trust`, `samples`, `team`) for the same pattern (a client component importing a non-`admin-*` API client); none found, this was isolated to projects.

**Re-verification:** `npx tsc --noEmit` clean; `docker compose build web` succeeds; live headless-browser session against a rebuilt `web` container confirmed both `/admin/projects/new` and `/admin/projects/1` now render the full form instead of hanging.

### Apparent bug — team "hide" (active=false) doesn't take effect on the public site after a refresh — investigated, not a defect

Traced end-to-end: drove the real admin UI (unchecked "Active", saved), confirmed the PUT sends `active:false` and returns 200, confirmed the backend persists it and `GET /api/team` excludes the member **immediately** at the API level (raw curl, no caching involved). The actual cause is that the public About page's `getTeam()` fetch is cached for up to 60 seconds (`next: { revalidate: 60 }`, a pre-existing, deliberate ISR setting — same as projects/trust) with no on-demand cache invalidation anywhere in the admin write paths. Confirmed live: a toggle takes effect on the public page well within that 60s window, never longer. The reporter had refreshed immediately after saving, inside the cache window. Not a persistence or logic bug — but a genuine admin-UX gap (an editor toggling "hide" reasonably expects it to be immediate, not up to a minute later) worth flagging, not silently dismissing. No fix applied this pass — see Known gaps.

## Bug-fix pass 4 (post-QA-approval, human click-through, 2026-07-19)

Another manual click-through, this time uploading an actual team photo through the admin console, found a real bug — every uploaded image (project, team, sample) rendered broken everywhere, admin and public alike.

### Bug — uploaded images never render — fixed

**Root cause:** `UploadService.store()` (api) returns a root-relative URL, e.g. `/uploads/team-photo/<uuid>.jpg`. `/uploads/**` is served by the **api** container's static resource handler (`UploadResourceConfig`), not by `web`. Every place that rendered an uploaded image used a plain `<img src={...}>` with that relative path verbatim — the browser resolved it against the **web** app's own origin (`http://localhost:3000/uploads/...`), which has no such route, so the request 404s. `curl`ing the URL directly against the API (`http://localhost:8080/uploads/...`) always worked, which is exactly why this was never caught by API-level testing (QA's prior "upload magic-byte rejection + oversize 413 + valid accept + public re-serve" checks verified the API endpoint in isolation, never an actual `<img>` tag rendering in a browser) — and why it was invisible until now: every seeded team member ships with `photo: null` (no photos were ever uploaded before), and seeded project images are static assets under `web/public/img/projects/*.svg` (correctly relative to `web`'s own origin, never routed through `/uploads/`), so no prior test, human or automated, had ever rendered a genuinely-uploaded image before.

**Fix:** added `resolveUploadUrl()` to `web/src/lib/utils.ts` — a small helper that prefixes only `/uploads/`-prefixed paths with `NEXT_PUBLIC_API_BASE_URL` (the existing browser-visible API origin env var, already used by `admin-client.ts`; defaults to `http://localhost:8080`, matching the Docker Compose port mapping) and leaves every other path (including the static seed image paths) untouched. Applied at all four `<img src={...}>` call sites that render a possibly-uploaded value: `components/admin/upload-field.tsx` (the admin preview thumbnail, shared by every resource's form), `components/team/team-card.tsx` (public About page), `components/projects/project-card.tsx` (public projects grid/marquee), `components/projects/project-detail.tsx` (public project detail hero).

**Re-verification:** `npx tsc --noEmit` clean; `docker compose build web` succeeds.

## Known gaps / follow-ons

- Public content changes (project/team/trust writes via the admin console) can take up to 60 seconds to appear on the public site — `next: { revalidate: 60 }` with no on-demand cache invalidation (`revalidatePath`/`revalidateTag`) anywhere in the admin write paths. Not a bug (confirmed working within the window), but a real admin-UX gap for anyone expecting an immediate effect, e.g. urgently hiding a team member. Fixing it means wiring `revalidatePath`/`revalidateTag` into each admin write path — out of scope for this bug-fix pass, flagged for a future pass.
- `worker` container fails to start under `docker compose up --build` (`AdminSecurityConfig` loaded in its non-web profile) — deferred per **DEC-032**, not blocking 012's close; zero functional impact today since no stream consumer exists yet. Must fix before feature 001 needs `worker` to actually run. See **PITFALL-009**.
- `sample` table ships with zero rows (V13 omitted) — no demo content until 004 or a manual seed.
- Orphaned uploads (uploaded then never attached to a saved record, or replaced) are not garbage-collected — accepted for v1 per architecture §8.
- `kb.changed` has no consumer yet (feature 001 unbuilt) — events accumulate harmlessly on the Redis Stream.
- Forgotten-password recovery remains an ops action (update the `admin_credential` row directly, or clear it and let the seeder repopulate from env on restart) — no UI, by design (v1 scope).
- `next build` is now verified — `docker compose build web` succeeds (bug-fix pass, see above). This machine's local Node (18.12.0) still cannot run `next build` directly (Next.js 15 requires ^18.18.0), so `docker compose build web`, not a bare local `next build`, is the actual verification gate; that is precisely the check the first implementation pass skipped, and skipping it is what let Bug #2 ship.
- No dedicated frontend test runner exists in this repo for `web/` — consistent with 011's precedent, not a new gap introduced by 012.
- Audit trail, multi-admin, bilingual editing, draft/publish workflow, and content versioning are all explicitly out of scope for v1 per requirements.md — not attempted here.
