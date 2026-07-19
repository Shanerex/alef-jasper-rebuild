---
feature: 012-admin-content-management
spec_id: "012"
phase: design
owner: Architect
status: approved
version: "0.2"
entry_criteria:
  - architecture.md drafted (this feature, v0.2)
exit_criteria:
  - Dev can build the admin API and UI from this file without making design decisions
---

# Design: Admin Content Management

This file specifies the concrete API contracts (request/response DTOs, validation, status codes, edge cases) and the admin UI/UX (screens, navigation, forms, list views, delete-confirmation, validation display, upload UX, password change, responsive behavior). It is the buildable companion to `architecture.md`; read that first for the decisions and rationale (DEC-021..DEC-026; DEC-021 and DEC-026 amended for requirements v1.1).

## Change log (v0.1 → v0.2)

- **New endpoint:** `POST /api/admin/password` — self-service password change (F12-AC28, F12-AC29), §A.7.
- **New screen:** `/admin/account` — change-password UI (§B.9).
- **Responsive (F12-AC27):** the "desktop-only" note is removed. New §B.10 specifies per-screen behavior at phone/tablet widths (shell drawer, list table→stacked cards, single-column forms, mobile upload). Part C updated.
- **Themeable tokens (F12-AC30):** the aesthetic section now mandates named design tokens, no hard-coded hex, as the enabler for the sibling spec 013 light/dark toggle.

**Aesthetic (decision / DEC-026, amended):** the admin uses a **functional dark admin theme** — the public palette tokens (`#07111C` page, `#0E2035` surface, `#C4973A` gold for primary actions/focus, `#E8E0D0` text, `#7A8FA8` muted) and **Montserrat** for all UI, but standard form/table density, `border-radius` allowed on controls, plain spacing, no hero patterns/serif display headlines. Reuse existing shadcn/ui primitives (`Button`, `Select`, `Separator`, `Badge`).

**Themeable design tokens — no hard-coded hex (F12-AC30):** every color in the admin UI **must** be referenced through the existing named design tokens exposed in `web/tailwind.config.ts` — `bg-page`, `bg-surface-1/2/3/4`, `text-text-primary`, `text-text-secondary`, `text-text-muted`, `text-gold` / `bg-gold` / `border-gold-subtle` / `bg-gold-ghost`, `bg-footer`, `text-slate`, etc. **No raw hex literal, no arbitrary-value color (`bg-[#0E2035]`, `text-[#C4973A]`), and no inline `style` color is allowed anywhere in admin components.** The hex values quoted above are the *token definitions*, not values to type into markup. Today those tokens resolve to static hex; funneling every admin color through the token layer is the precondition that lets **spec 013** (a sibling, non-blocking feature) introduce a CSS-variable indirection and a light/dark switch behind the same token names with **no admin-component rework**. 012 ships one (dark) theme via tokens; **012 does not build the toggle or the variable layer — that is 013's deliverable.**

**Fully responsive (F12-AC27):** the admin is usable on phone, tablet, and desktop. Breakpoints match the public app's Tailwind conventions (`sm` 640, `md` 768, `lg` 1024 — the portfolio grid already uses `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`). Per-screen behavior is specified in §B.10; each screen section below also notes its responsive collapse.

---

## Part A — API contracts

All admin endpoints: base `/api/admin`, require `ROLE_ADMIN`, JSON in/out, RFC 9457 `ProblemDetail` on error (matching `LeadExceptionHandler`). Mutating requests require the `X-XSRF-TOKEN` header (CSRF).

### Common error shapes

| Case | Status | Body |
|---|---|---|
| Unauthenticated request to `/api/admin/**` | **401** | `ProblemDetail` `title:"Unauthorized"` |
| Authenticated but CSRF token missing/invalid | **403** | `ProblemDetail` `title:"Forbidden"` |
| Bean-validation failure | **400** | `ProblemDetail` `title:"Validation Failed"`, extension `fields: { fieldName: [messages] }` (identical to `LeadExceptionHandler`) |
| Unique-key conflict (slug / item_key already exists) | **409** | `ProblemDetail` `title:"Conflict"`, `detail` names the field |
| Unknown `{id}` on GET/PUT/DELETE | **404** | `ProblemDetail` `title:"Not Found"` |
| Upload too large | **413** | `ProblemDetail` `title:"Payload Too Large"` |
| Upload wrong type | **400** | `ProblemDetail` `title:"Unsupported File Type"` |

The `fields` extension map lets each form map a server error back to its input, exactly as the contact form does today.

### A.1 Session / auth

**`POST /api/admin/session`** — login (F12-AC1).
Request: `{ "username": "admin", "password": "..." }`
- 200: `{ "authenticated": true, "username": "admin" }` + `Set-Cookie: ALEFADMIN=...; HttpOnly; SameSite=Lax; Secure(prod); Path=/`
- 401: bad credentials → `ProblemDetail` `title:"Invalid Credentials"`, `detail:"Username or password is incorrect."` (do **not** reveal which one).

The credential is validated against the persisted `admin_credential` row (seeded from env on first boot, mutable via §A.7 — architecture §2.1.1), not against env directly.

**`GET /api/admin/session`** — current session (used by web middleware & UI on load).
- 200: `{ "authenticated": true, "username": "admin" }`
- 401: no/expired session.

**`DELETE /api/admin/session`** — logout.
- 204: session invalidated, cookie cleared.

Edge cases: expired session → 401 on any admin call; the web client, on any 401, drops to `/admin/login`. Login is **rate-limited** (reuse the existing Redis per-IP pattern from `LeadService`: e.g. 10 attempts/IP/15 min → 429 with `Retry-After`) to blunt brute force — this is an auth-hardening requirement (production-readiness bar).

### A.7 Change password (F12-AC28, F12-AC29)

**`POST /api/admin/password`** — the single admin changes their own *known* password in-app. Authenticated (`ROLE_ADMIN`) + CSRF (`X-XSRF-TOKEN`).

Request:
```json
{ "currentPassword": "…", "newPassword": "…" }
```
(`confirmNewPassword` is a UI-only field — the server takes current + new; see §B.9.)

Behaviour:
1. Validate `currentPassword` against the stored BCrypt hash (`BCryptPasswordEncoder.matches`). **Mismatch → 400** `title:"Invalid Credentials"`, `fields: { currentPassword: ["Current password is incorrect."] }`. This is a request-validation failure, **not** a session failure — the caller stays logged in (no 401, session untouched).
2. Validate `newPassword` policy — **non-blank, minimum 12 characters, must differ from `currentPassword`**. Violation → **400** `title:"Validation Failed"`, `fields.newPassword` (e.g. `["Must be at least 12 characters.", "Must differ from the current password."]`).
3. On pass: re-hash `newPassword` with BCrypt, `UPDATE admin_credential SET password_hash=?, updated_at=now() WHERE id=1`, and return **204**.
4. **Never log or echo either password** in plaintext (F12-AC29) — no request-body logging on this route; error responses reference field names, never values.

Effect: the new password **takes effect on next login** (F12-AC28). The current session remains valid (no forced re-login in v1). Errors: 401 if no session, 403 if CSRF token missing/invalid.

**Not covered here (v1 Out of scope):** a *forgotten*-password self-service reset. Because env seeds the credential first-boot-only, a fully forgotten password is an **ops action** — an operator updates the `admin_credential` row with a freshly generated BCrypt hash (or clears the row and lets the seeder repopulate from env on restart). No API surface, no email flow in v1; deferred to v2 (architecture §2.1.1).

### A.2 Projects (F12-AC4..AC7)

**Full admin DTO** (`AdminProjectDto`) — the complete row, unlike the public summary/detail projections:
```json
{
  "id": 42,
  "slug": "doha-metro-gold-line",
  "name": "Doha Metro - Gold Line",
  "sector": "infrastructure_rail",
  "country": "Qatar",
  "status": "completed",
  "image": "/uploads/projects/9f3c1e2a.jpg",
  "description": "Rebar detailing and BBS for ...",
  "mainContractor": "…",
  "client": "…",
  "consultant": "…",
  "location": "Doha, Qatar",
  "scope": ["rebar", "BBS", "GA"],
  "featurable": true,
  "createdAt": "2026-06-22T09:00:00Z",
  "updatedAt": "2026-07-12T10:15:30Z"
}
```

`AdminProjectRequest` (POST/PUT body) — same fields minus `id`/`createdAt`/`updatedAt`.

| Endpoint | Behaviour |
|---|---|
| `GET /api/admin/projects?page=&size=` | Paged list of `AdminProjectDto` (all fields, incl. non-featurable). Reuse the `PagedResponse` envelope. |
| `GET /api/admin/projects/{id}` | Single `AdminProjectDto`; 404 unknown id. |
| `POST /api/admin/projects` | Create. 201 + created DTO. **Emits `kb.changed` `created`.** |
| `PUT /api/admin/projects/{id}` | Full replace. 200 + updated DTO; sets `updatedAt`. 404 unknown id. **Emits `kb.changed` `updated`.** |
| `DELETE /api/admin/projects/{id}` | 204. 404 unknown id. **Emits `kb.changed` `deleted`.** |

**Validation (F12-AC23):**
- `slug` **required**, unique (409 on conflict), pattern `^[a-z0-9]+(-[a-z0-9]+)*$` (clean slug, LEARNING-001). On PUT, uniqueness excludes the row itself.
- `name` **required**, non-blank.
- `sector` **required**, ∈ `Sector` vocabulary (400 otherwise — reuse `InvalidFilterValueException`/vocab-validation stance, DEC-009).
- `status` **required**, ∈ `{ongoing, completed}`.
- `country` **required**, non-blank.
- `scope` optional, each value ∈ the scope vocabulary (`rebar, BBS, GA, MEP, QS, as-built`).
- `featurable` optional, default `false`.
- credit/narrative fields (`image, description, mainContractor, client, consultant, location`) optional (nullable columns).

**Edge cases:** editing `slug` changes the public URL and the `kb.changed` key — allowed (it is an `updated` event carrying the new slug); no old-slug redirect in v1. Deleting a `featurable` project silently reduces the derived marquee count (correct, single source of truth).

### A.3 Team (F12-AC8..AC11)

`AdminTeamDto` / `AdminTeamRequest`:
```json
{ "id": 3, "name": "K. Jeyaraman", "role": "Managing Director",
  "company": "ALEF", "email": "…", "photo": "/uploads/team/uuid.jpg",
  "displayOrder": 0, "active": true }
```

| Endpoint | Behaviour |
|---|---|
| `GET /api/admin/team` | All rows **including `active=false`**, ordered by `displayOrder`. |
| `GET /api/admin/team/{id}` | Single; 404 unknown. |
| `POST /api/admin/team` | Create; 201. |
| `PUT /api/admin/team/{id}` | Update (incl. `active`, `displayOrder`); 200. |
| `DELETE /api/admin/team/{id}` | Hard delete; 204. |

Validation: `name` **required**; `role` **required**; `company`, `email` (valid email format if present), `photo` optional; `displayOrder` integer (default 0); `active` boolean (default true). No `kb.changed` (team not in v1 RAG scope).

Edge case: the UI **defaults the "remove" action to soft-hide** (`active=false` via PUT), presenting hard delete as a secondary, confirmed action (DEC-020 preserves history).

### A.4 Trust content (F12-AC12..AC15)

`AdminTrustRow` / `AdminTrustRequest`:
```json
{ "id": 7, "itemKey": "years_in_business", "itemType": "stat",
  "label": "Years in Business", "value": "18", "unit": null, "displayOrder": 0 }
```

**`GET /api/admin/trust`** returns rows grouped by type **plus** read-only derived facts for context (DEC-025):
```json
{
  "stats":     [ { "id":1, "itemKey":"years_in_business", ... } ],
  "software":  [ { "id":4, "itemKey":"autocad", "label":"AutoCAD", "value":null, ... } ],
  "standards": [ { "id":7, "itemKey":"bs_8666", "label":"BS 8666", ... } ],
  "derived": {
    "marqueeProjectCount": 12,
    "clients": ["Six Construct", "ALEC", "…"],
    "contractors": ["…"],
    "note": "These values are derived live from Projects and are edited via the Projects section."
  }
}
```

| Endpoint | Behaviour |
|---|---|
| `POST /api/admin/trust` | Create a stat/software/standard row; 201. |
| `PUT /api/admin/trust/{id}` | Update `label`/`value`/`unit`/`displayOrder` (and `itemKey`/`itemType` if needed); 200. |
| `DELETE /api/admin/trust/{id}` | Delete; 204. |

Validation: `itemKey` **required**, unique (409); `itemType` **required** ∈ `{stat, software, standard}` (`ItemType` vocabulary); `label` **required**; `value` required for `stat`, may be null for `software`/`standard` badges (mirrors 005: badge rows carry only a label); `unit` optional; `displayOrder` integer. The `derived` block is **not** writable — attempting to POST/PUT a marquee/client/contractor value is simply out of the schema.

### A.5 Samples (F12-AC16..AC19)

`AdminSampleDto` / `AdminSampleRequest`:
```json
{ "id": 2, "slug": "prequalification-profile", "title": "Prequalification Profile",
  "category": "prequalification", "preview": "/uploads/samples/previews/uuid.png",
  "file": "/uploads/samples/files/uuid.pdf", "displayOrder": 0 }
```

| Endpoint | Behaviour |
|---|---|
| `GET /api/admin/samples` | All rows, ordered by `displayOrder`. |
| `GET /api/admin/samples/{id}` | Single; 404 unknown. |
| `POST /api/admin/samples` | Create; 201. |
| `PUT /api/admin/samples/{id}` | Update; 200. |
| `DELETE /api/admin/samples/{id}` | Delete; 204. |

Validation: `slug` **required**, unique (409), clean-slug pattern; `title` **required**; `category` **required** ∈ the five-book vocabulary (`prequalification, bbs, drawings, bridge_drawings, roads_utility` — app-validated, DEC-009); `preview`, `file` optional (attach later). No `kb.changed` in v1.

### A.6 Uploads (F12-AC20..AC22)

**`POST /api/admin/uploads`** — multipart form.
Parts: `file` (the binary), `category` ∈ `{project-image, team-photo, sample-preview, sample-file}`.

- 201:
```json
{ "url": "/uploads/projects/9f3c1e2a.jpg",
  "filename": "9f3c1e2a.jpg", "contentType": "image/jpeg", "size": 481234 }
```
- Validation:
  - `category` required (400 if missing/unknown).
  - Image categories (`project-image`, `team-photo`, `sample-preview`): content-type ∈ `{image/jpeg, image/png, image/webp}`, extension matches, magic-byte sniff passes, size ≤ **5 MB**.
  - Document category (`sample-file`): content-type `application/pdf`, `%PDF` magic bytes, size ≤ **25 MB**.
  - Wrong type → **400** `title:"Unsupported File Type"`, `detail` lists allowed types. Too large → **413** (Spring multipart cap is the first gate; the service re-checks the per-category limit).
- Server writes to `${ALEF_UPLOAD_DIR}/<subfolder>/<uuid>.<ext>` (subfolder by category), returns the public `/uploads/...` path. **The returned `url` is then submitted by the caller as the record's `image`/`photo`/`preview`/`file` field on the next save** — upload and record-save are separate steps (architecture §2.4/§3.5).
- **Mobile (F12-AC27):** the endpoint is source-agnostic — on a phone the same multipart POST is fed by the OS file picker / camera roll (an `<input type="file" accept="image/*">`, optionally `capture` for a live photo). No desktop-only assumption; see §B.5/§B.10.

**Static serving:** `GET /uploads/**` (public, unauthenticated) returns the stored bytes via the Spring resource handler mapped to `ALEF_UPLOAD_DIR`. Read-only; no directory listing.

---

## Part B — Admin UI / UX

### B.1 Route map (`web/src/app/admin/**`)

| Route | Screen |
|---|---|
| `/admin/login` | Login form (public; the only unguarded admin route) |
| `/admin` | Dashboard — links + counts (projects, team, trust items, samples) |
| `/admin/account` | Account — change password (§B.9) |
| `/admin/projects` | Projects list |
| `/admin/projects/new` | Create project form |
| `/admin/projects/[id]` | Edit project form |
| `/admin/team` | Team list |
| `/admin/team/new`, `/admin/team/[id]` | Create / edit team member |
| `/admin/trust` | Trust editor (three grouped tables + read-only derived panel) |
| `/admin/trust/new`, `/admin/trust/[id]` | Create / edit trust row |
| `/admin/samples` | Samples list |
| `/admin/samples/new`, `/admin/samples/[id]` | Create / edit sample |

**Middleware guard (F12-AC1):** `web` middleware matches `/admin/:path*` except `/admin/login`; if the `ALEFADMIN` cookie is absent it redirects to `/admin/login?next=<path>`. Presence-check only; the API's 401 is authoritative. On any API 401 during use, the client redirects to `/admin/login`.

**Admin shell:** a persistent left sidebar (Dashboard, Projects, Team, Trust, Samples) + a top bar showing the logged-in username, a link to **Account** (change password), and a **Logout** button (calls `DELETE /api/admin/session`, then routes to `/admin/login`). The shell is visually distinct from the public site (no marketing nav/footer) so staff always know they are in the admin tool. On phone/tablet the sidebar collapses to a drawer (see §B.10).

### B.2 Login screen (`/admin/login`)

- Centered card on the dark page background (token `bg-page`, card `bg-surface-2`). Heading "ALEF Admin" (Montserrat, not serif display). Two fields — Username, Password (both required) — and a primary gold "Sign In" button. The single centered-card layout is naturally responsive: it scales from phone to desktop with horizontal padding and a max card width (see §B.10).
- Submit → `POST /api/admin/session` with `credentials:'include'`. On 200, route to `next` or `/admin`. On 401, show an inline error banner "Username or password is incorrect." (no field-level leak of which). On 429, show "Too many attempts, try again later."
- No "forgot password"/registration in v1 (single credential; a fully-forgotten password is an ops action — §A.7). A link to `/admin/account` is **not** shown here (changing a password requires being logged in).

### B.3 List views (Projects / Team / Samples)

A consistent pattern (desktop table; collapses to stacked cards on phone — §B.10):
- **Header row:** section title + a primary gold **"＋ New …"** button (routes to `…/new`).
- **Table** (dark surface `bg-surface-2`, subtle `border-gold-subtle` row dividers, Montserrat):
  - **Projects:** columns = Name, Sector, Country, Status (Badge — gold for ongoing, outline for completed), Featured (a star/check), and a right-aligned **Actions** cell (Edit, Delete).
  - **Team:** columns = Photo thumbnail (or serif-monogram fallback when `photo` null), Name, Role, Company, Active (badge; muted when inactive), Actions.
  - **Samples:** columns = Title, Category, Preview (thumb / "—"), File (a "PDF" chip / "—"), Actions.
- **Empty state:** a muted "No … yet — create the first one." with the New button.
- Row **Edit** → the edit form; **Delete** → the confirmation dialog (B.6).
- Team list additionally offers an inline **Active** toggle (soft-hide, PUT) as the recommended alternative to deletion.

### B.4 Create / edit forms (Projects, Team, Samples, Trust row)

Shared form conventions (functional theme; single-column on phone — §B.10):
- Inputs use the design-system form tokens (bg `bg-surface-2`, border `border-gold-subtle`, focus border `focus:border-gold`) but at comfortable data-entry density; labels in Montserrat uppercase gold (`text-gold`); `border-radius` allowed. **All colors via tokens — no hard-coded hex** (F12-AC30).
- On wide screens, related fields may sit in a **2-column grid** (`grid-cols-1 md:grid-cols-2`) to reduce vertical scroll; at `< md` they stack to a single column (§B.10). Textareas and the upload widget always span the full width.
- **Required fields** marked with a gold asterisk. **Client-side validation** blocks submit and shows inline messages; **server-side** 400 `fields` map is merged onto the same inputs (F12-AC23) so nothing saves with a missing required field. A top-of-form error summary appears on any server error.
- Vocabulary fields use the shadcn `Select` populated from the vocabulary (sector/status from `GET /api/projects/filters`; sample category from a static list mirroring the app vocabulary; trust `itemType` from a fixed list). This keeps the admin from typing an invalid enum.
- **Save** (primary gold) and **Cancel** (secondary, back to list) buttons; Save disabled while submitting. On success → toast "Saved" + return to the list.

**Project form fields:** slug*, name*, sector* (Select), country*, status* (Select), scope (multi-select of the scope vocabulary), featurable (toggle — with helper text "Shows on the home marquee and counts toward the trust 'marquee projects' stat"), image (upload widget, B.5), description (textarea), mainContractor, client, consultant, location.
- Helper note near client/contractor: "Client and contractor names shown in the site's trust strip come from these fields across all projects."

**Team form fields:** name*, role*, company, email, photo (upload widget), displayOrder (number), active (toggle, default on).

**Sample form fields:** slug*, title*, category* (Select), preview (image upload widget), file (document upload widget), displayOrder.

**Trust row form fields:** itemType* (Select: Stat / Software / Standard), itemKey*, label*, value (required when type=Stat, hidden/optional for badges — the form adapts to the selected type), unit (shown for stats), displayOrder.

### B.5 Upload widget UX (F12-AC20, F12-AC22)

An inline component reused by the image/file fields (works on desktop and mobile — §B.10):
- Shows the **current** asset (image thumbnail, or a "PDF ✓" chip for files, or "None") plus a **"Choose file"** button and an optional **"Remove"** (sets the field to null on save).
- The trigger is a native `<input type="file">` with an `accept` filter matching the category (`image/jpeg,image/png,image/webp` or `application/pdf`). On phone/tablet this invokes the OS **file picker / camera roll**; image inputs may set `capture` to allow taking a photo directly. Nothing about the flow assumes a desktop drag-and-drop; drag-and-drop can be an additive desktop enhancement but is not required.
- On selection: client pre-checks type & size against the category limits and shows an immediate error if wrong ("PNG, JPG or WEBP up to 5 MB" / "PDF up to 25 MB") **before** uploading.
- On valid selection: `POST /api/admin/uploads` (multipart, with the field's `category`), shows a progress/spinner state; on 201 stores the returned `url` into the form field and swaps the preview to the new asset. On 400/413 shows the server error message inline (F12-AC22).
- The record is **not** saved by uploading — the returned URL is held in form state and persisted only when the admin clicks **Save** on the record form.

### B.6 Delete confirmation (F12-AC24)

Every destructive action opens a **modal confirmation dialog** — no one-click deletes:
- Title: "Delete this <thing>?" Body names the record ("Delete project 'Doha Metro – Gold Line'? This removes it from the public site immediately and cannot be undone.").
- Buttons: **Cancel** (default focus) and a **destructive Delete** (visually distinct — an outline/red-tinted button via a token, not the primary gold, so it can't be hit by muscle memory).
- Only on explicit confirm does the client call `DELETE …/{id}`. On success → toast + remove the row. For **team**, the dialog also surfaces the softer option: "Prefer to hide instead? Set the member inactive." with a link back to the toggle.
- This is the safeguard against "accidental deletion silently breaking the public site" (F12-AC24). For projects, the confirm copy notes the public-site + marquee/RAG impact.
- On phone the dialog is a centered sheet with full-width stacked buttons (§B.10) — the safeguard holds at every width.

### B.7 Validation & error display (F12-AC23)

- **Field level:** inline message under the input, red text (via a token, not raw hex); the input border turns red. Driven by both client rules and the server `fields` map.
- **Form level:** a summary banner at the top of the form on any submit error ("Please fix the fields below" or a 409 "That slug is already used").
- **Global:** a toast for network/500 errors ("Something went wrong, please retry"). A 401 anywhere routes to `/admin/login`.
- Required-field enforcement is **both** client (blocks submit) and server (400) — the client convenience never replaces the server guard.

### B.8 Freshness feedback

After a successful save/delete, the admin can trust the change is live: under `force-dynamic` the public page reflects it on next load (architecture §2.6). The success toast may include a "View on site →" link to the affected public page (e.g. the project detail) so staff can verify. No manual "publish" step exists (no workflow in v1 — Out of scope).

### B.9 Account — change password (F12-AC28, F12-AC29)

Screen at `/admin/account`, reachable from the top bar. A single centered form card (naturally responsive; single-column at every width):
- Three fields, all required, all `type="password"`:
  - **Current password**
  - **New password**
  - **Confirm new password**
- **Client validation before submit:** New ≥ 12 characters; New must equal Confirm; New must differ from Current. Inline messages under the offending field; Submit disabled until the client rules pass. (A password-strength hint line — e.g. "At least 12 characters." — sits under the New field.)
- Submit → `POST /api/admin/password` with `{ currentPassword, newPassword }` (the Confirm field is UI-only, not sent), `credentials:'include'`, `X-XSRF-TOKEN` header.
- **Success (204):** show a success toast "Password changed. Use it next time you sign in." Clear the fields. The current session stays valid (F12-AC28: takes effect on next login) — the admin is **not** logged out.
- **Error states:**
  - **400 wrong current password** → inline error on the Current field: "Current password is incorrect." (mapped from `fields.currentPassword`). No leak of the stored value; the session is untouched.
  - **400 policy failure** (e.g. server-side length) → inline error on the New field from `fields.newPassword`.
  - **403 CSRF** → global toast "Session problem, please sign in again," then route to `/admin/login`.
  - **429** (if the change route is rate-limited like login) → "Too many attempts, try again later."
- **No plaintext handling beyond the request:** fields are `type="password"`, values are never logged client-side, and are dropped from memory after submit (F12-AC29).
- Styling follows DESIGN_SYSTEM.md **Forms** (label Montserrat uppercase gold, input `bg-surface-2` / `border-gold-subtle` / `focus:border-gold`, primary gold "Change Password" button) — all via tokens, no hard-coded hex.

There is **no** "forgot password" affordance here (v1): a fully forgotten password is an ops action, documented in §A.7 and architecture §2.1.1.

### B.10 Responsive behavior (F12-AC27)

The admin is usable at phone, tablet, and desktop widths. Breakpoints are Tailwind defaults, matching the public app (`sm` 640px, `md` 768px, `lg` 1024px). General rule: **desktop-first density that gracefully collapses**, not a separate mobile app. Nothing here changes the API (§ architecture 2.7) — the phone renders the same records the desktop does, only laid out differently.

- **Admin shell / navigation.** At `lg` and up the left sidebar is persistent alongside content. Below `lg` the sidebar collapses into a **hamburger-triggered drawer** (slide-over from the left); the top bar keeps the username, Account link, and Logout, condensing the latter two into an overflow menu on the narrowest widths. Content area is full-width with comfortable side padding (`px-4` on phone, larger at `md`/`lg`).
- **List views (Projects / Team / Samples).** At `md` and up, the dense **table** (§B.3). Below `md`, each row renders as a **stacked card** (`bg-surface-2`, `border-gold-subtle`): the primary field (Name/Title) as the card heading, the secondary fields as labelled lines, the Status/Active badge inline, and the **Actions** (Edit / Delete, plus the Team Active toggle) as full-width or clearly-tappable buttons at the card foot. Same data, same endpoints — a presentation switch (`hidden md:table` table + `md:hidden` card list, or an equivalent single responsive component). Tap targets ≥ 44px. Pagination controls wrap and stay reachable.
- **Create / edit forms.** Multi-column field grids (`md:grid-cols-2`) **stack to a single column** below `md`. Selects, textareas, toggles, and the upload widget are full-width. Save/Cancel become full-width stacked buttons on phone; the top-of-form error summary and inline field errors are unchanged.
- **Trust editor.** The three grouped tables follow the same table→stacked-card collapse; the read-only **derived facts** panel stacks beneath the editable groups on phone.
- **Upload widget (§B.5).** Fully functional on mobile: the "Choose file" control opens the OS file picker / camera roll (image inputs may offer `capture`); the current-asset preview scales to the card width; progress and inline validation errors render the same as desktop.
- **Delete confirmation (§B.6).** Renders as a centered modal on desktop and a bottom/centered sheet on phone, with stacked full-width Cancel / Delete buttons — the confirmation safeguard is width-independent.
- **Login (§B.2) and Account (§B.9).** Already single-card, single-column; they simply gain responsive horizontal padding and a max card width so they read well from 320px up.

All of the above uses **only** the named color tokens (F12-AC30) — the responsive variants introduce no new hex.

---

## Part C — Edge cases & limitations (summary)

- **Slug edit** changes the public URL and `kb.changed` key; no old-slug redirect in v1.
- **Orphaned uploads** (uploaded then not saved, or replaced) are not garbage-collected in v1; accepted (local, cheap).
- **Derived trust facts** (marquee count, client/contractor names) are **not** editable in the Trust screen — edited via Projects (DEC-025); shown read-only with a note.
- **`kb.changed` has no consumer yet** (001 unbuilt) — events accumulate harmlessly.
- **Forgotten password** (as opposed to a *known* password change, which is in scope via §A.7/§B.9) is an **ops action** in v1 — update the `admin_credential` row / clear-and-re-seed via env. No self-service *forgotten*-password reset and no email flow in v1; deferred to v2 (architecture §2.1.1).
- **Single admin, no audit trail, no roles, no draft/publish, no bilingual editing, no versioning/undo** — all explicitly Out of scope for v1 (requirements). The admin edits canonical content and it goes live directly.
- **Responsive (F12-AC27):** the console works at phone/tablet/desktop widths (§B.10). It is desktop-first density that collapses gracefully, not a bespoke mobile app; drag-and-drop upload is a desktop-only additive nicety, not a requirement.
- **Themeable tokens (F12-AC30):** the admin ships a single dark theme built entirely on named tokens (no hard-coded hex). The light/dark **toggle** itself is spec 013's deliverable (a sibling feature); 012 only guarantees token-readiness.
- **On deploy to ISR**, freshness becomes eventual (≤ revalidate window) rather than immediate; a cache-purge-on-write hook is a future enhancement.
