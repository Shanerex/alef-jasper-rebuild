import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Merges Tailwind class names, resolving conflicts.
 * Standard shadcn/ui utility used across all components.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Resolves a stored media URL (e.g. "/uploads/team-photo/xxx.jpg") to one
 * the browser can actually fetch.
 *
 * `/uploads/**` is served by the `api` container's static resource handler,
 * not by `web` -- a bare relative path resolves against the wrong origin
 * (the web app's own host) and 404s in the browser, even though the file
 * exists and `curl`ing the API directly works fine. Static bundled assets
 * (e.g. the seeded "/img/projects/*.svg") already live under web's own
 * `/public` and must NOT be rewritten -- only paths actually under
 * `/uploads/` need the API origin prefixed (feature 012 bug-fix pass 4).
 */
export function resolveUploadUrl(url: string | null): string | null {
  if (!url || !url.startsWith("/uploads/")) return url;
  const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
  return `${apiBaseUrl}${url}`;
}
