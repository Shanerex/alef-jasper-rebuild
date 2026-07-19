"use client";

import { useRef, useState } from "react";
import { uploadAdminFile } from "@/lib/api/admin-uploads";
import type { UploadCategory } from "@/lib/types/admin";
import { AdminApiError } from "@/lib/api/admin-client";
import { resolveUploadUrl } from "@/lib/utils";

/**
 * Reusable upload widget for image/file fields (design.md §B.5, F12-AC20, F12-AC22).
 *
 * Works identically on desktop and mobile (§B.10): the trigger is a native
 * `<input type="file">`, which on a phone opens the OS file picker / camera
 * roll. Upload and record-save are separate steps (architecture §2.4) -- this
 * component only manages the field's current URL value; the parent form
 * persists it on Save.
 */
export function UploadField({
  label,
  category,
  value,
  onChange,
  accept,
  hint,
  kind = "image",
}: {
  label: string;
  category: UploadCategory;
  value: string | null;
  onChange: (url: string | null) => void;
  accept: string;
  hint: string;
  kind?: "image" | "document";
}) {
  const [status, setStatus] = useState<"idle" | "uploading" | "error">("idle");
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  async function handleSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;

    setStatus("uploading");
    setError(null);
    try {
      const result = await uploadAdminFile(file, category);
      onChange(result.url);
      setStatus("idle");
    } catch (err) {
      const message =
        err instanceof AdminApiError && err.problem?.detail
          ? err.problem.detail
          : "Upload failed. Please try again.";
      setError(message);
      setStatus("error");
    } finally {
      if (inputRef.current) inputRef.current.value = "";
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <span className="font-sans text-[10px] font-bold uppercase tracking-[0.18em] text-gold">
        {label}
      </span>

      <div className="flex flex-wrap items-center gap-3">
        {value ? (
          kind === "image" ? (
            // eslint-disable-next-line @next/next/no-img-element -- admin-only preview, arbitrary uploaded origin
            <img
              src={resolveUploadUrl(value) ?? undefined}
              alt=""
              className="h-16 w-16 border border-gold-subtle object-cover"
            />
          ) : (
            <span className="border border-gold-subtle bg-surface-2 px-2.5 py-1 font-sans text-[10px] font-bold uppercase text-gold">
              File attached
            </span>
          )
        ) : (
          <span className="font-sans text-[12px] font-light text-text-muted">None</span>
        )}

        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          disabled={status === "uploading"}
          className="border border-gold-subtle bg-transparent px-3 py-2 font-sans text-[10px] font-bold uppercase tracking-[0.12em] text-text-primary hover:border-gold disabled:opacity-50"
        >
          {status === "uploading" ? "Uploading…" : "Choose file"}
        </button>

        {value && (
          <button
            type="button"
            onClick={() => onChange(null)}
            className="font-sans text-[10px] font-bold uppercase tracking-[0.12em] text-gold hover:text-gold-light"
          >
            Remove
          </button>
        )}

        <input
          ref={inputRef}
          type="file"
          accept={accept}
          onChange={handleSelect}
          className="hidden"
        />
      </div>

      {error ? (
        <p role="alert" className="font-sans text-[11px] text-gold">
          {error}
        </p>
      ) : (
        <p className="font-sans text-[11px] font-light text-text-muted">{hint}</p>
      )}
    </div>
  );
}
