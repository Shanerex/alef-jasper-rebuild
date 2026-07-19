"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { listAdminSamples, deleteAdminSample } from "@/lib/api/admin-samples";
import type { AdminSample } from "@/lib/types/admin";
import { AdminPageHeader } from "@/components/admin/admin-page-header";
import { ResponsiveList, type ListColumn } from "@/components/admin/responsive-list";
import { DeleteConfirmDialog } from "@/components/admin/delete-confirm-dialog";

/** Samples list (design.md §B.1/§B.3, F12-AC16..AC19). */
export default function AdminSamplesListPage() {
  const [samples, setSamples] = useState<AdminSample[] | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminSample | null>(null);
  const [deleting, setDeleting] = useState(false);

  function refresh() {
    listAdminSamples().then(setSamples);
  }

  useEffect(refresh, []);

  async function handleConfirmDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteAdminSample(deleteTarget.id);
      setDeleteTarget(null);
      refresh();
    } finally {
      setDeleting(false);
    }
  }

  const columns: ListColumn<AdminSample>[] = [
    { header: "Title", primary: true, render: (s) => s.title },
    { header: "Category", render: (s) => s.category },
    { header: "Preview", render: (s) => (s.preview ? "Yes" : "—") },
    { header: "File", render: (s) => (s.file ? "PDF" : "—") },
  ];

  return (
    <div>
      <AdminPageHeader title="Samples" newHref="/admin/samples/new" newLabel="New Sample" />

      {samples === null ? (
        <p className="font-sans text-[12px] text-text-muted">Loading…</p>
      ) : (
        <ResponsiveList
          columns={columns}
          rows={samples}
          keyFn={(s) => s.id}
          emptyMessage="No samples yet — create the first one."
          actions={(s) => (
            <>
              <Link
                href={`/admin/samples/${s.id}`}
                className="font-sans text-[10px] font-bold uppercase tracking-[0.12em] text-gold hover:text-gold-light"
              >
                Edit
              </Link>
              <button
                onClick={() => setDeleteTarget(s)}
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
        title="Delete this sample?"
        description={`Delete "${deleteTarget?.title}"? This cannot be undone.`}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={handleConfirmDelete}
        confirming={deleting}
      />
    </div>
  );
}
