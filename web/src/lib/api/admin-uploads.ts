import { adminFetch } from "@/lib/api/admin-client";
import type { UploadCategory, UploadResponse } from "@/lib/types/admin";

/**
 * Uploads a single file for the given category (design.md §A.6, F12-AC20..AC22).
 * Source-agnostic: the caller's <input type="file"> may come from a desktop
 * dialog or a phone's OS file picker / camera roll (F12-AC27) -- this
 * function does not care which.
 */
export async function uploadAdminFile(file: File, category: UploadCategory): Promise<UploadResponse> {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("category", category);
  return adminFetch<UploadResponse>("/api/admin/uploads", { method: "POST", formData });
}
