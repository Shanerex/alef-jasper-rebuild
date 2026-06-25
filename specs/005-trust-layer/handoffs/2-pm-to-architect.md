# Handoff: Trust Layer
| Field | Value |
|---|---|
| Feature # | 005 |
| From / To | Architect -> Designer |
| Status | ready |
| Spec | specs/005-trust-layer/architecture.md |

## What was decided

The Trust Layer is one self-contained, read-only page block (no user interaction beyond viewing), fed by a single API call `GET /api/trust/overview`. It has three parts mapping to the three acceptance criteria:

- **Headline stats (F5-AC1)** -> `stats[]`: years in business, staff count, monthly steel capacity, and a marquee project count. Each item is `{ key, label, value, unit }`. The marquee count is computed live from the portfolio; the other three come from a new `trust_content` table. Render these as the **Stat Display** pattern in DESIGN_SYSTEM.md (gold-inverted bg `#C4973A`, Cormorant number 42-48px, Montserrat 8px/700 uppercase label). `unit` may be null; render it as a suffix/sub-label when present (e.g. "tonnes / month").
- **Client / contractor strip (F5-AC2)** -> `clients[]` and `contractors[]`: distinct names pulled live from the portfolio. v1 renders these as a **named text strip** styled per the design system. A logo treatment is allowed but not required by the data; see open questions.
- **Capability badges (F5-AC3)** -> `software[]` (AutoCAD, CADS RC, SteelPac RC) and `standards[]` (international detailing standards). Render as **capability badges** in the gold-muted tag / Service Card style.

Data ownership: `api` is the source of truth. The editorial facts live in a new `trust_content` table (Flyway V4 schema, V5 seed). Project-derived facts (marquee count, client/contractor names) are derived from the existing `project` table and never duplicated. Rendering is ISR, consistent with F003.

## What the Designer needs to know

- Design the three sub-components — `TrustStats`, `ClientStrip`, `CapabilityBadges` — and how they compose into one `TrustLayer` block. Match the existing DESIGN_SYSTEM.md patterns named above; do not invent new visual primitives.
- The block is **placed on Home by F011**, not by this feature. Design the block as a portable, self-contained section.
- All copy/labels/values come from the API payload (`label`, `value`, `unit`, and the name/badge arrays). Do not hardcode stat strings in the design; design for variable-length labels and values (e.g. "120" vs "5,000", "18" vs "120+").
- Any group can come back empty -> design graceful degradation: a section with no items simply does not render. Design what the block looks like with, say, only stats and no logos.
- Stats are display strings shown verbatim (may include "+", commas, ranges). Do not assume fixed-width numerics.

## Constraints

- Dark-first, Dark Prestige system only (DESIGN_SYSTEM.md). Stat Display is the one gold-inverted element; everything else stays on the dark surfaces.
- No interactivity: no filters, toggles, hover-to-load, or client state. This is a static credibility block.
- Real client/contractor names appear (F5-AC2). Cleared for local demo by DEC-008; a pre-public-deploy gate still applies. Not a design blocker.

## Open questions for Design

1. **Client/contractor: text strip vs. logo grid.** The data is names (strings). Do you want a pure named text strip (v1, no assets needed), or a logo grid (requires sourcing/clearing logo image assets)? If logos, flag the asset-sourcing dependency back so Architecture can add an optional image path to `trust_content` before Dev. Default assumption is text strip.
2. **Stat ordering and grouping.** The API returns stats in `display_order`; confirm the visual order/grouping you want (e.g. is marquee count grouped with the other three or set apart?).
3. **Bilingual labels (F008).** Trust `label`/`value` localization is deferred to F008 and not designed here; note any layout that must accommodate Arabic later (RTL, longer strings) so it is not a retrofit.
