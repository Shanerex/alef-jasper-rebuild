"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getAdminTrustOverview, deleteAdminTrustRow } from "@/lib/api/admin-trust";
import type { AdminTrustOverview, AdminTrustRow } from "@/lib/types/admin";
import { AdminPageHeader } from "@/components/admin/admin-page-header";
import { ResponsiveList, type ListColumn } from "@/components/admin/responsive-list";
import { DeleteConfirmDialog } from "@/components/admin/delete-confirm-dialog";

/**
 * Trust editor (design.md §B.1, F12-AC12..AC15, DEC-025): three grouped
 * tables (stats/software/standards) plus a read-only derived-facts panel.
 * The derived panel is never editable here -- marquee count and client/
 * contractor names change through Projects CRUD (§2.2 reconciliation).
 */
export default function AdminTrustPage() {
  const [overview, setOverview] = useState<AdminTrustOverview | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminTrustRow | null>(null);
  const [deleting, setDeleting] = useState(false);

  function refresh() {
    getAdminTrustOverview().then(setOverview);
  }

  useEffect(refresh, []);

  async function handleConfirmDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteAdminTrustRow(deleteTarget.id);
      setDeleteTarget(null);
      refresh();
    } finally {
      setDeleting(false);
    }
  }

  const columns: ListColumn<AdminTrustRow>[] = [
    { header: "Label", primary: true, render: (r) => r.label },
    { header: "Item Key", render: (r) => r.itemKey },
    { header: "Value", render: (r) => (r.value ? `${r.value}${r.unit ? " " + r.unit : ""}` : "—") },
  ];

  function actions(row: AdminTrustRow) {
    return (
      <>
        <Link
          href={`/admin/trust/${row.id}`}
          className="font-sans text-[10px] font-bold uppercase tracking-[0.12em] text-gold hover:text-gold-light"
        >
          Edit
        </Link>
        <button
          onClick={() => setDeleteTarget(row)}
          className="font-sans text-[10px] font-bold uppercase tracking-[0.12em] text-text-muted hover:text-text-primary"
        >
          Delete
        </button>
      </>
    );
  }

  return (
    <div>
      <AdminPageHeader title="Trust Content" newHref="/admin/trust/new" newLabel="New Row" />

      {!overview ? (
        <p className="font-sans text-[12px] text-text-muted">Loading…</p>
      ) : (
        <div className="flex flex-col gap-8">
          <section>
            <h2 className="mb-3 font-sans text-[11px] font-bold uppercase tracking-[0.14em] text-gold">
              Stats
            </h2>
            <ResponsiveList
              columns={columns}
              rows={overview.stats}
              keyFn={(r) => r.id}
              emptyMessage="No stats yet."
              actions={actions}
            />
          </section>

          <section>
            <h2 className="mb-3 font-sans text-[11px] font-bold uppercase tracking-[0.14em] text-gold">
              Software
            </h2>
            <ResponsiveList
              columns={columns}
              rows={overview.software}
              keyFn={(r) => r.id}
              emptyMessage="No software badges yet."
              actions={actions}
            />
          </section>

          <section>
            <h2 className="mb-3 font-sans text-[11px] font-bold uppercase tracking-[0.14em] text-gold">
              Standards
            </h2>
            <ResponsiveList
              columns={columns}
              rows={overview.standards}
              keyFn={(r) => r.id}
              emptyMessage="No standards badges yet."
              actions={actions}
            />
          </section>

          {/* Read-only derived facts panel (DEC-025) -- stacks beneath the editable groups on phone (§B.10) */}
          <section className="border border-gold-subtle bg-surface-2 p-5">
            <h2 className="mb-3 font-sans text-[11px] font-bold uppercase tracking-[0.14em] text-gold">
              Derived (read-only)
            </h2>
            <dl className="flex flex-col gap-2">
              <div className="flex justify-between gap-3">
                <dt className="font-sans text-[12px] text-text-muted">Marquee project count</dt>
                <dd className="font-sans text-[12px] text-text-primary">{overview.derived.marqueeProjectCount}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="font-sans text-[12px] text-text-muted">Clients</dt>
                <dd className="font-sans text-[12px] text-text-primary">
                  {overview.derived.clients.join(", ") || "—"}
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="font-sans text-[12px] text-text-muted">Contractors</dt>
                <dd className="font-sans text-[12px] text-text-primary">
                  {overview.derived.contractors.join(", ") || "—"}
                </dd>
              </div>
            </dl>
            <p className="mt-3 font-sans text-[11px] font-light text-text-muted">{overview.derived.note}</p>
          </section>
        </div>
      )}

      <DeleteConfirmDialog
        open={deleteTarget !== null}
        title="Delete this trust row?"
        description={`Delete "${deleteTarget?.label}"? This removes it from the public trust layer immediately and cannot be undone.`}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={handleConfirmDelete}
        confirming={deleting}
      />
    </div>
  );
}
