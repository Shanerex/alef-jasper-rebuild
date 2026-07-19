import { AdminShell } from "@/components/admin/admin-shell";

/**
 * Layout for every authenticated admin screen (design.md §B.1).
 * Route-group `(protected)` keeps this shell off `/admin/login`, which must
 * render without the sidebar/session-check chrome (it IS the way to get a
 * session in the first place).
 */
export default function AdminProtectedLayout({ children }: { children: React.ReactNode }) {
  return <AdminShell>{children}</AdminShell>;
}
