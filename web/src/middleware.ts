import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * Route guard for the admin console (design.md §B.1, F12-AC1).
 *
 * Presence-check ONLY: redirects to /admin/login when the ALEFADMIN session
 * cookie is absent. This is a UX convenience, not the security boundary --
 * the API's 401/403 responses are authoritative (architecture §2.1). A stale
 * or expired cookie still passes this check and is caught by AdminShell's
 * client-side GET /api/admin/session call, which redirects on 401.
 *
 * /admin/login itself is excluded (it must be reachable while logged out).
 */
export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (pathname === "/admin/login") {
    return NextResponse.next();
  }

  const hasSessionCookie = request.cookies.has("ALEFADMIN");
  if (!hasSessionCookie) {
    const loginUrl = new URL("/admin/login", request.url);
    loginUrl.searchParams.set("next", pathname);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/admin/:path*"],
};
