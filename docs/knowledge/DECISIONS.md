# DECISIONS.md

> Append-only. Newest first. Why a choice was made, not just what.

## DEC-021: Light theme approved as a design-system extension; dark remains the brand default (2026-07-19)
- Context: Feature 014 (theme toggle) requests a dark/light toggle at the top-right with a polished transition. `DESIGN_SYSTEM.md` is explicitly dark-first ("Not a light/white theme") and defines no light palette, so the business handoff flagged the conflict as requiring human approval.
- Decision: The owner reviewed the conflict and approved a light theme for the site (2026-07-19). A light palette must be defined as a human-reviewed extension of `DESIGN_SYSTEM.md` before 014's design/implementation phases; the "Dark Prestige" direction stays the canonical brand expression and the dark palette remains the reference for all new components.
- Why: Readability preference and bright-daylight office viewing in the GCC justify a light option, but brand equity lives in the dark presentation — so light mode is an alternate rendering of the same system (navy/gold/serif language preserved), not a second brand.
- Consequences: `DESIGN_SYSTEM.md` gains a light-palette section (token-for-token mapping of backgrounds, surfaces, text, gold treatment) subject to owner sign-off. 014 is unblocked for /orchestrate once that section lands. Gold-on-light contrast must meet accessibility budgets (treated as correctness per ARCHITECTURE.md).

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
