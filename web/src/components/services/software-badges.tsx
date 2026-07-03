import { Badge } from "@/components/ui/badge";

/**
 * Software toolchain band on the Services page (design §1.4, F11-AC2).
 *
 * Reuses the same Badge (variant="outline") treatment as 005's CapabilityBadges
 * BadgeGroup. The array comes from getTrustOverview().software (005 endpoint) —
 * never a static re-list (architecture §3.3 "do not duplicate content").
 *
 * Self-hides entirely if software is empty (cold API or pre-V11 seed),
 * keeping the Services page functional without the badge band.
 */

interface SoftwareBadgesProps {
  /** The software array from getTrustOverview() (005). NOT a static re-list. */
  software: string[];
}

export function SoftwareBadges({ software }: SoftwareBadgesProps) {
  if (software.length === 0) return null;

  return (
    <section className="bg-surface-1 py-20 lg:py-[120px]">
      <div className="mx-auto max-w-site px-6 sm:px-12 lg:px-[80px]">
        {/* Section head */}
        <p
          className="mb-4 font-sans text-[10px] font-bold uppercase text-gold"
          style={{ letterSpacing: "0.22em" }}
        >
          Our Toolchain
        </p>
        <h2 className="mb-3 font-serif text-[36px] font-light text-text-primary">
          The software behind the detailing.
        </h2>
        <p className="mb-8 font-sans text-[13px] font-normal text-text-muted">
          Industry-standard detailing software, used across every project.
        </p>

        {/* 28px gold rule — Service Card / BadgeGroup pattern */}
        <div className="mb-5 h-[1px] w-7 bg-gold" />

        {/* Badge row — outline variant matching 005 CapabilityBadges */}
        <div className="flex flex-wrap gap-2">
          {software.map((item) => (
            <Badge key={item} variant="outline">
              {item}
            </Badge>
          ))}
        </div>
      </div>
    </section>
  );
}
