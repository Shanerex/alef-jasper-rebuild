"use client";

import { TeamForm } from "@/components/admin/team/team-form";

/** Create-team-member screen (design.md §B.1, F12-AC8). */
export default function NewTeamMemberPage() {
  return (
    <div>
      <h1 className="mb-6 font-serif text-[28px] font-normal text-text-primary">New Team Member</h1>
      <TeamForm mode="create" />
    </div>
  );
}
