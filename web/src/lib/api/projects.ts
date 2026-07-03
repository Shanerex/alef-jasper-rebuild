import type {
  PagedResponse,
  ProjectDetail,
  ProjectFilters,
  ProjectQuery,
  ProjectSummary,
} from "@/lib/types/project";

/**
 * Base URL for the API service.
 *
 * In Docker: http://api:8080 (server-side SSR fetches via the Docker network).
 * Locally in dev: http://localhost:8080.
 * The env var is set in docker-compose.yml and the Dockerfile.
 */
const API_BASE_URL = process.env.API_BASE_URL || "http://localhost:8080";

/**
 * Fetches the summary catalogue of projects, optionally filtered and paginated.
 *
 * For v1 the list page calls this with no params (load-all, filter client-side).
 * The params exist so the same function serves the marquee fetch (featurable=true)
 * and the future server-side-filtering growth path.
 */
export async function getProjects(
  params?: ProjectQuery
): Promise<PagedResponse<ProjectSummary>> {
  const url = new URL(`${API_BASE_URL}/api/projects`);

  if (params) {
    if (params.sector) url.searchParams.set("sector", params.sector);
    if (params.country) url.searchParams.set("country", params.country);
    if (params.status) url.searchParams.set("status", params.status);
    if (params.featurable !== undefined)
      url.searchParams.set("featurable", String(params.featurable));
    if (params.page !== undefined)
      url.searchParams.set("page", String(params.page));
    if (params.size !== undefined)
      url.searchParams.set("size", String(params.size));
  }

  const res = await fetch(url.toString(), {
    next: { revalidate: 60 },
  });

  if (!res.ok) {
    throw new Error(`Failed to fetch projects: ${res.status}`);
  }

  return res.json();
}

/**
 * Fetches one project's full detail by slug.
 *
 * Returns null on 404 so the page component can call notFound().
 * Throws on other errors.
 */
export async function getProject(slug: string): Promise<ProjectDetail | null> {
  const res = await fetch(`${API_BASE_URL}/api/projects/${slug}`, {
    next: { revalidate: 60 },
  });

  if (res.status === 404) {
    return null;
  }

  if (!res.ok) {
    throw new Error(`Failed to fetch project ${slug}: ${res.status}`);
  }

  return res.json();
}

/**
 * Fetches the sector/status/country filter vocabularies.
 *
 * Called once at page render time and passed to the client-side filter
 * controls so they never hardcode vocabulary values (DEC-009).
 */
export async function getProjectFilters(): Promise<ProjectFilters> {
  const res = await fetch(`${API_BASE_URL}/api/projects/filters`, {
    next: { revalidate: 60 },
  });

  if (!res.ok) {
    throw new Error(`Failed to fetch project filters: ${res.status}`);
  }

  return res.json();
}
