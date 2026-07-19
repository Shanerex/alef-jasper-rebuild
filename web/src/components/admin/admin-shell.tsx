"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { getSession, logout as logoutRequest } from "@/lib/api/admin-session";

/** Sidebar navigation entries (design.md §B.1 route map). */
const NAV_LINKS = [
  { href: "/admin", label: "Dashboard" },
  { href: "/admin/projects", label: "Projects" },
  { href: "/admin/team", label: "Team" },
  { href: "/admin/trust", label: "Trust" },
  { href: "/admin/samples", label: "Samples" },
] as const;

/**
 * Admin shell: persistent sidebar + top bar (design.md §B.1, functional dark
 * admin theme per DEC-026). Wraps every screen under `/admin/(protected)`.
 *
 * Responsive (F12-AC27, design §B.10): the sidebar is persistent at `lg` and
 * up; below `lg` it collapses into a hamburger-triggered drawer. This is a
 * UI-only concern -- no API/DTO shape changes.
 *
 * Also owns the client-side session check: middleware.ts only checks cookie
 * *presence* (UX signal); this component calls GET /api/admin/session on
 * mount and redirects to /admin/login on any 401, which is the authoritative
 * enforcement point on the client side (architecture §2.1).
 */
export function AdminShell({ children }: { children: React.ReactNode }) {
  const [username, setUsername] = useState<string | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [checked, setChecked] = useState(false);
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    let cancelled = false;
    getSession()
      .then((session) => {
        if (!cancelled) {
          setUsername(session.username);
          setChecked(true);
        }
      })
      .catch(() => {
        if (!cancelled) router.replace(`/admin/login?next=${encodeURIComponent(pathname)}`);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- run once on mount per route
  }, []);

  async function handleLogout() {
    await logoutRequest().catch(() => undefined);
    router.replace("/admin/login");
  }

  if (!checked) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-page">
        <p className="font-sans text-[12px] uppercase tracking-[0.14em] text-text-muted">
          Checking session…
        </p>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-page">
      {/* Sidebar -- persistent at lg+, drawer below lg (design §B.10) */}
      <aside
        className={`fixed inset-y-0 left-0 z-40 w-64 border-r border-gold-subtle bg-surface-1 transition-transform lg:static lg:translate-x-0 ${
          drawerOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="flex h-16 items-center border-b border-gold-subtle px-6">
          <span className="font-serif text-[20px] font-semibold text-gold">ALEF Admin</span>
        </div>
        <nav className="flex flex-col gap-1 p-4">
          {NAV_LINKS.map((link) => {
            const active = pathname === link.href;
            return (
              <Link
                key={link.href}
                href={link.href}
                onClick={() => setDrawerOpen(false)}
                className={`px-3 py-2.5 font-sans text-[11px] font-bold uppercase tracking-[0.12em] ${
                  active ? "bg-gold-ghost text-gold" : "text-text-muted hover:text-text-primary"
                }`}
              >
                {link.label}
              </Link>
            );
          })}
        </nav>
      </aside>

      {/* Drawer backdrop (mobile only) */}
      {drawerOpen && (
        <button
          aria-label="Close menu"
          onClick={() => setDrawerOpen(false)}
          className="fixed inset-0 z-30 bg-page/70 lg:hidden"
        />
      )}

      {/* Main column */}
      <div className="flex min-h-screen w-full flex-1 flex-col lg:pl-0">
        {/* Top bar */}
        <header className="flex h-16 items-center justify-between border-b border-gold-subtle bg-page px-4 sm:px-6">
          <button
            aria-label="Open menu"
            onClick={() => setDrawerOpen(true)}
            className="border border-gold-subtle px-2.5 py-1.5 font-sans text-[12px] text-text-primary lg:hidden"
          >
            ☰
          </button>
          <div className="hidden font-sans text-[11px] uppercase tracking-[0.14em] text-text-muted lg:block">
            {username}
          </div>
          <div className="flex items-center gap-4">
            <Link
              href="/admin/account"
              className="font-sans text-[10px] font-bold uppercase tracking-[0.12em] text-gold hover:text-gold-light"
            >
              Account
            </Link>
            <button
              onClick={handleLogout}
              className="border border-gold-subtle px-3 py-1.5 font-sans text-[10px] font-bold uppercase tracking-[0.12em] text-text-primary hover:border-gold"
            >
              Logout
            </button>
          </div>
        </header>

        <main className="flex-1 px-4 py-6 sm:px-6 lg:px-10 lg:py-10">{children}</main>
      </div>
    </div>
  );
}
