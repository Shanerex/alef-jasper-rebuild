import { Badge } from "@/components/ui/badge";

interface CapabilityBadgesProps {
  software: string[];
  standards: string[];
}

/**
 * Band 3 of the Trust Layer (F5-AC3) -- software and detailing standards shown
 * as gold-muted capability badges.
 *
 * Reuses the portfolio Badge (variant="outline") so capability tags match the
 * scope-tag treatment. Each group has a Service-Card-style gold rule + Section
 * Label, then a wrapping row of badges. A group renders only when non-empty;
 * the band returns null when both are empty (graceful degradation).
 */
export function CapabilityBadges({ software, standards }: CapabilityBadgesProps) {
  if (software.length === 0 && standards.length === 0) return null;

  return (
    <div className="grid grid-cols-1 gap-12 md:grid-cols-2">
      {software.length > 0 && (
        <BadgeGroup title="Software" items={software} />
      )}
      {standards.length > 0 && (
        <BadgeGroup title="Standards" items={standards} />
      )}
    </div>
  );
}

interface BadgeGroupProps {
  title: string;
  items: string[];
}

/**
 * One capability group: a 28px gold rule, a gold Section Label, and a wrapping
 * row of outline badges. Shared by the software and standards groups.
 */
function BadgeGroup({ title, items }: BadgeGroupProps) {
  return (
    <div>
      {/* Gold rule (Service Card pattern) */}
      <div className="mb-4 h-[1px] w-7 bg-gold" />
      <p
        className="mb-5 font-sans text-[10px] font-bold uppercase text-gold"
        style={{ letterSpacing: "0.22em" }}
      >
        {title}
      </p>
      <div className="flex flex-wrap gap-2">
        {items.map((item) => (
          <Badge key={item} variant="outline">
            {item}
          </Badge>
        ))}
      </div>
    </div>
  );
}
