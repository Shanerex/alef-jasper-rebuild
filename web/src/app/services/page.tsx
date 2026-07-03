import { PageHero } from "@/components/marketing/page-hero";
import { ServicesList } from "@/components/services/services-list";
import { SoftwareBadges } from "@/components/services/software-badges";
import { HomeCTA } from "@/components/home/home-cta";
import { getTrustOverview } from "@/lib/api/trust";
import { SERVICES } from "@/lib/content/services";
import type { Metadata } from "next";

/**
 * Services page (feature 011, F11-AC5).
 *
 * Server component. The 8 specializations come from the static SERVICES constant
 * (DEC-016 — fixed marketing copy). The software badges come from 005's
 * getTrustOverview() endpoint to avoid a second copy (architecture §3.3).
 *
 * SoftwareBadges self-hides when the software array is empty (cold API or
 * pre-V11 seed); the rest of the page is unaffected.
 */
export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Services",
  description:
    "Rebar shop drawings, BBS, general arrangement, concrete works, setting-out, road infrastructure, estimation, MEP, and as-built drawings for GCC construction.",
};

export default async function ServicesPage() {
  let software: string[] = [];

  try {
    const trust = await getTrustOverview();
    software = trust.software;
  } catch {
    // Cold API: software badges self-hide; specializations (static) always render.
    software = [];
  }

  return (
    <div>
      <PageHero
        breadcrumb="Services"
        sectionLabel="What We Do"
        heading="Services"
        arabicSubtitle="&#1582;&#1583;&#1605;&#1575;&#1578;&#1606;&#1575;"
        description="Eight structural detailing specializations delivered from our purpose-built campus in Tamil Nadu."
      />
      {/* 4px gold accent bar */}
      <div className="h-1 bg-gold" />

      <ServicesList services={SERVICES} />
      <SoftwareBadges software={software} />
      <HomeCTA />
    </div>
  );
}
