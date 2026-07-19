"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { getAdminSample } from "@/lib/api/admin-samples";
import type { AdminSample } from "@/lib/types/admin";
import { SampleForm } from "@/components/admin/samples/sample-form";

/** Edit-sample screen (design.md §B.1, F12-AC17). */
export default function EditSamplePage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const [sample, setSample] = useState<AdminSample | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    getAdminSample(id)
      .then(setSample)
      .catch(() => setNotFound(true));
  }, [id]);

  if (notFound) {
    return <p className="font-sans text-[13px] text-text-muted">Sample not found.</p>;
  }

  return (
    <div>
      <h1 className="mb-6 font-serif text-[28px] font-normal text-text-primary">Edit Sample</h1>
      {sample ? (
        <SampleForm mode="edit" initial={sample} />
      ) : (
        <p className="font-sans text-[12px] text-text-muted">Loading…</p>
      )}
    </div>
  );
}
