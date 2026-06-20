# LEARNINGS.md

> Append-only. Newest first. Patterns that worked, pitfalls, workarounds.

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
