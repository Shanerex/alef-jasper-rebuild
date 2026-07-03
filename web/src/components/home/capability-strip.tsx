/**
 * Capability strip — thin horizontal band under the hero conveying breadth at a glance.
 *
 * Items are separated by a gold middot (·) between each pair. The strip wraps
 * naturally on mobile; middots between wrapped rows are visually acceptable
 * (flex-wrap, no layout change needed at mobile breakpoints).
 *
 * Design §1.2: bg-page, 40px vertical padding, gold-subtle top/bottom borders.
 */

interface CapabilityStripProps {
  /** Sector/capability labels to display. Static list passed by the page. */
  items: string[];
}

export function CapabilityStrip({ items }: CapabilityStripProps) {
  if (items.length === 0) return null;

  return (
    <section
      className="bg-page py-[40px]"
      style={{
        borderTop: "1px solid rgba(196,151,58,0.15)",
        borderBottom: "1px solid rgba(196,151,58,0.15)",
      }}
    >
      <div className="mx-auto max-w-site px-6 sm:px-12 lg:px-[80px]">
        <div className="flex flex-wrap items-center justify-center gap-x-10 gap-y-4">
          {items.map((item, i) => (
            <div key={item} className="flex items-center gap-x-10">
              <span
                className="font-sans text-[10px] font-bold uppercase text-text-secondary"
                style={{ letterSpacing: "0.14em" }}
              >
                {item}
              </span>
              {/* Gold middot separator — omitted after the last item */}
              {i < items.length - 1 && (
                <span
                  className="font-sans text-[14px] text-gold"
                  style={{ opacity: 0.5 }}
                  aria-hidden="true"
                >
                  &middot;
                </span>
              )}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
