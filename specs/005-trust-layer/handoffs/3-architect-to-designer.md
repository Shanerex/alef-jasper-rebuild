# Handoff: Trust Layer
| Field | Value |
|---|---|
| Feature # | 005 |
| From / To | Designer -> Dev |
| Status | ready |
| Spec | specs/005-trust-layer/design.md |

## Summary of design decisions

The Trust Layer is one portable, self-contained `<section>` (no route) that F011 mounts on Home. It is
dark-first Dark Prestige throughout, with the gold Stat Display row as the single inverted element.
Three bands map to the three acceptance criteria:

- **Band 1 — TrustStats (F5-AC1):** gold-inverted Stat Display row, 4 stats, Cormorant numbers on
  `#C4973A`, Montserrat uppercase labels. Renders `stats[]` verbatim (no formatting). `unit` is an
  optional sub-line.
- **Band 2 — ClientStrip (F5-AC2):** resolved to a **named text strip** (no logos, no asset pipeline,
  no architecture change). Clients labelled "Trusted By", contractors "Main Contractors", names flow as
  a wrapping list separated by gold middots, on the dark surface.
- **Band 3 — CapabilityBadges (F5-AC3):** software and standards as gold-muted **outline** badges
  (reuse the existing `Badge` component, `variant="outline"`), each group introduced by a 28px gold rule
  + Section Label.

Open questions resolved: text strip (not logo grid); stat order = Years · Engineers · Capacity ·
Marquee, marquee count grouped as a peer; F008 RTL/length tolerance designed in (no hardcoded copy in
the data path, all bands wrap, structural eyebrows isolated for future i18n).

Every visual choice traces to DESIGN_SYSTEM.md; no new primitive was invented. Full copy-paste-ready
JSX templates for all five files are in design.md §5.

## Components to build (web only)

| File | What |
|------|------|
| `web/src/lib/types/trust.ts` | `TrustStat`, `TrustOverview` interfaces (design §4.1). |
| `web/src/lib/api/trust.ts` | `getTrustOverview()` typed client over `GET /api/trust/overview` (§4.2). |
| `web/src/components/trust/trust-stats.tsx` | Band 1, gold Stat Display row (§5.1). |
| `web/src/components/trust/client-strip.tsx` | Band 2, named text strip (§5.2). |
| `web/src/components/trust/capability-badges.tsx` | Band 3, capability badges (§5.3). |
| `web/src/components/trust/trust-layer.tsx` | Composition of the three bands + heading (§5.4). |
| `web/src/components/trust/trust-layer-section.tsx` | Optional self-fetching async wrapper for F011 drop-in (§5.5). |

Reused, not created: `web/src/components/ui/badge.tsx`, Tailwind tokens in `tailwind.config.ts`.

## Constraints & notes for implementation

- **Backend is fixed elsewhere.** The `trust_content` table, Flyway V4/V5, `com.alef.api.trust.*`
  controller/service/dto, and `GET /api/trust/overview` are specified in architecture.md (§3, §5, §6).
  Build them to that contract; design.md does not re-specify them. The JSON shape the web layer expects
  is in design.md §4.1 / architecture §3.1.
- **No client state, no interactivity.** All components are pure presentational server components. No
  filters, hooks, `"use client"`, or event handlers.
- **Graceful degradation is required, not optional.** Each band self-hides when its arrays are empty;
  the whole section hides when everything is empty. Inter-band margins live on the wrapper conditionals
  so a hidden band leaves no double gap (design §8). This must hold — it is how the section behaves
  before content is fully seeded.
- **Do not format stat values.** Values arrive as display strings ("18", "120+", "5,000"); render
  verbatim. Do not assume fixed-width numerics or parse them.
- **Rendering strategy is an F011 page concern.** The components are render-strategy-agnostic. The API
  client keeps `next: { revalidate: 60 }`. F011 will likely mirror the portfolio page's `force-dynamic`
  approach (see `web/src/app/projects/page.tsx` doc comment / PITFALL-007) because the web image builds
  before `api` is up in Compose. Do not add `export const revalidate` to a component file.
- **One template line is intentionally disposable:** in `client-strip.tsx` the leading
  `import type { Fragment }` is illustrative — delete it (noted inline in design §5.2).
- **Doc comment every function** (CLAUDE.md #6) — intent-focused. The templates already include them;
  keep them.
- **Accessibility:** `<section>` + `<h2>`, `<ul>/<li>` for the name strip, `aria-hidden` middots
  (design §9). The group eyebrows are `<p>` by default; promoting them to `<h3>` is a low-risk option if
  F011 wants them in the outline.
- **F008/bilingual:** out of scope to implement; just keep all visible copy that is NOT from the API
  confined to `trust-layer.tsx` and the group sub-components (the natural future i18n surface). Do not
  hardcode stat/badge strings.
- **DEC-008 name clearance** (real client/contractor names) is inherited from the architecture; not a
  Dev blocker for the local demo, but do not invent or alter names — they come from the `project` table.

## Status reminder

design.md is `status: draft` and awaits human approval before Dev starts. This handoff is sequence 3.
