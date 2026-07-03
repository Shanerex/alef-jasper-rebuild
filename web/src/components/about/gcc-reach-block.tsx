/**
 * GCC regional footprint band for the About page (design §1.3).
 *
 * Static content. Surface 1 background with grid-pattern overlay (same treatment
 * as hero sections) to differentiate from the page-bg sections around it.
 * Six country chips + an optional "+Other GCC" chip, each styled as a Service-Card-mini.
 */
export function GccReachBlock() {
  const countries = [
    "UAE",
    "Qatar",
    "Bahrain",
    "Oman",
    "Kuwait",
    "Saudi Arabia",
  ];

  return (
    <section className="relative bg-surface-1 py-20 lg:py-[120px]">
      {/* Subtle grid-pattern overlay */}
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
        {/* Section head */}
        <p
          className="mb-4 font-sans text-[10px] font-bold uppercase text-gold"
          style={{ letterSpacing: "0.22em" }}
        >
          Regional Footprint
        </p>
        <h2 className="mb-3 font-serif text-[36px] font-light text-text-primary lg:text-[48px]">
          On the ground across the Gulf.
        </h2>
        <p className="mb-12 font-sans text-[13px] font-normal text-text-muted">
          Dedicated onsite coordinators in every GCC market.
        </p>

        {/* Country chips — Service-Card-mini style */}
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
          {countries.map((country) => (
            <div
              key={country}
              className="bg-surface-2 p-6"
              style={{ border: "1px solid rgba(196,151,58,0.15)" }}
            >
              {/* 28px gold top rule */}
              <div className="mb-4 h-[1px] w-7 bg-gold" />
              <p
                className="font-sans text-[10px] font-bold uppercase text-text-secondary"
                style={{ letterSpacing: "0.14em" }}
              >
                {country}
              </p>
            </div>
          ))}

          {/* "+Other GCC" muted chip */}
          <div
            className="bg-surface-2 p-6"
            style={{ border: "1px solid rgba(196,151,58,0.08)" }}
          >
            <div className="mb-4 h-[1px] w-7 bg-gold" style={{ opacity: 0.3 }} />
            <p
              className="font-sans text-[10px] font-bold uppercase text-text-muted"
              style={{ letterSpacing: "0.14em", opacity: 0.6 }}
            >
              + Other GCC
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
