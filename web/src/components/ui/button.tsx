import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

/**
 * Button component per Dark Prestige DESIGN_SYSTEM.md.
 *
 * Primary: gold bg, dark text. Secondary: gold border, gold text.
 * Ghost: gold text only. All sharp corners, Montserrat uppercase.
 */
const buttonVariants = cva(
  "inline-flex items-center justify-center whitespace-nowrap font-sans text-[10px] font-bold uppercase ring-offset-page transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        /** Gold filled -- primary CTA. */
        default:
          "bg-gold text-page hover:bg-gold-light",
        /** Gold outlined -- secondary action. */
        outline:
          "border border-gold text-gold bg-transparent hover:bg-gold/10",
        /** Text-only ghost -- used for clear/reset actions. */
        ghost:
          "text-gold hover:text-gold-light",
        /** Link-style with underline. */
        link:
          "text-gold underline-offset-4 hover:underline",
      },
      size: {
        default: "px-8 py-3.5",
        sm: "px-4 py-2",
        lg: "px-10 py-4",
        icon: "h-10 w-10",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

/** Action button with Dark Prestige styling -- gold accent, sharp corners. */
const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button";
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        style={{ letterSpacing: "0.16em" }}
        ref={ref}
        {...props}
      />
    );
  }
);
Button.displayName = "Button";

export { Button, buttonVariants };
