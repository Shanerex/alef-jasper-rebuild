---
feature: 005-trust-layer
spec_id: "005"
phase: design
owner: Designer
status: approved
version: "1.0"
entry_criteria:
  - upstream phase approved
exit_criteria:
  - Designer has produced this file and a human has approved it
---

# Design: Trust Layer

This design bridges the approved architecture (`architecture.md`) to implementation for the web
presentation of the Trust Layer. It names concrete components, files, prop interfaces, type shapes,
and provides full UI templates (copy-paste-ready JSX/TSX). It contains no backend implementation
code — the API (`GET /api/trust/overview`), the `trust_content` table, and the composition rules are
fixed by `architecture.md` and are not re-litigated here. Every visual choice traces to
`DESIGN_SYSTEM.md`; no new visual primitive is invented.

The Trust Layer is a **portable, self-contained section** with no route of its own. F011 (Home)
mounts it. This feature delivers the section, its three sub-components, the typed API client, and the
types.

---

## 1. Section Anatomy & Layout

The Trust Layer is one vertical block on a dark page, composed top-to-bottom of three bands. It
follows the page system: max-width `1280px` (`max-w-site`), `80px` side padding, section vertical
padding in the `100–120px` band (DESIGN_SYSTEM Layout).

```
┌──────────────────────────────────────────────────────────────┐
│  TrustLayer  <section>  bg-page (#07111C)                      │
│                                                                │
│   Section label  "OUR TRACK RECORD"  (gold, 10px/700)          │
│   Display L headline + Arabic accent subtitle                  │
│                                                                │
│   ┌──── BAND 1: TrustStats ────────────────────────────────┐  │
│   │  full-width gold-inverted Stat Display row (4 stats)    │  │
│   │  bg #C4973A · Cormorant numbers · Montserrat labels     │  │
│   └─────────────────────────────────────────────────────────┘  │
│                                                                │
│   ┌──── BAND 2: ClientStrip ───────────────────────────────┐  │
│   │  dark band · "Trusted by" label · named text strip      │  │
│   │  clients + contractors as gold-ruled name items         │  │
│   └─────────────────────────────────────────────────────────┘  │
│                                                                │
│   ┌──── BAND 3: CapabilityBadges ──────────────────────────┐  │
│   │  dark band · two label groups (Software / Standards)    │  │
│   │  gold-muted capability badges                           │  │
│   └─────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

### 1.1 Band rationale (traced to DESIGN_SYSTEM)

- **Band 1 — TrustStats** is the single gold-inverted element in the whole section. DESIGN_SYSTEM
  names the **Stat Display** as the one inverted surface (`bg #C4973A`, dark numbers/labels). It is
  visually loudest and therefore placed first — the scale story leads. It runs edge-to-edge inside the
  `1280px` container as a 4-up row.
- **Band 2 — ClientStrip** and **Band 3 — CapabilityBadges** stay on the dark surfaces
  (`bg-page` / `surface-2`), per the constraint "Stat Display is the one gold-inverted element;
  everything else stays on the dark surfaces" (handoff 2, Constraints).
- A thin **gold rule** (2px `#C4973A`) optionally separates the section from whatever F011 places above
  it; the section itself owns top/bottom padding only and stays mountable anywhere.

### 1.2 Vertical rhythm

- Section vertical padding: `py-[120px]` desktop (the `2xl` step, 120px), reducing on smaller
  breakpoints (see §6).
- Gap between the heading block and Band 1: `40px` (`lg` step).
- Gap between Band 1 → Band 2 → Band 3: `80px` (`xl` step) on desktop, `40px` on mobile.

### 1.3 Heading block (section chrome)

Reuses the established hero/section header pattern from `projects/page.tsx`:

- Section label: `Montserrat 10px / 700`, uppercase, `letter-spacing 0.22em`, `text-gold`. Copy:
  `"OUR TRACK RECORD"`.
- Headline: `Cormorant Garamond 48px / 300` (Display L), `text-text-primary`. Copy:
  `"Built on Scale & Trust"` (placeholder copy; final wording is a content task — variable length is
  designed for).
- Arabic accent subtitle: `Noto Serif Arabic`, reduced gold opacity `rgba(196,151,58,0.5)`,
  `direction: rtl`, matching `projects/page.tsx`. This is decorative only.

---

## 2. Components

All components live in `web/src/components/trust/`. They are **pure presentational, server-renderable,
no client state** (architecture §4: "no client state"). File naming follows the existing
kebab-case convention (`project-card.tsx` → `trust-stats.tsx`).

| Component | File | Props | Responsibility |
|-----------|------|-------|----------------|
| `TrustLayer` | `trust-layer.tsx` | `{ data: TrustOverview }` | Composes the section: heading block + the three bands. Renders only bands whose arrays are non-empty (graceful degradation). |
| `TrustStats` | `trust-stats.tsx` | `{ stats: TrustStat[] }` | Band 1 — gold-inverted Stat Display row (F5-AC1). |
| `ClientStrip` | `client-strip.tsx` | `{ clients: string[]; contractors: string[] }` | Band 2 — named text strip (F5-AC2). |
| `CapabilityBadges` | `capability-badges.tsx` | `{ software: string[]; standards: string[] }` | Band 3 — gold-muted capability badges (F5-AC3). |

Each takes already-fetched data as props; none fetches. The data fetch happens in the F011 page (or in
a thin server wrapper) via the API client in §4.

---

## 3. Visual Specifications

### 3.1 TrustStats (F5-AC1) — Stat Display pattern

Traces directly to DESIGN_SYSTEM "Stat Display":

| Element | Token / value |
|---------|---------------|
| Cell background | Gold Primary `#C4973A` (`bg-gold`) — the inverted surface |
| Number (`value` + `unit` inline suffix) | Cormorant Garamond, 48px desktop / 42px tablet, weight 500, color `#07111C` (`text-page`) |
| `unit` suffix (when present) | Montserrat 11px / 700, uppercase, `letter-spacing 0.12em`, color `rgba(7,17,28,0.55)`, rendered on a second line beneath the number |
| Label | Montserrat 8px / 700, uppercase, `letter-spacing 0.14em`, color `rgba(7,17,28,0.6)` |
| Cell padding | `40px 32px` (the `lg`/`md` steps) |
| Separators between cells | 1px vertical rule `rgba(7,17,28,0.12)` (dark hairline on gold), desktop only |

- **Order (open question resolved, §7.2):** `years_in_business`, `staff_count`,
  `monthly_steel_capacity_tonnes`, `marquee_projects` — left to right. The API already returns them in
  `display_order`; the component renders in array order and does **not** re-sort, so F012/content owns
  ordering. The marquee count sits **with** the other three as a peer (not set apart) — it reads as one
  unified scale story.
- **Variable-length values:** values are display strings shown verbatim ("18", "120+", "5,000"). Cells
  are equal-width flex/grid columns; text is not fixed-width. Long values wrap within the cell rather
  than overflow. No numeric assumptions.
- **`unit` is optional:** rendered as a sub-line only when non-null; absent units leave the number alone
  (no empty line reserved).

### 3.2 ClientStrip (F5-AC2) — named text strip

The strip is **dark-surface**, not inverted. Resolves the text-strip-vs-logo-grid open question to
**text strip** (§7.1).

| Element | Token / value |
|---------|---------------|
| Band background | `bg-page` `#07111C` (sits on the page; no card) |
| Group eyebrow ("Trusted By" / "Main Contractors") | Section Label: Montserrat 10px / 700, uppercase, `letter-spacing 0.22em`, `text-gold` |
| Name item | Montserrat 13px / 300, `letter-spacing 0.04em`, color `text-secondary` `#C8C0B0` |
| Item separator | Gold diamond/middot `·` in `gold-subtle`, or a 1px `gold-subtle` vertical rule between items |
| Row gap | `24px` horizontal, `16px` vertical (wraps) |
| Top rule of band | 1px `gold-subtle` (`rgba(196,151,58,0.2)`) divider above the band |

- Two sub-groups: **clients** (`clients[]`) labelled "Trusted By", and **contractors**
  (`contractors[]`) labelled "Main Contractors". Each renders only if its array is non-empty.
- Names flow as an inline-wrapping list (flexbox `flex-wrap`) separated by a gold middot. This reads as a
  curated marquee of names — quiet, editorial, trust-conveying — not a noisy logo wall.
- No interactivity, no links (architecture: names are derived strings, no per-client page exists).

### 3.3 CapabilityBadges (F5-AC3) — gold-muted capability badges

Reuses the existing `Badge` component (`web/src/components/ui/badge.tsx`, `variant="outline"` →
`border-gold/30 text-gold`) so software/standards match the portfolio's scope-tag treatment exactly.
Each group is introduced by a Service-Card-style gold rule + label.

| Element | Token / value |
|---------|---------------|
| Band background | `surface-2` `#0E2035` card OR `bg-page` band — see template; default `bg-page` with a `surface-2` inner card per group |
| Group title ("Software" / "Standards") | Montserrat 10px / 700, uppercase, `letter-spacing 0.22em`, `text-gold` (Section Label) |
| Gold rule above each group title | 28px wide × 1px `bg-gold` (DESIGN_SYSTEM Service Card gold rule) |
| Badge | `Badge variant="outline"`: border `gold/30`, text gold, Montserrat 8px / 700 uppercase, `letter-spacing 0.14em`, sharp corners, padding `px-2.5 py-1` |
| Badge gap | `8px` (`xs`) row and column |

- Two groups: **Software** (`software[]` — AutoCAD, CADS RC, SteelPac RC) and **Standards**
  (`standards[]` — BS 8666, ACI 318, BS EN ISO 3766). Each renders only if non-empty.
- Variable count: badges wrap freely; no fixed grid.

---

## 4. API Client & Types

Mirrors the F003 convention (`web/src/lib/api/projects.ts`, `web/src/lib/types/project.ts`).

### 4.1 Types — `web/src/lib/types/trust.ts`

```ts
/**
 * TypeScript mirrors of the GET /api/trust/overview DTO (architecture 3.1).
 *
 * All fields are display-ready strings from the API; the web layer never
 * derives or formats stat values. Any array may be empty -> the matching
 * band does not render (graceful degradation, architecture 3.1 / 4).
 */

/** One headline stat in the Stat Display row (F5-AC1). */
export interface TrustStat {
  /** Stable machine key, e.g. "years_in_business" | "marquee_projects". */
  key: string;
  /** Display label, e.g. "Years in Business". */
  label: string;
  /** Display value shown verbatim, e.g. "18", "120+", "5,000". */
  value: string;
  /** Optional unit/suffix, e.g. "tonnes / month"; null when absent. */
  unit: string | null;
}

/** Composed payload backing the whole Trust Layer (architecture 3.1). */
export interface TrustOverview {
  stats: TrustStat[];
  clients: string[];
  contractors: string[];
  software: string[];
  standards: string[];
}
```

### 4.2 API client — `web/src/lib/api/trust.ts`

```ts
import type { TrustOverview } from "@/lib/types/trust";

/**
 * Base URL for the API service.
 *
 * In Docker: http://api:8080 (server-side SSR fetches via the Docker network).
 * Locally in dev: http://localhost:8080. Mirrors lib/api/projects.ts.
 */
const API_BASE_URL = process.env.API_BASE_URL || "http://localhost:8080";

/**
 * Fetches the composed Trust Layer payload (stats, client/contractor names,
 * software/standards) in one read (architecture 3.1).
 *
 * Uses Next's revalidate cache option so an F012 edit surfaces within the ISR
 * window without a redeploy, consistent with the portfolio client.
 */
export async function getTrustOverview(): Promise<TrustOverview> {
  const res = await fetch(`${API_BASE_URL}/api/trust/overview`, {
    next: { revalidate: 60 },
  });

  if (!res.ok) {
    throw new Error(`Failed to fetch trust overview: ${res.status}`);
  }

  return res.json();
}
```

### 4.3 Data-fetch / ISR pattern

The Trust Layer is mounted by F011 (Home), so this feature does **not** own the page. It exposes a
clean contract for F011: fetch once via `getTrustOverview()`, pass the result to `<TrustLayer data=…/>`.
Two acceptable wiring shapes (Dev/F011 picks; this feature designs for either):

1. **F011 fetches and passes down** (preferred): the Home server component calls `getTrustOverview()`
   alongside its other fetches and renders `<TrustLayer data={trust} />`. This keeps one ISR window for
   the whole page.
2. **Self-fetching async wrapper** (fallback): an async server component `TrustLayerSection` that calls
   `getTrustOverview()` itself and renders `<TrustLayer data={…} />`. Useful if F011 wants the section to
   be drop-in with zero data wiring.

Rendering strategy follows F003's pragmatic note (`projects/page.tsx`): the architecture prescribes ISR
(`revalidate=60`), but in the Docker Compose build the web image builds before `api` is up. F011 should
therefore use the same approach the portfolio page settled on — `force-dynamic` at the page level (or
the documented switch to `export const revalidate = 60` once the API is build-time-available). The
client's `next: { revalidate: 60 }` is preserved so the Data Cache still applies. This is an F011 page
concern; the Trust Layer components are render-strategy-agnostic.

---

## 5. UI Templates (copy-paste-ready)

These are full component templates using Tailwind classes and the exact tokens above, following the
conventions in `web/src/components/projects/`. Doc comments are intent-focused per the project invariant
(CLAUDE.md #6).

### 5.1 `web/src/components/trust/trust-stats.tsx`

```tsx
import type { TrustStat } from "@/lib/types/trust";

interface TrustStatsProps {
  stats: TrustStat[];
}

/**
 * Band 1 of the Trust Layer (F5-AC1) -- the gold-inverted Stat Display row.
 *
 * Renders the headline stats (years in business, staff count, monthly steel
 * capacity, marquee project count) as the one inverted surface in the section
 * per DESIGN_SYSTEM.md "Stat Display": gold bg #C4973A, Cormorant numbers,
 * Montserrat uppercase labels, dark text. Values are display strings shown
 * verbatim ("18", "120+", "5,000"); never formatted here. Renders nothing when
 * the array is empty (graceful degradation).
 */
export function TrustStats({ stats }: TrustStatsProps) {
  if (stats.length === 0) return null;

  return (
    <div className="grid grid-cols-2 bg-gold lg:grid-cols-4">
      {stats.map((stat, index) => (
        <div
          key={stat.key}
          className="border-page/10 px-8 py-10 sm:border-l first:border-l-0 lg:border-l"
          style={{
            borderLeftColor: index === 0 ? "transparent" : "rgba(7,17,28,0.12)",
          }}
        >
          {/* Number + optional unit */}
          <p className="font-serif text-[42px] font-medium leading-[1.05] text-page lg:text-[48px]">
            {stat.value}
          </p>
          {stat.unit && (
            <p
              className="mt-1 font-sans text-[11px] font-bold uppercase"
              style={{
                letterSpacing: "0.12em",
                color: "rgba(7,17,28,0.55)",
              }}
            >
              {stat.unit}
            </p>
          )}

          {/* Label */}
          <p
            className="mt-3 font-sans text-[8px] font-bold uppercase"
            style={{ letterSpacing: "0.14em", color: "rgba(7,17,28,0.6)" }}
          >
            {stat.label}
          </p>
        </div>
      ))}
    </div>
  );
}
```

### 5.2 `web/src/components/trust/client-strip.tsx`

```tsx
import type { Fragment } from "react";

interface ClientStripProps {
  clients: string[];
  contractors: string[];
}

/**
 * Band 2 of the Trust Layer (F5-AC2) -- a named text strip of past clients and
 * main contractors, derived live from the portfolio.
 *
 * v1 is a styled text strip (no logo assets): names flow as a wrapping inline
 * list separated by a gold middot, on the dark page surface. Each group renders
 * only when its array is non-empty; the whole band returns null when both are
 * empty (graceful degradation). No interactivity -- credibility, not navigation.
 */
export function ClientStrip({ clients, contractors }: ClientStripProps) {
  if (clients.length === 0 && contractors.length === 0) return null;

  return (
    <div
      className="border-t pt-12"
      style={{ borderTopColor: "rgba(196,151,58,0.2)" }}
    >
      {clients.length > 0 && (
        <NameGroup label="Trusted By" names={clients} />
      )}
      {contractors.length > 0 && (
        <NameGroup
          label="Main Contractors"
          names={contractors}
          className={clients.length > 0 ? "mt-10" : ""}
        />
      )}
    </div>
  );
}

interface NameGroupProps {
  label: string;
  names: string[];
  className?: string;
}

/**
 * One labelled row of names -- a gold Section Label eyebrow above a wrapping
 * list of names separated by gold middots. Shared by the clients and
 * contractors groups so both read identically.
 */
function NameGroup({ label, names, className = "" }: NameGroupProps) {
  return (
    <div className={className}>
      <p
        className="mb-5 font-sans text-[10px] font-bold uppercase text-gold"
        style={{ letterSpacing: "0.22em" }}
      >
        {label}
      </p>
      <ul className="flex flex-wrap items-center gap-x-6 gap-y-4">
        {names.map((name, index) => (
          <li key={name} className="flex items-center gap-x-6">
            {index > 0 && (
              <span
                aria-hidden="true"
                className="text-gold"
                style={{ opacity: 0.4 }}
              >
                &middot;
              </span>
            )}
            <span
              className="font-sans text-[13px] font-light text-text-secondary"
              style={{ letterSpacing: "0.04em" }}
            >
              {name}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
```

> Note: the `import type { Fragment }` line above is illustrative only; the template does not require it
> — Dev should omit unused imports. Kept here so the snippet is obviously a real file, not pseudocode.
> (Dev: delete that first line.)

### 5.3 `web/src/components/trust/capability-badges.tsx`

```tsx
import { Badge } from "@/components/ui/badge";

interface CapabilityBadgesProps {
  software: string[];
  standards: string[];
}

/**
 * Band 3 of the Trust Layer (F5-AC3) -- software and detailing standards shown
 * as gold-muted capability badges.
 *
 * Reuses the portfolio Badge (variant="outline") so capability tags match the
 * scope-tag treatment. Each group has a Service-Card-style gold rule + Section
 * Label, then a wrapping row of badges. A group renders only when non-empty;
 * the band returns null when both are empty (graceful degradation).
 */
export function CapabilityBadges({ software, standards }: CapabilityBadgesProps) {
  if (software.length === 0 && standards.length === 0) return null;

  return (
    <div className="grid grid-cols-1 gap-12 md:grid-cols-2">
      {software.length > 0 && (
        <BadgeGroup title="Software" items={software} />
      )}
      {standards.length > 0 && (
        <BadgeGroup title="Standards" items={standards} />
      )}
    </div>
  );
}

interface BadgeGroupProps {
  title: string;
  items: string[];
}

/**
 * One capability group: a 28px gold rule, a gold Section Label, and a wrapping
 * row of outline badges. Shared by the software and standards groups.
 */
function BadgeGroup({ title, items }: BadgeGroupProps) {
  return (
    <div>
      {/* Gold rule (Service Card pattern) */}
      <div className="mb-4 h-[1px] w-7 bg-gold" />
      <p
        className="mb-5 font-sans text-[10px] font-bold uppercase text-gold"
        style={{ letterSpacing: "0.22em" }}
      >
        {title}
      </p>
      <div className="flex flex-wrap gap-2">
        {items.map((item) => (
          <Badge key={item} variant="outline">
            {item}
          </Badge>
        ))}
      </div>
    </div>
  );
}
```

### 5.4 `web/src/components/trust/trust-layer.tsx` (composition)

```tsx
import type { TrustOverview } from "@/lib/types/trust";
import { TrustStats } from "@/components/trust/trust-stats";
import { ClientStrip } from "@/components/trust/client-strip";
import { CapabilityBadges } from "@/components/trust/capability-badges";

interface TrustLayerProps {
  data: TrustOverview;
}

/**
 * The Trust Layer (feature 005) -- a portable, self-contained credibility
 * section mounted on Home by F011. Surfaces scale and track record where buyers
 * decide: headline stats (F5-AC1), a client/contractor name strip (F5-AC2), and
 * software/standards capability badges (F5-AC3).
 *
 * Dark Prestige per DESIGN_SYSTEM.md: dark page surface throughout, with the
 * Stat Display row as the single gold-inverted element. Composes three bands;
 * each band self-hides when its data is empty, so the section degrades
 * gracefully to whatever content exists. No client state, no interactivity.
 */
export function TrustLayer({ data }: TrustLayerProps) {
  const { stats, clients, contractors, software, standards } = data;

  // Nothing to show at all -> render nothing rather than an empty section.
  const hasAny =
    stats.length > 0 ||
    clients.length > 0 ||
    contractors.length > 0 ||
    software.length > 0 ||
    standards.length > 0;
  if (!hasAny) return null;

  return (
    <section className="bg-page py-20 lg:py-[120px]">
      <div className="mx-auto max-w-site px-6 sm:px-12 lg:px-[80px]">
        {/* Heading block */}
        <div className="mb-10">
          <p
            className="mb-4 font-sans text-[10px] font-bold uppercase text-gold"
            style={{ letterSpacing: "0.22em" }}
          >
            Our Track Record
          </p>
          <h2 className="mb-3 font-serif text-[36px] font-light leading-[1.2] text-text-primary lg:text-[48px]">
            Built on Scale &amp; Trust
          </h2>
          <p
            className="font-arabic text-[20px] font-light"
            style={{ color: "rgba(196,151,58,0.5)", direction: "rtl" }}
          >
            &#1582;&#1576;&#1585;&#1577; &#1608;&#1602;&#1583;&#1585;&#1577;
          </p>
        </div>

        {/* Band 1 -- gold Stat Display row */}
        <TrustStats stats={stats} />

        {/* Band 2 -- client / contractor name strip */}
        {(clients.length > 0 || contractors.length > 0) && (
          <div className="mt-12 lg:mt-20">
            <ClientStrip clients={clients} contractors={contractors} />
          </div>
        )}

        {/* Band 3 -- software / standards capability badges */}
        {(software.length > 0 || standards.length > 0) && (
          <div className="mt-12 lg:mt-20">
            <CapabilityBadges software={software} standards={standards} />
          </div>
        )}
      </div>
    </section>
  );
}
```

### 5.5 Optional self-fetching wrapper — `web/src/components/trust/trust-layer-section.tsx`

Provided for F011 drop-in use (§4.3 option 2). F011 may instead fetch and pass `data` directly.

```tsx
import { getTrustOverview } from "@/lib/api/trust";
import { TrustLayer } from "@/components/trust/trust-layer";

/**
 * Drop-in async server wrapper: fetches the composed trust payload and renders
 * the Trust Layer. Lets F011 mount the section with zero data wiring. If F011
 * prefers a single page-level fetch, use <TrustLayer data={...}/> directly and
 * skip this wrapper. Fails soft -> renders nothing if the API is unreachable,
 * so the Home page never breaks on a trust fetch error.
 */
export async function TrustLayerSection() {
  try {
    const data = await getTrustOverview();
    return <TrustLayer data={data} />;
  } catch {
    return null;
  }
}
```

---

## 6. Responsive Behaviour

Breakpoints use Tailwind defaults (`sm` 640, `md` 768, `lg` 1024). Mobile-first, matching
`project-grid.tsx`.

| Region | Desktop (`lg+`) | Tablet (`sm`–`md`) | Mobile (`<sm`) |
|--------|-----------------|--------------------|----------------|
| Section padding | `py-[120px]`, `px-[80px]` | `py-20`, `px-12` | `py-20`, `px-6` |
| Heading | Display L 48px | 40–48px | 36px |
| **TrustStats** | 4 columns (`lg:grid-cols-4`), vertical hairline separators | 2 columns (`grid-cols-2`) | 2 columns (`grid-cols-2`) — keeps stats compact, avoids a tall single-file stack |
| Stat number | 48px | 42px | 42px |
| **ClientStrip** | inline wrapping list with middots | same, wraps sooner | same, wraps to multiple lines; middots preserved |
| **CapabilityBadges** | 2 groups side by side (`md:grid-cols-2`) | 2 columns at `md`, 1 below | 1 column stacked |
| Inter-band gap | `80px` (`mt-20`) | `48px` | `48px` (`mt-12`) |

Notes:
- TrustStats uses a 2-up grid on mobile rather than 1-up so four numbers do not produce an excessively
  tall gold block; this keeps the inverted band proportionate to the dark section around it.
- No horizontal scroll anywhere; all rows wrap.

---

## 7. Open Question Resolutions

### 7.1 Client/contractor — text strip vs. logo grid → **Text strip (v1)**

Resolved to the **named text strip** (the Architect's default). Rationale:

- The data is names (strings) derived live from `project`; there is **no logo asset pipeline** and none
  is in scope. A logo grid would require sourcing, clearing, optimising, and storing image assets, plus
  an architecture change to add an image-path column to a derived-data path — disproportionate for a
  near-static credibility band.
- A quiet, editorial name strip suits the Dark Prestige tone better than a noisy logo wall and reads as
  "calibre of past clients" (the F5-AC2 user story) without competing with the gold Stat Display.
- **No asset-sourcing dependency is flagged back to Architecture.** If a future iteration wants logos,
  that is a new Design+Architecture change (add an optional image path to a client/contractor source);
  it is explicitly out of scope for v1 and does not block Dev.

### 7.2 Stat ordering & grouping → **Single unified row, fixed order, marquee count as a peer**

- Visual order, left→right: **Years in Business · Detailing Engineers · Monthly Steel Capacity ·
  Marquee Projects**.
- The component renders in **API array order** and never re-sorts, so `display_order` (owned by content/
  F012) is authoritative; the marquee count is appended by the API as the last stat and therefore sits
  last by default.
- The marquee count is **grouped with** the other three (one 4-up row), not set apart — the four
  together read as a single scale-and-track-record statement, which is the F5-AC1 intent.

### 7.3 Bilingual readiness (F008) — layout notes, not implemented here

F008 localisation of `label`/`value` is deferred; this design only ensures it is not a retrofit:

- **No hardcoded English copy in the stat/badge data path.** All stat labels/values and badge text come
  from the API payload (§3.1, §4.1). The only English strings the components own are the structural
  eyebrows ("Trusted By", "Main Contractors", "Software", "Standards") and the section heading — these
  are the natural F008 i18n surface and are isolated to `trust-layer.tsx` / the group sub-components, not
  scattered.
- **RTL tolerance.** Bands use logical, flow-based layouts (flex-wrap lists, grids) that mirror cleanly
  under `dir="rtl"`. The `border-l` separators in TrustStats and the middot separators in ClientStrip are
  symmetric and need no special RTL handling beyond a future `dir` flip. The Arabic accent subtitle
  already uses `direction: rtl`, matching `projects/page.tsx`.
- **Longer strings.** Arabic labels can run longer; all label slots already wrap (no fixed widths, no
  truncation, no `text-nowrap`), so Band heights flex. No layout assumes a label fits on one line.
- No further F008 work is done here; this is a readiness note only.

---

## 8. Empty-State / Graceful Degradation Summary

Every level self-hides when empty, so the section renders only the content that exists (architecture
§3.1: "200 with empty arrays … the page renders only the sections that have content"):

| Condition | Behaviour |
|-----------|-----------|
| `stats` empty | TrustStats renders `null`; the gold band disappears. |
| `clients` and `contractors` both empty | ClientStrip renders `null`; band gone. |
| only one of clients/contractors present | only that labelled group shows. |
| `software` and `standards` both empty | CapabilityBadges renders `null`; band gone. |
| only one of software/standards present | only that group shows (single column). |
| everything empty | TrustLayer renders `null`; no empty section, no orphan heading. |

Inter-band margins live on the wrapper conditionals in `trust-layer.tsx`, so a hidden band leaves no
double gap.

---

## 9. Accessibility

- **Semantic structure:** the block is a `<section>`; the heading is an `<h2>` (it nests under the
  Home page `<h1>`, owned by F011 — the section deliberately uses `h2`, not `h1`). Each band group label
  is a `<p>` eyebrow, not a heading, to avoid heading-rank noise; if F011 wants them in the document
  outline, they can be promoted to `<h3>` — flagged to Dev as a low-risk option.
- **ClientStrip** uses a `<ul>/<li>` list so assistive tech announces the count of names; the middot
  separators are `aria-hidden` decorative spans.
- **CapabilityBadges** groups are lists of short tags; badge text is real text (not an icon), so it is
  readable by screen readers with no extra ARIA.
- **Contrast:** the gold Stat Display uses dark text (`#07111C` / `rgba(7,17,28,0.6)`) on gold `#C4973A`
  — this is the DESIGN_SYSTEM-specified inversion and meets contrast for large display numbers and bold
  labels. Dark-band text uses `text-secondary` `#C8C0B0` / `text-gold` on `#07111C`, consistent with the
  rest of the site.
- **No interactivity** means no focus traps, no keyboard handlers, nothing to tab through beyond the page
  flow.
- **Images:** none in v1 (text strip), so no alt-text burden.

---

## 10. File Tree (Dev implementation checklist — web only)

The backend (`trust_content` table, V4/V5 migrations, `com.alef.api.trust.*` controller/service/dto,
`GET /api/trust/overview`) is fixed by `architecture.md` §3, §5, §6 and is not part of this Design's
file list. Web files introduced by this feature:

```
web/
└── src/
    ├── components/trust/
    │   ├── trust-layer.tsx            // §5.4  composition
    │   ├── trust-stats.tsx            // §5.1  Band 1 (F5-AC1)
    │   ├── client-strip.tsx           // §5.2  Band 2 (F5-AC2)
    │   ├── capability-badges.tsx      // §5.3  Band 3 (F5-AC3)
    │   └── trust-layer-section.tsx    // §5.5  optional self-fetching wrapper
    └── lib/
        ├── api/trust.ts               // §4.2  typed client
        └── types/trust.ts             // §4.1  DTO mirrors
```

Reused, not created: `web/src/components/ui/badge.tsx` (`variant="outline"`), Tailwind tokens in
`tailwind.config.ts` (`bg-gold`, `text-page`, `text-text-primary`, `text-text-secondary`, `text-gold`,
`max-w-site`, `font-serif`, `font-sans`, `font-arabic`).

---

## 11. Acceptance Criteria Traceability

| AC | Design element |
|----|----------------|
| **F5-AC1** Years in business, staff count, monthly steel capacity, marquee project count | `TrustStats` (§3.1, §5.1) renders `stats[]` as the gold-inverted Stat Display row; values/labels/unit straight from the API; marquee count is a peer stat (§7.2). |
| **F5-AC2** Client/contractor names as a logo or named strip | `ClientStrip` (§3.2, §5.2) renders `clients[]` + `contractors[]` as a styled named text strip on the dark surface; text-strip resolution recorded in §7.1. |
| **F5-AC3** Software & standards as capability badges | `CapabilityBadges` (§3.3, §5.3) renders `software[]` + `standards[]` as gold-muted outline badges with Service-Card gold-rule group headers. |

All three compose into the portable `TrustLayer` (§5.4) that F011 mounts; no route is added by this
feature.
