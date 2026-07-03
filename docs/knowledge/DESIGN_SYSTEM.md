# DESIGN_SYSTEM.md

> Visual direction for the entire ALEF site. Every feature's implementation must follow these guidelines.
> This is a GCC structural engineering / rebar detailing consultancy — not a SaaS product or a startup.
> The visual language conveys **precision, scale, and trust**.
> Direction: **Dark Prestige** — deep navy, gold accents, serif headlines, luxury engineering aesthetic.

## Fonts

Three font families (load from Google Fonts):
- **Cormorant Garamond** (serif) — all headings. Light weight (300) for display, 400 for H3+.
  Italic in gold for accent words.
- **Montserrat** (sans-serif) — body text, labels, nav, buttons. Light (300) for body,
  500 for nav, 700 for labels/buttons. Uppercase with letter-spacing for labels.
- **Noto Serif Arabic** — decorative Arabic accents (e.g. `ألف` beside the logo,
  Arabic subheadings). Weight 300, displayed at reduced opacity as an elegant touch.

## Color System

### Backgrounds & Surfaces
| Token | Hex | Usage |
|-------|-----|-------|
| Page BG | `#07111C` | Main page background |
| Surface 1 | `#0C1B2E` | Hero sections, elevated areas |
| Surface 2 (Cards) | `#0E2035` | Card backgrounds, nav dropdowns |
| Surface 3 | `#101F32` | Nested surfaces |
| Surface 4 (Hover) | `#122339` | Hover states on cards |
| Footer BG | `#040C14` | Footer |

### Gold Accent Palette
| Token | Hex | Usage |
|-------|-----|-------|
| Gold Primary | `#C4973A` | CTAs, accents, rules, section labels, active states |
| Gold Light | `#D4A94A` | Hover states |
| Gold Dark | `#A8812E` | Pressed states |
| Gold Subtle | `rgba(196,151,58,0.2)` | Borders, dividers |
| Gold Ghost | `rgba(196,151,58,0.08)` | Background tints, patterns |

### Text Colors
| Token | Hex | Usage |
|-------|-----|-------|
| Text Primary | `#E8E0D0` | Headlines, primary body text (warm cream) |
| Text Secondary | `#C8C0B0` | Subheadings |
| Text Muted | `#7A8FA8` | Captions, descriptions, nav links |
| Text Disabled | `#4A5B6E` | Placeholder text |
| Slate | `#5A6B80` | Meta text, sub-labels |

## Typography Scale

| Role | Font | Size | Weight | Line-height | Notes |
|------|------|------|--------|-------------|-------|
| Display XL | Cormorant Garamond | 64px | 300 | 1.1 | Hero headlines |
| Display L | Cormorant Garamond | 48px | 300 | 1.15 | Section headlines |
| Display Italic | Cormorant Garamond | 48px | 300 italic | 1.15 | Accent words, gold color |
| H2 | Cormorant Garamond | 36px | 300 | 1.25 | Section titles |
| H3 | Cormorant Garamond | 26px | 400 | 1.35 | Card titles, subsections |
| Body L | Montserrat | 16px | 300 | 1.75 | Hero descriptions |
| Body M | Montserrat | 13px | 400 | 1.7 | General body text |
| Section Label | Montserrat | 10px | 700 | — | Uppercase, letter-spacing 0.22em, gold |
| Nav Link | Montserrat | 9.5px | 500 | — | Uppercase, letter-spacing 0.12em |
| Button | Montserrat | 10px | 700 | — | Uppercase, letter-spacing 0.14–0.16em |
| Card Location | Montserrat | 8px | 700 | — | Uppercase, letter-spacing 0.14em, gold |

## Navigation

- Sticky, height 72px, background `#07111C`, bottom border `rgba(196,151,58,0.22)`.
- Logo: "ALEF" in Cormorant Garamond 26px/600 gold, with `ألف` in Noto Serif Arabic beside it
  at reduced gold opacity. Tagline "Architectural & Cadding Services" below in Montserrat 7.5px.
- Nav links: Montserrat 9.5px/500 uppercase, color `#7A8FA8`, hover `#E8E0D0`, active `#C4973A`.
- CTA button: "Enquire Now" — gold bg, dark text.
- Max-width 1280px, centered, 80px side padding.

## Buttons

- **Primary:** background `#C4973A`, text `#07111C`, hover `#D4A94A`. No border-radius.
  Montserrat 10px/700 uppercase.
- **Secondary:** border `1px solid #C4973A`, text `#C4973A`, transparent bg. No border-radius.
- **Ghost:** text only `#C4973A`, no bg/border. Often with arrow `→`.
- **Disabled:** bg `#07111C`, border `rgba(196,151,58,0.25)`, text `#7A8FA8`, opacity 0.5.
- All buttons: padding ~14px 32px, no border-radius (sharp corners).

## Cards

### Project Card
- Card surface: `#0E2035`. No border-radius.
- **Gold top border**: 2px solid `#C4973A` between the image and content area.
- Image area: variable height, project photograph.
- Status badge overlaid on image: ongoing = gold bg `#C4973A` with dark text;
  completed = dark bg `#0E2035` with gold border and gold text.
- Content padding: 28px 32px.
- Location: Montserrat 8px/700 uppercase gold, letter-spacing 0.14em. First line.
- Title: Cormorant Garamond 24px (or 28px for featured)/400, color `#E8E0D0`.
- Description: Montserrat 12px/300, color `#5A6B80`.
- "Read More →" link: Montserrat 8.5px/700 uppercase gold.

### Service Card
- Surface `#0E2035`, border `1px solid rgba(196,151,58,0.15)`, padding 24px.
- Gold rule (28px wide, 1px height) at top.
- Title: Montserrat 10px/600 uppercase, color `#C8C0B0`.
- Description: Montserrat 9px/300, color `#6A7D95`.

### Stat Display
- Background `#C4973A` (gold, inverted).
- Number: Cormorant Garamond 42–48px/500, color `#07111C`.
- Label: Montserrat 8px/700 uppercase, color `rgba(7,17,28,0.6)`.

## Layout

- Page max-width: **1280px**, centered with margin auto.
- Side padding: **80px** on desktop.
- Section vertical padding: **100–120px**.
- Nav height: **72px**.
- Card grid: **3 columns** for projects, gap 24px.
  First project can span 2 columns for emphasis.
- Spacing scale: 8px (xs), 16px (sm), 24px (md/card padding), 40px (lg), 80px (xl), 120px (2xl).

## Page-Specific Patterns

### Projects Page
- **Hero section**: Surface 1 bg (`#0C1B2E`), subtle grid pattern overlay
  `rgba(196,151,58,0.03)`, breadcrumb trail, section label "Our Portfolio" in gold,
  Display XL headline "Projects", Arabic subtitle, description in muted text.
- **Gold accent bar**: 4px `#C4973A` divider below hero.
- **Filter bar**: height 64px, bg `#07111C`, bottom border. Filter chips as toggle buttons —
  active chip has gold bg with dark text, inactive has border `rgba(196,151,58,0.2)` with muted text.
  "Showing all projects" counter on the right side.
- **Project grid**: 3 columns, first project spans 2 cols. Each card has the standard project
  card pattern (gold top border, location, title, description, Read More link).
- **CTA section**: full-width gold bg, Cormorant Garamond headline, Arabic subtitle,
  dark "Send an Enquiry" button.

### Project Detail Page
- Hero image full-width or contained.
- Project name as Display L, status badge, location.
- Credit fields in a structured data sheet layout on dark surface.
- Scope as badges/tags in the gold-muted style.

## Decorative Elements

- **Grid pattern overlay**: `repeating-linear-gradient(rgba(196,151,58,0.03) 0 1px, transparent 1px 72px)` both horizontal and vertical, 72px grid. Used on hero sections.
- **Gold rules**: thin gold lines (1px or 2px `#C4973A`) as dividers and accents.
- **Diamond frames**: rotated 45° border-only squares at reduced gold opacity, used as
  background decoration in hero areas.

## What this is NOT

- Not a light/white theme — this is dark-first throughout.
- Not a minimalist tech startup landing page.
- Not a colorful, illustration-heavy SaaS marketing site.
- Not a developer tool aesthetic.
- Not Inter/sans-serif headings — headings are always Cormorant Garamond serif.

## Forms

- Input fields: bg `#0E2035`, border `1px solid rgba(196,151,58,0.25)`, focus border `#C4973A`.
- Label: Montserrat 8.5px/700 uppercase gold, letter-spacing 0.2em.
- Input text: Montserrat 12px/300, color `#E8E0D0`, placeholder `#4A5B6E`.

## Footer

- Background `#040C14`, top border `rgba(196,151,58,0.15)`.
- Logo "ALEF" in Cormorant Garamond 20px/600 at reduced gold opacity.
- Copyright in Montserrat 10px/300, very muted.
