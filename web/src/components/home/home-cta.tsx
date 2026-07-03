import Link from "next/link";

/**
 * Full-width gold CTA band (design §1.2).
 *
 * Structurally identical to the shipped ProjectsCTA in projects/page.tsx for
 * consistency (F11-AC8). The copy is updated to reflect the Home/About context.
 * Reused on About and Services pages as well (design §2.2, §2.3).
 */
export function HomeCTA() {
  return (
    <section className="bg-gold">
      <div className="mx-auto flex max-w-site flex-col gap-6 px-6 py-20 sm:px-12 lg:flex-row lg:items-center lg:justify-between lg:px-[80px]">
        {/* Left copy */}
        <div>
          <h2 className="mb-2 font-serif text-[40px] font-light leading-[1.1] text-page lg:text-[48px]">
            Have drawings ready? Start an enquiry.
          </h2>
          <p
            className="font-arabic text-[16px] font-light"
            style={{ color: "rgba(7,17,28,0.5)", direction: "rtl" }}
            aria-hidden="true"
          >
            &#1575;&#1576;&#1583;&#1571; &#1605;&#1588;&#1585;&#1608;&#1593;&#1603;
            &#1575;&#1604;&#1602;&#1575;&#1583;&#1605; &#1605;&#1593;
            &#1571;&#1604;&#1601;
          </p>
        </div>

        {/* CTA button — bg-page on gold bg, text-gold */}
        <Link
          href="/contact"
          className="inline-block flex-shrink-0 bg-page px-10 py-[18px] font-sans text-[10px] font-bold uppercase text-gold transition-colors hover:bg-surface-1"
          style={{ letterSpacing: "0.14em" }}
        >
          Send an Enquiry
        </Link>
      </div>
    </section>
  );
}
