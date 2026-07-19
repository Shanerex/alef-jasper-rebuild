import { adminFetch } from "@/lib/api/admin-client";
import type { PasswordChangePayload } from "@/lib/types/admin";

/** POST /api/admin/password -- self-service password change (design.md §A.7, F12-AC28). */
export async function changePassword(payload: PasswordChangePayload): Promise<void> {
  await adminFetch<null>("/api/admin/password", { method: "POST", body: payload });
}
