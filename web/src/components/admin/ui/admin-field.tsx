import type { ReactNode } from "react";

/**
 * Label + input slot + inline error wrapper for admin forms (design.md §B.4/§B.7).
 *
 * All colors via named Tailwind tokens (F12-AC30): label text-gold, error
 * text via a token (text-gold, not a raw red hex -- the design system has no
 * dedicated error-red token, so gold-on-dark carries the same "look here"
 * weight as the rest of the admin palette without introducing a new color).
 */
export function AdminField({
  id,
  label,
  required,
  error,
  hint,
  children,
}: {
  id: string;
  label: string;
  required?: boolean;
  error?: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label
        htmlFor={id}
        className="font-sans text-[10px] font-bold uppercase tracking-[0.18em] text-gold"
      >
        {label}
        {required && <span className="ml-1 text-gold">*</span>}
      </label>
      {children}
      {hint && !error && (
        <p className="font-sans text-[11px] font-light text-text-muted">{hint}</p>
      )}
      {error && (
        <p role="alert" className="font-sans text-[11px] font-normal text-gold">
          {error}
        </p>
      )}
    </div>
  );
}
