/**
 * Direct channels band for the Contact page (design §1.5, F11-AC7).
 *
 * Three channel cards alongside the form and offices — not instead of them.
 * The concierge and WhatsApp slots are placeholders until features 001 and 009
 * wire their real endpoints; until then they route to safe targets.
 * The call card links to the Dubai head-office number via tel:.
 *
 * Static content; no data fetch required.
 */
export function DirectChannels() {
  const channels = [
    {
      key: "concierge",
      title: "Ask the AI Concierge",
      body: "Get grounded answers on capability, capacity, and turnaround.",
      cta: "Start a chat →",
      href: "#contact-form", // 001 rewires this to the streaming chat UI
    },
    {
      key: "whatsapp",
      title: "Message us on WhatsApp",
      body: "Quick questions, fastest reply.",
      cta: "Open WhatsApp →",
      href: "#", // 009 wires the real link
    },
    {
      key: "call",
      title: "Call the Dubai office",
      body: "+971 4 2513840",
      cta: "Call now →",
      href: "tel:+97142513840",
    },
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
          Talk To Us Directly
        </p>
        <h2 className="mb-10 font-serif text-[36px] font-light text-text-primary">
          Prefer a faster channel?
        </h2>

        {/* Channel cards */}
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-3">
          {channels.map((channel) => (
            <div
              key={channel.key}
              className="bg-surface-2 p-6"
              style={{ border: "1px solid rgba(196,151,58,0.15)" }}
            >
              {/* 28px gold rule */}
              <div className="mb-4 h-[1px] w-7 bg-gold" />

              <h3
                className="mb-2 font-sans text-[10px] font-semibold uppercase text-text-secondary"
                style={{ letterSpacing: "0.12em" }}
              >
                {channel.title}
              </h3>

              <p className="mb-6 font-sans text-[12px] font-normal leading-[1.7] text-text-muted">
                {channel.body}
              </p>

              <a
                href={channel.href}
                className="font-sans text-[10px] font-bold uppercase text-gold transition-colors hover:text-gold-light"
                style={{ letterSpacing: "0.12em" }}
              >
                {channel.cta}
              </a>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
