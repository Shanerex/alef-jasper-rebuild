"use client";

import { useEffect, useState } from "react";
import { getAdminProject } from "@/lib/api/admin-projects";
import type { AdminProject } from "@/lib/types/admin";
import type { ProjectFilters } from "@/lib/types/project";
import { ProjectForm } from "@/components/admin/projects/project-form";

/**
 * Client half of the edit-project screen (design.md §B.1, F12-AC5).
 *
 * `filters` arrives pre-fetched from the server-component parent (see
 * `page.tsx`) -- only the single record, which needs the browser's admin
 * session cookie via `adminFetch`, is fetched client-side here.
 */
export function EditProjectClient({ id, filters }: { id: number; filters: ProjectFilters }) {
  const [project, setProject] = useState<AdminProject | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    getAdminProject(id)
      .then(setProject)
      .catch(() => setNotFound(true));
  }, [id]);

  if (notFound) {
    return <p className="font-sans text-[13px] text-text-muted">Project not found.</p>;
  }

  return project ? (
    <ProjectForm mode="edit" initial={project} filters={filters} />
  ) : (
    <p className="font-sans text-[12px] text-text-muted">Loading…</p>
  );
}
