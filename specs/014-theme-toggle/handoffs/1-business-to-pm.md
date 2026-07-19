# Handoff: Theme Toggle

| Field | Value |
|---|---|
| Feature # | 014 |
| From / To | Business -> PM |
| Status | ready |
| Spec | specs/014-theme-toggle/requirements.md |

## Context

Request: add a dark/light theme toggle at the top-right corner of the screen, with a polished animation/transition when the user switches modes.

Rationale: two market/user factors. First, user preference and readability — visitors browsing on a phone or laptop expect the option to switch away from a dark UI, and some find sustained dark UI harder to read for long stretches (e.g. a consultant reading a long project scope or spec sheet). Second, this is a GCC B2B site aimed at contractors and consultants who often view it on a laptop or tablet in a brightly lit office during the day — a light mode can reduce screen glare/reflection in that daytime office context, which a pure dark theme does not accommodate.

This request sits directly against an existing, deliberate design decision. `docs/knowledge/DESIGN_SYSTEM.md` states the site's direction is "Dark Prestige — deep navy, gold accents, serif headlines, luxury engineering aesthetic," and explicitly lists under "What this is NOT": "Not a light/white theme — this is dark-first throughout." That constraint was chosen to convey precision, scale, and trust for a structural engineering consultancy, not a SaaS product. A light theme is therefore not a pure implementation task — it is a **design-system extension** that does not yet exist and needs deliberate design work and human approval before code is written.

## Success criteria

- A visible, discoverable toggle control sits at the top-right of the screen (consistent with nav conventions already in DESIGN_SYSTEM.md) on every page the toggle covers.
- Switching themes is a single click/tap and applies with a smooth, polished visual transition — not an abrupt flash or reflow.
- The chosen theme persists across page navigation and return visits (no flash of the wrong theme on load).
- Light mode (once defined) preserves brand legibility and trust cues — it must still read as ALEF, not as a generic light SaaS template, per DESIGN_SYSTEM.md's stated "NOT a minimalist tech startup landing page" / "NOT Inter/sans-serif headings" guardrails.
- Both themes meet accessible contrast for text and interactive elements (gold-on-light is unproven and must be checked, not assumed from the existing gold-on-dark values).

## Risks / open questions for PM

1. **Design-system conflict (top risk).** DESIGN_SYSTEM.md is explicit and dark-first only; it has no light palette defined (no light background/surface tokens, no light text-color scale, no gold-on-light contrast treatment). Per CLAUDE.md, ARCHITECTURE.md is human-approval only to change, and by the same logic a full light-mode addition to DESIGN_SYSTEM.md is a brand-identity decision, not a routine spec addition — it should be raised explicitly and approved by a human before design/implementation proceeds, not inferred or improvised by an agent.
2. **Brand equity in light mode.** The "Dark Prestige — luxury engineering aesthetic" positioning was a deliberate differentiator against generic SaaS sites. A poorly executed light mode (e.g. reusing the same gold at the same values against a white/light surface) risks looking cheap or off-brand rather than luxury. This needs real design work, not a mechanical color inversion.
3. **Accessibility/contrast of gold accents.** Gold Primary (#C4973A) and its variants were tuned for dark surfaces (#07111C, #0E2035, etc.). Contrast ratios for gold-on-light have not been validated and may need a distinct light-mode gold treatment.
4. **Scope: full site vs. progressive rollout.** Should light mode cover every existing page (marketing pages, portfolio/projects, trust layer, contact, future admin/concierge surfaces) at once, or ship progressively page-by-page? This affects both design effort and delivery risk.
5. **Default and persistence mechanism.** Business has no strong opinion on whether the site should default to system preference, default to dark (matching current brand direction) with light as opt-in, or remember the user's last choice. PM should decide and document, since this affects first-impression brand exposure for new visitors.
6. **No-flash-of-wrong-theme.** Persisting the theme choice must not cause a visible flash of the wrong theme on initial page load — a technical detail PM/Architect should confirm is achievable within the Next.js stack.

## What the receiving role (PM) must do

Turn this into testable acceptance criteria with F14-ACn ids. Explicitly raise the DESIGN_SYSTEM.md light-palette gap as an Open Questions Gate item requiring human approval before architecture/design work proceeds — do not let an agent invent a light palette unilaterally. Decide and document scope (which pages/surfaces are covered in v1), default theme behavior, and persistence approach. Confirm the animation/transition requirement is testable (e.g. "no abrupt flash," "transition duration within X ms") rather than purely subjective.

## Read first

- docs/knowledge/DESIGN_SYSTEM.md (full file — dark-first constraint, color system, typography, "What this is NOT" section)
- docs/knowledge/ARCHITECTURE.md (human-approval-only precedent for foundational/system-level docs, for the analogous argument re: DESIGN_SYSTEM.md)
- specs/011-core-marketing-pages/ (pages and components that would need a light variant if scope is full-site)

## Do not touch

- Do not define the actual light-mode color palette, contrast values, or animation implementation — that is design/architecture work, not business scope.
- Do not assume DESIGN_SYSTEM.md can be extended without human sign-off; that approval gate is not this handoff's to grant.

---

## Addendum: stakeholder decisions (2026-07-19, relayed by orchestrator)

The owner reviewed this handoff's top risk and made the following decisions, which resolve open questions 5 and (partially) 1:

1. **Light theme is approved.** Recorded as DEC-021 in docs/knowledge/DECISIONS.md. The light palette still requires owner sign-off as a DESIGN_SYSTEM.md extension before design/implementation phases — that gate stands; only the in-principle approval is granted.
2. **Default theme: follow system preference.** A first-time visitor gets their OS light/dark setting; the toggle overrides it and the override persists across visits.
3. **Palette design is reference-driven.** The owner will supply visual references (sites/screenshots/colors) that the light palette must be built around. Design work is blocked until those references are provided; requirements work is not.
