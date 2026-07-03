import { ServiceCard } from "@/components/services/service-card";
import type { ServiceItem } from "@/lib/content/services";

/**
 * Grid of all 8 service specializations for the Services page (design §1.4, F11-AC5).
 *
 * 3 columns desktop / 2 tablet / 1 mobile. 8 cards flow across 3 columns with
 * the trailing 2 on the last row — no special-casing needed.
 * Data source: static SERVICES constant (DEC-016).
 */

interface ServicesListProps {
  /** The 8 outcome-reframed specializations from the static SERVICES constant. */
  services: ServiceItem[];
}

export function ServicesList({ services }: ServicesListProps) {
  return (
    <section className="bg-page py-20 lg:py-[120px]">
      <div className="mx-auto max-w-site px-6 sm:px-12 lg:px-[80px]">
        {/* Section head */}
        <div className="mb-10">
          <p
            className="mb-4 font-sans text-[10px] font-bold uppercase text-gold"
            style={{ letterSpacing: "0.22em" }}
          >
            Specializations
          </p>
          <h2 className="font-serif text-[36px] font-light text-text-primary lg:text-[48px]">
            What we return to you.
          </h2>
        </div>

        {/* 8-card grid — 3/2/1 columns */}
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {services.map((service) => (
            <ServiceCard key={service.key} service={service} />
          ))}
        </div>
      </div>
    </section>
  );
}
