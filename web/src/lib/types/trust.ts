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
