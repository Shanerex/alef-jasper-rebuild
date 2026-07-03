import { HomeHero } from "@/components/home/home-hero";
import { CapabilityStrip } from "@/components/home/capability-strip";
import { FeaturedProjects } from "@/components/home/featured-projects";
import { HomeCTA } from "@/components/home/home-cta";
import { TrustLayer } from "@/components/trust/trust-layer";
import { getProjects } from "@/lib/api/projects";
import { getTrustOverview } from "@/lib/api/trust";
import type { Metadata } from "next";

/**
 * Home page (feature 011, F11-AC1, F11-AC2).
 *
 * Server component. Replaces the placeholder with the full marketing home page.
 * Composes featured projects from 003 (getProjects) and trust stats from 005
 * (getTrustOverview) without duplicating data or components (F11-AC2).
 *
 * force-dynamic: defers fetches to request time — the web container builds
 * before the api container is running in Docker Compose (PITFALL-007).
 * The API clients already pass next: { revalidate: 60 } so flipping to ISR
 * is a one-line change once the deploy model makes the API available at build time.
 *
 * Graceful degradation: the try/catch renders static sections and a muted
 * loading message in place of the data-fetched sections on a cold API.
 */
export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  // Home is the root — does not use the "%s | ALEF …" template; it IS the brand.
  title: "ALEF Architectural & Cadding Services",
  description:
    "GCC rebar detailing and structural drafting consultancy. 23 years. 70,000 tonnes/month. Six countries. Rebar shop drawings, BBS, GA, MEP, setting-out and as-built drawings.",
};

/** Static capability list for the strip under the hero (design §1.2). */
const CAPABILITY_ITEMS = [
  "Rebar Shop Drawings",
  "Bar Bending Schedules",
  "GA Drawings",
  "Setting-Out",
  "MEP",
  "As-Built",
];

export default async function HomePage() {
  try {
    const [projectsResponse, trustOverview] = await Promise.all([
      getProjects({ featurable: true, size: 50 }),
      getTrustOverview(),
    ]);

    return (
      <div>
        <HomeHero />
        {/* 4px gold accent bar */}
        <div className="h-1 bg-gold" />

        <CapabilityStrip items={CAPABILITY_ITEMS} />
        <FeaturedProjects projects={projectsResponse.content} />
        <TrustLayer data={trustOverview} />
        <HomeCTA />
      </div>
    );
  } catch {
    // Cold API fallback: static sections render; data sections show loading message.
    return (
      <div>
        <HomeHero />
        <div className="h-1 bg-gold" />
        <CapabilityStrip items={CAPABILITY_ITEMS} />
        <div className="bg-page py-20 text-center">
          <p className="font-sans text-[15px] font-light text-text-muted">
            Featured work and stats are loading &mdash; please refresh in a
            moment.
          </p>
        </div>
        <HomeCTA />
      </div>
    );
  }
}
