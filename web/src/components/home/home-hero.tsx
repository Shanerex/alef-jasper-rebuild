import Link from "next/link";

/**
 * Home page hero — single tall hero replacing the three repeated placeholder banners (F11-AC1).
 *
 * Surface 1 background + grid-pattern overlay, no breadcrumb (Home is the root).
 * The concierge entry-point slot (design §8) is a two-button CTA row that routes
 * to /contact#contact-form until feature 001 rewires it to the streaming chat UI.
 *
 * The optional diamond frame decoration is rendered only on lg+ screens
 * (hidden below lg) and is purely decorative (aria-hidden).
 */
export function HomeHero() {
  return (
    <section className="relative overflow-hidden bg-surface-1 pb-[120px] pt-[120px]">
      {/* Grid pattern overlay */}
      <div
        className="pointer-events-none absolute inset-0"
        aria-hidden="true"
        style={{
          backgroundImage:
            "repeating-linear-gradient(rgba(196,151,58,0.03) 0 1px, transparent 1px 72px), repeating-linear-gradient(90deg, rgba(196,151,58,0.03) 0 1px, transparent 1px 72px)",
          backgroundSize: "72px 72px",
        }}
      />

      {/* Diamond frame decoration — design-system Decorative Elements, desktop only */}
      <div
        className="pointer-events-none absolute right-[80px] top-[80px] hidden lg:block"
        aria-hidden="true"
        style={{
          width: "240px",
          height: "240px",
          border: "1px solid rgba(196,151,58,0.2)",
          transform: "rotate(45deg)",
          opacity: 0.6,
        }}
      />

      <div className="relative mx-auto max-w-site px-6 sm:px-12 lg:px-[80px]">
        {/* Section label */}
        <p
          className="mb-6 font-sans text-[10px] font-bold uppercase text-gold"
          style={{ letterSpacing: "0.22em" }}
        >
          Rebar Detailing &amp; Structural Drafting
        </p>

        {/* Display heading — "Precision" in gold italic per design §1.2 */}
        <h1 className="mb-4 font-serif text-[40px] font-light leading-[1.1] text-text-primary lg:text-[72px]">
          <em className="not-italic text-gold" style={{ fontStyle: "italic" }}>
            Precision
          </em>{" "}
          detailing at the scale
          <br className="hidden sm:block" /> the Gulf builds.
        </h1>

        {/* Arabic decorative subtitle */}
        <p
          className="mb-8 mt-4 font-arabic text-[22px] font-light"
          style={{ color: "rgba(196,151,58,0.5)", direction: "rtl" }}
          aria-hidden="true"
        >
          &#1571;&#1604;&#1601;
        </p>

        {/* Sub-description — Body L */}
        <p className="mb-10 max-w-[620px] font-sans text-[16px] font-light leading-[1.75] text-text-muted">
          23 years turning structural drawings into detailing-standard shop
          drawings and BBS — 70,000 tonnes of rebar a month, across six GCC
          countries, from a purpose-built delivery centre.
        </p>

        {/* Concierge entry-point slot — design §8 */}
        <div className="flex flex-col gap-4 sm:flex-row">
          {/* Primary CTA — concierge slot (routes to /contact#contact-form until 001) */}
          <div>
            <Link
              href="/contact#contact-form"
              className="inline-block bg-gold px-8 py-[16px] font-sans text-[10px] font-bold uppercase text-page transition-colors hover:bg-gold-light"
              style={{ letterSpacing: "0.15em" }}
            >
              Ask the Concierge &rarr;
            </Link>
            <p
              className="mt-2 font-sans text-[9px] font-normal text-text-muted"
              style={{ letterSpacing: "0.04em" }}
            >
              Grounded answers on capability, capacity &amp; turnaround.
            </p>
          </div>

          {/* Secondary CTA — portfolio */}
          <Link
            href="/projects"
            className="inline-block self-start border border-gold px-8 py-[16px] font-sans text-[10px] font-bold uppercase text-gold transition-colors hover:bg-gold/10"
            style={{ letterSpacing: "0.15em" }}
          >
            View Portfolio &rarr;
          </Link>
        </div>
      </div>
    </section>
  );
}
