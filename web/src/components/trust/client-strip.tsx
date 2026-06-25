interface ClientStripProps {
  clients: string[];
  contractors: string[];
}

/**
 * Band 2 of the Trust Layer (F5-AC2) -- a named text strip of past clients and
 * main contractors, derived live from the portfolio.
 *
 * v1 is a styled text strip (no logo assets): names flow as a wrapping inline
 * list separated by a gold middot, on the dark page surface. Each group renders
 * only when its array is non-empty; the whole band returns null when both are
 * empty (graceful degradation). No interactivity -- credibility, not navigation.
 */
export function ClientStrip({ clients, contractors }: ClientStripProps) {
  if (clients.length === 0 && contractors.length === 0) return null;

  return (
    <div
      className="border-t pt-12"
      style={{ borderTopColor: "rgba(196,151,58,0.2)" }}
    >
      {clients.length > 0 && (
        <NameGroup label="Trusted By" names={clients} />
      )}
      {contractors.length > 0 && (
        <NameGroup
          label="Main Contractors"
          names={contractors}
          className={clients.length > 0 ? "mt-10" : ""}
        />
      )}
    </div>
  );
}

interface NameGroupProps {
  label: string;
  names: string[];
  className?: string;
}

/**
 * One labelled row of names -- a gold Section Label eyebrow above a wrapping
 * list of names separated by gold middots. Shared by the clients and
 * contractors groups so both read identically.
 */
function NameGroup({ label, names, className = "" }: NameGroupProps) {
  return (
    <div className={className}>
      <p
        className="mb-5 font-sans text-[10px] font-bold uppercase text-gold"
        style={{ letterSpacing: "0.22em" }}
      >
        {label}
      </p>
      <ul className="flex flex-wrap items-center gap-x-6 gap-y-4">
        {names.map((name, index) => (
          <li key={name} className="flex items-center gap-x-6">
            {index > 0 && (
              <span
                aria-hidden="true"
                className="text-gold"
                style={{ opacity: 0.4 }}
              >
                &middot;
              </span>
            )}
            <span
              className="font-sans text-[13px] font-light text-text-secondary"
              style={{ letterSpacing: "0.04em" }}
            >
              {name}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
