import type { TrustOverview } from "@/lib/types/trust";
import { TrustStats } from "@/components/trust/trust-stats";
import { ClientStrip } from "@/components/trust/client-strip";
import { CapabilityBadges } from "@/components/trust/capability-badges";

interface TrustLayerProps {
  data: TrustOverview;
}

/**
 * The Trust Layer (feature 005) -- a portable, self-contained credibility
 * section mounted on Home by F011. Surfaces scale and track record where buyers
 * decide: headline stats (F5-AC1), a client/contractor name strip (F5-AC2), and
 * software/standards capability badges (F5-AC3).
 *
 * Dark Prestige per DESIGN_SYSTEM.md: dark page surface throughout, with the
 * Stat Display row as the single gold-inverted element. Composes three bands;
 * each band self-hides when its data is empty, so the section degrades
 * gracefully to whatever content exists. No client state, no interactivity.
 */
export function TrustLayer({ data }: TrustLayerProps) {
  const { stats, clients, contractors, software, standards } = data;

  // Nothing to show at all -> render nothing rather than an empty section.
  const hasAny =
    stats.length > 0 ||
    clients.length > 0 ||
    contractors.length > 0 ||
    software.length > 0 ||
    standards.length > 0;
  if (!hasAny) return null;

  return (
    <section className="bg-page py-20 lg:py-[120px]">
      <div className="mx-auto max-w-site px-6 sm:px-12 lg:px-[80px]">
        {/* Heading block */}
        <div className="mb-10">
          <p
            className="mb-4 font-sans text-[10px] font-bold uppercase text-gold"
            style={{ letterSpacing: "0.22em" }}
          >
            Our Track Record
          </p>
          <h2 className="mb-3 font-serif text-[36px] font-light leading-[1.2] text-text-primary lg:text-[48px]">
            Built on Scale &amp; Trust
          </h2>
          <p
            className="font-arabic text-[20px] font-light"
            style={{ color: "rgba(196,151,58,0.5)", direction: "rtl" }}
          >
            &#1582;&#1576;&#1585;&#1577; &#1608;&#1602;&#1583;&#1585;&#1577;
          </p>
        </div>

        {/* Band 1 -- gold Stat Display row */}
        <TrustStats stats={stats} />

        {/* Band 2 -- client / contractor name strip */}
        {(clients.length > 0 || contractors.length > 0) && (
          <div className="mt-12 lg:mt-20">
            <ClientStrip clients={clients} contractors={contractors} />
          </div>
        )}

        {/* Band 3 -- software / standards capability badges */}
        {(software.length > 0 || standards.length > 0) && (
          <div className="mt-12 lg:mt-20">
            <CapabilityBadges software={software} standards={standards} />
          </div>
        )}
      </div>
    </section>
  );
}
