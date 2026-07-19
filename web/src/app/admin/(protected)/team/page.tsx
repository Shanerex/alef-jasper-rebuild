"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { listAdminTeam, deleteAdminTeamMember, updateAdminTeamMember } from "@/lib/api/admin-team";
import type { AdminTeamMember } from "@/lib/types/admin";
import { AdminPageHeader } from "@/components/admin/admin-page-header";
import { ResponsiveList, type ListColumn } from "@/components/admin/responsive-list";
import { DeleteConfirmDialog } from "@/components/admin/delete-confirm-dialog";

/** Team list (design.md §B.1/§B.3, F12-AC8..AC11). */
export default function AdminTeamListPage() {
  const [members, setMembers] = useState<AdminTeamMember[] | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminTeamMember | null>(null);
  const [deleting, setDeleting] = useState(false);

  function refresh() {
    listAdminTeam().then(setMembers);
  }

  useEffect(refresh, []);

  async function handleConfirmDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteAdminTeamMember(deleteTarget.id);
      setDeleteTarget(null);
      refresh();
    } finally {
      setDeleting(false);
    }
  }

  /** Soft-hide alternative to hard delete (design §A.3/§B.3: "recommended alternative"). */
  async function toggleActive(member: AdminTeamMember) {
    await updateAdminTeamMember(member.id, { ...member, active: !member.active });
    refresh();
  }

  const columns: ListColumn<AdminTeamMember>[] = [
    { header: "Name", primary: true, render: (m) => m.name },
    { header: "Role", render: (m) => m.role },
    { header: "Company", render: (m) => m.company ?? "—" },
    { header: "Active", render: (m) => (m.active ? "Yes" : "No") },
  ];

  return (
    <div>
      <AdminPageHeader title="Team" newHref="/admin/team/new" newLabel="New Team Member" />

      {members === null ? (
        <p className="font-sans text-[12px] text-text-muted">Loading…</p>
      ) : (
        <ResponsiveList
          columns={columns}
          rows={members}
          keyFn={(m) => m.id}
          emptyMessage="No team members yet — create the first one."
          actions={(m) => (
            <>
              <Link
                href={`/admin/team/${m.id}`}
                className="font-sans text-[10px] font-bold uppercase tracking-[0.12em] text-gold hover:text-gold-light"
              >
                Edit
              </Link>
              <button
                onClick={() => toggleActive(m)}
                className="font-sans text-[10px] font-bold uppercase tracking-[0.12em] text-text-muted hover:text-text-primary"
              >
                {m.active ? "Hide" : "Unhide"}
              </button>
              <button
                onClick={() => setDeleteTarget(m)}
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
        title="Delete this team member?"
        description={`Delete "${deleteTarget?.name}"? This removes them from the public About page immediately and cannot be undone. Prefer to hide instead? Cancel and use "Hide" to set them inactive.`}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={handleConfirmDelete}
        confirming={deleting}
      />
    </div>
  );
}
