# ALEF Rebuild: Claude Context

## What this project is
A modern, lead-generating rebuild of the ALEF Architectural & Cadding Services site, a GCC rebar detailing and structural drafting consultancy. Stack: Next.js web, Spring Boot 4 + Spring AI api, Postgres, Qdrant, Redis, Ollama, all local via Docker Compose. Stage: greenfield, local demo, may become the primary production site.

## What this project is NOT
- Not e-commerce, not a public CAD viewer, not a quoting engine.
- Does not store payments.
- Does not send confidential uploads to a free model tier that trains on data.

---

## Architecture constraints (always-on)
@docs/knowledge/ARCHITECTURE.md

## Visual design system (always-on)
@docs/knowledge/DESIGN_SYSTEM.md

## How this repo is run agentically
Roles are subagents in .claude/agents/. Procedures are skills in .claude/skills/. Enforcement is in .claude/settings.json (hooks). The playbook is docs/AGENTIC_ENGINEERING_PLAYBOOK.md.
- Start a pipeline: /orchestrate
- Stage a new feature: /feature <short name>
- See status: /status

The specs are the source of truth, not this conversation and not the code. If reality and a spec disagree, the owning role fixes the spec first, then code follows. No silent drift.

---

## Key invariants (advisory here, enforced by hooks where noted)
1. Read the knowledge layer before any structural change. [hook: guard-architecture]
2. ARCHITECTURE.md is human-approval only. [hook: guard-architecture]
3. A feature is not done until the full suite passes. [hook: gate-tests]
4. No feature code before a task is selected from the active feature's plan; every task cites an acceptance id (F<n>-AC<m>).
5. Never assume an external schema. If not derivable from code, STOP and ask.
6. Doc comment on every function, intent-focused.
7. Confidential uploads use a paid or private model only.
8. DECISIONS.md and LEARNINGS.md are append-only.

(Coverage is risk-based, not a uniform floor. Versioning is per-deploy, not per-feature. See DEC-006.)

---

## Implemented specs
| ID | Feature | Status |
|----|---------|--------|
| 001 | AI RFQ Concierge | requirements approved |
| 002 | Turnaround Estimator | requirements approved |
| 003 | Portfolio with Filtering | done |
| 004 | Sample Explorer | requirements approved |
| 005 | Trust Layer | done |
| 006 | Client Portal (stretch) | requirements draft |
| 007 | Savings Calculator | requirements approved |
| 008 | Bilingual EN/AR | requirements approved |
| 009 | WhatsApp Handoff | requirements approved |
| 010 | Drawing-Aware RFQ (phase 2) | requirements draft, blocked on paid/private model |
| 011 | Core Marketing Pages (Home/About/Services/Contact) | done |
| 012 | Admin Content Management | done (closed 2026-07-19; testing.md v1.5, 30/30 F12-ACn PASS; 4 bug-fix passes; PM close: specs/012-admin-content-management/handoffs/15-pm-close.md) |

Downstream phases (architecture, design, implementation, testing) are produced per feature by running /orchestrate.

## API endpoints (implemented)
| Method | Path | Feature | Notes |
|--------|------|---------|-------|
| GET | `/api/projects` | 003 | Filterable, paginated summary list |
| GET | `/api/projects/{slug}` | 003 | Full project detail |
| GET | `/api/projects/filters` | 003 | Sector/status vocabularies + distinct countries |
| GET | `/api/trust/overview` | 005 | Composed trust payload: stats, clients, contractors, software, standards |
| GET | `/api/team` | 011 | Active team members ordered by display_order; envelope `{ members }` |
| GET | `/api/offices` | 011 | All offices ordered by display_order; envelope `{ offices }` |
| POST | `/api/leads` | 011 | Submit contact lead; honeypot + per-IP rate limit (10/hour); returns `{ id, status }` |
| POST | `/api/admin/session` | 012 | Admin login; sets `ALEFADMIN` session cookie; rate-limited (10/15min); CSRF-exempt (DEC-028) |
| GET | `/api/admin/session` | 012 | Current session check; 401 if not authenticated |
| DELETE | `/api/admin/session` | 012 | Admin logout; invalidates session; CSRF-**protected** as of the bug-fix pass (DEC-030 narrowed the exemption to `POST` only) |
| GET | `/api/admin/csrf` | 012 | Primes the `XSRF-TOKEN` cookie for the web client (DEC-028, LEARNING-002); public |
| POST | `/api/admin/password` | 012 | Self-service password change; requires current password; CSRF-protected |
| GET/POST | `/api/admin/projects`, `/api/admin/projects/{id}` (+PUT/DELETE) | 012 | Full admin project CRUD; emits `kb.changed` on write (DEC-024) |
| GET/POST | `/api/admin/team`, `/api/admin/team/{id}` (+PUT/DELETE) | 012 | Full admin team CRUD; includes inactive rows |
| GET/POST | `/api/admin/trust`, `/api/admin/trust/{id}` (+PUT/DELETE) | 012 | Grouped stat/software/standard rows + read-only derived facts (DEC-025) |
| GET/POST | `/api/admin/samples`, `/api/admin/samples/{id}` (+PUT/DELETE) | 012 | Full admin sample CRUD (DEC-023) |
| POST | `/api/admin/uploads` | 012 | Multipart upload; validates type/size/magic-bytes; returns public `/uploads/...` URL |
| GET | `/uploads/**` | 012 | Static resource handler serving `ALEF_UPLOAD_DIR`; public, read-only |

## Database tables (implemented)
| Table | Feature | Notes |
|-------|---------|-------|
| `project` | 003 | 15 seed rows via Flyway V2; write path added by 012 (admin CRUD) |
| `trust_content` | 005 | 9 seed rows via Flyway V5 (3 stats, 3 software, 3 standards); write path added by 012 |
| `team` | 011 | 10 seed rows via Flyway V7; `active` flag, `display_order`, photo null until assets exist; write path added by 012 |
| `office` | 011 | 2 seed rows via Flyway V9 (Dubai, India); `address_lines TEXT[]`, `phones TEXT[]` |
| `lead` | 011 | Contact form submissions; minimal subset — no attachments, no scope array (DEC-017) |
| `sample` | 012 | Flyway V12 DDL, empty (no seed — V13 reserved, intentionally unused, see implementation.md); admin CRUD only, DEC-023 |
| `admin_credential` | 012 | Flyway V14 DDL, singleton row (`CHECK id=1`); seeded app-side on first boot from `ADMIN_USERNAME`/`ADMIN_PASSWORD_HASH`, DEC-021 |

## Key files
- `api/pom.xml` -- Maven project, Spring Boot 3.4.1
- `api/src/main/resources/application.yml` -- datasource, profiles, Spring AI config
- `api/src/main/java/com/alef/api/portfolio/` -- controller, service, repository, entity, dto, vocabulary, error
- `api/src/main/java/com/alef/api/trust/` -- controller, service, repository, entity, dto, vocabulary (feature 005)
- `api/src/main/resources/db/migration/` -- V1 (DDL), V2 (seed), V4 (trust_content DDL), V5 (trust_content seed), V6 (team DDL), V7 (team seed), V8 (office DDL), V9 (office seed), V10 (lead DDL), V12 (sample DDL, feature 012; V13 reserved/unused), V14 (admin_credential DDL, feature 012)
- `api/src/main/java/com/alef/api/team/` -- controller, service, repository, entity, dto (feature 011)
- `api/src/main/java/com/alef/api/office/` -- controller, service, repository, entity, dto (feature 011)
- `api/src/main/java/com/alef/api/lead/` -- controller, service, repository, entity, dto, error (feature 011)
- `api/src/main/java/com/alef/api/sample/` -- entity, repository, vocabulary (feature 012; admin CRUD lives under `admin.sample`)
- `api/src/main/java/com/alef/api/admin/security/` -- `AdminSecurityConfig` (filter chain, CORS, CSRF; scoped to `/api/admin/**` via `.securityMatcher`, DEC-029), `AdminUserDetailsService`, `AdminCredentialSeeder` (first-boot), `SessionController`/`AdminSessionService`, `CsrfTokenController` (priming, DEC-028), `PasswordChangeController`/`PasswordChangeService`, error handlers (feature 012)
- `api/src/main/java/com/alef/api/admin/project/`, `.../admin/team/`, `.../admin/trust/`, `.../admin/sample/` -- admin CRUD controllers/services/DTOs, reusing the existing entities/repositories (feature 012)
- `api/src/main/java/com/alef/api/admin/upload/` -- `UploadController`/`UploadService` (type/size/magic-byte validation), `UploadResourceConfig` (`/uploads/**` static handler), vocabulary (feature 012)
- `api/src/main/java/com/alef/api/admin/kb/` -- `KbChangedEvent`/`KbChangedPublisher`, AFTER_COMMIT Redis Stream producer on the project write path (feature 012, DEC-024)
- `api/src/main/java/com/alef/api/admin/error/` -- shared `AdminExceptionHandler` (404/409/400) for the four admin CRUD resources (feature 012)
- `web/package.json` -- Next.js 15, Tailwind, shadcn/ui
- `web/src/app/layout.tsx` -- root layout; `<html>`/`<body>`/fonts/metadata only, deliberately carries no nav/footer (feature 012 bug-fix pass 2, DEC-032-adjacent fix, so `/admin/**` never inherits marketing chrome)
- `web/src/app/(marketing)/layout.tsx` -- public-site layout (`SiteNav`/`SiteFooter`); wraps every route under `(marketing)` only (feature 012 bug-fix pass 2)
- `web/src/app/(marketing)/page.tsx` -- Home page (feature 011; replaces placeholder; moved under the route group in 012's bug-fix pass 2)
- `web/src/app/(marketing)/about/page.tsx` -- About page (feature 011)
- `web/src/app/(marketing)/services/page.tsx` -- Services page (feature 011)
- `web/src/app/(marketing)/contact/page.tsx` -- Contact page (feature 011)
- `web/src/app/(marketing)/projects/` -- list and detail pages (ISR)
- `web/src/components/projects/` -- ProjectCard, ProjectGrid, ProjectFilters, ProjectDetail, ProjectBrowser
- `web/src/components/ui/` -- Badge, Button, Select, Separator (shadcn/ui)
- `web/src/components/marketing/page-hero.tsx` -- shared PageHero block used by about/services/contact (feature 011)
- `web/src/components/home/` -- HomeHero, CapabilityStrip, FeaturedProjects, HomeCTA (feature 011)
- `web/src/components/about/` -- CompanyStory, GccReachBlock, LandmarkProjectsStrip (feature 011)
- `web/src/components/team/` -- TeamGrid, TeamMemberCard (feature 011)
- `web/src/components/services/` -- ServicesList, SoftwareBadges (feature 011)
- `web/src/components/contact/` -- ContactForm, OfficeLocations, DirectChannels (feature 011)
- `web/src/lib/api/projects.ts` -- typed API client
- `web/src/lib/api/team.ts` -- typed API client for GET /api/team (feature 011)
- `web/src/lib/api/offices.ts` -- typed API client for GET /api/offices (feature 011)
- `web/src/lib/api/leads.ts` -- typed API client for POST /api/leads (feature 011)
- `web/src/lib/types/project.ts` -- TypeScript types mirroring API DTOs
- `web/src/lib/types/team.ts` -- TypeScript types for team (feature 011)
- `web/src/lib/types/office.ts` -- TypeScript types for office (feature 011)
- `web/src/lib/content/services.ts` -- static SERVICES array (8 entries, DEC-016, feature 011)
- `web/src/components/trust/` -- TrustLayer, TrustStats, ClientStrip, CapabilityBadges, TrustLayerSection (feature 005)
- `web/src/lib/api/trust.ts` -- typed API client for trust overview
- `web/src/lib/types/trust.ts` -- TypeScript types mirroring trust API DTOs
- `web/src/middleware.ts` -- route guard for `/admin/**` (cookie presence check only; API 401/403 is authoritative), feature 012
- `web/src/app/admin/login/page.tsx` -- admin login screen (public), feature 012; wraps its `useSearchParams()`-consuming form in `<Suspense>` (bug-fix pass, required by `next build`'s static prerendering)
- `web/src/app/admin/(protected)/` -- route group for every authenticated admin screen: dashboard, `account`, `projects`, `team`, `trust`, `samples` (list/new/[id] each), feature 012. `projects/new` and `projects/[id]` are Server Components (`force-dynamic`) that fetch filter vocabulary server-side and pass it as a prop -- a client-side fetch to that public, CORS-less endpoint hung forever (bug-fix pass 3, PITFALL-010); `projects/[id]/edit-project-client.tsx` holds the client-side half (the project record itself, which needs the browser's session cookie)
- `web/src/components/admin/` -- `AdminShell` (responsive sidebar/drawer + top bar), `ResponsiveList` (table→card), `DeleteConfirmDialog` (confirm button uses the `danger` token as of the bug-fix pass, DEC-031), `UploadField`, `AdminPageHeader`/`FormErrorBanner`, per-resource forms (`projects/`, `team/`, `trust/`, `samples/`), and `ui/` (token-only `AdminInput`/`AdminTextarea`/`AdminSelect`/`AdminField`/`AdminCheckbox` — deliberately not reusing `ui/select.tsx`/`ui/separator.tsx`, which bake in raw-color inline styles; see implementation.md), feature 012
- `web/src/lib/api/admin-client.ts` -- shared fetch wrapper (`credentials:'include'`, CSRF header injection, `AdminApiError`, `primeCsrfToken()`), feature 012
- `web/src/lib/api/admin-session.ts`, `admin-account.ts`, `admin-projects.ts`, `admin-team.ts`, `admin-trust.ts`, `admin-samples.ts`, `admin-uploads.ts` -- typed admin API clients, feature 012
- `web/src/lib/types/admin.ts` -- TypeScript types mirroring the `/api/admin/**` DTOs, feature 012
- `web/src/lib/utils.ts` -- shared client-safe utilities (`cn`, plus `resolveUploadUrl` added in feature 012's bug-fix pass 4 -- prefixes `/uploads/**` paths with the browser-visible API origin, since that static content is served by `api`, not `web`; used at every `<img>` site that can render an uploaded value)

## TODOs (known gaps, not bugs)
- ~~Infra Compose skeleton (all services talking) before feature 001.~~ -> api/ and web/ Dockerfiles created; compose should work.
- ~~Confirm marquee project names are cleared for public marketing~~ -> cleared for local demo (DEC-008); revisit before public deploy.
- Decide lead delivery target (email only vs email plus Google Sheet/CRM).
- Seed remaining ~23 projects (15 of ~38 seeded in V2).
- On deploy: switch portfolio pages and marketing pages from `force-dynamic` to ISR (`revalidate=60`) if API is available at build time (see PITFALL-007).
- Team photo assets not yet uploaded; TeamMemberCard shows initials fallback (photo = NULL in seed).
- Lead delivery (email notification on `lead.created`) not yet implemented — deferred (DEC-018).
- `sample` table (feature 012) ships empty — no V13 seed was written (architecture flagged V13 as optional/Dev's call); feature 004's public read path and any demo content are future work.
- Orphaned uploads (uploaded then never saved to a record, or replaced) are not garbage-collected — accepted for v1 per architecture §8.
- `kb.changed` has no consumer yet (feature 001 unbuilt) — events accumulate harmlessly on the Redis Stream.
- ~~Local `web/` production build (`next build`) could not be verified on this machine~~ -> now verified via `docker compose build web` (feature 012 bug-fix pass); this machine's local Node (18.12.0) still can't run `next build` directly, so `docker compose build web` remains the actual gate going forward, not a bare local `next build`. See implementation.md.
- `worker` container fails to start under `docker compose up --build` -- `AdminSecurityConfig` (feature 012) has no profile/web-application guard, so it's loaded inside the `worker` profile's non-web context and crashes resolving `MvcRequestMatcher`. Deliberately deferred, not blocking 012's close, since no stream consumer exists yet (feature 001 unbuilt) -- see DEC-032, PITFALL-009. Must be fixed before feature 001 needs the `worker` process to actually run.
- Public content changes made via the admin console (project/team/trust) can take up to 60s to appear on the public site -- `next: { revalidate: 60 }` with no on-demand cache invalidation anywhere in the admin write paths. Confirmed working within that window, not broken, but a real UX gap for an admin expecting an immediate effect (e.g. urgently hiding a team member). See implementation.md's "Bug-fix pass 3."

## Running locally
\`\`\`bash
make infra-up      # postgres, qdrant, redis, ollama
make pull-models   # llama3.2 + nomic-embed-text
make up            # full stack once web/ and api/ are scaffolded
\`\`\`

## Environment variables
| Variable | Required | Default | Notes |
|----------|----------|---------|-------|
| SPRING_AI_OLLAMA_BASE_URL | local | http://ollama:11434 | local model endpoint |
| ANTHROPIC_API_KEY / GROQ_API_KEY / GEMINI_API_KEY | deploy only | none | provider swap for deploy |
| RESEND_API_KEY | deploy only | none | lead notification email |
| ADMIN_USERNAME | feature 012 | none | first-boot seed for the single admin login; persisted to `admin_credential`, not re-read after seeding |
| ADMIN_PASSWORD_HASH | feature 012 | none | first-boot seed — a BCrypt hash, never plaintext; does not override an in-app password change |
| WEB_ORIGIN | feature 012 | http://localhost:3000 | CORS allowed origin for credentialed admin requests |
| ALEF_UPLOAD_DIR | feature 012 | /var/alef/uploads | filesystem dir for admin uploads; backed by the `alef-uploads` volume |
| NEXT_PUBLIC_API_BASE_URL | optional | http://localhost:8080 | browser-visible API origin for admin client-side fetches (feature 012); the default already matches `docker compose up`'s host-mapped `api` port, so this is only needed to override for a non-default topology |
