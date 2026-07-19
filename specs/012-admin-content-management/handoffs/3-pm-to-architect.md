# Handoff: Admin Content Management (requirements revision v1.1)

| Field | Value |
|---|---|
| Feature # | 012 |
| From / To | PM -> Architect |
| Status | ready |
| Spec | specs/012-admin-content-management/requirements.md |

## Context

The human stakeholder reviewed your draft architecture (architecture.md v0.1, DEC-021..DEC-026) before approving it and directed two scope changes. These are settled decisions, not open for re-litigation:

1. **The admin console must be fully responsive.** ALEF staff use desktop and mobile roughly 50/50. This reverses the earlier desktop-only stance (your DEC-026 / F12-AC27 rationale).
2. **v1 stays single-admin**, but that one admin must be able to **self-change a known password in-app**, with no redeploy required. Multiple admin accounts and a fully-forgotten-password self-service reset (e.g., email-based) are explicitly deferred to **v2** — v1 has no email infrastructure, so a truly forgotten password remains an ops action (rotating the seed credential) in v1. That limitation is accepted.

Separately: a sibling spec, **013 (light/dark theme toggle)**, will ship in v1 alongside 012. 012 does not build the toggle; it only owes **theme-readiness** — the admin UI must be built on tokens, not hard-coded colors, so 013 can apply the toggle without rework.

requirements.md is now **v1.1, status: draft** (awaiting fresh human approval of this revision, not just your amended architecture).

## What changed in requirements.md (v1.0 -> v1.1)

No existing acceptance id was renumbered or removed. Deltas:

**Revised**
- **F12-AC27** — was "functional on desktop; mobile is a nice-to-have, not required for v1." Now: the admin UI is **fully responsive** — lists, forms, and the upload flow all work correctly at phone and tablet widths as well as desktop. Required for v1.

**Added (new section "Account Security", plus one addition to "Admin UI")**
- **F12-AC28** — An admin can change their own password from within the admin UI without a code change or redeploy. Requires supplying the current password; takes effect on next login.
- **F12-AC29** — The admin credential is stored only as a one-way BCrypt hash, seeded from environment configuration on first boot and **persisted thereafter** (so it can change at runtime, unlike a value re-read from env on every boot). Plaintext passwords are never stored or logged. Local-first / F12-AC26 stays intact: the *initial* secret still lives in env, never the repo.
- **F12-AC30** — The admin UI is built on themeable design tokens with no hard-coded colors, so spec 013's toggle can be applied without rework.

**Out of scope — updated**
- The single-admin bullet now explicitly names what's deferred to v2: multiple admin accounts, and fully-forgotten-password self-service reset. In-app change of a *known* password is in v1 scope (F12-AC28); recovering a *fully forgotten* password stays an ops action in v1.
- New bullet: the theme toggle itself is spec 013's deliverable, not 012's; 012 owes only token-readiness (F12-AC30).

**Open Questions — updated**
- Audit-trail question (#2) now notes that in-app password change plus eventual v2 multi-admin make an audit trail more warranted at that point — still deferred, revisit alongside the v2 multi-admin decision.
- Added #3, non-blocking: confirms the password-reset mechanism is resolved for this revision and that no AC here assumes an external schema — persistence shape for the credential is explicitly left to you.

## What you (Architect) must do

Your architecture.md v0.1 and design.md were written against v1.0 and need targeted amendment, not a rewrite:

1. **Amend DEC-021 (§2.1), do not replace it.** The current design reads the admin credential from `ADMIN_USERNAME`/`ADMIN_PASSWORD_HASH` env vars on every boot via a `UserDetailsService` with **no user table**. That can no longer be the whole story: F12-AC28/AC29 require the hash to be **persisted and mutable at runtime** (env can seed it once, but a password change has to survive without editing env/redeploying). Work out and document:
   - A minimal persisted credential store (your call on shape — e.g. a single-row admin-credential table, or another mechanism consistent with "no user table" spirit if you can justify it) seeded from `ADMIN_PASSWORD_HASH` **on first boot only**, then authoritative thereafter.
   - A `PUT`/`POST /api/admin/session/password`-style endpoint (or your naming) implementing F12-AC28: requires current password, validates it, re-hashes with BCrypt, persists, never logs plaintext.
   - Whether re-seeding from env should ever override a changed hash (recommend: no — first-boot-only seed, so an admin's in-app change survives restarts/redeploys; document why single-admin-simplicity doesn't fight this).
2. **Flip DEC-026 (§2.7) to responsive.** F12-AC27 now requires full responsiveness — phone, tablet, desktop, for lists, forms, and the upload flow. Update the "Desktop-only" line and any consequence it has on the API/DTO shape (unlikely, but check pagination/list density assumptions your design.md made for desktop-only).
3. **Add the themeable-token constraint to design.md.** F12-AC30 requires the admin UI be built on design tokens (CSS variables / a token layer), not hard-coded hex values, so spec 013 can add a toggle later without a rework pass. Note the dependency on spec 013 as a **sibling**, not a blocker — 012 ships its own (currently single, presumably dark) theme via tokens; 013 adds the switch.
4. **Note spec 013 as a sibling dependency** in your overview/risks section — informational, not something 012 blocks on or builds.

**Reminder:** your ARCHITECTURE.md / DECISIONS.md edits (proposed in §9/§10 of your current architecture.md) remain **PROPOSED ONLY** and await human approval, same as before. Nothing here changes that gate. Re-propose amended language for DEC-021 and DEC-026 in the same "proposed, not applied" posture.

## Read first

- specs/012-admin-content-management/requirements.md (v1.1 — the revised acceptance criteria above)
- specs/012-admin-content-management/architecture.md (your v0.1 draft — §2.1 DEC-021, §2.7 DEC-026, §9, §10 are the sections to amend)
- specs/012-admin-content-management/design.md (your existing draft — check for desktop-only assumptions and hard-coded color values)

## Do not touch

- requirements.md is PM-owned; if you find an acceptance criterion untestable or contradictory, raise it back to PM rather than editing it.
- ARCHITECTURE.md and DECISIONS.md stay human-approval-only, same as before — propose, don't apply.
- Do not reopen the settled scope decisions (responsive, single-admin-with-in-app-change, 013-is-separate) — they came from the human stakeholder, not from analysis you can revisit.
