# DECISIONS.md

> Append-only. Newest first. Why a choice was made, not just what.

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
