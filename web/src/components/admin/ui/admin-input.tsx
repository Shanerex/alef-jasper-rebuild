import * as React from "react";
import { cn } from "@/lib/utils";

/**
 * Native admin text input, token-only styling (F12-AC30).
 *
 * Not built on the marketing `ui/select.tsx`/inputs -- those bake in raw
 * `rgba(...)` colors via inline `style` (see implementation.md deviation
 * note), which would fail the "zero inline style colors" check on any admin
 * screen that rendered them. This component uses only Tailwind token classes.
 */
export const AdminInput = React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input
      ref={ref}
      className={cn(
        "w-full border border-gold-subtle bg-surface-2 px-3 py-2 font-sans text-[13px] font-light text-text-primary outline-none placeholder:text-text-disabled focus:border-gold disabled:opacity-50",
        className
      )}
      {...props}
    />
  )
);
AdminInput.displayName = "AdminInput";

export const AdminTextarea = React.forwardRef<
  HTMLTextAreaElement,
  React.TextareaHTMLAttributes<HTMLTextAreaElement>
>(({ className, ...props }, ref) => (
  <textarea
    ref={ref}
    className={cn(
      "w-full resize-y border border-gold-subtle bg-surface-2 px-3 py-2 font-sans text-[13px] font-light text-text-primary outline-none placeholder:text-text-disabled focus:border-gold disabled:opacity-50",
      className
    )}
    {...props}
  />
));
AdminTextarea.displayName = "AdminTextarea";
