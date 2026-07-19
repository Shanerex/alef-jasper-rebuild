"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { listAdminProjects, deleteAdminProject } from "@/lib/api/admin-projects";
import type { AdminProject } from "@/lib/types/admin";
import { AdminPageHeader } from "@/components/admin/admin-page-header";
import { ResponsiveList, type ListColumn } from "@/components/admin/responsive-list";
import { DeleteConfirmDialog } from "@/components/admin/delete-confirm-dialog";

/** Projects list (design.md §B.1/§B.3, F12-AC4..AC7). */
export default function AdminProjectsListPage() {
  const [projects, setProjects] = useState<AdminProject[] | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminProject | null>(null);
  const [deleting, setDeleting] = useState(false);

  function refresh() {
    listAdminProjects(0, 200).then((r) => setProjects(r.content));
  }

  useEffect(refresh, []);

  async function handleConfirmDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteAdminProject(deleteTarget.id);
      setDeleteTarget(null);
      refresh();
    } finally {
      setDeleting(false);
    }
  }

  const columns: ListColumn<AdminProject>[] = [
    { header: "Name", primary: true, render: (p) => p.name },
    { header: "Sector", render: (p) => p.sector },
    { header: "Country", render: (p) => p.country },
    { header: "Status", render: (p) => p.status },
    { header: "Featured", render: (p) => (p.featurable ? "Yes" : "—") },
  ];

  return (
    <div>
      <AdminPageHeader title="Projects" newHref="/admin/projects/new" newLabel="New Project" />

      {projects === null ? (
        <p className="font-sans text-[12px] text-text-muted">Loading…</p>
      ) : (
        <ResponsiveList
          columns={columns}
          rows={projects}
          keyFn={(p) => p.id}
          emptyMessage="No projects yet — create the first one."
          actions={(p) => (
            <>
              <Link
                href={`/admin/projects/${p.id}`}
                className="font-sans text-[10px] font-bold uppercase tracking-[0.12em] text-gold hover:text-gold-light"
              >
                Edit
              </Link>
              <button
                onClick={() => setDeleteTarget(p)}
                className="font-sans text-[10px] font-bold uppercase tracking-[0.12em] text-text-muted hover:text-text-primary"
              >
                Delete
              </button>
            </>
          )}
        />
      )}

      <DeleteConfirmDialog
        open={deleteTarget !== null}
        title="Delete this project?"
        description={`Delete project "${deleteTarget?.name}"? This removes it from the public site immediately (including the home marquee and RAG knowledge base) and cannot be undone.`}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={handleConfirmDelete}
        confirming={deleting}
      />
    </div>
  );
}
