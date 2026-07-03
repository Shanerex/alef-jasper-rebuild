import type { ServiceItem } from "@/lib/content/services";

/**
 * Service card (design §6, F11-AC5).
 *
 * Design-system Service Card pattern: Surface 2, gold-subtle border, 24px padding,
 * 28px×1px gold rule at top, no border-radius. Hover: border brightens + bg Surface 4.
 *
 * discipline eyebrow preserves the live-site capability name (F11-AC5 — nothing dropped).
 * title is elevated to Cormorant Garamond 24px (design extension — outcome deserves prestige).
 * outcome body: Montserrat 12px/300, slate text.
 */

interface ServiceCardProps {
  service: ServiceItem;
}

export function ServiceCard({ service }: ServiceCardProps) {
  const { discipline, title, outcome } = service;

  return (
    <div
      className="group border border-[rgba(196,151,58,0.15)] bg-surface-2 p-6 transition-all duration-300 hover:border-[rgba(196,151,58,0.35)] hover:bg-[#122339]"
    >
      {/* 28px × 1px gold rule */}
      <div className="mb-4 h-[1px] w-7 bg-gold" />

      {/* Discipline eyebrow — preserves capability name */}
      <p
        className="mb-3 font-sans text-[8px] font-bold uppercase text-text-muted"
        style={{ letterSpacing: "0.14em" }}
      >
        {discipline}
      </p>

      {/* Outcome title — serif 24px (deliberate design extension per spec §6) */}
      <h3 className="mb-3 font-serif text-[24px] font-normal leading-[1.25] text-text-primary">
        {title}
      </h3>

      {/* Outcome body */}
      <p className="font-sans text-[12px] font-light leading-[1.7] text-slate">
        {outcome}
      </p>
    </div>
  );
}
