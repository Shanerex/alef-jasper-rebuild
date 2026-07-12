# ALEF Rebuild

Spec-driven rebuild of the **ALEF Architectural & Cadding Services** site — a GCC rebar detailing and structural drafting consultancy. Modern lead-generating site with an AI RFQ concierge. Runs entirely locally at zero cost via Docker Compose.

Built through the **Agentic Engineering Playbook v2**: role-based subagents, skills, commands, and enforced hooks in Claude Code.

---

## Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Next.js · TypeScript · Tailwind · shadcn/ui |
| API | Spring Boot 4 · Spring AI |
| Worker | Same Spring codebase, `worker` profile |
| Database | PostgreSQL 16 |
| Vector store | Qdrant |
| Cache / streams | Redis 7 |
| Local LLM | Ollama (`llama3.2` + `nomic-embed-text`) |

---

## Repo layout

```
.
├─ CLAUDE.md                          project context + invariants (read first)
├─ docker-compose.yml                 7-service local stack
├─ Makefile                           infra-up / up / down / logs / pull-models
├─ .env.example                       deploy-only secrets (no secrets needed for local demo)
├─ docs/
│   ├─ AGENTIC_ENGINEERING_PLAYBOOK.md
│   └─ knowledge/
│       ├─ ARCHITECTURE.md            system constraints, hook-protected
│       ├─ DECISIONS.md               append-only decision log
│       └─ LEARNINGS.md               append-only pitfalls log
├─ .claude/
│   ├─ agents/                        business · pm · architect · dev · qa · reviewer
│   ├─ skills/                        engineering-standards · spec-authoring · handoff-protocol · definition-of-done · brownfield-archaeology
│   ├─ commands/                      /orchestrate · /feature · /status · /setup-agentic
│   ├─ hooks/                         guard-architecture · gate-tests
│   └─ settings.json                  wires hooks to events
├─ specs/
│   └─ NNN-feature/                   per-feature: requirements · architecture · design · implementation · testing · handoffs
└─ web/   api/   infra/               scaffolded by the Dev role per feature
```

---

## Features

| ID | Name | Status |
|----|------|--------|
| 001 | AI RFQ Concierge | requirements approved |
| 002 | Turnaround Estimator | requirements approved |
| 003 | Portfolio with Filtering | requirements approved |
| 004 | Sample Explorer | requirements approved |
| 005 | Trust Layer | requirements approved |
| 006 | Client Portal | requirements draft (stretch) |
| 007 | Savings Calculator | requirements approved |
| 008 | Bilingual EN/AR | requirements approved |
| 009 | WhatsApp Handoff | requirements approved |
| 010 | Drawing-Aware RFQ | requirements draft (blocked: needs paid/private model) |
| 011 | Core Marketing Pages | requirements approved |
| 012 | Admin Content Management | requirements approved |

---

## Running locally

### 1 — Start infrastructure (datastores + Ollama)

Works before any app code exists:

```bash
make infra-up      # postgres · qdrant · redis · ollama
make pull-models   # pulls llama3.2 and nomic-embed-text into Ollama
```

### 2 — Start the full stack

Once `web/` and `api/` are scaffolded by the Dev agent:

```bash
make up            # docker compose up --build
```

### 3 — Ports

| Service | Port |
|---------|------|
| web (Next.js) | 3000 |
| api (Spring Boot) | 8080 |
| postgres | 5432 |
| qdrant | 6333 |
| redis | 6379 |
| ollama | 11434 |

---

## Environment variables

No secrets are needed for the local demo. These apply only at the deploy rung:

| Variable | Purpose |
|----------|---------|
| `ANTHROPIC_API_KEY` / `GROQ_API_KEY` / `GEMINI_API_KEY` | LLM provider swap (pick one) |
| `RESEND_API_KEY` | Lead notification email via the worker |

Copy `.env.example` and fill in only what you need.

---

## Agentic pipeline

Start Claude Code in this directory and drive the pipeline:

```
/status          # see where every feature stands
/feature <name>  # stage a new feature (business → pm → architect → dev → qa)
/orchestrate     # present the next gate; approve to spawn the next role
```

- Per-feature specs live in `specs/NNN-feature/`.
- Cross-cutting system spec lives in `docs/knowledge/` (human-approval only to modify).
- The specs are the source of truth — if code and a spec disagree, fix the spec first.
