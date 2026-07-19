import { adminFetch } from "@/lib/api/admin-client";
import type { AdminProject, AdminProjectRequest, PagedResponse } from "@/lib/types/admin";

/** Typed client for admin project CRUD (design.md §A.2, F12-AC4..AC7). */

export async function listAdminProjects(page = 0, size = 100): Promise<PagedResponse<AdminProject>> {
  return adminFetch<PagedResponse<AdminProject>>(`/api/admin/projects?page=${page}&size=${size}`);
}

export async function getAdminProject(id: number): Promise<AdminProject> {
  return adminFetch<AdminProject>(`/api/admin/projects/${id}`);
}

export async function createAdminProject(payload: AdminProjectRequest): Promise<AdminProject> {
  return adminFetch<AdminProject>("/api/admin/projects", { method: "POST", body: payload });
}

export async function updateAdminProject(id: number, payload: AdminProjectRequest): Promise<AdminProject> {
  return adminFetch<AdminProject>(`/api/admin/projects/${id}`, { method: "PUT", body: payload });
}

export async function deleteAdminProject(id: number): Promise<void> {
  await adminFetch<null>(`/api/admin/projects/${id}`, { method: "DELETE" });
}
