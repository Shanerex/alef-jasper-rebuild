"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { getAdminTeamMember } from "@/lib/api/admin-team";
import type { AdminTeamMember } from "@/lib/types/admin";
import { TeamForm } from "@/components/admin/team/team-form";

/** Edit-team-member screen (design.md §B.1, F12-AC9). */
export default function EditTeamMemberPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const [member, setMember] = useState<AdminTeamMember | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    getAdminTeamMember(id)
      .then(setMember)
      .catch(() => setNotFound(true));
  }, [id]);

  if (notFound) {
    return <p className="font-sans text-[13px] text-text-muted">Team member not found.</p>;
  }

  return (
    <div>
      <h1 className="mb-6 font-serif text-[28px] font-normal text-text-primary">Edit Team Member</h1>
      {member ? (
        <TeamForm mode="edit" initial={member} />
      ) : (
        <p className="font-sans text-[12px] text-text-muted">Loading…</p>
      )}
    </div>
  );
}
