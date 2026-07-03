import type { TeamResponse } from "@/lib/types/team";

/**
 * Base URL for the API service — mirrors the pattern in projects.ts and trust.ts.
 * In Docker: http://api:8080 (server-side SSR fetches via the Docker network).
 * Locally: http://localhost:8080.
 */
const API_BASE_URL = process.env.API_BASE_URL || "http://localhost:8080";

/**
 * Fetches the active leadership roster for the About page (architecture §3.1, feature 011).
 *
 * Returns the { members } envelope. Throws on non-2xx so the About page
 * try/catch can fall back to a muted loading line without a hard error page
 * (PITFALL-007 graceful-degradation pattern, matching projects.ts / trust.ts).
 *
 * Caches with revalidate: 60 so an F012 edit surfaces within the ISR window;
 * the About page is force-dynamic so flipping is a one-line page change later.
 */
export async function getTeam(): Promise<TeamResponse> {
  const res = await fetch(`${API_BASE_URL}/api/team`, {
    next: { revalidate: 60 },
  });

  if (!res.ok) {
    throw new Error(`Failed to fetch team: ${res.status}`);
  }

  return res.json();
}
