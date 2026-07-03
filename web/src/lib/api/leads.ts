import type { LeadPayload, LeadResponse } from "@/lib/types/lead";

/**
 * Base URL for the API service — mirrors the pattern in projects.ts and trust.ts.
 */
const API_BASE_URL = process.env.API_BASE_URL || "http://localhost:8080";

/**
 * Submits a contact form lead to POST /api/leads (architecture §3.4, feature 011).
 *
 * No cache: this is a mutation, not a read.
 *
 * Throws an error carrying the HTTP status code as a number property on the
 * thrown object so the ContactForm can distinguish:
 *   - 400 → validation error (mark fields)
 *   - 429 → rate limit (show retry copy)
 *   - 5xx / network → server error (show retry copy with email fallback)
 *
 * The caller (ContactForm) catches and maps the status to the error state.
 */
export async function submitLead(payload: LeadPayload): Promise<LeadResponse> {
  const res = await fetch(`${API_BASE_URL}/api/leads`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
    cache: "no-store",
  });

  if (!res.ok) {
    const err = new Error(`Lead submission failed: ${res.status}`) as Error & {
      status: number;
    };
    err.status = res.status;
    throw err;
  }

  return res.json();
}
