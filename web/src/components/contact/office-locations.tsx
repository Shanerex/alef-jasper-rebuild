import { OfficeCard } from "@/components/contact/office-card";
import type { Office } from "@/lib/types/office";

/**
 * Office locations section on the Contact page (design §1.5, F11-AC7).
 *
 * Two cards side-by-side on desktop (2-col grid), stacked on mobile/tablet.
 * Data comes from getOffices() — never a frontend constant (DEC-019).
 *
 * Empty/degraded: renders a muted fallback line when offices array is empty.
 * The Contact page try/catch also guards a cold API independently.
 */

interface OfficeLocationsProps {
  /** Both offices from getOffices(). Ordered server-side (Dubai first). */
  offices: Office[];
}

export function OfficeLocations({ offices }: OfficeLocationsProps) {
  return (
    <section className="bg-page py-20 lg:py-[120px]">
      <div className="mx-auto max-w-site px-6 sm:px-12 lg:px-[80px]">
        {/* Section head */}
        <div className="mb-10">
          <p
            className="mb-4 font-sans text-[10px] font-bold uppercase text-gold"
            style={{ letterSpacing: "0.22em" }}
          >
            Our Offices
          </p>
          <h2 className="font-serif text-[36px] font-light text-text-primary lg:text-[48px]">
            Two offices, one delivery engine.
          </h2>
        </div>

        {offices.length === 0 ? (
          <p className="font-sans text-[15px] font-light text-text-muted">
            Office details are loading&hellip;
          </p>
        ) : (
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            {offices.map((office) => (
              <OfficeCard key={office.key} office={office} />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
