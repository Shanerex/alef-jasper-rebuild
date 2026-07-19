import { adminFetch } from "@/lib/api/admin-client";
import type { AdminTeamMember, AdminTeamRequest } from "@/lib/types/admin";

/** Typed client for admin team CRUD (design.md §A.3, F12-AC8..AC11). */

export async function listAdminTeam(): Promise<AdminTeamMember[]> {
  return adminFetch<AdminTeamMember[]>("/api/admin/team");
}

export async function getAdminTeamMember(id: number): Promise<AdminTeamMember> {
  return adminFetch<AdminTeamMember>(`/api/admin/team/${id}`);
}

export async function createAdminTeamMember(payload: AdminTeamRequest): Promise<AdminTeamMember> {
  return adminFetch<AdminTeamMember>("/api/admin/team", { method: "POST", body: payload });
}

export async function updateAdminTeamMember(id: number, payload: AdminTeamRequest): Promise<AdminTeamMember> {
  return adminFetch<AdminTeamMember>(`/api/admin/team/${id}`, { method: "PUT", body: payload });
}

export async function deleteAdminTeamMember(id: number): Promise<void> {
  await adminFetch<null>(`/api/admin/team/${id}`, { method: "DELETE" });
}
