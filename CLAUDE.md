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
| 011 | Core Marketing Pages (Home/About/Services/Contact) | requirements approved |
| 012 | Admin Content Management | requirements approved |

Downstream phases (architecture, design, implementation, testing) are produced per feature by running /orchestrate.

## API endpoints (implemented)
| Method | Path | Feature | Notes |
|--------|------|---------|-------|
| GET | `/api/projects` | 003 | Filterable, paginated summary list |
| GET | `/api/projects/{slug}` | 003 | Full project detail |
| GET | `/api/projects/filters` | 003 | Sector/status vocabularies + distinct countries |
| GET | `/api/trust/overview` | 005 | Composed trust payload: stats, clients, contractors, software, standards |

## Database tables (implemented)
| Table | Feature | Notes |
|-------|---------|-------|
| `project` | 003 | 15 seed rows via Flyway V2 |
| `trust_content` | 005 | 9 seed rows via Flyway V5 (3 stats, 3 software, 3 standards) |

## Key files
- `api/pom.xml` -- Maven project, Spring Boot 3.4.1
- `api/src/main/resources/application.yml` -- datasource, profiles, Spring AI config
- `api/src/main/java/com/alef/api/portfolio/` -- controller, service, repository, entity, dto, vocabulary, error
- `api/src/main/java/com/alef/api/trust/` -- controller, service, repository, entity, dto, vocabulary (feature 005)
- `api/src/main/resources/db/migration/` -- V1 (DDL), V2 (seed), V4 (trust_content DDL), V5 (trust_content seed)
- `web/package.json` -- Next.js 15, Tailwind, shadcn/ui
- `web/src/app/projects/` -- list and detail pages (ISR)
- `web/src/components/projects/` -- ProjectCard, ProjectGrid, ProjectFilters, ProjectDetail, ProjectBrowser
- `web/src/components/ui/` -- Badge, Button, Select, Separator (shadcn/ui)
- `web/src/lib/api/projects.ts` -- typed API client
- `web/src/lib/types/project.ts` -- TypeScript types mirroring API DTOs
- `web/src/components/trust/` -- TrustLayer, TrustStats, ClientStrip, CapabilityBadges, TrustLayerSection (feature 005)
- `web/src/lib/api/trust.ts` -- typed API client for trust overview
- `web/src/lib/types/trust.ts` -- TypeScript types mirroring trust API DTOs

## TODOs (known gaps, not bugs)
- ~~Infra Compose skeleton (all services talking) before feature 001.~~ -> api/ and web/ Dockerfiles created; compose should work.
- ~~Confirm marquee project names are cleared for public marketing~~ -> cleared for local demo (DEC-008); revisit before public deploy.
- Decide lead delivery target (email only vs email plus Google Sheet/CRM).
- Seed remaining ~23 projects (15 of ~38 seeded in V2).
- On deploy: switch portfolio pages from `force-dynamic` to ISR (`revalidate=60`) if API is available at build time (see PITFALL-007).

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
