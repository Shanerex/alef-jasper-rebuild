"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import type { AdminTeamMember, AdminTeamRequest } from "@/lib/types/admin";
import { createAdminTeamMember, updateAdminTeamMember } from "@/lib/api/admin-team";
import { AdminApiError } from "@/lib/api/admin-client";
import { AdminField } from "@/components/admin/ui/admin-field";
import { AdminInput } from "@/components/admin/ui/admin-input";
import { AdminCheckbox } from "@/components/admin/ui/admin-checkbox";
import { UploadField } from "@/components/admin/upload-field";
import { FormErrorBanner } from "@/components/admin/admin-page-header";

/** Create/edit form for a team member (design.md §A.3/§B.4, F12-AC8/AC9/AC23). */
export function TeamForm({ mode, initial }: { mode: "create" | "edit"; initial?: AdminTeamMember }) {
  const router = useRouter();
  const [form, setForm] = useState<AdminTeamRequest>({
    name: initial?.name ?? "",
    role: initial?.role ?? "",
    company: initial?.company ?? null,
    email: initial?.email ?? null,
    photo: initial?.photo ?? null,
    displayOrder: initial?.displayOrder ?? 0,
    active: initial?.active ?? true,
  });
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [banner, setBanner] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function set<K extends keyof AdminTeamRequest>(key: K, value: AdminTeamRequest[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function validate(): Record<string, string> {
    const errors: Record<string, string> = {};
    if (!form.name.trim()) errors.name = "Name is required.";
    if (!form.role.trim()) errors.role = "Role is required.";
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      errors.email = "Must be a valid email address.";
    }
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
        await createAdminTeamMember(form);
      } else if (initial) {
        await updateAdminTeamMember(initial.id, form);
      }
      router.push("/admin/team");
    } catch (err) {
      if (err instanceof AdminApiError && err.problem?.fields) {
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
        <AdminField id="t-name" label="Name" required error={fieldErrors.name}>
          <AdminInput
            id="t-name"
            required
            disabled={submitting}
            value={form.name}
            onChange={(e) => set("name", e.target.value)}
          />
        </AdminField>
        <AdminField id="t-role" label="Role" required error={fieldErrors.role}>
          <AdminInput
            id="t-role"
            required
            disabled={submitting}
            value={form.role}
            onChange={(e) => set("role", e.target.value)}
          />
        </AdminField>
        <AdminField id="t-company" label="Company">
          <AdminInput
            id="t-company"
            disabled={submitting}
            value={form.company ?? ""}
            onChange={(e) => set("company", e.target.value || null)}
          />
        </AdminField>
        <AdminField id="t-email" label="Email" error={fieldErrors.email}>
          <AdminInput
            id="t-email"
            type="email"
            disabled={submitting}
            value={form.email ?? ""}
            onChange={(e) => set("email", e.target.value || null)}
          />
        </AdminField>
        <AdminField id="t-order" label="Display Order">
          <AdminInput
            id="t-order"
            type="number"
            disabled={submitting}
            value={form.displayOrder}
            onChange={(e) => set("displayOrder", Number(e.target.value))}
          />
        </AdminField>
      </div>

      <div className="mt-5">
        <UploadField
          label="Photo"
          category="team-photo"
          value={form.photo}
          onChange={(url) => set("photo", url)}
          accept="image/jpeg,image/png,image/webp"
          hint="JPG, PNG or WEBP up to 5 MB."
        />
      </div>

      <div className="mt-5">
        <AdminCheckbox
          id="t-active"
          label="Active"
          checked={form.active}
          onChange={(v) => set("active", v)}
          helperText="Inactive profiles are hidden from the public About page but not deleted."
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
          onClick={() => router.push("/admin/team")}
          className="w-full border border-gold-subtle px-6 py-3 font-sans text-[11px] font-bold uppercase tracking-[0.16em] text-text-primary hover:border-gold sm:w-auto"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
