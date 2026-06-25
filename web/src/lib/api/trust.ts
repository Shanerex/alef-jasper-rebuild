import type { TrustOverview } from "@/lib/types/trust";

/**
 * Base URL for the API service.
 *
 * In Docker: http://api:8080 (server-side SSR fetches via the Docker network).
 * Locally in dev: http://localhost:8080. Mirrors lib/api/projects.ts.
 */
const API_BASE_URL = process.env.API_BASE_URL || "http://localhost:8080";

/**
 * Fetches the composed Trust Layer payload (stats, client/contractor names,
 * software/standards) in one read (architecture 3.1).
 *
 * Uses Next's revalidate cache option so an F012 edit surfaces within the ISR
 * window without a redeploy, consistent with the portfolio client.
 */
export async function getTrustOverview(): Promise<TrustOverview> {
  const res = await fetch(`${API_BASE_URL}/api/trust/overview`, {
    next: { revalidate: 60 },
  });

  if (!res.ok) {
    throw new Error(`Failed to fetch trust overview: ${res.status}`);
  }

  return res.json();
}
