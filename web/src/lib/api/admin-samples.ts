import { adminFetch } from "@/lib/api/admin-client";
import type { AdminSample, AdminSampleRequest } from "@/lib/types/admin";

/** Typed client for admin sample CRUD (design.md §A.5, F12-AC16..AC19). */

export async function listAdminSamples(): Promise<AdminSample[]> {
  return adminFetch<AdminSample[]>("/api/admin/samples");
}

export async function getAdminSample(id: number): Promise<AdminSample> {
  return adminFetch<AdminSample>(`/api/admin/samples/${id}`);
}

export async function createAdminSample(payload: AdminSampleRequest): Promise<AdminSample> {
  return adminFetch<AdminSample>("/api/admin/samples", { method: "POST", body: payload });
}

export async function updateAdminSample(id: number, payload: AdminSampleRequest): Promise<AdminSample> {
  return adminFetch<AdminSample>(`/api/admin/samples/${id}`, { method: "PUT", body: payload });
}

export async function deleteAdminSample(id: number): Promise<void> {
  await adminFetch<null>(`/api/admin/samples/${id}`, { method: "DELETE" });
}
