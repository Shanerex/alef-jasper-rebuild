"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import type { AdminSample, AdminSampleRequest } from "@/lib/types/admin";
import { SAMPLE_CATEGORY_VOCABULARY } from "@/lib/types/admin";
import { createAdminSample, updateAdminSample } from "@/lib/api/admin-samples";
import { AdminApiError } from "@/lib/api/admin-client";
import { AdminField } from "@/components/admin/ui/admin-field";
import { AdminInput } from "@/components/admin/ui/admin-input";
import { AdminSelect } from "@/components/admin/ui/admin-select";
import { UploadField } from "@/components/admin/upload-field";
import { FormErrorBanner } from "@/components/admin/admin-page-header";

/** Create/edit form for a sample (design.md §A.5/§B.4, F12-AC16/AC17/AC23). */
export function SampleForm({ mode, initial }: { mode: "create" | "edit"; initial?: AdminSample }) {
  const router = useRouter();
  const [form, setForm] = useState<AdminSampleRequest>({
    slug: initial?.slug ?? "",
    title: initial?.title ?? "",
    category: initial?.category ?? SAMPLE_CATEGORY_VOCABULARY[0],
    preview: initial?.preview ?? null,
    file: initial?.file ?? null,
    displayOrder: initial?.displayOrder ?? 0,
  });
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [banner, setBanner] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function set<K extends keyof AdminSampleRequest>(key: K, value: AdminSampleRequest[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function validate(): Record<string, string> {
    const errors: Record<string, string> = {};
    if (!form.slug.trim()) errors.slug = "Slug is required.";
    else if (!/^[a-z0-9]+(-[a-z0-9]+)*$/.test(form.slug)) {
      errors.slug = "Lowercase alphanumeric with hyphens.";
    }
    if (!form.title.trim()) errors.title = "Title is required.";
    if (!form.category) errors.category = "Category is required.";
    return errors;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setBanner(null);
    const errors = validate();
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      setBanner("Please fix the fields below.");
      return;
    }
    setFieldErrors({});
    setSubmitting(true);
    try {
      if (mode === "create") {
        await createAdminSample(form);
      } else if (initial) {
        await updateAdminSample(initial.id, form);
      }
      router.push("/admin/samples");
    } catch (err) {
      if (err instanceof AdminApiError && err.status === 409) {
        setFieldErrors({ slug: "That slug is already used." });
        setBanner("That slug is already used.");
      } else if (err instanceof AdminApiError && err.problem?.fields) {
        const mapped: Record<string, string> = {};
        for (const [field, messages] of Object.entries(err.problem.fields)) {
          mapped[field] = messages.join(" ");
        }
        setFieldErrors(mapped);
        setBanner("Please fix the fields below.");
      } else {
        setBanner("Something went wrong, please retry.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-[640px]">
      <FormErrorBanner message={banner} />

      <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
        <AdminField id="s-slug" label="Slug" required error={fieldErrors.slug}>
          <AdminInput
            id="s-slug"
            required
            disabled={submitting}
            value={form.slug}
            onChange={(e) => set("slug", e.target.value)}
          />
        </AdminField>
        <AdminField id="s-title" label="Title" required error={fieldErrors.title}>
          <AdminInput
            id="s-title"
            required
            disabled={submitting}
            value={form.title}
            onChange={(e) => set("title", e.target.value)}
          />
        </AdminField>
        <AdminField id="s-category" label="Category" required error={fieldErrors.category}>
          <AdminSelect
            id="s-category"
            required
            disabled={submitting}
            value={form.category}
            onChange={(e) => set("category", e.target.value)}
          >
            {SAMPLE_CATEGORY_VOCABULARY.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </AdminSelect>
        </AdminField>
        <AdminField id="s-order" label="Display Order">
          <AdminInput
            id="s-order"
            type="number"
            disabled={submitting}
            value={form.displayOrder}
            onChange={(e) => set("displayOrder", Number(e.target.value))}
          />
        </AdminField>
      </div>

      <div className="mt-5">
        <UploadField
          label="Preview Image"
          category="sample-preview"
          value={form.preview}
          onChange={(url) => set("preview", url)}
          accept="image/jpeg,image/png,image/webp"
          hint="JPG, PNG or WEBP up to 5 MB."
        />
      </div>

      <div className="mt-5">
        <UploadField
          label="Downloadable File"
          category="sample-file"
          kind="document"
          value={form.file}
          onChange={(url) => set("file", url)}
          accept="application/pdf"
          hint="PDF up to 25 MB."
        />
      </div>

      <div className="mt-8 flex flex-col gap-3 sm:flex-row">
        <button
          type="submit"
          disabled={submitting}
          className="w-full bg-gold px-6 py-3 font-sans text-[11px] font-bold uppercase tracking-[0.16em] text-page hover:bg-gold-light disabled:opacity-50 sm:w-auto"
        >
          {submitting ? "Saving…" : "Save"}
        </button>
        <button
          type="button"
          onClick={() => router.push("/admin/samples")}
          className="w-full border border-gold-subtle px-6 py-3 font-sans text-[11px] font-bold uppercase tracking-[0.16em] text-text-primary hover:border-gold sm:w-auto"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
