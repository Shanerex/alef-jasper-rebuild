"use client";

/**
 * Destructive-action confirmation modal (design.md §B.6, F12-AC24).
 *
 * No one-click deletes anywhere in the admin: every delete flow opens this
 * dialog first. Renders as a centered modal on desktop and a centered sheet
 * with full-width stacked buttons on phone (§B.10) -- the safeguard holds at
 * every width because it's the same component, just narrower.
 *
 * Bug-fix pass (post-QA, handoffs/5-qa-to-dev.md Concern #4): the Delete
 * button now uses the `danger` token (tailwind.config.ts) instead of gold,
 * matching design.md §B.6's "outline/red-tinted, not primary gold" spec so
 * it can't be hit by muscle memory alongside every other gold action button
 * in the admin.
 */
export function DeleteConfirmDialog({
  open,
  title,
  description,
  onCancel,
  onConfirm,
  confirming,
}: {
  open: boolean;
  title: string;
  description: string;
  onCancel: () => void;
  onConfirm: () => void;
  confirming?: boolean;
}) {
  if (!open) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-page/80 p-4"
    >
      <div className="w-full max-w-[420px] border border-gold-subtle bg-surface-2 p-6">
        <h2 className="mb-3 font-sans text-[15px] font-bold text-text-primary">{title}</h2>
        <p className="mb-6 font-sans text-[13px] font-light leading-relaxed text-text-muted">
          {description}
        </p>
        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            autoFocus
            onClick={onCancel}
            className="w-full border border-gold-subtle bg-transparent px-5 py-2.5 font-sans text-[11px] font-bold uppercase tracking-[0.14em] text-text-primary hover:border-gold sm:w-auto"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={confirming}
            className="w-full border border-danger bg-transparent px-5 py-2.5 font-sans text-[11px] font-bold uppercase tracking-[0.14em] text-danger hover:bg-danger hover:text-page disabled:opacity-50 sm:w-auto"
          >
            {confirming ? "Deleting…" : "Delete"}
          </button>
        </div>
      </div>
    </div>
  );
}
