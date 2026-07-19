"use client";

import { TrustForm } from "@/components/admin/trust/trust-form";

/** Create-trust-row screen (design.md §B.1). */
export default function NewTrustRowPage() {
  return (
    <div>
      <h1 className="mb-6 font-serif text-[28px] font-normal text-text-primary">New Trust Row</h1>
      <TrustForm mode="create" />
    </div>
  );
}
