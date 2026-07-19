# ARCHITECTURE.md

> Design decisions and constraints for the whole system. Human-approval only to change. The `guard-architecture` hook blocks writes to this file.

## System overview

A modern, lead-generating rebuild of the ALEF Architectural & Cadding Services site, a GCC rebar detailing and structural drafting consultancy. The site converts a strong portfolio and capacity story into qualified RFQs. Its centerpiece is an AI concierge that answers grounded capability questions and captures structured leads.

The whole system runs locally via `docker compose up` at zero cost for the demo. It can become the primary production site without a rewrite.

What it is NOT: an e-commerce site, a public CAD viewer, or a quoting engine. It does not store payments.

## Components

- **web**: Next.js + TypeScript + Tailwind + shadcn/ui. Marketing pages, project pages, and a streaming chat UI over SSE.
- **api**: Spring Boot 4 + Spring AI. Content read, RFQ and leads, and the concierge (ChatClient + `@Tool` beans + advisors). RAG over Qdrant. Also runs the internal admin console's auth layer (Spring Security; single admin credential persisted in `admin_credential`, seeded from env — `ADMIN_USERNAME`/`ADMIN_PASSWORD_HASH` — on first boot only and mutable at runtime via `POST /api/admin/password`) and serves uploaded content assets under `/uploads/**` from a local Docker volume.
- **worker**: same Spring codebase, `worker` profile. Consumes Redis Streams (lead.created, kb.changed).
- **postgres**: projects, leads, portal data.
- **qdrant**: knowledge-base vectors for RAG.
- **redis**: event streams, cache, rate limiting, and the admin session store (Spring Session).
- **ollama**: local LLM and embeddings (llama3.2 + nomic-embed-text).

## Constraints (do not violate)

1. Local-first. The full system runs with `docker compose up` and no external accounts for the demo.
2. Backend is Java: Spring Boot + Spring AI. The point is to learn Spring's AI stack.
3. The model provider is swappable by Spring AI config. Never hard-wire one provider.
4. The chat response never blocks on email or embedding. Side effects go through Redis Streams to the worker.
5. Confidential client documents are never sent to any model tier that trains on submitted data. Only a paid or private model may process uploads.
6. Tool calls are the source of truth for what gets saved. The model gathers; the `capture_rfq` tool validates and persists.
7. Keep a low-cost, non-GCP upgrade path. Never assume paid infrastructure for the demo.

## Production-readiness bar (risk-based, not uniform)

Rigor goes where failure is expensive:
- Heavy tests on RFQ capture, the agent tool contract, the event consumer, and auth (when the portal lands).
- LLM hardening: rate limiting, max-token caps, prompt-injection resistance, graceful fallback when a free quota is hit.
- Lead delivery is reliable: retry and dead-letter on the `lead.created` consumer so a real enquiry is never lost.
- Form abuse mitigation (honeypot plus rate limit), input validation, secrets in env not repo.
- SEO, performance, and accessibility budgets are treated as correctness for a marketing site.
- Doc comment on every function, focused on intent and gotchas (helps future maintainers, including future-you).
- Coverage is risk-based, not a uniform percentage floor.

## Data model

```
project  { id, slug, name, sector, country, status, image, description,
           main_contractor, client, consultant, location,
           scope[], featurable, created_at, updated_at }
team     { id, name, role, company, email, photo, display_order, active,
           created_at, updated_at }
office   { id, office_key, name, address_lines[], phones[], email,
           map_query, display_order, created_at, updated_at }
sample   { id, slug, title, category, preview, file, display_order,
           created_at, updated_at }
lead     { id, created_at, source, name, email, company, phone, message,
           project_name, sector, country, scope[], tonnage,
           drawing_count, software_standard, deadline, attachment_ref,
           conversation_ref }
trust_content { id, item_key, item_type, label, value, unit, display_order,
               created_at, updated_at }
admin_credential { id, username, password_hash, updated_at }   // single row; BCrypt hash only
kb_chunk // in Qdrant: { id, source_type, source_ref, text, vector, payload }
```

## Open architectural questions

- Lead delivery target: email only, or also a Google Sheet / CRM?
- Qdrant vs PGVector for v1 (one fewer container if PGVector).
- Where the bilingual content (008) is stored once project descriptions get localized.
