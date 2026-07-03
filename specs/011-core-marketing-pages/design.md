---
feature: 011-core-marketing-pages
spec_id: "011"
phase: design
owner: Designer
status: approved
version: "1.0"
entry_criteria:
  - architecture approved (v2.1)
exit_criteria:
  - Dev can build all four pages from this spec without making design decisions
---

# Design: Core Marketing Pages (Home, About, Services, Contact)

> Component-level design spec for feature 011. Every visual decision below is drawn from `docs/knowledge/DESIGN_SYSTEM.md` (Dark Prestige). Where the design system does not cover a case, this spec extends it using the same tokens, font roles, and sharp-corner philosophy — no new palette color, no new font, no border-radius on cards or buttons.
>
> **Reuse over redesign.** Home mounts 003's `ProjectCard`/`ProjectGrid` and 005's `TrustLayer`/`TrustStats` unchanged (architecture §8). About's `LandmarkProjectsStrip` and Contact's `SoftwareBadges` also consume existing components/data. This spec designs only the **new** components and the four page shells.
>
> **Legal-name guard (repeated because it is load-bearing):** the company name is "ALEF Architectural & Cadding Services" — "Cadding" is the legal DED trade-license spelling and MUST NOT be "corrected" anywhere. Only the About-page body typos in F11-AC4 are corrected ("Intenational"→"International", "Concorse"→"Concourse").

---

## 0. Design tokens quick-reference (from DESIGN_SYSTEM.md)

Dev uses the existing Tailwind token classes already in the codebase (`bg-page`, `bg-surface-1`, `bg-surface-2`, `bg-footer`, `text-gold`, `text-gold-light`, `text-text-primary`, `text-text-secondary`, `text-text-muted`, `text-text-disabled`, `text-slate`, `font-serif`, `font-sans`, `font-arabic`, `max-w-site`). Where a token is not in Tailwind config, use the inline `style` hex, matching the pattern already used across `project-card.tsx` and `trust-stats.tsx`.

| Token name | Hex | Token name | Hex |
|---|---|---|---|
| Page BG | `#07111C` | Gold Primary | `#C4973A` |
| Surface 1 | `#0C1B2E` | Gold Light | `#D4A94A` |
| Surface 2 (cards) | `#0E2035` | Gold Dark | `#A8812E` |
| Surface 3 | `#101F32` | Gold Subtle | `rgba(196,151,58,0.2)` |
| Surface 4 (hover) | `#122339` | Gold Ghost | `rgba(196,151,58,0.08)` |
| Footer BG | `#040C14` | Text Primary | `#E8E0D0` |
| Text Secondary | `#C8C0B0` | Text Muted | `#7A8FA8` |
| Text Disabled | `#4A5B6E` | Slate | `#5A6B80` |

**Spacing scale (use only these):** 8 / 16 / 24 / 40 / 80 / 120 px.
**Layout invariants:** max-width 1280px (`max-w-site`), 80px desktop side padding, 100–120px section vertical padding, 72px nav.
**Section-label pattern (used on every hero + section head):** Montserrat 10px / 700 / uppercase / gold / letter-spacing 0.22em.
**Grid-pattern overlay (hero only):** `repeating-linear-gradient(rgba(196,151,58,0.03) 0 1px, transparent 1px 72px)` both axes, 72px grid — copy verbatim from `projects/page.tsx` `ProjectsHero`.

**Responsive breakpoints (Tailwind defaults, matching the shipped projects grid):**
- `< 640px` (mobile): 1 column; side padding drops from 80px to 24px (`px-6`); hero display type scales down (see per-component specs).
- `640–1024px` (`sm`, tablet): 2 columns; side padding 48px (`px-12`).
- `≥ 1024px` (`lg`, desktop): full layout, 80px padding (`px-[80px]`).

This is exactly the `px-6 sm:px-12 lg:px-[80px]` pattern already used in `trust-layer.tsx`. Every new section container uses it.

---

## 1. Component inventory

All new components live under `web/src/components/<section>/`. File naming follows the shipped kebab-case convention (`project-card.tsx`, `trust-layer.tsx`).

### 1.1 Shared hero primitive — `PageHero`

**Path:** `web/src/components/marketing/page-hero.tsx`
**Used by:** About, Services, Contact heroes (and referenced by Home hero, which extends it). Extracts the exact `ProjectsHero` pattern so all four pages share one hero treatment (F11-AC8 consistency).

```ts
interface PageHeroProps {
  /** Breadcrumb label for the current page, e.g. "About". Home is always the first crumb. */
  breadcrumb: string;
  /** Gold section label, uppercase, e.g. "Who We Are". */
  sectionLabel: string;
  /** Display XL heading text, e.g. "About ALEF". */
  heading: string;
  /** Arabic decorative subtitle (RTL, reduced gold). Optional. */
  arabicSubtitle?: string;
  /** Muted description paragraph under the heading. Optional. */
  description?: string;
}
```

**Visual spec** — identical to `ProjectsHero` in `projects/page.tsx`:
- `<section>`: `relative overflow-hidden bg-surface-1 pb-20 pt-[100px]` (Surface 1 `#0C1B2E`, 120px top / 80px bottom).
- Grid-pattern overlay div (both axes, 72px) — copy verbatim.
- Inner container: `relative mx-auto max-w-site px-6 sm:px-12 lg:px-[80px]`.
- Breadcrumb row (`mb-6`): "Home" link (`text-text-disabled`, hover `text-text-muted`, 8.5px/700 uppercase, 0.12em) → gold `›` separator → current crumb (gold, 8.5px/700 uppercase, 0.12em). "Home" links to `/`.
- Section label (`mb-4`): Montserrat 9.5px/700 uppercase gold, letter-spacing 0.24em (matches shipped hero; slightly tighter than the 10px section-label default, kept for hero parity).
- Heading: `font-serif text-[72px] font-light leading-[1.1] text-text-primary`. Mobile: `text-[44px]` at base, `sm:text-[56px]`, `lg:text-[72px]`.
- Arabic subtitle (if present, `mb-7 mt-3`): `font-arabic text-[22px] font-light`, `color rgba(196,151,58,0.5)`, `direction: rtl`.
- Description (if present): `max-w-[600px] font-sans text-[15px] font-light leading-[1.8] text-text-muted`.
- **Immediately after every hero on every page:** a `<div className="h-1 bg-gold" />` (the 4px gold accent bar), matching the projects page. This is part of the page shell, not the hero component.

**Responsive:** display heading scales as above; side padding collapses 80→48→24. Everything else unchanged.

---

### 1.2 Home components (`web/src/components/home/`)

#### `HomeHero`
**Path:** `web/src/components/home/home-hero.tsx`
**Props:** none (static copy; the concierge slot routes to `/contact` until 001).

**Visual spec** — a single tall hero (F11-AC1), taller and more prominent than `PageHero` because it is the landing surface. It reuses the Surface 1 + grid-overlay treatment but does NOT show a breadcrumb (Home is the root).
- `<section>`: `relative overflow-hidden bg-surface-1 pt-[120px] pb-[120px]`.
- Grid-pattern overlay (both axes, 72px) — verbatim.
- Optional decorative diamond frame (design-system "Decorative Elements"): a 45°-rotated border-only square, `border: 1px solid rgba(196,151,58,0.2)`, ~240px, absolutely positioned top-right, `pointer-events-none`, behind content. This is a nicety; if omitted the hero is still complete.
- Inner container: `relative mx-auto max-w-site px-6 sm:px-12 lg:px-[80px]`.
- Section label: "Rebar Detailing & Structural Drafting" — Montserrat 10px/700 uppercase gold, 0.22em, `mb-6`.
- Display heading: `font-serif font-light leading-[1.1] text-text-primary`, `text-[64px] lg:text-[72px]` (mobile `text-[40px]`). Copy: **"Precision detailing at the scale the Gulf builds."** The word **"Precision"** is rendered in gold italic (Display Italic role: `font-serif italic` + `text-gold`) per the design-system accent-word treatment.
- Arabic subtitle (`mt-4 mb-8`): `font-arabic text-[22px] font-light`, `rgba(196,151,58,0.5)`, RTL. A single decorative `ألف` is sufficient.
- Sub-description: `max-w-[620px] font-sans text-[16px] font-light leading-[1.75] text-text-muted` (Body L). Copy: **"23 years turning structural drawings into detailing-standard shop drawings and BBS — 70,000 tonnes of rebar a month, across six GCC countries, from a purpose-built delivery centre."** (Figures echo the trust stats; they are copy here, not a live data pull — the live figures render in the TrustLayer section below.)
- **Concierge entry-point slot** — see §8 for exact copy and treatment. Rendered as a horizontal CTA row (`mt-10`, `flex flex-col sm:flex-row gap-4`): a primary gold "Ask the Concierge" button and a secondary "View Portfolio" button.

**Responsive:** heading 40→64→72; CTA row stacks vertically on mobile (`flex-col`), horizontal from `sm`. Diamond decoration hidden below `lg` (`hidden lg:block`).

#### `CapabilityStrip`
**Path:** `web/src/components/home/capability-strip.tsx`
```ts
interface CapabilityStripProps {
  /** Sector/capability labels to display. Static list passed by the page. */
  items: string[];
}
```
**Visual spec** — a thin horizontal band under the hero conveying breadth at a glance.
- `<section>`: `bg-page py-[40px]`, top and bottom border `1px solid rgba(196,151,58,0.15)`.
- Inner container: standard `max-w-site` + responsive padding.
- Items rendered as a wrapping flex row (`flex flex-wrap items-center gap-x-10 gap-y-4 justify-center`), each item Montserrat 10px/700 uppercase, `text-text-secondary`, letter-spacing 0.14em, separated by a gold `·` middot (`text-gold`, `opacity 0.5`) between items.
- **Item source:** static list passed by the page — a curated 6-item capability line, NOT the full 8 specializations (that lives on Services). Suggested items: `Rebar Shop Drawings` · `Bar Bending Schedules` · `GA Drawings` · `Setting-Out` · `MEP` · `As-Built`.

**Responsive:** wraps naturally; on mobile the middots between wrapped rows are visually fine (flex-wrap). No layout change needed.

#### `FeaturedProjects`
**Path:** `web/src/components/home/featured-projects.tsx`
```ts
interface FeaturedProjectsProps {
  /** Featured project summaries from getProjects({ featurable: true }). */
  projects: ProjectSummary[];
}
```
**Wraps/reuses:** 003's `ProjectGrid` (which itself renders `ProjectCard`). No new card. This component is only the section framing (label + heading + the grid + a ghost link to `/projects`).
**Visual spec:**
- `<section>`: `bg-page py-20 lg:py-[120px]`.
- Section head block (`mb-10`): section label "Selected Work" (10px/700 uppercase gold, 0.22em); H2 heading `font-serif text-[36px] lg:text-[48px] font-light text-text-primary` — **"Projects that define the skyline."**; optional Arabic subtitle (RTL reduced gold), matching the `TrustLayer` head block exactly.
- Grid: renders `<ProjectGrid projects={projects} />` unchanged — first card spans 2 columns, 3-col desktop / 2-col tablet / 1-col mobile, 24px gap (this behavior already lives in `ProjectGrid`).
- Below the grid (`mt-12`): a ghost link "View All Projects →" (Montserrat 10px/700 uppercase gold, hover `gold-light`) linking to `/projects`, right-aligned.
- **Empty/degraded:** if `projects` is empty, `ProjectGrid` already renders its own "No projects…" message; the section head still renders. The page's try/catch (see §2) also guards a cold API.

#### `HomeTrust` (mounted directly)
**Decision:** Mount 005's `<TrustLayer data={trustOverview} />` directly in `page.tsx`. No wrapper component is needed; `TrustLayer` already renders its own section label ("Our Track Record"), H2 ("Built on Scale & Trust"), Arabic subtitle, gold stat row, client strip, and capability badges, all self-hiding on empty data. Reuse verbatim (F11-AC2 — no duplicated data; the numbers are whatever 005's seed serves after the V11 update).

#### `HomeCTA`
**Path:** `web/src/components/home/home-cta.tsx`
**Props:** none.
**Wraps/reuses:** structurally identical to the shipped `ProjectsCTA` (full-width gold band). Reuse that exact treatment for consistency (F11-AC8).
**Visual spec:**
- `<section className="bg-gold">`, inner `mx-auto flex max-w-site flex-col gap-6 px-6 sm:px-12 lg:px-[80px] py-20 lg:flex-row lg:items-center lg:justify-between`.
- Left: H2 `font-serif text-[40px] lg:text-[48px] font-light leading-[1.1] text-page` — **"Have drawings ready? Start an enquiry."** + Arabic subtitle `font-arabic text-[16px] font-light` `rgba(7,17,28,0.5)` RTL.
- Right: primary-on-gold button — `bg-page px-10 py-[18px] font-sans text-[10px] font-bold uppercase text-gold` (0.14em), hover `bg-surface-1`. Label "Send an Enquiry". Links to `/contact`.
**Responsive:** stacks (`flex-col`) below `lg`, headline `text-[40px]` on mobile.

---

### 1.3 About components (`web/src/components/about/`)

#### `CompanyStory`
**Path:** `web/src/components/about/company-story.tsx`
**Props:** none (static content; typos corrected per F11-AC4; "Cadding" preserved).
**Visual spec** — a two-part prose section (ALEF Dubai + Jasper India delivery arm), told as a scale/cost-advantage story.
- `<section>`: `bg-page py-20 lg:py-[120px]`.
- Inner container standard `max-w-site` + responsive padding.
- Two-column layout on `lg` (`grid grid-cols-1 lg:grid-cols-2 gap-x-[80px] gap-y-16`); single column on mobile.
- **Column A — "The Firm" (ALEF):** section label "Who We Are" (gold, 0.22em); H2 `font-serif text-[36px] font-light text-text-primary` "Founded in Dubai, 2003."; body Montserrat 13px/400 leading-1.7 `text-text-muted` (Body M) — 2–3 paragraphs: ALEF Architectural & Cadding Services, established March 2003, 23 years of rebar detailing and structural drafting for the GCC's landmark projects.
- **Column B — "The Delivery Centre" (Jasper India):** section label "Delivery Model"; H2 "A campus built for scale."; body paragraphs on the Jasper International Engineering Consultants campus in Vasudevanallur, Tamil Nadu: purpose-built, capacity for 250 staff, in-house accommodation for 196 staff with canteen, full departments (QC, Technical, Administration, Project Management), 2 project departments / 10 sections, checkers and detailers in teams. Frame as cost + scale advantage.
- **Inline scale stats (within Column B):** to avoid competing with the gold TrustLayer band, render these as *dark* mini-stats: Cormorant Garamond 36px `text-gold` number + Montserrat 8px/700 uppercase `text-text-muted` label, in a 2×2 grid (`grid grid-cols-2 gap-6`). Figures: "250 Staff Capacity", "196 In-house Beds", "10 Detailing Sections", "2 Project Departments". These are About-specific campus facts, NOT the company-wide trust stats (no duplication with 005).

**Responsive:** two columns collapse to one below `lg`; mini-stat grid stays 2×2.

#### `GccReachBlock`
**Path:** `web/src/components/about/gcc-reach-block.tsx`
**Props:** none (static; the six GCC countries + "other").
**Visual spec** — a geographic-footprint band conveying onsite coordination across the Gulf.
- `<section>`: `bg-surface-1 py-20 lg:py-[120px]` (Surface 1 to differentiate from the page-bg sections around it), with the grid-pattern overlay (subtle, both axes) for a hero-adjacent feel.
- Section head: label "Regional Footprint" (gold, 0.22em); H2 `font-serif text-[36px] lg:text-[48px] font-light text-text-primary` "On the ground across the Gulf." + muted lead line "Dedicated onsite coordinators in every GCC market."
- Country row: `grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4` of six country chips — each chip a Service-Card-mini: `bg-surface-2` `border 1px solid rgba(196,151,58,0.15)` padding 24px, a 28px gold top rule, country name Montserrat 10px/700 uppercase `text-text-secondary`. Countries in order: UAE, Qatar, Bahrain, Oman, Kuwait, Saudi Arabia. A seventh muted "+ Other GCC" chip may follow.
**Responsive:** 6 cols desktop → 3 tablet → 2 mobile.

#### `LandmarkProjectsStrip`
**Path:** `web/src/components/about/landmark-projects-strip.tsx`
See the dedicated §4 spec below (layout, animation, data source).

#### `TeamGrid`
**Path:** `web/src/components/team/team-grid.tsx`
```ts
interface TeamGridProps {
  /** Ordered team members from getTeam() ({ members }). Already sorted by display_order server-side. */
  members: TeamMember[];
}
```
`TeamMember` type (mirrors `GET /api/team`, architecture §3.1) — to be declared in `web/src/lib/types/team.ts`:
```ts
interface TeamMember {
  name: string;
  role: string;
  company: string | null;
  email: string | null;
  photo: string | null;
}
interface TeamResponse { members: TeamMember[]; }
```
**Visual spec** — a responsive grid that flows for **N** cards (never hardcode 10; the team is expanding — architecture §5.3, DEC-020).
- `<section>`: `bg-page py-20 lg:py-[120px]`.
- Section head: label "Leadership" (gold, 0.22em); H2 `font-serif text-[36px] lg:text-[48px] font-light text-text-primary` "The people behind the drawings."
- Grid: `grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6` — 3 cols desktop, 2 tablet, 1 mobile, 24px gap. This flows naturally for any count (10 today, more later). **No first-card span, no per-count special-casing.**
- Renders one `<TeamCard member={m} />` per member (`key` = `name + role`, the natural key).
- **Empty/degraded:** if `members` is empty, render a muted single-line message "Leadership profiles are loading — please refresh in a moment." (mirrors the projects-page degradation copy). The page try/catch also guards a cold API.

#### `TeamCard`
**Path:** `web/src/components/team/team-card.tsx`
See the dedicated §5 spec below (photo present, photo null, hover).

---

### 1.4 Services components (`web/src/components/services/`)

#### `ServicesList`
**Path:** `web/src/components/services/services-list.tsx`
```ts
interface ServicesListProps {
  /** The 8 outcome-reframed specializations. Static constant from web/src/lib/content/services.ts. */
  services: ServiceItem[];
}
```
`ServiceItem` type (declared alongside the constant in `web/src/lib/content/services.ts`):
```ts
interface ServiceItem {
  /** Stable key for React list + future reference, e.g. "rebar-shop-drawings". */
  key: string;
  /** Outcome-oriented card title (short), e.g. "Shop-ready rebar, bar for bar". */
  title: string;
  /** The underlying capability, shown as a small uppercase eyebrow, e.g. "Rebar Shop Drawings & BBS". */
  discipline: string;
  /** One-sentence outcome-framed body. */
  outcome: string;
}
```
**Visual spec:**
- `<section>`: `bg-page py-20 lg:py-[120px]`.
- Section head: label "Specializations" (gold, 0.22em); H2 `font-serif text-[36px] lg:text-[48px] font-light text-text-primary` "What we return to you."
- Grid: `grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6` (8 cards flow across 3 columns; the trailing 2 cards sit on the last row — no special casing). Renders one `<ServiceCard service={s} />` per item.
**Responsive:** 3 → 2 → 1 columns.

#### `ServiceCard`
**Path:** `web/src/components/services/service-card.tsx`
See the dedicated §6 spec below (layout + the outcome copy for all 8).

#### `SoftwareBadges` (on Services)
**Path:** `web/src/components/services/software-badges.tsx`
```ts
interface SoftwareBadgesProps {
  /** The software array from getTrustOverview() (005). NOT a static re-list. */
  software: string[];
}
```
**Wraps/reuses:** the same Badge treatment as 005's `CapabilityBadges` `BadgeGroup`. Renders the "Software" group using the shipped `Badge` (variant `outline`) exactly as `capability-badges.tsx` does.
**Visual spec:**
- `<section>`: `bg-surface-1 py-20 lg:py-[120px]`.
- Section head: label "Our Toolchain" (gold, 0.22em); H2 `font-serif text-[36px] font-light text-text-primary` "The software behind the detailing."; muted lead line.
- A 28px gold rule (`h-[1px] w-7 bg-gold`), then a wrapping row of outline badges (`flex flex-wrap gap-2`) — one `<Badge variant="outline">` per software string (AutoCAD, Steel PAC RCD, Steel PAC RCS, CADS RC, CADMATE, LOHA, Multi-Rc once the V11 005-seed update lands).
- **Single source of truth (F11-AC2 / "do not duplicate content"):** the array comes from `getTrustOverview().software`, never a static list. If the array is empty (cold API or pre-V11 seed), the section self-hides (`if (software.length === 0) return null`).

---

### 1.5 Contact components (`web/src/components/contact/`)

#### `ContactForm`
**Path:** `web/src/components/contact/contact-form.tsx` — **client component** (`"use client"`).
See the dedicated §7 spec below (fields, honeypot, all four states, exact copy).

#### `OfficeLocations`
**Path:** `web/src/components/contact/office-locations.tsx`
```ts
interface OfficeLocationsProps {
  /** Both offices from getOffices() ({ offices }). Ordered server-side (Dubai first). */
  offices: Office[];
}
```
`Office` type (mirrors `GET /api/offices`, architecture §3.2) — declared in `web/src/lib/types/office.ts`:
```ts
interface Office {
  key: string;            // "dubai" | "india"
  name: string;
  addressLines: string[];
  phones: string[];
  email: string | null;
  mapQuery: string | null;
}
interface OfficesResponse { offices: Office[]; }
```
**Visual spec:**
- `<section>`: `bg-page py-20 lg:py-[120px]`.
- Section head: label "Our Offices" (gold, 0.22em); H2 `font-serif text-[36px] lg:text-[48px] font-light text-text-primary` "Two offices, one delivery engine."
- Grid: `grid grid-cols-1 lg:grid-cols-2 gap-6` — two cards side-by-side on desktop, stacked on mobile/tablet. Renders one `<OfficeCard office={o} />` per office.
- **Empty/degraded:** if `offices` is empty, section renders a muted "Office details are loading…" line; page try/catch guards cold API.

#### `OfficeCard`
**Path:** `web/src/components/contact/office-card.tsx`
```ts
interface OfficeCardProps { office: Office; }
```
**Visual spec** — a card in the standard Surface-2 treatment with a gold top rule.
- Card: `bg-surface-2` (no radius), `border-t-2 border-gold` (gold top border, matching ProjectCard), inner padding 24px (`p-6`) with `40px` between the text block and the map.
- Office name: `font-serif text-[26px] font-normal text-text-primary` (H3), `mb-4`.
- Address lines: each line Montserrat 13px/400 `text-text-muted` on its own line (map over `addressLines`), `leading-1.7`.
- Divider: a `1px` `rgba(196,151,58,0.15)` rule (or shipped `Separator`), `my-4`.
- Phones: label "Phone" (Montserrat 8.5px/700 uppercase gold, 0.2em) then each phone on its own line as a `tel:` link (strip spaces in the `href`) — Montserrat 13px/400 `text-text-secondary`, hover `text-gold`. Map over `phones` (Dubai has two, India one).
- Email (if non-null): label "Email" (same label style) then a `mailto:` link — 13px/400 `text-text-secondary`, hover `text-gold`.
- Map: `<ContactMap mapQuery={office.mapQuery} name={office.name} />` at the bottom (see below). If `mapQuery` is null, the map is simply omitted; the card is still complete (F11-AC7 satisfied by the address text).
**Responsive:** cards stack below `lg`; internals unchanged.

#### `ContactMap`
**Path:** `web/src/components/contact/contact-map.tsx`
```ts
interface ContactMapProps {
  /** Plaintext location string fed to the keyless embed. Nullable. */
  mapQuery: string | null;
  /** Office name for the iframe title / a11y. */
  name: string;
}
```
**Visual spec** — a keyless Google Maps `<iframe>` (architecture §10 Decision 3). **No API key, no paid SDK.**
- If `mapQuery` is null → render nothing.
- Else: an `<iframe>` with `src = "https://maps.google.com/maps?q=" + encodeURIComponent(mapQuery) + "&output=embed"`, `title = "Map: " + name`, `loading="lazy"`, `width 100%`, `height 220px`, no border-radius, border `1px solid rgba(196,151,58,0.2)`.
- The iframe is **decorative, not structural** — if it fails to load (cold demo, no internet), the surrounding `OfficeCard` still shows the full address and phone. Do not gate F11-AC7 on the map rendering.
**Responsive:** full-width of the card; fixed 220px height at all breakpoints.

#### `DirectChannels`
**Path:** `web/src/components/contact/direct-channels.tsx`
**Props:** none (static; the concierge + WhatsApp slots route to placeholders until 001/009).
**Visual spec** — a band offering direct/instant channels *alongside* the form and offices (F11-AC7: not instead of them).
- `<section>`: `bg-surface-1 py-20 lg:py-[120px]` with subtle grid overlay.
- Section head: label "Talk To Us Directly" (gold, 0.22em); H2 `font-serif text-[36px] font-light text-text-primary` "Prefer a faster channel?"
- A `grid grid-cols-1 sm:grid-cols-3 gap-6` of three channel cards (Service-Card style: `bg-surface-2`, `border 1px solid rgba(196,151,58,0.15)`, padding 24px, 28px gold top rule):
  1. **Concierge** — title "Ask the AI Concierge", body "Get grounded answers on capability, capacity, and turnaround.", ghost CTA "Start a chat →" anchor-scrolling to `#contact-form` until 001 rewires it.
  2. **WhatsApp** — title "Message us on WhatsApp", body "Quick questions, fastest reply.", ghost CTA "Open WhatsApp →" routing to `#` (placeholder; 009 wires the real link). Kept visually equal to the others — it is a reserved slot.
  3. **Call** — title "Call the Dubai office", body the Dubai head-office number, ghost CTA "Call now →" as a `tel:+97142513840` link.
**Responsive:** 3 cols → 1 col below `sm`.

---

## 2. Page layouts (section-by-section)

Each page inherits `SiteNav` + `SiteFooter` from the root layout (F11-AC8). Data sources are already resolved in the architecture; this table just references them. All server pages use the **exact** try/catch graceful-degradation pattern from the shipped `projects/page.tsx` and set `export const dynamic = "force-dynamic"` (PITFALL-007; one-line flip to `revalidate = 60` later — architecture §7). Each page sets a `metadata` export (title/description) inheriting the root title template.

### 2.1 Home — `web/src/app/page.tsx` (replaces the placeholder)
Server component. `export const dynamic = "force-dynamic"`.
Fetches, in a single `Promise.all` inside a try: `getProjects({ featurable: true, size: 50 })` and `getTrustOverview()`.

| # | Section | Component | Data | Notes |
|---|---|---|---|---|
| 1 | Hero + concierge slot | `HomeHero` | static | 4px gold bar after (`<div className="h-1 bg-gold" />`) |
| 2 | Capability strip | `CapabilityStrip` | static list (page-supplied) | thin band |
| 3 | Featured projects | `FeaturedProjects` | 003 `getProjects({featurable:true})` | reuses `ProjectGrid` |
| 4 | Trust stats/clients/software | `TrustLayer` (005, verbatim) | 005 `getTrustOverview()` | no wrapper |
| 5 | Primary CTA | `HomeCTA` | static | full-width gold |

**Catch fallback:** render `HomeHero` + `CapabilityStrip` + `HomeCTA` (the static sections) and a muted "Featured work and stats are loading — please refresh in a moment." in place of sections 3–4. Never a hard error page.

### 2.2 About — `web/src/app/about/page.tsx`
Server component. `export const dynamic = "force-dynamic"`.
Fetches inside a try: `getTeam()` and (for the landmark strip) `getProjects({ featurable: true, size: 50 })`.

| # | Section | Component | Data | Notes |
|---|---|---|---|---|
| 1 | Hero | `PageHero` (breadcrumb "About", label "Who We Are", heading "About ALEF") | static | + 4px gold bar |
| 2 | Company story (ALEF + Jasper India) | `CompanyStory` | static (typos fixed, "Cadding" kept) | 2-col |
| 3 | GCC reach | `GccReachBlock` | static | Surface 1 |
| 4 | Landmark projects strip | `LandmarkProjectsStrip` | 003 `getProjects({featurable:true})` | see §4 |
| 5 | Leadership grid | `TeamGrid` → `TeamCard` | new `getTeam()` | see §5 |
| 6 | CTA | reuse `HomeCTA` treatment | static | consistency |

**Catch fallback:** render hero + `CompanyStory` + `GccReachBlock` (static) and a muted loading line in place of the team grid and landmark strip.

### 2.3 Services — `web/src/app/services/page.tsx`
Server component (fetches software from 005). `export const dynamic = "force-dynamic"`.
Fetches inside a try: `getTrustOverview()` (for `software` only). Specializations come from the static constant `SERVICES` in `web/src/lib/content/services.ts`.

| # | Section | Component | Data | Notes |
|---|---|---|---|---|
| 1 | Hero | `PageHero` (breadcrumb "Services", label "What We Do", heading "Services") | static | + 4px gold bar |
| 2 | Specializations | `ServicesList` → `ServiceCard` | static `SERVICES` (8) | see §6 |
| 3 | Software toolchain | `SoftwareBadges` | 005 `getTrustOverview().software` | self-hides if empty |
| 4 | CTA | reuse `HomeCTA` treatment | static | to `/contact` |

**Catch fallback:** the specializations (static) always render; the software band self-hides on empty/cold data. Even on a total fetch failure, wrap so hero + `ServicesList` + CTA render.

### 2.4 Contact — `web/src/app/contact/page.tsx`
Server component for the office block; the form is a nested client component. `export const dynamic = "force-dynamic"`.
Fetches inside a try: `getOffices()`.

| # | Section | Component | Data | Rendering |
|---|---|---|---|---|
| 1 | Hero | `PageHero` (breadcrumb "Contact", label "Get In Touch", heading "Contact") | static | + 4px gold bar |
| 2 | Form (wrap in `id="contact-form"`) | `ContactForm` | POST `/api/leads` at submit | **client** |
| 3 | Offices + maps | `OfficeLocations` → `OfficeCard` → `ContactMap` | new `getOffices()` | server-fetched |
| 4 | Direct channels | `DirectChannels` | static | server |

Layout default: sections 1 → 2 → 3 → 4 stacked full-width. Dev may optionally place the form (section 2) and the first office in a two-column arrangement (`lg:grid-cols-2`) on desktop; both satisfy the spec.
**Catch fallback:** the form (client) always renders (independent of the office fetch). If `getOffices()` fails, render a muted "Office details are loading…" in the offices section; `DirectChannels` (static, carries the Dubai phone) still gives a direct channel.

---

## 3. Nav and footer wiring (F11-AC8, F11-AC9)

Edit `web/src/app/layout.tsx` (proposed; Dev implements). Two changes only; no new layout component.

**3.1 Nav hrefs.** In `NAV_LINKS`, wire the three routes 011 ships (Samples stays `#` — that is feature 004):
```ts
const NAV_LINKS = [
  { href: "/about",    label: "About" },
  { href: "/services", label: "Services" },
  { href: "/projects", label: "Projects" },   // already wired
  { href: "#",         label: "Samples" },     // 004, stays #
  { href: "/contact",  label: "Contact" },
] as const;
```
The "Enquire Now" CTA button (currently `href="#"`) → `href="/contact"` until 001 wires the concierge.

**3.2 Footer dynamic year (F11-AC9).** In `SiteFooter`, replace the hardcoded `2025` with the current year computed at render:
```
{new Date().getFullYear()}
```
so the copyright line reads: `© {year} ALEF Architectural & Cadding Services LLC · Dubai, UAE`. The full legal entity name "ALEF ARCHITECTURAL & CADDING SERVICES L.L.C" is appropriate in the footer per architecture §5.2. **"Cadding" is correct — do not change it.** The current footer also lowercases "Alef" in the copyright text; correct it to "ALEF" for brand consistency while here (this is the "other stale content" F11-AC9 permits, not a scope change).

No border-radius, no palette change: the nav/footer styles are already design-system-correct.

---

## 4. `LandmarkProjectsStrip` spec (About prestige strip)

**Path:** `web/src/components/about/landmark-projects-strip.tsx`
```ts
interface LandmarkProjectsStripProps {
  /** Featured projects from getProjects({ featurable: true }) — the SAME 003 source Home uses. */
  projects: ProjectSummary[];
}
```
**Data source (architecture §10 Decision 4):** pulls from 003's `featurable=true` projects — the landmark names (Ski Dubai, Mall of the Emirates, Dubai Airport Concourses 3 & 4, Abu Dhabi Midfield Terminal, New Doha International Airport, Route 2020 Metro, Dubai Hills Estate Mall, Bluewaters Island) are seeded/being-seeded in the `project` table, not a parallel static list. Single source of truth. DEC-008 demo clearance applies.

**Aesthetic** — a horizontal, prestige "marquee" of landmark names, distinct from the `FeaturedProjects` card grid on Home (this strip is *names and scale*, not cards).

**Visual spec:**
- `<section>`: `bg-surface-1 py-20 lg:py-[120px]`, with the grid-pattern overlay (both axes, 72px) and an optional 4px gold rule at top.
- Section head (left-aligned): label "Landmark Projects" (gold, 0.22em); H2 `font-serif text-[36px] lg:text-[48px] font-light text-text-primary` "Where our detailing stands today."
- **The strip itself:** a single horizontal row of landmark names that scrolls continuously (a slow, infinite marquee).
  - Container: `relative w-full overflow-hidden`, with a left/right fade mask (a `linear-gradient` overlay on each edge, ~80px wide, `bg-surface-1`→transparent) so names fade in/out at the edges rather than hard-clipping.
  - Track: an inline-flex row of items, each item = the project `name` in `font-serif text-[28px] lg:text-[36px] font-light text-text-secondary`, separated by a small gold diamond glyph (a 8px 45°-rotated gold square, `opacity 0.5`) between names.
  - **Animation:** CSS `@keyframes` translateX from `0` to `-50%` over `40s linear infinite`, direction right-to-left; render the array **twice back-to-back** so the loop is seamless. Pause on hover (`hover:[animation-play-state:paused]`) is an optional nicety.
  - **Reduced motion:** under `@media (prefers-reduced-motion: reduce)` the track is static (no animation) and instead wraps/centers the names as a static list (`flex-wrap justify-center gap-x-10 gap-y-4`). Keeps it accessible (F11-AC8 a11y budget).
- Below the strip (`mt-10`, centered): a muted caption "…and 100+ more across six GCC countries." (Montserrat 12px/300 `text-text-muted`).
- **Empty/degraded:** if `projects` is empty, the whole section self-hides (`if (projects.length === 0) return null`) — the About page is still complete without it.

**Responsive:** name size 28px mobile → 36px desktop; the marquee mechanic is width-agnostic. Under reduced-motion or on very small screens the static wrapped fallback reads cleanly.

**Design-system consistency:** serif names, gold diamond separators (design-system "Decorative Elements"), Surface 1 + grid overlay, no border-radius, no new color. This is a net-new pattern the design system did not define; it is built entirely from existing tokens.

---

## 5. `TeamCard` spec (graceful photo degradation)

**Path:** `web/src/components/team/team-card.tsx`
```ts
interface TeamCardProps { member: TeamMember; }
```
Card follows the design-system card family: Surface 2, sharp corners, 2px gold top border, degrades gracefully when optional fields are null (mirrors the 003 nullable-credit-field stance).

**Card frame (both states):**
- `bg-surface-2` (`#0E2035`), no border-radius, `border-t-2 border-gold` (2px gold top border) between the photo area and the content block (same structure as ProjectCard: image area → gold border → content).
- Content padding `28px 32px` (matches ProjectCard content padding).
- Hover: background transitions to Surface 4 hover (`#122339`) on `group-hover`, and if a photo is present it scales `1.02` like the ProjectCard image. Transition `duration-300`. No border-radius introduced.

**Photo area (top of card, above the gold border):**
- Fixed box, `h-[280px] w-full overflow-hidden bg-surface-2`.
- **Photo present** (`member.photo` non-null): `<img src={photo} alt={name} class="h-full w-full object-cover ... group-hover:scale-[1.02]" loading="lazy" />`.
- **Photo null** (graceful degradation): render a dignified monogram placeholder (NOT the "project photograph" diagonal hatch used for projects):
  - Background: `bg-surface-3` (`#101F32`) with a faint gold-ghost tint (`rgba(196,151,58,0.08)`).
  - Centered monogram: the member's initials (first letter of first token + first letter of last token, e.g. "K. Jeyaraman" → "KJ"; single-name members like "Namasivayam" → "N") in `font-serif text-[48px] font-light` `text-gold` at `opacity 0.4`.
  - No "photo missing" label text — the monogram is the placeholder.

**Content block (below the gold border):**
- **Role eyebrow** (first line): `member.role` in Montserrat 8px/700 uppercase `text-gold`, letter-spacing 0.14em (mirrors ProjectCard location line). Always present (role is required).
- **Name** (title): `member.name` in `font-serif text-[24px] font-normal leading-[1.2] text-text-primary`, `mb-2`.
- **Company** (if non-null): `member.company` in Montserrat 10px/700 uppercase `text-text-muted`, letter-spacing 0.12em. Omitted entirely if null (no empty line).
- **Email** (if non-null): a `mailto:` link, Montserrat 11px/400 `text-text-muted`, hover `text-gold`, prefixed with a small gold `→` affordance. Omitted entirely if null.
- Fields render top-to-bottom; each optional field simply drops out when absent so the card never shows blank gaps (graceful degradation, F11-AC3 nullable fields).

**Responsive:** the card is grid-sized by `TeamGrid` (3/2/1 cols). Internals fluid; photo box stays 280px tall at all breakpoints.

---

## 6. `ServiceCard` spec + the 8 outcome-reframed specializations (F11-AC5)

**Path:** `web/src/components/services/service-card.tsx`
```ts
interface ServiceCardProps { service: ServiceItem; }
```
Follows the design-system **"Service Card"** pattern: Surface 2, `border 1px solid rgba(196,151,58,0.15)`, padding 24px, a **28px-wide × 1px gold rule at the top**, no border-radius.

**Visual spec:**
- Card: `bg-surface-2`, `border 1px solid rgba(196,151,58,0.15)`, `p-6` (24px), no radius. Hover: border brightens to `rgba(196,151,58,0.35)` and background to Surface 4 (`#122339`), `transition duration-300`.
- Top: 28px × 1px gold rule (`h-[1px] w-7 bg-gold`), `mb-4`.
- **Discipline eyebrow:** `service.discipline` in Montserrat 8px/700 uppercase `text-text-muted`, letter-spacing 0.14em, `mb-3`. (This preserves the underlying live-site capability name so nothing is "dropped" — F11-AC5 — while the title reframes it as an outcome.)
- **Title:** `service.title` in `font-serif text-[24px] font-normal leading-[1.25] text-text-primary`, `mb-3`. (The design-system Service Card uses a small Montserrat title; here it is elevated to serif to carry the outcome line with more prestige, consistent with the Dark Prestige direction — a deliberate, token-consistent extension.)
- **Outcome body:** `service.outcome` in Montserrat 12px/300 leading-1.7 `text-slate` (`#5A6B80`).
- No CTA per card (the page CTA covers conversion).

**The 8 specializations — static content for `web/src/lib/content/services.ts`.** Discipline = the reconciled capability name (F11-AC5: nothing dropped); Title + Outcome = the outcome reframing. Copy is final; Dev uses it verbatim.

| key | discipline (eyebrow) | title (outcome) | outcome (body) |
|---|---|---|---|
| `rebar-shop-drawings` | Rebar Shop Drawings & BBS | Shop-ready rebar, bar for bar | We turn your structural design into fabrication-ready rebar shop drawings and bar bending schedules — every bar counted, cut, and scheduled to detailing standard. |
| `ga-drawings` | General Arrangement Drawings | The whole structure, clearly set out | Coordinated general arrangement drawings that give your site team an unambiguous picture of how every element fits together. |
| `concrete-works-drawings` | Concrete Works Drawings | Concrete detailed for the pour | Detailed concrete works drawings that take the guesswork out of formwork, placement, and sequencing on site. |
| `setting-out-coordinates` | Global Co-ordinates / Setting-Out | Positioned to the millimetre | Global co-ordinate and setting-out drawings that put every element exactly where the design intends — no field re-interpretation. |
| `road-infrastructure` | Road Setting-Out, Profiles, Cross-Sections, Marking, Signage & Utilities | Infrastructure detailed end to end | Road setting-out, profiles and cross-sections, road marking, signage, and utility-service drawings — the full linear-infrastructure package in one place. |
| `estimation-qs` | Estimation & Quantity Surveying | Quantities you can budget against | Estimation and quantity surveying that turn drawings into reliable material and cost figures before a single bar is ordered. |
| `mep-drawings` | MEP Drawings | Services coordinated, clashes caught | MEP drawings coordinated against the structure so mechanical, electrical, and plumbing routes are resolved before they reach the field. |
| `as-built-drawings` | As-Built Drawings | The record of what was truly built | Accurate as-built drawings that capture the structure as constructed — your definitive handover and maintenance record. |

**Responsive:** the card is grid-sized by `ServicesList` (3/2/1 cols); internals fluid.

---

## 7. `ContactForm` states (F11-AC6)

**Path:** `web/src/components/contact/contact-form.tsx` — **client component** (`"use client"`).
Posts to `POST /api/leads` via a new typed client `submitLead(payload)` in `web/src/lib/api/leads.ts` (architecture §5.4), which returns `{ id, status }` on 201 and throws on non-2xx (carrying the HTTP status) so the form can distinguish success from validation/rate-limit/server error.

**Fields (design-system "Forms" spec):**
- Inputs: `bg-surface-2` (`#0E2035`), `border 1px solid rgba(196,151,58,0.25)`, focus border `#C4973A` (no ring/glow, sharp corners, no radius). Input text Montserrat 12px/300 `text-text-primary`, placeholder `text-text-disabled` (`#4A5B6E`).
- Labels: Montserrat 8.5px/700 uppercase gold, letter-spacing 0.2em, `mb-2`, real `<label htmlFor>`.
- Field vertical rhythm: 24px between fields.
- Fields, in order:
  1. **Name** — required. Label "Your Name". Placeholder "Full name".
  2. **Email** — required, email format. Label "Email". Placeholder "you@company.com".
  3. **Phone** — optional. Label "Phone (optional)". Placeholder "+971 …".
  4. **Company** — optional. Label "Company (optional)". Placeholder "Company name".
  5. **Message** — required, `<textarea>` min 5 rows. Label "How can we help?". Placeholder "Tell us about your project, scope, or timeline.".
  6. **`website` HONEYPOT** — hidden from humans, submitted as `website`. Wrap in a container `style={{ position: 'absolute', left: '-9999px' }}`, input `tabIndex={-1}`, `autoComplete="off"`, `aria-hidden="true"`. A real human leaves it blank.
- **Submit button:** primary gold button (design-system Primary) — `bg-gold text-page px-8 py-[14px] font-sans text-[10px] font-bold uppercase` (0.14em), hover `bg-gold-light`, no radius. Full-width on mobile, auto width on desktop.

**Client validation before POST** (mirror the server rules in architecture §3.4 for instant feedback; the server re-validates authoritatively): Name non-blank; Email non-blank + basic email pattern; Message non-blank. On invalid, block submit and mark each offending field with a **gold left-border accent** (`border-l-2 border-gold`) plus an inline message in `text-text-secondary` (the palette has no dedicated red, so error affordance is gold-accent + text tone, never a new color). Field-error copy is in state 4 below.

**The four states** — a single `status` state machine: `"idle" | "submitting" | "success" | "error"`, with a `fieldErrors` map and an `errorKind` of `"validation" | "rate_limit" | "server"`:

1. **Idle** — the form as described. Submit button enabled, label **"Send Enquiry"**.
2. **Submitting** — on submit, `status = "submitting"`: disable all inputs and the button (`disabled`, not just visual); button shows the design-system Disabled treatment (`bg-page`, `border rgba(196,151,58,0.25)`, `text-text-muted`, `opacity 0.5`) with label **"Sending…"**. Any "…" animation respects reduced-motion.
3. **Success** (server `201`) — replace the form body with a success panel: a 28px gold rule, eyebrow **"Enquiry received"** (gold, 0.22em), H3 `font-serif text-[26px] text-text-primary` **"Thank you — we've got your enquiry."**, and a Montserrat 13px/400 `text-text-muted` line: **"A member of the ALEF team will get back to you shortly. For anything urgent, call the Dubai office on +971 4 2513840."** Include a ghost "Send another enquiry →" link (gold) that resets to idle. (The honeypot path returns 201 with no row — a bot sees this same panel, which is intended.)
4. **Error** — `status = "error"`; copy depends on `errorKind`:
   - **Validation (`400`, or client-side):** keep the form populated, mark offending fields, show a top-of-form line **"Please check the highlighted fields and try again."** (Montserrat 12px/400 `text-text-secondary`, gold left rule). Field-level copy: Name → "Please enter your name."; Email → "Please enter a valid email address."; Message → "Please tell us how we can help.".
   - **Rate limit (`429`):** top-of-form line **"You've sent a few enquiries already. Please wait a little while before sending another — or call us on +971 4 2513840."** Keep the form populated; keep the button enabled.
   - **Server / network (`5xx` or fetch throw):** top-of-form line **"Something went wrong on our side and your enquiry didn't send. Please try again, or email us directly at alefllc@eim.ae."** Keep the form populated and the button enabled so the user can retry.

**POST payload** (architecture §3.4) — the client sends exactly:
```json
{ "name": "…", "email": "…", "phone": "…", "company": "…", "message": "…", "website": "" }
```
`source` and `created_at` are set server-side; the client never sends them. The `id` in the 201 response is informational only and not shown to the user.

**Accessibility (F11-AC8 budget):** honeypot `aria-hidden` + out of tab order; error messages associated via `aria-describedby`; disabled state uses the `disabled` attribute; visible gold focus outline on inputs/links.

**Responsive:** single-column form full-width; on `lg` the form may be constrained to `max-w-[560px]`. Submit full-width on mobile, auto on desktop.

---

## 8. Concierge entry-point slot (Home hero) — F11-AC1

Rendered inside `HomeHero` (§1.2), above the fold. It is a styled placeholder that 001 later rewires to the streaming chat UI; until then it is **never dead** — it routes to `/contact#contact-form`.

**Treatment:** a two-button CTA row (`mt-10 flex flex-col sm:flex-row gap-4`):
- **Primary — the concierge slot:** design-system Primary button. `bg-gold text-page px-8 py-[16px] font-sans text-[10px] font-bold uppercase` letter-spacing 0.15em, hover `bg-gold-light`, no radius. **Label: "Ask the Concierge →"**. `href="/contact#contact-form"`. Directly below it, a Montserrat 9px/400 `text-text-muted` micro-line: **"Grounded answers on capability, capacity & turnaround."** (sets expectation for what 001 will deliver; reads fine today routing to the form).
- **Secondary:** design-system Secondary button (`border 1px solid #C4973A`, `text-gold`, transparent bg, hover bg `rgba(196,151,58,0.08)`), no radius. **Label: "View Portfolio →"**. `href="/projects"`.

The "Enquire Now" nav CTA (§3.1) also points to `/contact` for a consistent entry path. When 001 lands, both the hero primary and the nav CTA are rewired to open the concierge; this spec keeps them pointing at `/contact` so nothing is dead in the interim.

---

## 9. New typed clients, types, and content constants (reference — Dev implements)

Mirrors the 003/005 convention (architecture §5.4). Listed so Dev knows the shape the components above expect. No new design decisions here.

| Path | Export | Notes |
|---|---|---|
| `web/src/lib/types/team.ts` | `TeamMember`, `TeamResponse` | §5 shape; all optional fields nullable |
| `web/src/lib/api/team.ts` | `getTeam()` | returns `{ members }`; `next: { revalidate: 60 }` |
| `web/src/lib/types/office.ts` | `Office`, `OfficesResponse` | §1.5 shape; `phones`/`addressLines` arrays; `mapQuery` nullable |
| `web/src/lib/api/offices.ts` | `getOffices()` | returns `{ offices }`; same caching |
| `web/src/lib/types/lead.ts` | `LeadPayload`, `LeadResponse` | payload = the 6 fields incl. `website`; response `{ id, status }` |
| `web/src/lib/api/leads.ts` | `submitLead(payload)` | POST `/api/leads`; no cache; throws on non-2xx carrying the status so the form maps 400/429/5xx |
| `web/src/lib/content/services.ts` | `ServiceItem`, `SERVICES` | the 8 rows in §6, verbatim |

Home reuses the existing `web/src/lib/api/projects.ts` and `web/src/lib/api/trust.ts` — no new client for its data (F11-AC2).

---

## 10. Accessibility, mobile & Lighthouse budget (F11-AC8)

- **Semantic headings:** one `<h1>` per page (the hero heading); section headings `<h2>`; card titles `<h3>`. No skipped levels.
- **Images:** every `<img>` has a meaningful `alt` (project name, member name); decorative overlays/diamonds are `aria-hidden`/CSS-only.
- **Contrast:** all text tokens on their specified surfaces meet the design-system pairings already shipped. On the gold band use dark tones (`rgba(7,17,28,*)`) as `TrustStats` does — never `text-slate`/`text-text-disabled` on gold.
- **Reduced motion:** the landmark marquee (§4), any hover-scale, and the "Sending…" affordance all respect `prefers-reduced-motion: reduce`.
- **Keyboard:** honeypot out of tab order; all links/buttons focusable with a visible gold focus outline.
- **SSR for crawlers (SEO):** all primary content is server-rendered (§2) — no client-side data waterfall for content; only `ContactForm` is client (interaction, not content). Each page sets `metadata` title/description inheriting the root template "%s | ALEF Architectural & Cadding Services".
- **Lazy images/iframes:** project/team images and the map iframe use `loading="lazy"`.

---

## 11. Acceptance-criteria traceability

| AC | Satisfied by |
|---|---|
| **F11-AC1** Single hero + concierge entry point above fold | §1.2 `HomeHero` (one hero, replaces three banners) + §8 concierge slot (primary gold CTA, routes to `/contact#contact-form` until 001), above the fold. |
| **F11-AC2** Featured projects (003) + trust stats (005), no duplication | §1.2 `FeaturedProjects` wrapping 003 `ProjectGrid`/`ProjectCard` fed by `getProjects({featurable:true})`; `TrustLayer` (005) mounted verbatim fed by `getTrustOverview()`; `SoftwareBadges` on Services also reads 005. No stored copy. |
| **F11-AC3** Company history + 10 team profiles, API-served, admin-manageable, responsive grid | §2.2 About: `CompanyStory` (ALEF + Jasper India) + `TeamGrid`/`TeamCard` fed by `getTeam()`; grid flows for N cards (§1.3/§5), no hardcoded count. |
| **F11-AC4** Typos corrected; "Cadding" preserved | §1.3 `CompanyStory` static copy corrects "Intenational"→"International", "Concorse"→"Concourse"; §3 + repeated guards keep "Cadding". |
| **F11-AC5** All 8 specializations reframed as outcomes, nothing dropped | §6 `ServiceCard` + the 8-row `SERVICES` constant — discipline eyebrow preserves each live-site capability name, title/body reframe as outcome. |
| **F11-AC6** Contact form submits, creates lead, success/error state | §7 `ContactForm` (client) → `submitLead()` → `POST /api/leads`; idle/submitting/success/error (validation/429/5xx) states with exact copy. |
| **F11-AC7** Both offices + working maps + direct channels | §1.5 `OfficeLocations`/`OfficeCard` (Dubai + India from `getOffices()`) with multi-line address, phones, email; `ContactMap` keyless iframe; `DirectChannels` (phone/email/concierge/WhatsApp slots). |
| **F11-AC8** Shared nav/footer, mobile + Lighthouse budgets | §3 nav/footer inherited + wired; §10 accessibility/SSR/reduced-motion; all four heroes share `PageHero` treatment; responsive breakpoints per §0. |
| **F11-AC9** Copyright year dynamic + stale content fixed | §3.2 footer year `new Date().getFullYear()`; "Alef"→"ALEF" copyright casing fix. |

---

## 12. What this design does NOT introduce (guardrails)

- No new palette color, no new font, no border-radius on any card or button (design-system sharp-corner philosophy held throughout).
- No redesign of 003 `ProjectCard`/`ProjectGrid` or 005 `TrustLayer`/`TrustStats` — reused verbatim.
- No live concierge (001), no Arabic localization (008), no CMS/admin editing (012), no WhatsApp wiring (009) — those are reserved placeholder slots only.
- No `lead.created` event UI — the form persists and confirms; delivery is 001's concern (DEC-018). The success copy therefore promises a human follow-up, not an instant automated reply.
