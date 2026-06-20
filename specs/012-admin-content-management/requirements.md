---
feature: 012-admin-content-management
spec_id: "012"
phase: requirements
owner: PM
status: approved
version: "1.0"
entry_criteria:
  - business rationale captured in handoffs/1-business-to-pm.md
exit_criteria:
  - acceptance criteria are testable and id'd; architect can design from this
---

# Requirements: Admin Content Management

## Problem

Every content change on the ALEF site today -- adding a project, updating a team member, editing trust statistics, managing samples -- requires a developer to modify code or data files and redeploy. ALEF staff are non-technical. The portfolio grows (currently ~38 projects), team members rotate, trust-layer numbers change over time, and sample documents are updated. Without a content management surface, the site becomes stale or creates an ongoing developer dependency for routine updates.

This is an internal admin portal for ALEF staff. It is explicitly distinct from feature 006 (Client Portal), which is an external, client-facing surface scoped to submittal/RFI tracking behind per-client authentication.

## User stories

- As an ALEF admin, I can log in to a protected admin area so that only authorized staff can modify site content.
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

- [ ] F12-AC27 The admin UI is functional on desktop browsers. Mobile-friendly layout is a nice-to-have and is not required for v1.

## Out of scope

- **Per-client authentication and the client-facing portal.** Feature 006 owns external/client auth and submittal tracking. Feature 012 is internal/admin only.
- **Role hierarchies or multi-admin permissions.** v1 supports a single admin role. Granular roles (editor, viewer, super-admin) are deferred.
- **Audit trail.** Tracking who changed what and when is noted as a possible later iteration but is not required for v1.
- **Bilingual EN/AR editing.** Feature 008 owns the localization model. In v1, the admin edits canonical content only. Bilingual content editing is deferred until 008's localization approach is defined.
- **Approval or publishing workflows.** Edits go live directly; there is no draft/review/publish pipeline in v1.
- **Content versioning or undo.** Rollback to previous content states is deferred.

## Open Questions

1. **Trust stats storage model.** Trust-layer stats (years in business, staff count, capacity) and badges are not part of the data model in ARCHITECTURE.md today. Whether they live in a Postgres table, a config record, or another mechanism is an architecture decision for the Architect.
2. **Audit trail timing.** Deferred from v1, but the business may want it before multiple admins are onboarded. Flag for revisit after v1 ships.
