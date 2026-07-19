# DECISIONS.md

> Append-only. Newest first. Why a choice was made, not just what.

## DEC-033: Light theme approved as a design-system extension; dark remains the brand default (2026-07-19)
- Context: Feature 014 (theme toggle) requests a dark/light toggle at the top-right with a polished transition. `DESIGN_SYSTEM.md` is explicitly dark-first ("Not a light/white theme") and defines no light palette, so the business handoff flagged the conflict as requiring human approval.
- Decision: The owner reviewed the conflict and approved a light theme for the site (2026-07-19). A light palette must be defined as a human-reviewed extension of `DESIGN_SYSTEM.md` before 014's design/implementation phases; the "Dark Prestige" direction stays the canonical brand expression and the dark palette remains the reference for all new components.
- Why: Readability preference and bright-daylight office viewing in the GCC justify a light option, but brand equity lives in the dark presentation — so light mode is an alternate rendering of the same system (navy/gold/serif language preserved), not a second brand.
- Consequences: `DESIGN_SYSTEM.md` gains a light-palette section (token-for-token mapping of backgrounds, surfaces, text, gold treatment) subject to owner sign-off. 014 is unblocked for /orchestrate once that section lands. Gold-on-light contrast must meet accessibility budgets (treated as correctness per ARCHITECTURE.md).

## DEC-032: `worker` service crash under `AdminSecurityConfig` is deferred, not blocking 012 close (2026-07-17, owner call, QA re-pass)

- Context: QA's second re-verification pass (handoffs/7-qa-to-dev.md, New Bug #3) found that a fresh `docker compose up --build` leaves the `worker` container crash-exiting: `AdminSecurityConfig` is an unconditional `@Configuration @EnableWebSecurity` class with no profile/web-application guard, so component scan still picks it up inside the `worker` profile's non-web (`web-application-type: none`) context; its `securityMatcher`/`requestMatchers` resolve to `MvcRequestMatcher`, which needs a `HandlerMappingIntrospector` bean that only exists under real Spring MVC web infrastructure — bean creation throws and the container dies. No `restart:` policy is set, so it stays dead for the session rather than crash-looping.
- Decision: Not a blocker for feature 012's close. The owner does not run the `worker` service in this environment and did not intend it to be exercised by this verification pass; a worker that fails to boot and a worker that boots-and-does-nothing are indistinguishable today, since no consumer exists yet for `lead.created`/`kb.changed` (feature 001 unbuilt, DEC-018/DEC-024).
- Why: Rigor should go where failure is expensive (ARCHITECTURE.md's production-readiness bar). A silently-dead worker with zero live consumers has zero current blast radius. Scoping `AdminSecurityConfig` away from the non-web profile is real, contained work (QA's suggested direction: `@ConditionalOnWebApplication(type = Type.SERVLET)` or `@Profile("!worker")`) but is deliberately deferred rather than gating 012's close, since 012's actual deliverable (the admin console) is otherwise verified working end-to-end.
- Consequences: Added to CLAUDE.md's known-gaps list so it stays visible rather than silently forgotten. Must be fixed before feature 001 lands a real `lead.created`/`kb.changed` consumer and the `worker` process needs to actually run. See PITFALL-009.

## DEC-031: DeleteConfirmDialog's Delete button uses a new `danger` token, not gold (2026-07-15, Dev, bug-fix pass)
- Context: QA (handoffs/5-qa-to-dev.md Concern #4) flagged that the destructive confirm button used the same gold-family tokens as every primary action in the admin, not the "outline/red-tinted, not primary gold" treatment design.md §B.6 specifies, and that `tailwind.config.ts` had no danger/destructive token to express it with. F12-AC24 still passed functionally (the confirmation gate itself works); this was a design-fidelity gap only.
- Decision: Added a minimal `danger` color group to `tailwind.config.ts` (`DEFAULT` + `subtle`, a single muted red kept in tone with the rest of the Dark Prestige palette — DESIGN_SYSTEM.md defines no red anywhere, so this is additive, not a reinterpretation of an existing token) and switched `DeleteConfirmDialog`'s confirm button from `border-gold`/`text-gold` to `border-danger`/`text-danger`.
- Why: Named-token-only is required by F12-AC30; introducing exactly one new token (not a full red scale) is the minimal change that satisfies design.md §B.6 without expanding the color system beyond what this one component needs. Left `AdminField`'s existing gold-for-errors choice (pre-existing, documented inline) untouched — that was a separate, already-deliberate call, not part of this bug-fix pass's scope.
- Consequences: `tailwind.config.ts` gains a `danger` token available to any future admin component that needs a genuinely destructive visual cue.

## DEC-030: Narrowed the CSRF exemption to POST /api/admin/session only — logout (DELETE) now requires CSRF (2026-07-15, Dev, bug-fix pass)
- Context: QA (handoffs/5-qa-to-dev.md Concern #3) flagged that DEC-028's CSRF exemption covered the whole `/api/admin/session` path, so `DELETE /api/admin/session` (logout) rode along with login's exemption for no independently-stated reason — a cross-site request could force-log-out an authenticated admin (nuisance-level: no data read or mutated, but an unreviewed gap).
- Decision: The CSRF-ignore rule now matches only `POST /api/admin/session`. Logout is CSRF-protected like every other mutation on an authenticated session (password change, project/team/trust/sample/upload writes). Login's exemption stands, unchanged, for the reason DEC-028 already gives (no session yet at login time for a forged request to ride on).
- Why: Login and logout are not the same case — login has no existing session to protect (the classic CSRF prerequisite is absent), but logout does. Tightening it costs nothing in practice: `web/src/lib/api/admin-session.ts`'s `login()` already calls `primeCsrfToken()` before the login POST, so the `XSRF-TOKEN` cookie is already present by the time an authenticated admin later calls `logout()`, and `adminFetch()` already attaches `X-XSRF-TOKEN` to every mutating call including DELETE. Rejected: leaving logout exempt (the status quo QA flagged, defensible but not deliberate); exempting `/api/admin/csrf` alongside session paths (unrelated, not requested).
- Consequences: `AdminSecurityWebTest`'s `delete_session_without_authentication_returns_401` test needed updating — an unauthenticated, CSRF-token-less DELETE now hits the CSRF filter first (403), the same pattern already proven for POST /api/admin/projects; a companion test proves the auth boundary (401) still holds once CSRF alone is satisfied, and another proves the authenticated happy path still logs out (204).

## DEC-029: AdminSecurityConfig's filter chain scoped with .securityMatcher("/api/admin/**") (2026-07-15, Dev, bug-fix pass — correction to DEC-021/DEC-028's implementation)
- Context: QA (handoffs/5-qa-to-dev.md Bug #1), independently reproduced by the orchestrator, found that `AdminSecurityConfig`'s `SecurityFilterChain` had no `.securityMatcher(...)`. Because it was the only registered `SecurityFilterChain` bean, Spring Security applied the *entire* chain — CSRF filter included — to every request in the app, not just `/api/admin/**`. `authorizeHttpRequests`' `.anyRequest().permitAll()` only governs the authorization decision; it does not stop the CSRF filter (a distinct filter earlier in the same chain) from demanding a token on any unsafe-method request anywhere, including the pre-existing public `POST /api/leads` (feature 011), which broke outright (every real contact-form submission returned a CSRF-shaped 403).
- Decision: Added `.securityMatcher("/api/admin/**")` as the first configuration call on the `HttpSecurity` builder in `adminSecurityFilterChain()`. Requests outside that prefix now never enter this chain at all — not CSRF, not auth, not even this chain's CORS config — restoring their exact pre-012 behavior. (Pre-012 confirmed as the correct baseline: `POST /api/leads` is called server-side per `web/src/lib/api/leads.ts`, never from the browser, so it never needed CORS either.)
- Why: This is the architecturally correct fix, not a workaround — DEC-021 always intended `/api/admin/**` to be the entire scope of this feature's auth surface ("Locks down /api/admin/** to ROLE_ADMIN; every other endpoint stays public"), and DEC-028's CSRF-exemption design assumed the chain was already scoped there. The bug was an implementation gap in realizing that intent, not a new tradeoff — hence recording it as a correction alongside DEC-021/DEC-028 rather than a fresh open question. Rejected: adding `/api/leads` to the CSRF ignore list (treats the symptom, leaves every other non-admin path, including any future one, silently exposed to the same class of bug); leaving `authorizeHttpRequests`'s `.anyRequest().permitAll()` as the only guard (already proven insufficient — it never governed CSRF in the first place).
- Consequences: Added a regression test (`AdminSecurityWebTest.public_lead_submission_endpoint_stays_open_without_authentication_or_csrf`) that drives `POST /api/leads` through the *real* filter chain with no CSRF token — the specific gap `LeadControllerTest`'s `addFilters=false` (PITFALL-008) and the pre-existing "public endpoints stay open" test (GET-only) both missed. Documented as **LEARNING-003**.

## DEC-028: A dedicated GET /api/admin/csrf priming endpoint; only /api/admin/session is CSRF-exempt (2026-07-13, Dev, implementation choice)
- Context: design.md documents CSRF as required on every mutating `/api/admin/**` request but doesn't specify how the web client obtains its first token, and doesn't say whether `POST /api/admin/session` (login) itself needs a CSRF token. Building AdminSecurityWebTest surfaced that Spring Security 6 never naturally hands out a CSRF cookie to a plain JSON client before its first mutation (LEARNING-002).
- Decision: `POST /api/admin/session` (and `GET`/`DELETE` on the same path) is exempted from CSRF -- design's own error table for those three lists only 401/429, never 403, and there is no session yet at login time for a forged cross-site POST to ride on. Every other admin mutation (password change, all project/team/trust/sample CRUD, uploads) stays CSRF-protected, matching design's "mutating requests require X-XSRF-TOKEN" opening line. A new `GET /api/admin/csrf` endpoint (`CsrfTokenController`) forces token issuance on demand (Spring Security's documented SPA-priming pattern: inject `CsrfToken` and call `.getToken()`); it is public (no auth required) since a token must be obtainable before login.
- Why: keeps the real CSRF protection where it matters (an attacker riding the admin's already-authenticated session cookie against a state-changing action) without blocking the login flow on a token the client has no way to already possess. Alternatives rejected: CSRF-protecting login too (no clean way for an unauthenticated client to obtain a first token without its own exemption or priming step, and design's error table doesn't anticipate a 403 there); relying on incidental cookie issuance from a failed request (fragile, not a real client contract).
- Consequences: the web admin client must call `GET /api/admin/csrf` once (e.g. on login page mount) before any mutating call, including login. Documented in `AdminSecurityConfig`'s class doc and `web/src/lib/api/admin-client.ts`'s `primeCsrfToken()`.

## DEC-027: Added spring-security-test as a test-only dependency (2026-07-13, Dev, implementation choice)
- Context: architecture.md/design.md specify Spring Security + CSRF for the admin surface but do not enumerate test dependencies. Exercising the real filter chain (login, 401/403, CSRF, logout) in `AdminSecurityWebTest` needs MockMvc security test support.
- Decision: added `org.springframework.security:spring-security-test` in `test` scope only (api/pom.xml). No production dependency changes.
- Why: this is the standard, minimal way to test a Spring Security filter chain with MockMvc; omitting it would mean either no real auth-boundary tests (unacceptable given engineering-standards #3's heaviest-risk-area rule) or hand-rolling the same support.
- Consequences: none beyond a test-scope dependency. Does not affect the runtime artifact.

## DEC-026: Admin console is a responsive, token-themed dark tool, not the marketing design system (2026-07-13)
- Context: F12-AC27 requires the admin console to work on phone/tablet as well as desktop (ALEF staff use both ~50/50), and F12-AC30 requires it theme-ready for the forthcoming spec 013 light/dark toggle. The v0.1 draft assumed a desktop-only admin reusing the full "Dark Prestige" marketing system.
- Decision: The admin uses a functional dark admin theme — public palette tokens + Montserrat, reusing existing shadcn/ui primitives, but dropping marketing hero patterns and serif display type. It is fully responsive (lists collapse table→stacked-card, forms go single-column, upload uses the mobile file picker / camera roll, matching the public app's `sm`/`md`/`lg` breakpoints) and every admin color flows through named design tokens — no hard-coded hex — so spec 013's toggle can layer on behind the same tokens with no rework.
- Why: The admin is an internal data-entry tool; the marketing system's persuasion-tuned display type, hero patterns and sharp-corner buttons hurt CRUD legibility and density. Desktop-only was reversed by stakeholder direction (staff use mobile). Token-only color is the precondition that lets 013 introduce CSS-variable indirection without touching admin components. Rejected: full marketing system (wrong density/affordances), desktop-only (reversed by F12-AC27), hard-coded hex (won't flip under 013), third-party admin template (extra dependency, brand-incoherent, not token-compatible).
- Consequences: 012 builds the tokens and a single dark theme; sibling spec 013 (non-blocking) builds the switch. No second design system to maintain. The responsive change is UI-only — no API/DTO change.

## DEC-025: Admin trust editing operates on `trust_content` directly; derived facts edited via Projects (2026-07-13)
- Context: F12-AC12/AC14 name "marquee project count" and "client/contractor names" as trust content the admin edits, but DEC-014 makes those derived live from the `project` table.
- Decision: The admin edits `trust_content` rows directly (stats/software/standards). Derived facts (marquee count, client/contractor names) are shown read-only on the Trust editor with an "edited via Projects" note; they change through Projects CRUD, never as trust fields.
- Why: Making the derived facts editable trust fields would reintroduce the exact drift DEC-014's single-source-of-truth rule prevents. Reconciles the ACs with DEC-014 without a redundant editable copy. The human accepted this interpretation at the architecture gate.
- Consequences: No schema change to `trust_content`; the Trust editor carries a read-only derived section. Rejected: a redundant editable copy in `trust_content` (drift).

## DEC-024: `kb.changed` emitted on project writes via Redis Stream, AFTER_COMMIT, minimal payload; consumer is 001's (2026-07-13)
- Context: F12-AC25 requires a `kb.changed` signal when a project is created/edited/deleted so the concierge's RAG index can refresh, but the side effect must not block the admin save (DEC-004, constraint 4). Feature 001 (the consumer) is unbuilt.
- Decision: On project create/update/delete, emit `kb.changed` to a Redis Stream AFTER_COMMIT, fire-and-forget, with a minimal reference payload (`event, version, entityType, entityId, slug, changeType, occurredAt`). v1 emits for projects only; the consumer is 001's (emit-now / consume-later).
- Why: AFTER_COMMIT + fire-and-forget keeps the save snappy and the DB the source of truth; a minimal payload lets the future worker re-read fresh data. Rejected: synchronous re-embed (blocks save, violates constraint 4), a fat event carrying the full project (staleness, duplicates the source of truth), emitting for all content types now (no v1 consumer; extends by `entityType` later).
- Consequences: The stream accumulates entries until 001 creates a consumer group. Extends cleanly to other entity types when 001 needs them.

## DEC-023: Feature 012 materializes the `sample` table ahead of feature 004 (2026-07-13)
- Context: F12-AC16..AC19 (Samples CRUD) need a write target, but feature 004 (the public Sample Explorer that reads samples) is unbuilt.
- Decision: 012 creates the `sample` table now (Flyway V12 DDL, optional V13 seed), matching the `ARCHITECTURE.md` shape, so 004's public gated read attaches later with zero schema churn.
- Why: Same table-ahead-of-consumer pattern as DEC-020 (team) and DEC-013 (`trust_content`). Blocking Samples CRUD on 004 shipping first would needlessly couple a content-management feature to an unscheduled read feature. Rejected: block Samples CRUD on 004 first.
- Consequences: The `sample` table exists from 012; 004 adds only its public gated read path later.

## DEC-022: Uploaded files stored on a local filesystem Docker volume, served by `api` under `/uploads/**` (2026-07-13)
- Context: F12-AC20/AC21 require image/file upload through the admin, stored locally (F12-AC26, constraint 1) with no external account.
- Decision: Uploads go to a local filesystem directory (`ALEF_UPLOAD_DIR`, Docker named volume `alef-uploads`), served by `api` under `/uploads/**`; asset references are plain string columns on the existing records (`image`/`photo`/`preview`/`file`) — no asset table. UUID filenames + type/size/magic-byte validation (images ≤5 MB, PDF ≤25 MB). The same multipart endpoint serves the mobile file-picker / camera-roll path.
- Why: `api` is the content service, so it owns storage and serving; local-first with zero external accounts. Rejected: bytes in Postgres (row bloat, backup cost), S3/object storage (external account, violates local-first), serving from `web/public` (splits content ownership, needs redeploy/shared mount), an upload/asset table (join + orphan-tracking burden, no v1 payoff).
- Consequences: docker-compose gains the `alef-uploads` volume; orphaned uploads are accepted for v1 (a cleanup job is a future enhancement).

## DEC-021: Admin auth is Spring Security with a persisted, env-seeded, runtime-mutable single credential + Redis sessions (2026-07-13)
- Context: F12-AC1/AC2/AC3/AC26 require an admin-only, local-first auth surface standalone from feature 006; F12-AC28/AC29 additionally require the single admin to change a *known* password in-app (no redeploy) and have it survive restarts. The v0.1 draft used an immutable env-only credential, which cannot persist a password change.
- Decision: Spring Security with a single admin credential persisted in a one-row `admin_credential` table (`CHECK id=1`), seeded from env (`ADMIN_USERNAME` + `ADMIN_PASSWORD_HASH` BCrypt) on first boot only (insert iff empty, never overwrite), authoritative and mutable thereafter via `POST /api/admin/password`. `UserDetailsService` reads the store, not env. Redis-backed server-side sessions, HttpOnly `ALEFADMIN` cookie, CSRF on `/api/admin/**`. Only a BCrypt hash is ever stored or seeded. Multiple accounts and self-service *forgotten*-password recovery are deferred to v2 (v1 has no email; a fully forgotten password is an ops-level row update / env re-seed).
- Why: Env seeds once (first boot) rather than being re-read every boot, so an in-app change is never silently reverted (F12-AC28/AC29) while the initial secret still lives in env, not the repo (engineering-standards #8, local-first). The one-row singleton is a credential store, not a general user table, so it does not presuppose 006's per-client shape (F12-AC3). Redis sessions survive restart and keep a horizontal-scale door open (constraint 7). Rejected: immutable env-only credential (can't persist a password change — the reversed v0.1 stance), stateless JWT (revocation/logout complexity, no benefit for one admin), a general `user` table / shared 006 foundation (presupposes 006's unspecified shape — F12-AC3 forbids the dependency), external IdP (external account, violates local-first).
- Consequences: New Flyway V14 `admin_credential` table; an app-level first-boot seeder; `POST /api/admin/password`; `spring-boot-starter-security` + `spring-session-data-redis` added to `api`. v2 relaxes the singleton to multi-admin additively.

## DEC-020: Team table designed for admin CRUD from day one; `active` soft-hide; 10 profiles; "Cadding" confirmed (2026-07-01)
- Context: F011 requires the leadership team to be admin-manageable (012) and expanding — new profiles must be addable without a code change or migration. The prequalification doc lists 10 profiles (not 8). "Cadding" in the company name was flagged as a possible typo.
- Decision: `team` table carries `display_order` (reorder without a migration), `active` (retire/stage a profile without a delete or migration), and audit timestamps from the start so 012 adds write endpoints with zero schema churn. 011 ships only `GET /api/team` (active rows, ordered). Adding a profile is a single `INSERT`. "Cadding" is the legal name on the Dubai DED trade license — it is NOT a typo and must never be auto-corrected.
- Why: The "expanding team" requirement is literal — a consultancy leadership roster grows by ones. The `active` flag lets 012 hide a profile without a delete (preserves history, enables staging). The 10-profile correction comes from the prequalification document which supersedes earlier estimates. "Cadding" confirmed from the trade license; no code or copy should change it.
- Consequences: 011 migrations (V6/V7) ship the full schema including `active`/`display_order`/audit columns; 012 adds write endpoints, no schema churn needed.

## DEC-019: Office locations stored in an `office` table, not a frontend constant (2026-07-01)
- Context: The Contact page (F11-AC7) must show both offices (Dubai ALEF + India Jasper) with multi-valued phones and address lines. 012 will edit office details (a phone changes, a PO box moves).
- Decision: A small `office` table (`TEXT[]` phones/address-lines, `map_query` for a keyless embed) served by `GET /api/offices`, mirroring the `trust_content` pattern. Two rows (dubai, india), `office_key` as the stable machine-readable identity.
- Why: `ARCHITECTURE.md` names `api` as the content-read service; a frontend constant would force a redeploy to fix a phone number and split content ownership — the same argument as DEC-013 for `trust_content`. The reason for a table is editability and content-ownership, not row count. Arrays model the variable cardinality (two phones for Dubai) without nullable second-column sprawl.
- Consequences: Flyway V8 schema, V9 seed (two rows). 012 edits phone/address without a migration. `mapQuery` keeps the zero-account constraint (no API key).

## DEC-018: 011 persists contact leads but does NOT emit `lead.created` or build the worker consumer (2026-07-01)
- Context: `ARCHITECTURE.md` constraint 4 / DEC-004 route lead side effects (email notification, etc.) through Redis Streams to the worker with a retry/dead-letter bar.
- Decision: 011's minimal correct slice is a durable persisted row — never lost. Event emission and the worker consumer belong with 001/lead-delivery where the reliability bar lives, not this low-risk content feature.
- Why: Over-building in 011 would duplicate hardening 001 owns. Fire-and-forget email from the request thread violates constraint 4. A persisted row is the durable source of truth; a human reads new `lead` rows directly until 001 lands.
- Consequences: A contact-form lead is persisted but not yet notified until 001 lands. The gap is explicit and visible; the row is never lost.

## DEC-017: The `lead` table is created in 011 with only the contact-form column subset, extended additively by 001 (2026-07-01)
- Context: `ARCHITECTURE.md` defines a broad `lead` entity with RFQ-specific columns (tonnage, scope[], attachment_ref, conversation_ref, etc.) that have no consumer until 001.
- Decision: 011 creates `{id, created_at, source, name, email, phone, company, message}` (Flyway V10). 001 extends it with nullable RFQ-specific columns in its own migration. `source` discriminator makes it the shared lead path (F11-AC6) across all future origins.
- Why: Creating the full wide table now would be speculative (unused columns, unvalidated shape for 001). A separate `contact_message` table would violate F11-AC6's "same path as other lead sources" and force a later merge.
- Consequences: 001 adds columns additively; no structural rename or rebuild. 011's write path is minimal and correct within its scope.

## DEC-016: Services/specializations content is static frontend content, not an API/table (2026-07-01)
- Context: The Services page (F11-AC5) lists 8 specializations as marketing copy. No filtering, aggregation, or per-row identity. 011 does not edit them; 012 could.
- Decision: A typed constant in the frontend (`web/src/lib/content/services.ts`). Software badges shown on Services come from 005's `getTrustOverview()` endpoint, not a static re-list (no duplication).
- Why: Fixed marketing copy with no query/filter surface and no 011-scope edit consumer — a table+migration+endpoint+fetch would be heavier than the code it describes. Contrast `trust_content` (012 edits it, stats recur) and `office` (012 edits it, multi-valued structured data). If 012 later wants to edit specializations, it can promote them to a `trust_content`-style table (reversible, additive).
- Consequences: Services ships static; software badges depend on 005's endpoint. The 7-tool software list becomes accurate once the 005-seed forward migration (V11) lands.

## DEC-015: One composed GET /api/trust/overview endpoint (2026-06-24)
- Context: The Trust Layer is one logical page block with three sub-sections (stats, client/contractor strip, capability badges).
- Decision: One composed endpoint mixes stored editorial content from `trust_content` with live project aggregates, rather than several granular endpoints.
- Why: One fetch keeps `web` integration and ISR revalidation simple. The synthesized marquee count means F5-AC1 is satisfied without hand-syncing a number. Does not preclude F012 reuse — F012 edits the underlying rows and the change surfaces on next read.
- Consequences: Single round-trip for the whole block. If a future feature needs just one slice, a granular endpoint can be added without breaking this one.

## DEC-014: Project-derived trust facts computed live, never stored in trust_content (2026-06-24)
- Context: Marquee project count, client names, and contractor names are already in the `project` table (F003).
- Decision: Derive them at read time (`count(*) WHERE featurable = TRUE`, `DISTINCT client`, `DISTINCT main_contractor`) rather than duplicating into `trust_content`.
- Why: Prevents trust copy drifting from the portfolio; single source of truth. No redundant write path.
- Consequences: Trust Layer stats auto-update when projects are added/edited. No admin override for these specific values (correct — they should match reality).

## DEC-013: Trust editorial facts stored in a narrow trust_content key/value table (2026-06-24)
- Context: F5-AC1/AC3 require displaying stats (years in business, staff count, steel capacity) and capability badges (software, standards) that aren't derivable from existing tables. F012 (Admin Content Management) will need to edit this content.
- Decision: New `trust_content` key/value table in Postgres (Flyway V4 schema, V5 seed), not static JSON in `web/`.
- Why: `ARCHITECTURE.md` names `api` as the content-read service. JSON in `web` would force a redeploy to fix a stat and split content ownership. A key/value shape lets F012 add/edit/retire items without a per-field migration.
- Consequences: One small new table. F012 gets a ready-made write target. Exact seed values are demo content (DEC-008).

## DEC-012: Flyway migration plus idempotent SQL seed for project data (2026-06-22)
- Context: The ~38 projects need to be in Postgres for the local demo on a fresh `docker compose up`.
- Decision: Flyway versioned migration for the schema DDL, plus a separate idempotent SQL seed (`INSERT ... ON CONFLICT (slug) DO NOTHING`) for the hand-curated project data.
- Why: Idiomatic Spring Boot migration discipline, reproducible on a fresh volume, transactional, reused by 012. Alternative rejected: JSON fixture loaded at startup (bespoke loader, outside migration history).
- Consequences: Flyway is a project dependency from day one. Seed data never collides with admin-created rows.

## DEC-011: Public list uses summary projection; full record only on detail (2026-06-22)
- Context: The portfolio list page and marquee strip only need card-level fields, not the full project record.
- Decision: `GET /api/projects` returns a summary projection (slug, name, sector, country, status, image, location, featurable). Full record only on `GET /api/projects/{slug}`.
- Why: Keeps the list/marquee payload small and lets client-side filtering load the whole catalogue cheaply. Alternative rejected: returning full records in the list (larger payload, leaks detail-only fields).
- Consequences: Two DTO shapes in the API layer. Summary fields must stay in sync with card UI needs.

## DEC-010: scope stored as TEXT[], not a join table (2026-06-22)
- Context: A project can have multiple scopes (rebar, BBS, GA, MEP, QS, as-built). Need to store the multi-valued list.
- Decision: Postgres TEXT[] column on the project table.
- Why: Small, fixed value list, never independently queried/filtered today. ~38 rows makes a join table over-engineering. Migration path: GIN index or join table if scope becomes a filter dimension.
- Consequences: Simple schema. Scope filtering (if ever needed) requires a migration to add a GIN index.

## DEC-009: Sector/status validated in app layer, not Postgres ENUM/CHECK (2026-06-22)
- Context: sector and status are constrained sets that the frontend needs to display as filter options.
- Decision: Store as TEXT in Postgres, validate in the Spring app layer, and serve the vocabulary via `GET /api/projects/filters`.
- Why: Postgres ENUM requires a migration to extend; CHECK constraint duplicates the app's validation. A single source of truth (app + filters endpoint) avoids drift. Feature 012 can extend sectors without a DB migration.
- Consequences: The DB does not enforce the vocabulary — the app layer must. Filter values are always discoverable via the API.

## DEC-008: Marquee project names cleared for local demo (2026-06-21)
- Context: Open question whether real project names (clients, contractors) needed legal clearance before displaying on the site.
- Decision: Cleared for local demo use. Revisit before any public/cloud deployment.
- Why: The site will not be deployed publicly for now; the demo runs locally only. No audience exposure risk.
- Consequences: Features 003 (F3-AC4, marquee projects on home page) and 011 (Home page) are unblocked. A gate check before first public deploy must confirm this still holds.

## DEC-007: Added feature 011 for the core marketing pages (2026-06-18)
- Context: The playbook port (DEC-001 through DEC-006) carried over the six original PRD features plus four new ideas, but the foundational content pages from the live site (Home, About, Services/Specializations, Contact) never got their own feature spec. They were implied as "the site everything else sits inside" but absent from specs/, which means an agent building strictly from specs/ would have skipped rebuilding them.
- Decision: Added specs/011-core-marketing-pages, bundling all four pages as one feature rather than splitting per page, since they are low-risk content migration and layout, not independent capabilities with separable risk profiles (unlike 007-010).
- Why: Closing a real spec gap before build starts is cheap; finding it mid-build is not. Bundling matches the actual risk and coupling (shared nav/header/footer, one content-migration pass) while still giving the work its own id space and acceptance criteria.
- Consequences: Build order updated, 011 now sits early (after 003/005, since Home pulls their data, before 001) because Home needs the concierge entry point and the other dynamic features need a page to live on.

## DEC-006: Doc comment on every function; coverage is risk-based (2026-06-18)
- Context: The site is a solo build but may become the primary production site. Question was how much process rigor to apply.
- Decision: Keep "doc comment on every function" at full strength, focused on intent and gotchas. Make test coverage risk-based (heavy on RFQ capture, agent tools, event consumer, auth) rather than a uniform 80% floor with a test class per layer. Drop version-bump-per-feature; tag releases on deploy.
- Why: Production-readiness is a property of the artifact, not uniform process. Comments serve future maintainers and future-self; a uniform coverage floor spreads effort evenly when risk is concentrated.
- Consequences: Faster solo progress. If the brother adopts it for production, ratcheting coverage up on already-clean code is cheap.

## DEC-005: Confidential uploads never touch a data-training model tier (2026-06-18)
- Context: Feature 010 wants the assistant to read uploaded tender PDFs.
- Decision: Uploads are processed only by a paid or private model. Never a free tier that trains on submitted data.
- Why: Client tender documents are confidential, especially in the GCC market. A privacy breach would end the relationship.
- Consequences: Feature 010 is gated on a paid/private model and is phase 2.

## DEC-004: Event-driven side effects via Redis Streams (2026-06-18)
- Context: Lead capture must notify the team and embedding must refresh, but neither can block the chat response.
- Decision: api writes the lead then emits lead.created to a Redis Stream and returns. A worker consumes it. Content changes emit kb.changed for re-embedding.
- Why: Keeps the chat snappy; decouples slow side effects; demonstrates an event-driven pattern worth learning.
- Consequences: Redis serves three roles (streams, cache, rate limit). A worker process exists from day one.

## DEC-003: Qdrant for vectors, PGVector as the fallback (2026-06-18)
- Context: RAG needs a vector store. Spring AI supports both.
- Decision: Qdrant for its clean local Docker story. PGVector is the documented fallback to drop a container.
- Why: Better local developer experience; a free cloud tier exists for later.
- Consequences: One extra container locally. Easy retreat to PGVector if footprint matters.

## DEC-002: Backend is Java + Spring AI (2026-06-18)
- Context: The owner is a strong Java/Spring engineer who wants to learn AI capability in his own language, and explicitly opened the stack choice.
- Decision: Spring Boot 4 + Spring AI for the API and the concierge. Concierge built from ChatClient, @Tool beans, and advisors (RAG, memory, guardrails) rather than a graph library.
- Why: Spring AI is mature (20+ providers incl. Ollama and Anthropic, vector stores incl. Qdrant and PGVector, tool calling, RAG advisors). Learning lands in the strongest language and makes a rarer, more differentiating portfolio piece. The only Python-only gap (LangGraph) is not needed for a few-turn RFQ gatherer.
- Consequences: Polyglot repo (TS web, Java api). JVM memory matters on tiny free deploy tiers; GraalVM native image is the lever if needed.

## DEC-001: Local-first, ambitious architecture over a minimal brochure (2026-06-18)
- Context: Cost must be near zero for the business, but the demo runs locally and the project doubles as a learning and portfolio vehicle. An earlier single-Next.js design was correct on cost but uninteresting.
- Decision: Build the full multi-container, event-driven system and run it locally via Docker Compose. Keep a cheap, non-GCP deploy ladder (Cloudflare Pages, Render/Railway, Neon, Qdrant Cloud, Upstash) for later.
- Why: Local demo is free regardless of ambition. The richer system is the point of the exercise and keeps the door open to becoming the primary site.
- Consequences: More moving parts than a brochure needs, accepted deliberately. Deploy cost stays near zero, with realistic ~$5/mo for an always-warm JVM service if it goes live.
