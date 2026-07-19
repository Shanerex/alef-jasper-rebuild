import { PageHero } from "@/components/marketing/page-hero";
import { CompanyStory } from "@/components/about/company-story";
import { GccReachBlock } from "@/components/about/gcc-reach-block";
import { LandmarkProjectsStrip } from "@/components/about/landmark-projects-strip";
import { TeamGrid } from "@/components/team/team-grid";
import { HomeCTA } from "@/components/home/home-cta";
import { getTeam } from "@/lib/api/team";
import { getProjects } from "@/lib/api/projects";
import type { Metadata } from "next";

/**
 * About page (feature 011, F11-AC3, F11-AC4).
 *
 * Server component. Fetches team profiles (new endpoint) and featured projects
 * (003 — same source as Home) for the landmark strip. Both fetches share one
 * Promise.all inside the try/catch graceful-degradation pattern (PITFALL-007).
 *
 * Static sections (CompanyStory, GccReachBlock) always render even on a cold API.
 * TeamGrid and LandmarkProjectsStrip render their own empty-state copy when their
 * data is absent; the About page is complete without either.
 */
export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "About",
  description:
    "ALEF Architectural & Cadding Services — founded Dubai 2003. Company history, leadership team, GCC footprint, and the Jasper India delivery centre.",
};

export default async function AboutPage() {
  try {
    const [teamResponse, projectsResponse] = await Promise.all([
      getTeam(),
      getProjects({ featurable: true, size: 50 }),
    ]);

    return (
      <div>
        <PageHero
          breadcrumb="About"
          sectionLabel="Who We Are"
          heading="About ALEF"
          arabicSubtitle="&#1578;&#1593;&#1585;&#1601; &#1593;&#1604;&#1610;&#1606;&#1575;"
          description="23 years of structural detailing in the Gulf. One company, two continents, a single standard of precision."
        />
        {/* 4px gold accent bar */}
        <div className="h-1 bg-gold" />

        <CompanyStory />
        <GccReachBlock />
        <LandmarkProjectsStrip projects={projectsResponse.content} />
        <TeamGrid members={teamResponse.members} />
        <HomeCTA />
      </div>
    );
  } catch {
    // Cold API fallback: static sections render; data sections show muted loading lines.
    return (
      <div>
        <PageHero
          breadcrumb="About"
          sectionLabel="Who We Are"
          heading="About ALEF"
          description="23 years of structural detailing in the Gulf. One company, two continents, a single standard of precision."
        />
        <div className="h-1 bg-gold" />

        <CompanyStory />
        <GccReachBlock />
        <div className="bg-page py-20 text-center">
          <p className="font-sans text-[15px] font-light text-text-muted">
            Team and project data are loading &mdash; please refresh in a moment.
          </p>
        </div>
        <HomeCTA />
      </div>
    );
  }
}
