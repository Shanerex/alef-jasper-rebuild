import * as React from "react";

/**
 * Native admin checkbox/toggle with an inline label (design.md §B.4 "toggle").
 * Uses the browser's native checkbox rendering (accent-color via a token
 * class is not standard Tailwind, so the box itself is left unstyled; the
 * label and helper text are the token-colored surface here) -- no raw hex.
 */
export function AdminCheckbox({
  id,
  label,
  checked,
  onChange,
  helperText,
}: {
  id: string;
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  helperText?: string;
}) {
  return (
    <div className="flex items-start gap-2.5">
      <input
        id={id}
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        className="mt-0.5 h-4 w-4 border border-gold-subtle bg-surface-2 accent-gold"
      />
      <label htmlFor={id} className="flex flex-col">
        <span className="font-sans text-[13px] font-normal text-text-primary">{label}</span>
        {helperText && (
          <span className="font-sans text-[11px] font-light text-text-muted">{helperText}</span>
        )}
      </label>
    </div>
  );
}
