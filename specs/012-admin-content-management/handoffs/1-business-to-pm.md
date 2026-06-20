# Handoff: Admin Content Management

| Field | Value |
|---|---|
| Feature # | 012 |
| From / To | Business -> PM |
| Status | ready |
| Spec | specs/012-admin-content-management/requirements.md |

## Context

Today, every content change (adding a project, updating a team member, editing trust stats) requires a developer to touch code or data files and redeploy. ALEF staff are non-technical. The portfolio grows (currently ~38 projects), team members rotate, and trust-layer numbers change over time. Without a content management surface, the site becomes stale or creates an ongoing developer dependency for routine updates. This is operational sustainability, not a differentiator -- but without it, the differentiators (concierge, portfolio, trust layer) degrade.

This is an internal admin portal for ALEF staff. It is explicitly distinct from feature 006 (Client Portal), which is external, client-facing, and scoped to submittal/RFI tracking behind per-client auth.

## Success criteria

- A non-technical ALEF admin can add, edit, or remove a project and see the change reflected on the live site without a code change or redeploy.
- A non-technical ALEF admin can add, edit, or remove a team member and see the change reflected on the live site without a code change or redeploy.
- Content edits are safe: accidental deletion or bad input cannot silently break the public site.
- The admin surface runs locally with docker compose up, consistent with the local-first constraint.

## Risks / open questions for PM

1. **Authentication is new.** This is the first feature that requires login. Feature 006 (Client Portal) will also need auth but is a stretch feature in draft. PM must decide: should 012 lay a shared auth foundation that 006 can reuse, or is admin auth a simpler, standalone mechanism? Either way, admin auth needs heavy testing per the production-readiness bar in ARCHITECTURE.md.
2. **Scope boundary for v1.** Projects and team members are the clear minimum. Should v1 also include samples (feature 004), trust-layer stats (feature 005 -- years in business, staff count, capacity), capability badges, or client/contractor logos? What about bilingual content from feature 008 -- does the admin edit both EN and AR, or is that deferred?
3. **Image and file uploads.** Projects have images; team members have photos. Where are these stored? Local filesystem, object storage, or Postgres? Must work locally without external accounts.
4. **Knowledge base sync.** When an admin edits a project, the concierge's RAG knowledge base (Qdrant) should eventually reflect the change. The kb.changed Redis Stream event already exists in the architecture for this purpose. PM should note this as a requirement but not prescribe the mechanism.
5. **Audit trail.** With multiple admins possible, is there a business need to know who changed what and when?

## What the receiving role (PM) must do

Turn this into testable acceptance criteria with F12-ACn ids. Scope v1 tightly to the content types that change most often (projects, team). Explicitly mark what is deferred. Address the auth overlap question with feature 006.

## Read first

- docs/knowledge/ARCHITECTURE.md (data model for project/team/sample, auth note in production-readiness bar, local-first and event-driven constraints)
- specs/003-portfolio-filtering/requirements.md (the project entity and its fields)
- specs/005-trust-layer/requirements.md (trust stats the admin may need to edit)
- specs/006-client-portal/requirements.md (distinguish admin vs client portal; auth overlap)
- docs/knowledge/DECISIONS.md (DEC-004 on event-driven side effects via Redis Streams)

## Do not touch

- The specific auth mechanism, session strategy, or identity provider choice are architecture decisions, not product scope.
- The provider-swap, event-driven, and local-first patterns are settled architecture. Do not revisit them.
