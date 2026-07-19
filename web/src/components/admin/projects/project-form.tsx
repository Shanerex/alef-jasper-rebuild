"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import type { AdminProject, AdminProjectRequest } from "@/lib/types/admin";
import { PROJECT_STATUS_VOCABULARY, SCOPE_VOCABULARY } from "@/lib/types/admin";
import { createAdminProject, updateAdminProject } from "@/lib/api/admin-projects";
import { AdminApiError } from "@/lib/api/admin-client";
import { AdminField } from "@/components/admin/ui/admin-field";
import { AdminInput, AdminTextarea } from "@/components/admin/ui/admin-input";
import { AdminSelect } from "@/components/admin/ui/admin-select";
import { AdminCheckbox } from "@/components/admin/ui/admin-checkbox";
import { UploadField } from "@/components/admin/upload-field";
import { FormErrorBanner } from "@/components/admin/admin-page-header";
import type { ProjectFilters } from "@/lib/types/project";

/**
 * Create/edit form for a project (design.md §A.2/§B.4, F12-AC4/AC5/AC23).
 *
 * Two-column field grid on `md`+, single column below (§B.10). Required
 * fields marked with a gold asterisk; client validation blocks submit and
 * the server's 400 `fields` map is merged onto the same inputs so nothing
 * saves with a missing required field (F12-AC23).
 */
export function ProjectForm({
  mode,
  initial,
  filters,
}: {
  mode: "create" | "edit";
  initial?: AdminProject;
  filters: ProjectFilters;
}) {
  const router = useRouter();
  const [form, setForm] = useState<AdminProjectRequest>({
    slug: initial?.slug ?? "",
    name: initial?.name ?? "",
    sector: initial?.sector ?? filters.sectors[0] ?? "",
    country: initial?.country ?? "",
    status: initial?.status ?? PROJECT_STATUS_VOCABULARY[0],
    image: initial?.image ?? null,
    description: initial?.description ?? null,
    mainContractor: initial?.mainContractor ?? null,
    client: initial?.client ?? null,
    consultant: initial?.consultant ?? null,
    location: initial?.location ?? null,
    scope: initial?.scope ?? [],
    featurable: initial?.featurable ?? false,
  });
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [banner, setBanner] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function set<K extends keyof AdminProjectRequest>(key: K, value: AdminProjectRequest[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function toggleScope(item: string) {
    setForm((prev) => ({
      ...prev,
      scope: prev.scope.includes(item) ? prev.scope.filter((s) => s !== item) : [...prev.scope, item],
    }));
  }

  function validate(): Record<string, string> {
    const errors: Record<string, string> = {};
    if (!form.slug.trim()) errors.slug = "Slug is required.";
    else if (!/^[a-z0-9]+(-[a-z0-9]+)*$/.test(form.slug)) {
      errors.slug = "Lowercase alphanumeric with hyphens (e.g. doha-metro-gold-line).";
    }
    if (!form.name.trim()) errors.name = "Name is required.";
    if (!form.sector) errors.sector = "Sector is required.";
    if (!form.country.trim()) errors.country = "Country is required.";
    if (!form.status) errors.status = "Status is required.";
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
        await createAdminProject(form);
      } else if (initial) {
        await updateAdminProject(initial.id, form);
      }
      router.push("/admin/projects");
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
    <form onSubmit={handleSubmit} className="max-w-[860px]">
      <FormErrorBanner message={banner} />

      <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
        <AdminField id="p-slug" label="Slug" required error={fieldErrors.slug}>
          <AdminInput
            id="p-slug"
            required
            disabled={submitting}
            value={form.slug}
            onChange={(e) => set("slug", e.target.value)}
          />
        </AdminField>

        <AdminField id="p-name" label="Name" required error={fieldErrors.name}>
          <AdminInput
            id="p-name"
            required
            disabled={submitting}
            value={form.name}
            onChange={(e) => set("name", e.target.value)}
          />
        </AdminField>

        <AdminField id="p-sector" label="Sector" required error={fieldErrors.sector}>
          <AdminSelect
            id="p-sector"
            required
            disabled={submitting}
            value={form.sector}
            onChange={(e) => set("sector", e.target.value)}
          >
            {filters.sectors.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </AdminSelect>
        </AdminField>

        <AdminField id="p-country" label="Country" required error={fieldErrors.country}>
          <AdminInput
            id="p-country"
            required
            disabled={submitting}
            value={form.country}
            onChange={(e) => set("country", e.target.value)}
          />
        </AdminField>

        <AdminField id="p-status" label="Status" required error={fieldErrors.status}>
          <AdminSelect
            id="p-status"
            required
            disabled={submitting}
            value={form.status}
            onChange={(e) => set("status", e.target.value)}
          >
            {PROJECT_STATUS_VOCABULARY.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </AdminSelect>
        </AdminField>

        <AdminField id="p-location" label="Location">
          <AdminInput
            id="p-location"
            disabled={submitting}
            value={form.location ?? ""}
            onChange={(e) => set("location", e.target.value || null)}
          />
        </AdminField>
      </div>

      <div className="mt-5">
        <AdminField id="p-scope" label="Scope">
          <div className="flex flex-wrap gap-4">
            {SCOPE_VOCABULARY.map((item) => (
              <label key={item} className="flex items-center gap-2 font-sans text-[12px] text-text-primary">
                <input
                  type="checkbox"
                  className="h-4 w-4 border border-gold-subtle bg-surface-2 accent-gold"
                  checked={form.scope.includes(item)}
                  onChange={() => toggleScope(item)}
                  disabled={submitting}
                />
                {item}
              </label>
            ))}
          </div>
        </AdminField>
      </div>

      <div className="mt-5">
        <AdminCheckbox
          id="p-featurable"
          label="Featured on home marquee"
          checked={form.featurable}
          onChange={(v) => set("featurable", v)}
          helperText="Shows on the home marquee and counts toward the trust 'marquee projects' stat."
        />
      </div>

      <div className="mt-5">
        <UploadField
          label="Image"
          category="project-image"
          value={form.image}
          onChange={(url) => set("image", url)}
          accept="image/jpeg,image/png,image/webp"
          hint="JPG, PNG or WEBP up to 5 MB."
        />
      </div>

      <div className="mt-5">
        <AdminField id="p-description" label="Description">
          <AdminTextarea
            id="p-description"
            rows={4}
            disabled={submitting}
            value={form.description ?? ""}
            onChange={(e) => set("description", e.target.value || null)}
          />
        </AdminField>
      </div>

      <div className="mt-5 grid grid-cols-1 gap-5 md:grid-cols-3">
        <AdminField id="p-contractor" label="Main Contractor" hint="Shown in the site's trust strip.">
          <AdminInput
            id="p-contractor"
            disabled={submitting}
            value={form.mainContractor ?? ""}
            onChange={(e) => set("mainContractor", e.target.value || null)}
          />
        </AdminField>
        <AdminField id="p-client" label="Client" hint="Shown in the site's trust strip.">
          <AdminInput
            id="p-client"
            disabled={submitting}
            value={form.client ?? ""}
            onChange={(e) => set("client", e.target.value || null)}
          />
        </AdminField>
        <AdminField id="p-consultant" label="Consultant">
          <AdminInput
            id="p-consultant"
            disabled={submitting}
            value={form.consultant ?? ""}
            onChange={(e) => set("consultant", e.target.value || null)}
          />
        </AdminField>
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
          onClick={() => router.push("/admin/projects")}
          className="w-full border border-gold-subtle px-6 py-3 font-sans text-[11px] font-bold uppercase tracking-[0.16em] text-text-primary hover:border-gold sm:w-auto"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
