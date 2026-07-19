/**
 * TypeScript mirrors of the /api/admin/** DTOs (design.md Part A, feature 012).
 *
 * These are distinct from the public-facing types in project.ts/team.ts/trust.ts
 * (DEC-011 summary/detail projections) -- admin DTOs carry every column,
 * including the internal id the public projections omit.
 */

// ---- Session / auth (design §A.1) ----------------------------------------

export interface SessionResponse {
  authenticated: boolean;
  username: string;
}

export interface LoginPayload {
  username: string;
  password: string;
}

export interface PasswordChangePayload {
  currentPassword: string;
  newPassword: string;
}

// ---- Common envelope -------------------------------------------------------

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Field-name -> [error message, ...] map from a 400 ProblemDetail (design §A "Common error shapes"). */
export type FieldErrors = Record<string, string[]>;

/** Shape of an RFC 9457 ProblemDetail error body, as returned by every /api/admin/** error. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string | null;
  fields?: FieldErrors;
}

// ---- Projects (design §A.2) ------------------------------------------------

export interface AdminProject {
  id: number;
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
  createdAt: string;
  updatedAt: string;
}

export interface AdminProjectRequest {
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

/** Fixed scope vocabulary (architecture design §A.2) -- mirrors ScopeItem.java. */
export const SCOPE_VOCABULARY = ["rebar", "BBS", "GA", "MEP", "QS", "as-built"] as const;

/** Fixed status vocabulary -- mirrors ProjectStatus.java. */
export const PROJECT_STATUS_VOCABULARY = ["ongoing", "completed"] as const;

// ---- Team (design §A.3) -----------------------------------------------------

export interface AdminTeamMember {
  id: number;
  name: string;
  role: string;
  company: string | null;
  email: string | null;
  photo: string | null;
  displayOrder: number;
  active: boolean;
}

export interface AdminTeamRequest {
  name: string;
  role: string;
  company: string | null;
  email: string | null;
  photo: string | null;
  displayOrder: number;
  active: boolean;
}

// ---- Trust content (design §A.4, DEC-025) -----------------------------------

export interface AdminTrustRow {
  id: number;
  itemKey: string;
  itemType: "stat" | "software" | "standard";
  label: string;
  value: string | null;
  unit: string | null;
  displayOrder: number;
}

export interface AdminTrustRequest {
  itemKey: string;
  itemType: string;
  label: string;
  value: string | null;
  unit: string | null;
  displayOrder: number;
}

/** Read-only derived facts, never writable (DEC-025) -- edited via Projects. */
export interface DerivedTrustFacts {
  marqueeProjectCount: number;
  clients: string[];
  contractors: string[];
  note: string;
}

export interface AdminTrustOverview {
  stats: AdminTrustRow[];
  software: AdminTrustRow[];
  standards: AdminTrustRow[];
  derived: DerivedTrustFacts;
}

export const TRUST_ITEM_TYPES = ["stat", "software", "standard"] as const;

// ---- Samples (design §A.5) --------------------------------------------------

export interface AdminSample {
  id: number;
  slug: string;
  title: string;
  category: string;
  preview: string | null;
  file: string | null;
  displayOrder: number;
}

export interface AdminSampleRequest {
  slug: string;
  title: string;
  category: string;
  preview: string | null;
  file: string | null;
  displayOrder: number;
}

/** Fixed category vocabulary (architecture §3.4) -- mirrors SampleCategory.java. */
export const SAMPLE_CATEGORY_VOCABULARY = [
  "prequalification",
  "bbs",
  "drawings",
  "bridge_drawings",
  "roads_utility",
] as const;

// ---- Uploads (design §A.6) --------------------------------------------------

export type UploadCategory = "project-image" | "team-photo" | "sample-preview" | "sample-file";

export interface UploadResponse {
  url: string;
  filename: string;
  contentType: string;
  size: number;
}
