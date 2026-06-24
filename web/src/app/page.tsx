import Link from "next/link";

/**
 * Minimal home page placeholder in the Dark Prestige style.
 *
 * Feature 011 (Core Marketing Pages) will replace this with the full home
 * page including hero, stats bar, services, featured projects, about, and CTA.
 * For now, directs visitors to the portfolio with the prestige aesthetic.
 */
export default function Home() {
  return (
    <section className="bg-surface-1 relative overflow-hidden">
      {/* Grid pattern overlay */}
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage:
            "repeating-linear-gradient(rgba(196,151,58,0.03) 0 1px, transparent 1px 72px), repeating-linear-gradient(90deg, rgba(196,151,58,0.03) 0 1px, transparent 1px 72px)",
          backgroundSize: "72px 72px",
        }}
      />

      <div className="relative mx-auto max-w-site px-[80px] py-[120px]">
        <div className="max-w-2xl space-y-8">
          {/* Section label */}
          <p
            className="font-sans text-[9.5px] font-bold uppercase text-gold"
            style={{ letterSpacing: "0.24em" }}
          >
            Est. 2003 &middot; Dubai, UAE
          </p>

          {/* Headline */}
          <h1 className="font-serif text-[72px] font-light leading-[1.1] text-text-primary">
            Engineering{" "}
            <span className="italic text-gold">Excellence</span>
            <br />
            Since 2003.
          </h1>

          {/* Description */}
          <p className="max-w-[480px] font-sans text-[15px] font-light leading-[1.8] text-text-muted">
            Structural shop drawings, MEP co-ordination and quantity surveying
            for leading Gulf-region contractors and developers. Premium quality,
            precision delivery.
          </p>

          {/* CTAs */}
          <div className="flex items-center gap-3.5">
            <Link
              href="/projects"
              className="inline-block bg-gold px-9 py-4 font-sans text-[10px] font-bold uppercase text-page transition-colors hover:bg-gold-light"
              style={{ letterSpacing: "0.14em" }}
            >
              View Our Projects
            </Link>
            <Link
              href="#"
              className="inline-block border border-gold/40 px-9 py-4 font-sans text-[10px] font-bold uppercase text-gold transition-colors hover:border-gold"
              style={{ letterSpacing: "0.14em" }}
            >
              Our Services
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
