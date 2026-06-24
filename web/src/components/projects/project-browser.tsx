"use client";

import { useSearchParams, useRouter, usePathname } from "next/navigation";
import { useCallback, useMemo } from "react";
import { ProjectFilters } from "@/components/projects/project-filters";
import { ProjectGrid } from "@/components/projects/project-grid";
import type {
  ProjectSummary,
  ProjectFilters as ProjectFiltersType,
  FilterState,
} from "@/lib/types/project";

interface ProjectBrowserProps {
  projects: ProjectSummary[];
  filters: ProjectFiltersType;
}

/**
 * Client component that owns filter state and composes the filter bar + grid.
 *
 * URL search params are the source of truth so filtered views are shareable
 * and bookmarkable (architecture 4.1). Filtering is purely client-side over
 * the already-loaded catalogue -- no server round-trip for ~38 projects.
 * AND semantics across the three filter dimensions.
 *
 * This is the single client boundary on the list page, keeping the server
 * component a thin data-fetch shell for good SEO.
 */
export function ProjectBrowser({ projects, filters }: ProjectBrowserProps) {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  /** Parses the current URL search params into a FilterState. */
  const activeFilters: FilterState = useMemo(() => ({
    sector: searchParams.get("sector") || undefined,
    country: searchParams.get("country") || undefined,
    status: searchParams.get("status") || undefined,
  }), [searchParams]);

  /**
   * Pushes a new filter state to the URL query string via shallow navigation.
   * Undefined values are omitted from the URL to keep it clean.
   */
  const handleFilterChange = useCallback(
    (next: FilterState) => {
      const params = new URLSearchParams();
      if (next.sector) params.set("sector", next.sector);
      if (next.country) params.set("country", next.country);
      if (next.status) params.set("status", next.status);

      const qs = params.toString();
      router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
    },
    [router, pathname]
  );

  /** Applies the active filters to the full project list (AND semantics). */
  const filteredProjects = useMemo(() => {
    return projects.filter((p) => {
      if (activeFilters.sector && p.sector !== activeFilters.sector) return false;
      if (activeFilters.country && p.country !== activeFilters.country) return false;
      if (activeFilters.status && p.status !== activeFilters.status) return false;
      return true;
    });
  }, [projects, activeFilters]);

  return (
    <div>
      {/* Filter bar */}
      <ProjectFilters
        filters={filters}
        value={activeFilters}
        onChange={handleFilterChange}
        projectCount={filteredProjects.length}
      />

      {/* Project grid */}
      <section className="bg-page py-20">
        <div className="mx-auto max-w-site px-[80px]">
          <ProjectGrid projects={filteredProjects} />
        </div>
      </section>
    </div>
  );
}
