/**
 * TypeScript mirrors of the API DTOs for the project portfolio.
 *
 * Sector and ProjectStatus are literal unions for compile-time safety on the
 * web side. DTO interface fields use `string` at the boundary so an unknown
 * token from /filters or a new value added by feature 012 still renders
 * without a type error.
 */

/** Wire tokens for project sectors, matching the backend Sector vocabulary. */
export type Sector =
  | "airport"
  | "mall_retail"
  | "hotel_hospitality"
  | "residential"
  | "infrastructure_rail"
  | "leisure_museum";

/** Wire tokens for project statuses, matching the backend ProjectStatus vocabulary. */
export type ProjectStatus = "ongoing" | "completed";

/**
 * Summary projection for the project list and marquee strip (DEC-011).
 * Contains only card-level fields -- detail fields are excluded.
 */
export interface ProjectSummary {
  slug: string;
  name: string;
  sector: string;
  country: string;
  status: string;
  image: string | null;
  location: string | null;
  featurable: boolean;
}

/**
 * Full public record for the project detail page (F3-AC3).
 * Includes credit fields and scope that the summary omits.
 */
export interface ProjectDetail {
  slug: string;
  name: string;
  sector: string;
  country: string;
  status: string;
  image: string | null;
  description: string | null;
  mainContractor: string | null;
  client: string | null;
  consultant: string | null;
  location: string | null;
  scope: string[];
  featurable: boolean;
}

/**
 * Filter-options discoverability payload from GET /api/projects/filters.
 * Sectors and statuses are the fixed vocabulary; countries are data-derived.
 */
export interface ProjectFilters {
  sectors: string[];
  statuses: string[];
  countries: string[];
}

/**
 * Paginated envelope matching the API's stable contract (architecture 3.1).
 */
export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/**
 * Filter state for the project browser component.
 * Mirrors the URL query parameters. All dimensions are optional --
 * undefined means "show all" for that dimension.
 */
export interface FilterState {
  sector?: string;
  country?: string;
  status?: string;
}

/**
 * Query parameters for the getProjects API client function.
 * Includes pagination and the featurable flag for marquee fetches.
 */
export interface ProjectQuery {
  sector?: string;
  country?: string;
  status?: string;
  featurable?: boolean;
  page?: number;
  size?: number;
}

/**
 * Human-readable display labels for sector wire tokens.
 * Used by the filter controls and badges to show friendly names.
 */
export const SECTOR_LABELS: Record<string, string> = {
  airport: "Airport",
  mall_retail: "Mall / Retail",
  hotel_hospitality: "Hotel / Hospitality",
  residential: "Residential",
  infrastructure_rail: "Infrastructure / Rail",
  leisure_museum: "Leisure / Museum",
};

/**
 * Human-readable display labels for status wire tokens.
 */
export const STATUS_LABELS: Record<string, string> = {
  ongoing: "Ongoing",
  completed: "Completed",
};
