"use client";

import { SampleForm } from "@/components/admin/samples/sample-form";

/** Create-sample screen (design.md §B.1, F12-AC16). */
export default function NewSamplePage() {
  return (
    <div>
      <h1 className="mb-6 font-serif text-[28px] font-normal text-text-primary">New Sample</h1>
      <SampleForm mode="create" />
    </div>
  );
}
