import { getProjectFilters } from "@/lib/api/projects";
import { EditProjectClient } from "./edit-project-client";

/**
 * Edit-project screen (design.md §B.1, F12-AC5).
 *
 * Server Component: `getProjectFilters()` hits the public, CORS-less
 * `/api/projects/filters` endpoint, which only server-side (same-network)
 * fetches can reach -- a browser-side call to it 403s/CORS-blocks silently,
 * since only `/api/admin/**` has a CORS policy (AdminSecurityConfig). Fetching
 * here, server-side, and passing the result down as a prop avoids that
 * entirely (bug-fix pass 3; the previous client-side `useEffect` version
 * hung on "Loading…" forever with an unhandled CORS rejection). The project
 * record itself still needs the browser's admin session cookie, so it stays
 * client-fetched in `EditProjectClient` via `adminFetch`.
 *
 * force-dynamic: the `api` container isn't up during `docker build` (same
 * reason `(marketing)/projects/page.tsx` uses it), so this fetch must be
 * deferred to request time or the image build fails outright.
 */
export const dynamic = "force-dynamic";

export default async function EditProjectPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const filters = await getProjectFilters();

  return (
    <div>
      <h1 className="mb-6 font-serif text-[28px] font-normal text-text-primary">Edit Project</h1>
      <EditProjectClient id={Number(id)} filters={filters} />
    </div>
  );
}
