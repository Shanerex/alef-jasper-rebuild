# Handoff: Admin Content Management — QA -> Dev (BLOCKED, new finding)

| Field | Value |
|---|---|
| Feature # | 012 |
| From / To | QA -> Dev |
| Status | **blocked** — both original bugs confirmed fixed; 1 new high-severity bug found live, testing.md status remains `draft` |
| Spec | specs/012-admin-content-management/testing.md (this pass, v1.1) |
| Prior | handoffs/6-dev-to-qa.md (bug-fix pass) |

## Summary

I independently re-verified handoffs/6-dev-to-qa.md's bug-fix pass against a real, freshly rebuilt stack — not by trusting the self-reported test/build numbers. `cd api && ./mvnw test` reproduced 157/157 independently. `npx tsc --noEmit` reproduced clean. I then did `docker compose down` and a genuinely fresh `docker compose up --build`, plus explicit `docker compose build --no-cache api` and `--no-cache web` to rule out any stale-layer false confidence, and drove the running containers with `curl`/`psql`/`redis-cli`, plus — now that `web` actually builds — a real headless-Chrome (Puppeteer against the installed system Chrome) session driving the actual login -> project list -> delete-confirm-dialog flow at both 1440px and 390px.

**Both original blocking bugs are confirmed fixed, independently, live:**
- **Bug #1** (CSRF scoping breaking public `POST /api/leads`): `curl -X POST http://localhost:8080/api/leads` with no CSRF header now returns `201` (was a CSRF-shaped `403`). Spot-checked `GET /api/projects`, `GET /api/trust/overview`, `GET /api/team`, `GET /api/offices` all still `200` with no auth/CSRF. Admin boundary unaffected (`GET /api/admin/session` unauth -> `401`; `POST /api/admin/projects` unauth/no-CSRF -> `403`).
- **Bug #2** (`next build` failing, blocking `docker compose up --build`): a `--no-cache` rebuild of `web` succeeds; the build's route table shows `/admin/login` as `○ (Static)` (prerendered); live browser load of `/admin/login` (with and without `?next=`) works, and the `?next=` redirect target is honored after a real login submit (confirmed via `page.url()` post-submit, not just an HTTP status).

**Both non-blocking concerns (#3 logout CSRF, #4 delete-button styling) are confirmed to have received the deliberate fixes claimed:**
- Real login->logout flow works invisibly end-to-end (prime CSRF -> login -> `GET /api/admin/session` 200 -> logout 204 -> `GET /api/admin/session` 401). A raw `curl -X DELETE /api/admin/session` with no cookies/token now returns `403` (was `401`) — matches DEC-030 exactly, not a regression.
- Screenshots confirm the Delete confirmation dialog's Delete button is now a distinct red/danger treatment (outlined on desktop, solid-filled on phone) — visually unmistakable from the gold primary CTAs on the same screen. Matches DEC-031.

**However, this pass is the first time the full `docker compose` service set and a real browser were both available, and it surfaced one new issue Dev's bug-fix pass did not touch or claim:**

## New Bug #3 (recommend blocking) — the `worker` service fails to start under a fresh `docker compose up --build`

`docker logs alef-rebuild-worker-1` after a clean `docker compose down && docker compose up --build -d` (worker builds from the same `--no-cache`-rebuilt `api` image, `SPRING_PROFILES_ACTIVE=worker` is the only difference from `api`) shows:

```
Error creating bean with name 'adminSecurityFilterChain' defined in class path resource
[com/alef/api/admin/security/AdminSecurityConfig.class]: Failed to instantiate [SecurityFilterChain]:
Factory method 'adminSecurityFilterChain' threw exception with message: No bean named
'A Bean named mvcHandlerMappingIntrospector of type
org.springframework.web.servlet.handler.HandlerMappingIntrospector is required to use MvcRequestMatcher.
Please ensure Spring Security & Spring MVC are configured in a shared ApplicationContext.' available
***************************
APPLICATION FAILED TO START
***************************
```
`docker inspect alef-rebuild-worker-1` confirms `exited exitCode=1`. No `restart:` policy is set for `worker` in `docker-compose.yml`, so it doesn't crash-loop — it just stays dead for the life of the `docker compose up` session.

**Root cause I traced (not fixed — not my role):** `api/src/main/resources/application.yml:76-78` sets `main.web-application-type: none` under the `worker` profile — a deliberate non-web Spring context, matching architecture's "worker: same Spring codebase, `worker` profile." `AdminSecurityConfig` is an unconditional `@Configuration @EnableWebSecurity` class with no `@Profile`/`@ConditionalOnWebApplication` guard, so it's still picked up by component scan inside the worker's context. Its `adminSecurityFilterChain()` bean method uses `.securityMatcher("/api/admin/**")` and string-path `.requestMatchers(...)` — because `spring-webmvc` is on the classpath (same fat jar as `api`), these resolve to `MvcRequestMatcher`, which needs a `HandlerMappingIntrospector` bean that only exists when Spring MVC's web infrastructure is actually configured — which it never is under `web-application-type: none`. Bean creation throws, the whole context (and container) dies.

I could not determine from git history whether this predates DEC-029's `.securityMatcher(...)` addition specifically — `AdminSecurityConfig.java` is untracked/uncommitted on this branch, no prior-commit diff exists to inspect. It's plausible this was already broken the moment `AdminSecurityConfig` was first introduced (the pre-existing string-path `.requestMatchers(...)` calls independently trigger the same `MvcRequestMatcher` resolution) — i.e. this may not be a regression from your bug-fix pass specifically, but it was never verified live by either QA pass until this one, since neither pass had previously started the `worker` container.

**Why the suite didn't catch it:** no test in the 157-test suite boots the Spring context with `spring.profiles.active=worker`.

**Impact today: zero functionally** — no consumer code exists yet for `lead.created`/`kb.changed` (feature 001 unbuilt, lead delivery deferred per DEC-018), so a worker that boots-and-does-nothing and a worker that never boots are currently indistinguishable from the outside. But this is a real, live, reproducible failure of a named architecture component ("worker") under the project's canonical bring-up command, ARCHITECTURE.md constraint #1 is explicit ("the full system runs with `docker compose up`"), and it isn't in CLAUDE.md's "known gaps" list — it would silently stay broken forever without this being flagged.

**Suggested fix direction (not prescriptive — your call):** exclude `AdminSecurityConfig` (and any other `/api/admin/**`-only `@Configuration`) from the non-web `worker` profile — e.g. `@ConditionalOnWebApplication(type = Type.SERVLET)` on the class, or `@Profile("!worker")` if that's the idiom already in use elsewhere. A regression test that boots the context with `spring.profiles.active=worker` (`@SpringBootTest(webEnvironment = NONE)` + that profile) would have caught this immediately and is worth adding either way.

## New Concern #6 (non-blocking, for the record) — admin routes inherit the public marketing nav/footer

`web/src/app/layout.tsx` (root layout) unconditionally wraps `{children}` in `<SiteNav />`/`<SiteFooter />`. There's no route-group override for `/admin/**`, so every admin screen — login included — renders with the full public marketing header (About/Services/Projects/Samples/Contact, "Enquire Now" CTA) on top. design.md §B.1 is explicit: *"The shell is visually distinct from the public site (no marketing nav/footer) so staff always know they are in the admin tool."* Confirmed live via screenshot; not met as written.

At 390px the effect is worse than cosmetic-only: the marketing `SiteNav` has no responsive treatment at all (no hamburger, full desktop link row regardless of viewport), so it visibly overlaps/truncates above the (correctly responsive) `AdminShell` content underneath. It doesn't block interaction with the admin content itself (confirmed via screenshot — the admin shell's own drawer and the list's table->card collapse both work independently and correctly), but it's a real, broken-looking header on every admin page on a phone.

No F12-ACn literally requires "no marketing chrome on admin routes," so — consistent with how F12-AC24's non-token delete-button color was treated in the prior pass (a real design.md deviation, flagged as CONCERN not FAIL since it didn't break the AC's literal text) — I'm not blocking on this one. Flagging so it's a conscious call, not an oversight, same as before.

**Suggested fix direction (not prescriptive):** move `SiteNav`/`SiteFooter` into a route group scoped to the public marketing routes only (e.g. `web/src/app/(marketing)/layout.tsx`), leaving the root layout to provide just fonts/`<html>`/`<body>`.

## What I verified live this pass beyond the 5-item checklist (closing prior gaps)

- Team write -> public-read round trip (F12-AC11), independently driven (create via admin API -> immediately visible on `GET /api/team` -> cleanup delete) — not just inferred from the shared mechanism proven for projects, as the prior pass had to do.
- Trust write -> public-read round trip (F12-AC15), independently driven (edited a stat to a sentinel value via `PUT /api/admin/trust/1` -> immediately visible on `GET /api/trust/overview` -> reverted).
- F12-AC27 (responsive usability) upgraded from code-only CONCERN to browser-verified PASS — screenshots at 1440px and 390px of the login screen, projects list (table<->card collapse), delete dialog, and admin shell drawer all match design §B.10.
- Spot-checked a representative subset of the prior pass's 27 PASS ids for regression: CORS allow/deny by origin, upload magic-byte 400 rejection, password-change wrong-current-password 400 + session-stays-valid-after, `kb.changed` create/delete payload shape and timing, DB row counts unchanged after my own test-data cleanup (15 projects, 10 team members — back to seed state). No regressions found in any of it.

## What I did not verify (and why)

- Did not attempt to fix or work around New Bug #3 (e.g., did not try an env override to get the worker to boot) — reporting only, per QA's role.
- `qdrant`/`ollama` still correctly out of scope (no LLM/RAG path in 012).
- 004's public Sample Explorer still correctly out of scope (DEC-023).

## Next step

Fix New Bug #3 (scope `AdminSecurityConfig` away from the non-web `worker` profile) and make a deliberate call on New Concern #6 (fix or explicitly accept, your/PM's call — same pattern as DEC-030/DEC-031 last round), then hand back to QA for a final pass. I did not fix either — that's explicitly not QA's role. Full evidence, screenshots-referenced findings, and the updated AC table are in `specs/012-admin-content-management/testing.md` (v1.1).
