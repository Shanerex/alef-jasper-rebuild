import Link from "next/link";

/**
 * Shared hero primitive for About, Services, and Contact pages (design §1.1, F11-AC8).
 *
 * Extracts the exact ProjectsHero pattern from projects/page.tsx so all four
 * pages share one treatment. The 4px gold accent bar is NOT rendered inside
 * this component — each page shell renders it immediately after the hero.
 *
 * Surface 1 background + grid-pattern overlay + breadcrumb + section label +
 * Display XL serif heading + optional Arabic subtitle + optional description.
 */

interface PageHeroProps {
  /** Breadcrumb label for the current page, e.g. "About". Home is always the first crumb. */
  breadcrumb: string;
  /** Gold section label, uppercase, e.g. "Who We Are". */
  sectionLabel: string;
  /** Display XL heading text, e.g. "About ALEF". */
  heading: string;
  /** Arabic decorative subtitle (RTL, reduced gold). Optional. */
  arabicSubtitle?: string;
  /** Muted description paragraph under the heading. Optional. */
  description?: string;
}

export function PageHero({
  breadcrumb,
  sectionLabel,
  heading,
  arabicSubtitle,
  description,
}: PageHeroProps) {
  return (
    <section className="relative overflow-hidden bg-surface-1 pb-20 pt-[100px]">
      {/* Grid pattern overlay — verbatim from ProjectsHero */}
      <div
        className="pointer-events-none absolute inset-0"
        aria-hidden="true"
        style={{
          backgroundImage:
            "repeating-linear-gradient(rgba(196,151,58,0.03) 0 1px, transparent 1px 72px), repeating-linear-gradient(90deg, rgba(196,151,58,0.03) 0 1px, transparent 1px 72px)",
          backgroundSize: "72px 72px",
        }}
      />

      <div className="relative mx-auto max-w-site px-6 sm:px-12 lg:px-[80px]">
        {/* Breadcrumb row */}
        <div className="mb-6 flex items-center gap-3.5">
          <Link
            href="/"
            className="font-sans text-[8.5px] font-medium uppercase text-text-disabled transition-colors hover:text-text-muted"
            style={{ letterSpacing: "0.12em" }}
          >
            Home
          </Link>
          <span style={{ color: "rgba(196,151,58,0.4)", fontSize: "10px" }}>
            &rsaquo;
          </span>
          <span
            className="font-sans text-[8.5px] font-medium uppercase text-gold"
            style={{ letterSpacing: "0.12em" }}
          >
            {breadcrumb}
          </span>
        </div>

        {/* Section label */}
        <p
          className="mb-4 font-sans text-[9.5px] font-bold uppercase text-gold"
          style={{ letterSpacing: "0.24em" }}
        >
          {sectionLabel}
        </p>

        {/* Display XL heading — responsive scale from design §0 */}
        <h1 className="mb-3 font-serif text-[44px] font-light leading-[1.1] text-text-primary sm:text-[56px] lg:text-[72px]">
          {heading}
        </h1>

        {/* Arabic decorative subtitle */}
        {arabicSubtitle && (
          <p
            className="mb-7 mt-3 font-arabic text-[22px] font-light"
            style={{ color: "rgba(196,151,58,0.5)", direction: "rtl" }}
          >
            {arabicSubtitle}
          </p>
        )}

        {/* Optional description */}
        {description && (
          <p className="max-w-[600px] font-sans text-[15px] font-light leading-[1.8] text-text-muted">
            {description}
          </p>
        )}
      </div>
    </section>
  );
}
