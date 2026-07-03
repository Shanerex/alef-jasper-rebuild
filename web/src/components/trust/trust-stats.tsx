import type { TrustStat } from "@/lib/types/trust";

interface TrustStatsProps {
  stats: TrustStat[];
}

/**
 * Band 1 of the Trust Layer (F5-AC1) -- the gold-inverted Stat Display row.
 *
 * Renders the headline stats (years in business, staff count, monthly steel
 * capacity, marquee project count) as the one inverted surface in the section
 * per DESIGN_SYSTEM.md "Stat Display": gold bg #C4973A, Cormorant numbers,
 * Montserrat uppercase labels, dark text. Values are display strings shown
 * verbatim ("18", "120+", "5,000"); never formatted here. Renders nothing when
 * the array is empty (graceful degradation).
 */
export function TrustStats({ stats }: TrustStatsProps) {
  if (stats.length === 0) return null;

  return (
    <div className="grid grid-cols-2 bg-gold lg:grid-cols-4">
      {stats.map((stat, index) => (
        <div
          key={stat.key}
          className="px-8 py-10"
          style={{
            borderLeftWidth: index === 0 ? 0 : "1px",
            borderLeftStyle: "solid",
            borderLeftColor: "rgba(7,17,28,0.12)",
          }}
        >
          {/* Number + optional unit */}
          <p className="font-serif text-[42px] font-medium leading-[1.05] text-page lg:text-[48px]">
            {stat.value}
          </p>
          {stat.unit && (
            <p
              className="mt-1 font-sans text-[11px] font-bold uppercase"
              style={{
                letterSpacing: "0.12em",
                color: "rgba(7,17,28,0.55)",
              }}
            >
              {stat.unit}
            </p>
          )}

          {/* Label */}
          <p
            className="mt-3 font-sans text-[8px] font-bold uppercase"
            style={{ letterSpacing: "0.14em", color: "rgba(7,17,28,0.6)" }}
          >
            {stat.label}
          </p>
        </div>
      ))}
    </div>
  );
}
