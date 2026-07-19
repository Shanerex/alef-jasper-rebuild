# Handoff: Admin Content Management — Dev -> QA (bug-fix pass 4, reopens QA approval again)

| Field | Value |
|---|---|
| Feature # | 012 |
| From / To | Dev -> QA |
| Status | ready for re-verification — reopens handoffs/12-qa-to-pm.md's "ready to close" call; one real bug found post-approval and fixed |
| Prior | handoffs/12-qa-to-pm.md (QA approved, testing.md v1.4, routed to PM) |
| TypeScript check | `npx tsc --noEmit` exits 0, no output |
| Docker build | `docker compose build web` succeeds |
| Spec | specs/012-admin-content-management/requirements.md (v1.1), architecture.md (v0.2), design.md (v0.2) |
| Acceptance IDs | F12-AC20..AC22 (uploads) — re-check specifically; also touches any screen that renders an uploaded photo/image (team card, project card/detail, admin previews) |
| Implementation doc | specs/012-admin-content-management/implementation.md ("Bug-fix pass 4" section) |
| Testing spec | specs/012-admin-content-management/testing.md (QA owns; not touched by this pass) |

---

## Context

handoffs/12-qa-to-pm.md's "ready to close" verdict (testing.md v1.4, approved) was reached concurrently with, and without knowledge of, a manual click-through that uploaded an actual team photo through the running admin console and found it never rendered — not in the admin form's own preview, and not on the public About page. **This is the same pattern as bug-fix pass 3: a defect invisible to API-level checks, only visible when a real browser actually renders the result.** QA's own prior "public re-serve" verification (handoffs/5-qa-to-dev.md and later passes) confirmed `GET /uploads/**` works when hit directly — it never rendered the returned URL inside an actual `<img>` tag in a browser, which is exactly where this broke.

## What was found and done

### Bug (real, fixed): uploaded images never rendered, anywhere

**Root cause:** `UploadService.store()` (api) returns a root-relative URL (`/uploads/<category>/<uuid>.<ext>`) — correct as a path *on the api service*, since that's where `UploadResourceConfig` serves it from. Every place that rendered an uploaded value used a plain `<img src={...}>` with that relative path verbatim. A relative `src` resolves against the *current page's* origin — for any page served by `web` (`http://localhost:3000`), that's the wrong origin (the file lives on `api`, `http://localhost:8080`), so the image request 404s. `curl`ing the same path against the API directly always worked, which is why API-level checks never caught it. It was invisible until now because every seeded team member ships with `photo: null` and seeded project images are static assets under `web/public` (never routed through `/uploads/`) — no test, human or automated, had ever rendered a genuinely-uploaded image before this pass.

**Fix:** added `resolveUploadUrl()` to `web/src/lib/utils.ts` (prefixes only `/uploads/`-prefixed paths with `NEXT_PUBLIC_API_BASE_URL`, the existing browser-visible API origin env var; leaves everything else untouched). Applied at all four render sites: `components/admin/upload-field.tsx` (admin preview, shared by every resource form), `components/team/team-card.tsx`, `components/projects/project-card.tsx`, `components/projects/project-detail.tsx`. Full detail in implementation.md's "Bug-fix pass 4" section; the general gotcha is recorded as **PITFALL-011**.

**Re-verification done:** `tsc --noEmit` clean; `docker compose build web` succeeds; live headless-browser test: uploaded a real PNG to a team member through the actual admin form, confirmed the admin preview thumbnail rendered correctly (`naturalWidth > 0`, not broken); confirmed on the public About page that an existing team member's real (pre-existing, ~4MB) uploaded photo renders correctly end-to-end (`naturalWidth: 3798` after allowing for `loading="lazy"` to trigger — the first read showed `naturalWidth: 0` purely because the image is below the fold and hadn't lazy-loaded yet in a scripted, unscrolled page load; scrolling and waiting confirmed it loads fine, this is expected `loading="lazy"` behavior, not a bug). Test data (the uploaded photo on team id 1) was reverted back to `photo: null` afterward.

---

## What the receiving role must do

1. Independently re-verify the fix live, in a real browser — upload an actual image file to at least one resource type (team is the easiest to check end-to-end since the About page renders it directly; project image is worth spot-checking too) and confirm the resulting `<img>` actually renders (not a broken-image icon), both in the admin form's own preview and on the corresponding public page. Check the browser console/network tab for the actual resolved `src` — it should now be `http://localhost:8080/uploads/...`, not a bare `/uploads/...` path.
2. If checking the About page (or any page using `loading="lazy"`), scroll the element into view before judging whether an image loaded — a lazy image that hasn't entered the viewport yet will correctly show `naturalWidth: 0` with no network request at all; that's not a regression, don't misread it as one.
3. Spot-check the sample and project upload flows too (same shared `UploadField` component, same fix) — a project image upload через `/admin/projects/[id]` is the most direct check given F12-AC4/AC5 were also the subject of the last pass.
4. Clean up any test uploads/records you create.
5. Do not re-verify anything from prior passes' scope (worker boot failure/DEC-032, marketing nav/footer, team hide cache-latency, or the project-CRUD CORS fix from bug-fix pass 3, already independently confirmed in handoffs/12-qa-to-pm.md) — all unaffected by this pass.
6. If clean, testing.md can move back to `approved` and this should route to PM again.

## Read first

- docs/knowledge/LEARNINGS.md — PITFALL-011 (new)
- specs/012-admin-content-management/implementation.md — "Bug-fix pass 4" section
- web/src/lib/utils.ts (`resolveUploadUrl`), and its four call sites listed above

## Do not touch

- Everything already confirmed in handoffs/12-qa-to-pm.md (project CRUD, trust/samples screens, worker deferral, nav/footer fix) — out of scope, not re-litigated here.

---

## Environment setup

Unchanged: `docker compose up --build` (or `make infra-up` + `./mvnw spring-boot:run` + `npm run dev`). Seed admin credential: `ADMIN_USERNAME=admin`, password `changeme123`.
