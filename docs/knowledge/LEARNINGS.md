# LEARNINGS.md

> Append-only. Newest first. Patterns that worked, pitfalls, workarounds.

## PITFALL-007: ISR requires API at build time; force-dynamic is correct for Docker Compose (2026-06-24)
- What happened: Architecture spec prescribed ISR (revalidate=60) for the portfolio pages. In the Docker Compose setup, the web image is built before the api container starts, so any build-time API fetch (ISR initial render or generateStaticParams) fails and breaks the Docker build.
- Takeaway: force-dynamic is the correct rendering strategy when the API is not available at build time. Document the deviation rather than fighting the build order.
- How to apply: Keep `export const dynamic = "force-dynamic"` on both portfolio pages. Switch to ISR only if the deployment model guarantees API availability at build time.

## PITFALL-006: Spring AI Ollama auto-configuration starts eagerly (2026-06-22)
- What happened: Including the spring-ai-ollama-spring-boot-starter dependency causes auto-configuration to attempt Ollama connection on startup, even when no AI feature is active. Tests need explicit exclusion of OllamaAutoConfiguration.
- Takeaway: Exclude Spring AI auto-configuration in test profiles when the AI stack is not under test.
- How to apply: application-test.yml uses spring.autoconfigure.exclude to disable OllamaAutoConfiguration.

## PITFALL-005: Local JDK version vs Docker JDK version mismatch (2026-06-22)
- What happened: The pom.xml targeted Java 21 but the local machine has JDK 17. Compilation failed locally even though the Docker build (JDK 21) would succeed.
- Takeaway: Set the Maven java.version to match the local JDK for local dev. The Dockerfile's JDK version is independent. Document the requirement.
- How to apply: pom.xml uses java.version=17 for local compat; Dockerfile uses eclipse-temurin:21.

## PITFALL-004: Free LLM tiers may train on your data, and billing can delete the free tier (2026-06-18)
- What happened: Gemini's free tier is generous but free-tier inputs may be used for training, and enabling billing on a project removes the free tier for that whole project.
- Takeaway: Never send confidential uploads through a free tier. Keep prototype and production in separate projects.
- How to apply: Route only non-sensitive Q&A through a free tier; gate uploads to a paid/private model.

## PITFALL-003: JVM services are tight on tiny free hosts (2026-06-18)
- What happened: Free deploy tiers (e.g. Render free at 512MB) are cramped for a Spring Boot service, with cold starts on top.
- Takeaway: Budget ~$5/mo for an always-warm service, or build a GraalVM native image for low memory and fast startup.

## PITFALL-002: "Free" deploy tiers shift; verify before relying (2026-06-18)
- What happened: Fly.io removed its free tier (now a 2-hour trial); Railway has no permanent free tier ($5/mo); Vercel Hobby bans commercial use.
- Takeaway: Cloudflare Pages (free, commercial OK, unlimited bandwidth) and Render free (with cold starts) are the genuinely-free rungs. Re-check at deploy time.

## LEARNING-001: The old site's base64 IDs and Dropbox links were the SEO and trust problem (2026-06-18)
- What happened: The legacy site used base64 query-string IDs (projects_id=MTI=) and raw Dropbox sample links.
- Takeaway: Clean slugs make projects indexable; in-site gated samples capture leads instead of leaking to link rot.
- How to apply: Features 003 and 004 fix these directly.
