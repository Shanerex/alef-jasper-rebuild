import * as React from "react";
import { cn } from "@/lib/utils";

/** Native admin select, token-only styling (F12-AC30) -- see admin-input.tsx doc for why. */
export const AdminSelect = React.forwardRef<HTMLSelectElement, React.SelectHTMLAttributes<HTMLSelectElement>>(
  ({ className, children, ...props }, ref) => (
    <select
      ref={ref}
      className={cn(
        "w-full border border-gold-subtle bg-surface-2 px-3 py-2 font-sans text-[13px] font-light text-text-primary outline-none focus:border-gold disabled:opacity-50",
        className
      )}
      {...props}
    >
      {children}
    </select>
  )
);
AdminSelect.displayName = "AdminSelect";
