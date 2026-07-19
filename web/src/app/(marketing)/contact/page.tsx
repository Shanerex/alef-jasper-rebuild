import { PageHero } from "@/components/marketing/page-hero";
import { ContactForm } from "@/components/contact/contact-form";
import { OfficeLocations } from "@/components/contact/office-locations";
import { DirectChannels } from "@/components/contact/direct-channels";
import { getOffices } from "@/lib/api/offices";
import type { Office } from "@/lib/types/office";
import type { Metadata } from "next";

/**
 * Contact page (feature 011, F11-AC6, F11-AC7).
 *
 * Server component shell with a nested client ContactForm. The office block
 * is server-fetched; ContactForm is a client component independent of that fetch.
 *
 * Graceful degradation: if getOffices() throws (cold API), the form (client,
 * independent) always renders; OfficeLocations renders its empty-state copy;
 * DirectChannels (static, carries the Dubai phone) still gives a direct channel.
 */
export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Contact",
  description:
    "Contact ALEF Architectural & Cadding Services. Send an enquiry, find our Dubai and India offices, or reach us by phone.",
};

export default async function ContactPage() {
  let offices: Office[] = [];

  try {
    const officesResponse = await getOffices();
    offices = officesResponse.offices;
  } catch {
    // Cold API: offices section degrades; form is independent and always renders.
    offices = [];
  }

  return (
    <div>
      <PageHero
        breadcrumb="Contact"
        sectionLabel="Get In Touch"
        heading="Contact"
        arabicSubtitle="&#1578;&#1608;&#1575;&#1589;&#1604; &#1605;&#1593;&#1606;&#1575;"
        description="Send an enquiry, call the Dubai office, or reach the delivery team in India."
      />
      {/* 4px gold accent bar */}
      <div className="h-1 bg-gold" />

      {/* Contact form — client component, wrapped in the anchor target id */}
      <section
        id="contact-form"
        className="bg-page py-20 lg:py-[120px]"
      >
        <div className="mx-auto max-w-site px-6 sm:px-12 lg:px-[80px]">
          <div className="mb-10">
            <p
              className="mb-4 font-sans text-[10px] font-bold uppercase text-gold"
              style={{ letterSpacing: "0.22em" }}
            >
              Send an Enquiry
            </p>
            <h2 className="font-serif text-[36px] font-light text-text-primary lg:text-[48px]">
              Tell us about your project.
            </h2>
          </div>
          <ContactForm />
        </div>
      </section>

      {/* Office cards with maps */}
      <OfficeLocations offices={offices} />

      {/* Direct channel links */}
      <DirectChannels />
    </div>
  );
}
