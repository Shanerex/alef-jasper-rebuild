---
feature: 012-admin-content-management
spec_id: "012"
phase: requirements
owner: PM
status: approved
version: "1.1"
entry_criteria:
  - business rationale captured in handoffs/1-business-to-pm.md
  - v1.0 approved and carried into a draft architecture (architecture.md v0.1)
  - human stakeholder reviewed the draft architecture and directed two scope changes (see DEC note in handoff 3-pm-to-architect.md)
exit_criteria:
  - acceptance criteria are testable and id'd; architect can amend architecture.md/design.md from this
---

> **Feature closed 2026-07-19.** All 30 F12-AC1..30 confirmed PASS by id (testing.md v1.5, QA handoffs/14-qa-to-pm.md). Definition of Done confirmed by PM. See handoffs/15-pm-close.md for the formal close decision. This requirements.md stays `approved`/v1.1 — no acceptance criteria changed at close; this note is a status pointer only, per this repo's convention of tracking feature-level done-ness in CLAUDE.md's spec table rather than renumbering or re-versioning a shipped requirements doc.

# Requirements: Admin Content Management

## Problem

Every content change on the ALEF site today -- adding a project, updating a team member, editing trust statistics, managing samples -- requires a developer to modify code or data files and redeploy. ALEF staff are non-technical. The portfolio grows (currently ~38 projects), team members rotate, trust-layer numbers change over time, and sample documents are updated. Without a content management surface, the site becomes stale or creates an ongoing developer dependency for routine updates.

This is an internal admin portal for ALEF staff. It is explicitly distinct from feature 006 (Client Portal), which is an external, client-facing surface scoped to submittal/RFI tracking behind per-client authentication.

## User stories

- As an ALEF admin, I can log in to a protected admin area so that only authorized staff can modify site content.
- As an ALEF admin, I can use the admin console comfortably from my phone or tablet as well as my desktop, so that I can manage content wherever I'm working.
- As an ALEF admin, I can change my own password from within the admin UI, using my current password, so that I can maintain account security without needing a developer to redeploy.
- As an ALEF admin, I can add, edit, or delete a project and see the change reflected on the live public site without a code change or redeploy.
- As an ALEF admin, I can upload project images through the admin interface so that projects display correctly on the public site.
- As an ALEF admin, I can add, edit, or delete a team member (including their photo) so that the About/team section stays current.
- As an ALEF admin, I can edit the trust-layer statistics and badges (years in business, staff count, monthly steel capacity, marquee project count, software/standards badges, client/contractor names) so that the numbers on the home page reflect reality.
- As an ALEF admin, I can add, edit, or delete a sample document (including its preview and downloadable file) so that the Sample Explorer stays current.
- As an ALEF admin, I am protected from accidental data loss through validation and confirmation so that a mistake does not silently break the public site.

## Acceptance criteria

### Authentication

- [ ] F12-AC1 The admin surface is only accessible after successful authentication. Unauthenticated requests to admin pages are redirected to a login screen.
- [ ] F12-AC2 Unauthenticated requests to content-mutating API endpoints are rejected with an appropriate error (not silently ignored).
- [ ] F12-AC3 The authentication mechanism is standalone and admin-only. It does not depend on or presuppose the per-client auth that feature 006 will introduce later.

### Account Security

- [ ] F12-AC28 An admin can change their own password from within the admin UI without a code change or redeploy. The change requires supplying the current password and takes effect on next login.
- [ ] F12-AC29 The admin credential is stored only as a one-way BCrypt hash, seeded from environment configuration on first boot and persisted thereafter (so it can change at runtime). Plaintext passwords are never stored or logged. (This keeps local-first/F12-AC26 intact: the initial secret lives in env, never in the repo.)

### Projects CRUD

- [ ] F12-AC4 An admin can create a new project with all defined fields: slug, name, sector, country, status, image, description, main contractor, client, consultant, location, scope, and featurable flag (for marquee/home-page display per F3-AC4).
- [ ] F12-AC5 An admin can edit any field of an existing project.
- [ ] F12-AC6 An admin can delete a project.
- [ ] F12-AC7 Project changes (create, edit, delete) are reflected on the live public site without a code change or redeploy.

### Team CRUD

- [ ] F12-AC8 An admin can create a new team member with all defined fields: name, role, company, email, and photo.
- [ ] F12-AC9 An admin can edit any field of an existing team member.
- [ ] F12-AC10 An admin can delete a team member.
- [ ] F12-AC11 Team changes are reflected on the live public site without a code change or redeploy.

### Trust Stats and Badges

- [ ] F12-AC12 An admin can edit the trust-layer statistics displayed by feature 005: years in business, staff count, monthly steel capacity, and marquee project count.
- [ ] F12-AC13 An admin can edit the software/standards capability badges (e.g., AutoCAD, CADS RC, SteelPac RC, international detailing standards).
- [ ] F12-AC14 An admin can edit the client/contractor names shown in the trust strip.
- [ ] F12-AC15 Trust stats and badge changes are reflected on the live public site without a code change or redeploy.

### Samples CRUD

- [ ] F12-AC16 An admin can create a new sample with all defined fields: slug, title, preview, downloadable file, and category.
- [ ] F12-AC17 An admin can edit any field of an existing sample.
- [ ] F12-AC18 An admin can delete a sample.
- [ ] F12-AC19 Sample changes are reflected on the live public site without a code change or redeploy.

### Image and File Upload

- [ ] F12-AC20 An admin can upload project images, team member photos, sample preview images, and sample files through the admin interface.
- [ ] F12-AC21 Uploaded files are stored locally (consistent with the local-first constraint; the specific storage location is an architecture decision).
- [ ] F12-AC22 Uploads are validated for allowed file types and maximum file size. Invalid uploads are rejected with a clear error message.

### Input Validation and Safety

- [ ] F12-AC23 Required fields are validated on submission. The admin cannot save a record with missing required fields.
- [ ] F12-AC24 Destructive actions (delete) require explicit confirmation or equivalent safeguard so that accidental deletion cannot silently break the public site.

### Knowledge Base Re-sync

- [ ] F12-AC25 When a project is created, edited, or deleted through the admin interface, a kb.changed event is emitted so that the concierge's RAG knowledge base can update. The side effect is asynchronous and does not block the admin's save action (consistent with the event-driven architecture per DEC-004).

### Local-First

- [ ] F12-AC26 The entire admin surface (UI, API, storage, auth) runs locally with `docker compose up` and requires no external accounts or services.

### Admin UI

- [ ] F12-AC27 The admin UI is fully responsive and usable on phone and tablet as well as desktop: lists, forms, and the upload flow all work correctly at mobile widths, not just desktop. This is required for v1 (ALEF staff use desktop and mobile roughly equally).
- [ ] F12-AC30 The admin UI is built on themeable design tokens with no hard-coded colors, so the forthcoming light/dark theme toggle (spec 013) can be applied without rework.

## Out of scope

- **Per-client authentication and the client-facing portal.** Feature 006 owns external/client auth and submittal tracking. Feature 012 is internal/admin only.
- **Role hierarchies, multiple admin accounts, and fully-forgotten-password self-service reset.** v1 supports a single admin role and a single admin credential. Granular roles (editor, viewer, super-admin) and multiple simultaneous admin accounts are deferred to **v2**. In v1 the single admin can change a *known* password in-app (F12-AC28); a self-service flow for recovering a *fully forgotten* password (e.g., an emailed reset link) is also deferred to **v2** — v1 has no email infrastructure, so if the admin forgets their password entirely, restoring access remains an ops-level action (rotating the seed credential via environment configuration). This limitation is accepted for v1.
- **The light/dark theme toggle itself.** The toggle is owned by spec 013, staged as a separate feature shipping in v1 alongside 012. Feature 012 commits only to building the admin UI on themeable design tokens (F12-AC30) so 013 can layer the toggle on without rework.
- **Audit trail.** Tracking who changed what and when is noted as a possible later iteration but is not required for v1.
- **Bilingual EN/AR editing.** Feature 008 owns the localization model. In v1, the admin edits canonical content only. Bilingual content editing is deferred until 008's localization approach is defined.
- **Approval or publishing workflows.** Edits go live directly; there is no draft/review/publish pipeline in v1.
- **Content versioning or undo.** Rollback to previous content states is deferred.

## Open Questions

1. **Trust stats storage model.** Resolved at the architecture gate: `trust_content` table (DEC-013/DEC-025). No longer open.
2. **Audit trail timing.** Still deferred from v1 (out of scope, above). The business may want it before multiple admins are onboarded — flag for revisit alongside the v2 multi-admin decision. Not a blocker for this feature's v1 close.
3. **Password-reset mechanism.** Resolved for this revision: v1 stays single-admin, with in-app change of a *known* password (F12-AC28, DEC-021); full self-service recovery of a *forgotten* password is explicitly deferred to v2. No acceptance criterion in this revision assumed an external schema — how the credential is persisted at runtime was resolved by the Architect (F12-AC29, DEC-021). No longer open.
