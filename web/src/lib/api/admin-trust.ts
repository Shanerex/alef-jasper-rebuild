import { adminFetch } from "@/lib/api/admin-client";
import type { AdminTrustOverview, AdminTrustRequest, AdminTrustRow } from "@/lib/types/admin";

/** Typed client for admin trust_content CRUD (design.md §A.4, F12-AC12..AC15, DEC-025). */

export async function getAdminTrustOverview(): Promise<AdminTrustOverview> {
  return adminFetch<AdminTrustOverview>("/api/admin/trust");
}

export async function createAdminTrustRow(payload: AdminTrustRequest): Promise<AdminTrustRow> {
  return adminFetch<AdminTrustRow>("/api/admin/trust", { method: "POST", body: payload });
}

export async function updateAdminTrustRow(id: number, payload: AdminTrustRequest): Promise<AdminTrustRow> {
  return adminFetch<AdminTrustRow>(`/api/admin/trust/${id}`, { method: "PUT", body: payload });
}

export async function deleteAdminTrustRow(id: number): Promise<void> {
  await adminFetch<null>(`/api/admin/trust/${id}`, { method: "DELETE" });
}
