# ALEF / Jasper Website Rebuild

Spec-driven rebuild of the ALEF Architectural & Cadding Services site, run through the Agentic Engineering Playbook v2 (role-based subagents, skills, commands, hooks). Modern lead-generating site for a GCC rebar detailing consultancy, with an AI RFQ concierge. Runs locally at zero cost.

## Layout

```
.
├─ CLAUDE.md                     project context + invariants (read first)
├─ docker-compose.yml            7-service local stack
├─ Makefile                      infra-up / up / down / logs / pull-models
├─ docs/
│   ├─ AGENTIC_ENGINEERING_PLAYBOOK.md   the system this repo follows
│   └─ knowledge/                cross-cutting spec, human-gated
│       ├─ ARCHITECTURE.md       constraints every feature obeys (hook-protected)
│       ├─ DECISIONS.md          append-only, why choices were made
│       └─ LEARNINGS.md          append-only, pitfalls
├─ .claude/
│   ├─ agents/                   business pm architect dev qa reviewer
│   ├─ skills/                   engineering-standards spec-authoring handoff-protocol definition-of-done brownfield-archaeology
│   ├─ commands/                 orchestrate feature status setup-agentic
│   ├─ hooks/                    guard-architecture gate-tests
│   └─ settings.json             wires hooks to events
├─ specs/
│   └─ NNN-feature/              per-feature: requirements/architecture/design/implementation/testing + handoffs
└─ web/  api/  infra/            scaffolded by the Dev role per feature
```

## Two homes for specs
- Per-feature specs live in specs/NNN-feature/.
- Cross-cutting system spec lives in docs/knowledge/ (ARCHITECTURE, DECISIONS, LEARNINGS).

## Features
001 ai-concierge · 002 turnaround-estimator · 003 portfolio-filtering · 004 sample-explorer · 005 trust-layer · 006 client-portal (stretch) · 007 savings-calculator · 008 bilingual-en-ar · 009 whatsapp-handoff · 010 drawing-aware-rfq (phase 2) · 011 core-marketing-pages

## Get started
```bash
git init && git add -A && git commit -m "chore: playbook-conformant scaffold"
make infra-up && make pull-models
claude            # roles load from .claude/agents/, hooks from settings.json
```
Then drive the pipeline:
```
> /status
> /orchestrate        # presents the first gate; approve to spawn the next role
```
First build the infra Compose skeleton, then feature 001.
