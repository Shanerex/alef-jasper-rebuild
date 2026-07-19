import { adminFetch, primeCsrfToken } from "@/lib/api/admin-client";
import type { LoginPayload, SessionResponse } from "@/lib/types/admin";

/**
 * Typed client for the admin session endpoints (design.md §A.1, F12-AC1).
 */

/** POST /api/admin/session -- login. Primes the CSRF cookie first (not itself CSRF-protected -- DEC-028). */
export async function login(payload: LoginPayload): Promise<SessionResponse> {
  await primeCsrfToken();
  return adminFetch<SessionResponse>("/api/admin/session", { method: "POST", body: payload });
}

/** GET /api/admin/session -- current session check. Throws AdminApiError(401) when not logged in. */
export async function getSession(): Promise<SessionResponse> {
  return adminFetch<SessionResponse>("/api/admin/session");
}

/** DELETE /api/admin/session -- logout. */
export async function logout(): Promise<void> {
  await adminFetch<null>("/api/admin/session", { method: "DELETE" });
}
