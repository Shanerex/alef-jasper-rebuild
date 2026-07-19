# Handoff: Motion & Transitions

| Field | Value |
|---|---|
| Feature # | 013 |
| From / To | Business -> PM |
| Status | ready |
| Spec | specs/013-motion-transitions/requirements.md |

## Context

The site currently has zero motion design: no section transitions, no scroll reveals, no page-to-page transitions, nothing on hover beyond static color/border changes. ALEF competes for GCC structural-detailing and rebar-detailing work against firms that are judged on precision and credibility before a prospect ever picks up the phone. The site's job (per ARCHITECTURE.md) is to convert the portfolio and capacity story into qualified RFQs -- the Trust Layer, portfolio, and concierge are the differentiators; motion is not a differentiator on its own. It is finishing work that affects perceived quality. A site that feels static and unfinished can quietly undercut the "precision, scale, trust" impression the Dark Prestige design system (docs/knowledge/DESIGN_SYSTEM.md) is built to convey, even though no prospect will ever say "I left because there was no animation."

This is polish, not a capability. It does not unlock a new lead source, a new content type, or a new business workflow. It is being staged now because 011 (Core Marketing Pages) and 003/005 (Portfolio, Trust Layer) are done, giving the site its first complete set of pages worth polishing.

## Success criteria

- Motion on the site reads as restrained and deliberate -- consistent with a "luxury engineering aesthetic," not a playful SaaS product. No bouncy easing, no attention-grabbing entrance animation on every element, no motion that competes with the content it is meant to support.
- Motion should make the site feel more finished and credible on first impression, not slower or gimmicky. If a visitor notices the animation before the content, it has failed.
- No feature is worse off after this ships: forms remain fast and reliable, navigation stays instant, and nothing here should delay or complicate a prospect submitting an RFQ or contact lead.
- The site remains fully usable and legible with motion disabled or reduced (see risks below) -- this is not optional polish on top of a broken fallback, it is a baseline requirement.

## Risks / open questions for PM

1. **Performance budget conflict.** ARCHITECTURE.md's production-readiness bar treats SEO, performance, and accessibility as correctness for a marketing site, not nice-to-haves. Animation libraries and scroll-triggered effects are a common source of layout shift, blocked main-thread work, and degraded LCP/CLS. PM and Architect must set an explicit performance budget for this feature (e.g., no regression to Core Web Vitals on the pages 013 touches) before any implementation choice (library vs. CSS-only) is locked in.
2. **Accessibility -- `prefers-reduced-motion` is not optional.** Some visitors have vestibular disorders or motion sensitivity; this must be respected site-wide, not just on one page. PM should make this an explicit acceptance criterion, not an afterthought.
3. **Scope creep risk.** "Motion and transitions" can expand indefinitely (page transitions, scroll-linked parallax, cursor effects, micro-interactions on every button and card). Given this is explicitly nice-to-have, PM should scope v1 tightly -- likely restrained entrance/reveal transitions and hover refinements on existing components (Trust stats, project cards, section headers) -- and explicitly defer anything more elaborate (page-transition choreography, parallax, scroll-jacking).
4. **Brand risk of getting it wrong.** Motion that reads as trendy or SaaS-like actively undermines the "not a minimalist tech startup landing page" / "not a colorful, illustration-heavy SaaS marketing site" constraints in DESIGN_SYSTEM.md. This is a real brand-fit risk, not just a taste question -- PM should treat "does this look like a fintech landing page" as a rejection criterion during design review.
5. **Where does this sit relative to other polish/deferred work?** The repo has real open gaps (lead delivery email, remaining ~23 projects unseeded, team photos missing). PM should confirm 013 does not jump the queue ahead of gaps that affect whether a lead is actually captured and followed up.

## Priority

Nice-to-have. This is explicitly below revenue-driving and trust-critical features (001 AI RFQ Concierge, 002 Turnaround Estimator, 007 Savings Calculator, 009 WhatsApp Handoff, and closing the known lead-delivery gap noted in DEC-018). It should not consume time or introduce risk that delays those. If sequencing forces a tradeoff, 013 loses.

## What the receiving role (PM) must do

Turn this into testable acceptance criteria with F13-ACn ids. Scope v1 tightly (see risk 3) and state explicitly what is deferred. Make `prefers-reduced-motion` support and a stated performance budget first-class acceptance criteria, not implementation notes. Confirm placement in the build order relative to the nice-to-have priority above.

## Read first

- docs/knowledge/DESIGN_SYSTEM.md ("Dark Prestige" direction; "What this is NOT" section is the clearest signal for what motion should avoid)
- docs/knowledge/ARCHITECTURE.md (production-readiness bar: "SEO, performance, and accessibility budgets are treated as correctness for a marketing site")
- specs/011-core-marketing-pages/ (the pages and components 013 will likely apply motion to)
- specs/005-trust-layer/ (stat displays, a plausible candidate for restrained reveal motion)

## Do not touch

- The Dark Prestige visual system itself (colors, typography, layout) is settled and out of scope -- 013 adds motion to existing design, it does not redesign it.
- Do not let this feature introduce a new frontend animation dependency or pattern without Architect sign-off; keep the local-first, low-dependency posture consistent with the rest of `web`.
