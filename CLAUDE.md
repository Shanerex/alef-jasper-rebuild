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
| 003 | Portfolio with Filtering | requirements approved |
| 004 | Sample Explorer | requirements approved |
| 005 | Trust Layer | requirements approved |
| 006 | Client Portal (stretch) | requirements draft |
| 007 | Savings Calculator | requirements approved |
| 008 | Bilingual EN/AR | requirements approved |
| 009 | WhatsApp Handoff | requirements approved |
| 010 | Drawing-Aware RFQ (phase 2) | requirements draft, blocked on paid/private model |
| 011 | Core Marketing Pages (Home/About/Services/Contact) | requirements approved |
| 012 | Admin Content Management | requirements approved |

Downstream phases (architecture, design, implementation, testing) are produced per feature by running /orchestrate.

## TODOs (known gaps, not bugs)
- Infra Compose skeleton (all services talking) before feature 001.
- Confirm marquee project names are cleared for public marketing (business open question).
- Decide lead delivery target (email only vs email plus Google Sheet/CRM).

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
