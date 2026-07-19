# Handoff: Admin Content Management — Dev -> QA (bug-fix pass 2)

| Field | Value |
|---|---|
| Feature # | 012 |
| From / To | Dev -> QA |
| Status | ready — Concern #6 fixed and re-verified live; New Bug #3 deliberately deferred (owner call, DEC-032), not fixed |
| Prior | handoffs/7-qa-to-dev.md (blocked, 1 new bug + 1 new concern) |
| TypeScript check | `npx tsc --noEmit` exits 0, no output |
| Docker build | `docker compose build web` succeeds, route table unchanged (all URLs identical pre/post-move) |
| Spec | specs/012-admin-content-management/requirements.md (v1.1), architecture.md (v0.2), design.md (v0.2) |
| Acceptance IDs | F12-AC1 through F12-AC30 (unchanged) |
| Implementation doc | specs/012-admin-content-management/implementation.md ("Bug-fix pass 2" section) |
| Testing spec | specs/012-admin-content-management/testing.md (QA owns; not touched by this pass) |

---

## Context

handoffs/7-qa-to-dev.md confirmed both original bugs (Bug #1 CSRF scoping, Bug #2 `next build`) and both non-blocking concerns (#3 logout CSRF, #4 delete-button styling) from the first bug-fix pass fixed, live. It also surfaced two new findings from the first full-stack + real-browser pass:

- **New Bug #3:** the `worker` container fails to boot (`AdminSecurityConfig` has no profile guard, loads inside the worker's non-web context, crashes on `MvcRequestMatcher` resolution). QA recommended treating this as blocking.
- **New Concern #6:** every `/admin/**` page inherits the public marketing nav/footer from the root layout, contradicting design.md §B.1's "no marketing nav/footer" requirement, with a real overlap bug at 390px.

## What was done

### New Bug #3 — deliberately NOT fixed this pass

The owner reviewed QA's root-cause analysis and made the call that this is not blocking 012's close: the `worker` service is not run in this environment, and a worker that fails to boot is functionally indistinguishable from a worker that boots and does nothing, since no consumer exists yet for `lead.created`/`kb.changed` (feature 001 unbuilt). Recorded as **DEC-032** (docs/knowledge/DECISIONS.md) and **PITFALL-009** (docs/knowledge/LEARNINGS.md, documenting the general gotcha for whoever fixes it later). Added to CLAUDE.md's known-gaps list so it stays visible. QA's suggested fix direction (`@Profile("!worker")` or `@ConditionalOnWebApplication`) is recorded but not applied — this must be fixed before feature 001 needs the `worker` process to actually run, not before 012 closes.

**Please do not re-flag this as blocking** — it's a conscious, documented deferral (DEC-032), same pattern as DEC-018 (lead delivery deferred) and DEC-023 (sample table ahead of its consumer feature). Feel free to re-verify DEC-032's stated rationale still holds (i.e. confirm no worker consumer code has appeared) but the underlying `worker`-boot-failure defect itself doesn't need re-testing — nothing about it changed this pass.

### New Concern #6 — fixed

**Root cause:** `web/src/app/layout.tsx` (root layout) unconditionally wrapped `{children}` in `<SiteNav />`/`<SiteFooter />` with no route-group override, so every route in the app — including every `/admin/**` screen — inherited the full public marketing header/footer.

**Fix:** moved every public page into a new route group and moved the nav/footer components with it, so the root layout structurally cannot leak marketing chrome onto admin routes (not a per-route opt-out that could be missed later):

- `git mv` of `web/src/app/{page.tsx,about,contact,projects,services}` into `web/src/app/(marketing)/`. Route groups don't affect the URL, confirmed by an unchanged `next build` route table (`/`, `/about`, `/contact`, `/projects`, `/projects/[slug]`, `/services` all identical).
- New `web/src/app/(marketing)/layout.tsx` holds `SiteNav`/`SiteFooter` (moved verbatim, no visual changes) plus `<main>{children}</main>`.
- `web/src/app/layout.tsx` (root) now only provides `<html>`/`<body>`/font links/`metadata` — no nav, no footer, no `<main>` wrapper of its own.
- `web/src/middleware.ts` untouched and unaffected — it matches on URL path (`/admin/:path*`), not file location, so the route-group move doesn't change its behavior.
- `web/src/app/admin/**` (login page + the `(protected)` group's `AdminShell`) untouched — they were already outside any nav/footer wrapper structurally; the fix was entirely on the marketing side.

**Live re-verification** (fresh `docker compose up -d postgres redis api web`):
```
curl -s http://localhost:3000/about | grep -o "Enquire Now"        → Enquire Now (present, as expected)
curl -s http://localhost:3000/admin/login | grep -o "Enquire Now"  → (empty — absent, as expected)
curl -s http://localhost:3000/about | grep -o "ALEF Architectural &amp; Cadding Services LLC"       → present
curl -s http://localhost:3000/admin/login | grep -o "ALEF Architectural &amp; Cadding Services LLC" → (empty — absent)
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/          → 200
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/projects  → 200
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/admin     → 307 (redirect to /admin/login, middleware unaffected)
```

Recorded as an addendum to the "Bug-fix pass 2" section of implementation.md (no new DEC needed — this is a straightforward fix matching QA's own suggested direction, not a tradeoff).

---

## What the receiving role must do

1. Independently re-verify Concern #6 is fixed, live, per your own standard (not just trusting this handoff) — ideally including the 390px overlap check you originally screenshotted, to confirm the marketing nav no longer renders at all on `/admin/**` (not just that it no longer overlaps).
2. Confirm `docker compose build web` / `docker compose up --build` still succeed end-to-end and no route URLs changed (compare against your prior pass's route table if you kept it).
3. Re-run a browser-driven pass over `/admin/login` and at least one `(protected)` screen (e.g. `/admin/projects`) at both 1440px and 390px to confirm no marketing chrome anywhere in the admin tree, and that `AdminShell`'s own responsive behavior (drawer, table->card collapse) is unaffected by the layout restructure.
4. Do **not** re-verify or re-block on New Bug #3 (worker boot failure) — it's deliberately deferred per DEC-032. If you disagree with that call, raise it explicitly rather than silently re-blocking on it.
5. If everything above passes, this should be the pass that lets 012 close — hand off to PM rather than back to Dev.

## Read first

- docs/knowledge/DECISIONS.md — DEC-032 (new)
- docs/knowledge/LEARNINGS.md — PITFALL-009 (new)
- specs/012-admin-content-management/implementation.md — "Bug-fix pass 2" section
- web/src/app/layout.tsx, web/src/app/(marketing)/layout.tsx — the actual fix

## Do not touch

- `AdminSecurityConfig` / the `worker` profile boot failure — explicitly out of scope for this pass (DEC-032).
- Anything QA already marked PASS in handoffs/7-qa-to-dev.md / testing.md v1.1 that this handoff doesn't mention — untouched.

---

## Environment setup

Unchanged:

```bash
make infra-up   # postgres, qdrant, redis, ollama
cd api && ./mvnw spring-boot:run   # API on :8080
cd web && npm run dev              # Web on :3000
```

Or `docker compose up --build`. Seed admin credential (docker-compose.yml): `ADMIN_USERNAME=admin`, password `changeme123`.
