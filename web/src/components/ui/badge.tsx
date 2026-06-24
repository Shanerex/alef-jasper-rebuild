import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

/**
 * Badge component styled per Dark Prestige DESIGN_SYSTEM.md.
 *
 * Compact engineering-label aesthetic with gold accents on dark surfaces.
 * No border-radius, muted tones for secondary, gold outline for default/scope.
 */
const badgeVariants = cva(
  "inline-flex items-center border px-2.5 py-1 font-sans text-[8px] font-bold uppercase transition-colors",
  {
    variants: {
      variant: {
        /** Gold border with gold text -- used for sector tags, scope. */
        default:
          "border-gold/20 text-gold",
        /** Muted style for secondary metadata like country. */
        secondary:
          "border-slate/30 text-text-muted",
        /** Gold filled badge for active/ongoing status. */
        accent:
          "border-transparent bg-gold text-page",
        /** Gold outlined for completed status, scope tags. */
        outline:
          "border-gold/30 text-gold",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

/** Compact label badge for sector, status, country, and scope tags. */
function Badge({ className, variant, ...props }: BadgeProps) {
  return (
    <div
      className={cn(badgeVariants({ variant }), className)}
      style={{ letterSpacing: "0.14em" }}
      {...props}
    />
  );
}

export { Badge, badgeVariants };
