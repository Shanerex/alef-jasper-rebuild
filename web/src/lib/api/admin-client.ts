import type { ProblemDetail } from "@/lib/types/admin";

/**
 * Base URL for the API service -- mirrors the pattern in projects.ts/leads.ts.
 * Admin calls always run client-side (credentials: 'include' needs a real
 * browser), so this resolves to the browser-visible API origin, not the
 * Docker-internal one used by server-side fetches elsewhere in the app.
 */
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

/**
 * Thrown by adminFetch on any non-2xx response. Carries the parsed
 * ProblemDetail body (when present) so callers can map `fields` onto form
 * inputs (design.md Part A "Common error shapes").
 */
export class AdminApiError extends Error {
  readonly status: number;
  readonly problem: ProblemDetail | null;

  constructor(status: number, problem: ProblemDetail | null) {
    super(problem?.detail || `Admin API request failed: ${status}`);
    this.status = status;
    this.problem = problem;
  }
}

/**
 * Reads the XSRF-TOKEN cookie set by CookieCsrfTokenRepository
 * (architecture §2.1). Returns null if not yet issued -- the caller should
 * call primeCsrfToken() first (e.g. on the login page mount).
 */
function readCsrfCookie(): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : null;
}

/**
 * Calls the CSRF priming endpoint (GET /api/admin/csrf, see
 * CsrfTokenController) to obtain the XSRF-TOKEN cookie before the first
 * mutating request. A plain GET does not naturally trigger cookie issuance
 * in Spring Security 6 -- see AdminSecurityConfig's gotcha note -- so this
 * dedicated call is required at least once per browser session.
 */
export async function primeCsrfToken(): Promise<void> {
  await fetch(`${API_BASE_URL}/api/admin/csrf`, { credentials: "include", cache: "no-store" });
}

/** HTTP methods that require the CSRF header (all state-changing requests). */
const MUTATING_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

export interface AdminFetchOptions {
  method?: string;
  body?: unknown;
  /** Set for multipart uploads -- skips the JSON Content-Type/body serialization. */
  formData?: FormData;
}

/**
 * Shared fetch wrapper for every /api/admin/** call (design.md Part A).
 *
 * - Always sends credentials: 'include' so the ALEFADMIN session cookie rides
 *   along (architecture §2.1, direct-to-API pattern like the rest of web/lib/api).
 * - Adds the X-XSRF-TOKEN header on mutating requests, reading the value from
 *   the XSRF-TOKEN cookie (primed via primeCsrfToken()).
 * - Parses a ProblemDetail body on error and throws AdminApiError so callers
 *   can render `fields` inline and `detail`/`title` as a banner.
 * - Returns null for 204 No Content responses (no body to parse).
 */
export async function adminFetch<T>(path: string, options: AdminFetchOptions = {}): Promise<T> {
  const method = options.method || "GET";
  const headers: Record<string, string> = {};

  if (MUTATING_METHODS.has(method)) {
    const csrfToken = readCsrfCookie();
    if (csrfToken) headers["X-XSRF-TOKEN"] = csrfToken;
  }

  let body: BodyInit | undefined;
  if (options.formData) {
    body = options.formData;
    // Content-Type is deliberately omitted -- the browser sets the correct
    // multipart boundary automatically when the body is a FormData instance.
  } else if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(options.body);
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body,
    credentials: "include",
    cache: "no-store",
  });

  if (res.status === 204) {
    return null as T;
  }

  const isJson = res.headers.get("content-type")?.includes("json");
  const payload = isJson ? await res.json() : null;

  if (!res.ok) {
    throw new AdminApiError(res.status, payload as ProblemDetail | null);
  }

  return payload as T;
}
