"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { getAdminTrustOverview } from "@/lib/api/admin-trust";
import type { AdminTrustRow } from "@/lib/types/admin";
import { TrustForm } from "@/components/admin/trust/trust-form";

/**
 * Edit-trust-row screen (design.md §B.1).
 *
 * There is no GET /api/admin/trust/{id} in the contract (design §A.4 only
 * defines POST/PUT/DELETE by id plus the grouped GET /api/admin/trust) --
 * the row is found by id from the already-grouped overview response rather
 * than inventing an undocumented endpoint.
 */
export default function EditTrustRowPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const [row, setRow] = useState<AdminTrustRow | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    getAdminTrustOverview().then((overview) => {
      const found = [...overview.stats, ...overview.software, ...overview.standards].find((r) => r.id === id);
      if (found) setRow(found);
      else setNotFound(true);
    });
  }, [id]);

  if (notFound) {
    return <p className="font-sans text-[13px] text-text-muted">Trust row not found.</p>;
  }

  return (
    <div>
      <h1 className="mb-6 font-serif text-[28px] font-normal text-text-primary">Edit Trust Row</h1>
      {row ? <TrustForm mode="edit" initial={row} /> : <p className="font-sans text-[12px] text-text-muted">Loading…</p>}
    </div>
  );
}
