/**
 * Static service specializations constant (DEC-016, feature 011).
 *
 * These 8 items are fixed marketing copy: never filtered, never aggregated,
 * never edited by a non-technical user in 011's scope. A static constant is the
 * correct and reversible choice (see architecture §6.1). If feature 012 later
 * needs to make these editable, it can promote them to a trust_content-style
 * table additively.
 *
 * discipline: the reconciled live-site capability name (F11-AC5 — nothing dropped).
 * title: outcome-reframed card heading (Dark Prestige prestige direction).
 * outcome: one-sentence body framing the value delivered.
 *
 * Copy is verbatim from design.md §6 — do not paraphrase.
 */

export interface ServiceItem {
  /** Stable key for React list rendering and future reference. */
  key: string;
  /** The underlying capability shown as an uppercase eyebrow on the card. */
  discipline: string;
  /** Outcome-oriented card title in serif. */
  title: string;
  /** One-sentence outcome-framed body in Montserrat 12px/300. */
  outcome: string;
}

export const SERVICES: ServiceItem[] = [
  {
    key: "rebar-shop-drawings",
    discipline: "Rebar Shop Drawings & BBS",
    title: "Shop-ready rebar, bar for bar",
    outcome:
      "We turn your structural design into fabrication-ready rebar shop drawings and bar bending schedules — every bar counted, cut, and scheduled to detailing standard.",
  },
  {
    key: "ga-drawings",
    discipline: "General Arrangement Drawings",
    title: "The whole structure, clearly set out",
    outcome:
      "Coordinated general arrangement drawings that give your site team an unambiguous picture of how every element fits together.",
  },
  {
    key: "concrete-works-drawings",
    discipline: "Concrete Works Drawings",
    title: "Concrete detailed for the pour",
    outcome:
      "Detailed concrete works drawings that take the guesswork out of formwork, placement, and sequencing on site.",
  },
  {
    key: "setting-out-coordinates",
    discipline: "Global Co-ordinates / Setting-Out",
    title: "Positioned to the millimetre",
    outcome:
      "Global co-ordinate and setting-out drawings that put every element exactly where the design intends — no field re-interpretation.",
  },
  {
    key: "road-infrastructure",
    discipline:
      "Road Setting-Out, Profiles, Cross-Sections, Marking, Signage & Utilities",
    title: "Infrastructure detailed end to end",
    outcome:
      "Road setting-out, profiles and cross-sections, road marking, signage, and utility-service drawings — the full linear-infrastructure package in one place.",
  },
  {
    key: "estimation-qs",
    discipline: "Estimation & Quantity Surveying",
    title: "Quantities you can budget against",
    outcome:
      "Estimation and quantity surveying that turn drawings into reliable material and cost figures before a single bar is ordered.",
  },
  {
    key: "mep-drawings",
    discipline: "MEP Drawings",
    title: "Services coordinated, clashes caught",
    outcome:
      "MEP drawings coordinated against the structure so mechanical, electrical, and plumbing routes are resolved before they reach the field.",
  },
  {
    key: "as-built-drawings",
    discipline: "As-Built Drawings",
    title: "The record of what was truly built",
    outcome:
      "Accurate as-built drawings that capture the structure as constructed — your definitive handover and maintenance record.",
  },
];
