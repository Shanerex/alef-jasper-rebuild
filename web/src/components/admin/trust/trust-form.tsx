"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import type { AdminTrustRequest, AdminTrustRow } from "@/lib/types/admin";
import { TRUST_ITEM_TYPES } from "@/lib/types/admin";
import { createAdminTrustRow, updateAdminTrustRow } from "@/lib/api/admin-trust";
import { AdminApiError } from "@/lib/api/admin-client";
import { AdminField } from "@/components/admin/ui/admin-field";
import { AdminInput } from "@/components/admin/ui/admin-input";
import { AdminSelect } from "@/components/admin/ui/admin-select";
import { FormErrorBanner } from "@/components/admin/admin-page-header";

/**
 * Create/edit form for one trust_content row (design.md §A.4/§B.4, F12-AC12..AC15).
 * The form adapts to the selected itemType: `value` is required for `stat`,
 * hidden/optional for `software`/`standard` badges (design §B.4).
 */
export function TrustForm({ mode, initial }: { mode: "create" | "edit"; initial?: AdminTrustRow }) {
  const router = useRouter();
  const [form, setForm] = useState<AdminTrustRequest>({
    itemKey: initial?.itemKey ?? "",
    itemType: initial?.itemType ?? "stat",
    label: initial?.label ?? "",
    value: initial?.value ?? null,
    unit: initial?.unit ?? null,
    displayOrder: initial?.displayOrder ?? 0,
  });
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [banner, setBanner] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const isStat = form.itemType === "stat";

  function set<K extends keyof AdminTrustRequest>(key: K, value: AdminTrustRequest[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function validate(): Record<string, string> {
    const errors: Record<string, string> = {};
    if (!form.itemKey.trim()) errors.itemKey = "Item key is required.";
    if (!form.label.trim()) errors.label = "Label is required.";
    if (isStat && (!form.value || !form.value.trim())) errors.value = "Value is required for a stat.";
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
        await createAdminTrustRow(form);
      } else if (initial) {
        await updateAdminTrustRow(initial.id, form);
      }
      router.push("/admin/trust");
    } catch (err) {
      if (err instanceof AdminApiError && err.status === 409) {
        setFieldErrors({ itemKey: "That item key is already used." });
        setBanner("That item key is already used.");
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
    <form onSubmit={handleSubmit} className="max-w-[560px]">
      <FormErrorBanner message={banner} />

      <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
        <AdminField id="tr-type" label="Type" required>
          <AdminSelect
            id="tr-type"
            required
            disabled={submitting}
            value={form.itemType}
            onChange={(e) => set("itemType", e.target.value)}
          >
            {TRUST_ITEM_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </AdminSelect>
        </AdminField>

        <AdminField id="tr-key" label="Item Key" required error={fieldErrors.itemKey}>
          <AdminInput
            id="tr-key"
            required
            disabled={submitting}
            value={form.itemKey}
            onChange={(e) => set("itemKey", e.target.value)}
          />
        </AdminField>
      </div>

      <div className="mt-5">
        <AdminField id="tr-label" label="Label" required error={fieldErrors.label}>
          <AdminInput
            id="tr-label"
            required
            disabled={submitting}
            value={form.label}
            onChange={(e) => set("label", e.target.value)}
          />
        </AdminField>
      </div>

      {isStat && (
        <div className="mt-5 grid grid-cols-1 gap-5 md:grid-cols-2">
          <AdminField id="tr-value" label="Value" required error={fieldErrors.value}>
            <AdminInput
              id="tr-value"
              required
              disabled={submitting}
              value={form.value ?? ""}
              onChange={(e) => set("value", e.target.value || null)}
            />
          </AdminField>
          <AdminField id="tr-unit" label="Unit">
            <AdminInput
              id="tr-unit"
              disabled={submitting}
              value={form.unit ?? ""}
              onChange={(e) => set("unit", e.target.value || null)}
            />
          </AdminField>
        </div>
      )}

      <div className="mt-5">
        <AdminField id="tr-order" label="Display Order">
          <AdminInput
            id="tr-order"
            type="number"
            disabled={submitting}
            value={form.displayOrder}
            onChange={(e) => set("displayOrder", Number(e.target.value))}
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
          onClick={() => router.push("/admin/trust")}
          className="w-full border border-gold-subtle px-6 py-3 font-sans text-[11px] font-bold uppercase tracking-[0.16em] text-text-primary hover:border-gold sm:w-auto"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
