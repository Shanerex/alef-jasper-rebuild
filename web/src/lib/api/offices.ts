import type { OfficesResponse } from "@/lib/types/office";

/**
 * Base URL for the API service — mirrors the pattern in projects.ts and trust.ts.
 */
const API_BASE_URL = process.env.API_BASE_URL || "http://localhost:8080";

/**
 * Fetches both office locations for the Contact page (architecture §3.2, feature 011).
 *
 * Returns the { offices } envelope. Throws on non-2xx so the Contact page
 * try/catch can render a muted fallback without a hard error (PITFALL-007 pattern).
 *
 * Caches with revalidate: 60 so a F012 phone/address edit surfaces within the
 * ISR window without a redeploy — the primary reason offices are a table (DEC-019).
 */
export async function getOffices(): Promise<OfficesResponse> {
  const res = await fetch(`${API_BASE_URL}/api/offices`, {
    next: { revalidate: 60 },
  });

  if (!res.ok) {
    throw new Error(`Failed to fetch offices: ${res.status}`);
  }

  return res.json();
}
