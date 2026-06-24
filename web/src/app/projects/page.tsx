import { Suspense } from "react";
import Link from "next/link";
import { getProjects, getProjectFilters } from "@/lib/api/projects";
import { ProjectBrowser } from "@/components/projects/project-browser";

/**
 * Portfolio list page -- filterable grid of all projects (F3-AC1).
 *
 * Dark Prestige layout matching the Projects Template exactly:
 * - Hero section: Surface 1 bg, grid pattern overlay, breadcrumb, section label,
 *   Display XL "Projects" heading, Arabic subtitle, muted description
 * - 4px gold accent bar below the hero
 * - Filter bar with toggle chips (delegated to ProjectBrowser/ProjectFilters)
 * - 3-column project grid with first card spanning 2 cols
 * - CTA section: full-width gold bg with enquiry prompt
 *
 * Server Component. Fetches the full summary catalogue and filter options,
 * then passes them to the client-side ProjectBrowser.
 *
 * Rendering strategy: force-dynamic instead of ISR (revalidate=60).
 * The architecture spec (4.1) prescribes ISR, but in the Docker Compose setup
 * the web image is built before the api container is running. ISR with
 * revalidate would attempt an API fetch at build time and fail, breaking the
 * Docker build. force-dynamic defers all fetches to request time, which is
 * correct for the local-first constraint (ARCHITECTURE.md constraint 1).
 * The API client's fetch options still include `next: { revalidate: 60 }`,
 * which enables Next.js Data Cache at the request level even under
 * force-dynamic. If the deployment model changes such that the API is
 * available at build time, switch to `export const revalidate = 60` and
 * add generateStaticParams for pre-rendering.
 */
export const dynamic = "force-dynamic";

export const metadata = {
  title: "Portfolio",
  description:
    "Browse our portfolio of rebar detailing and structural drafting projects across the GCC region. Filter by sector, country, and status.",
};

export default async function ProjectsPage() {
  try {
    const [projectsResponse, filters] = await Promise.all([
      getProjects({ size: 50 }),
      getProjectFilters(),
    ]);

    return (
      <div>
        {/* Hero section */}
        <ProjectsHero />

        {/* 4px gold accent bar */}
        <div className="h-1 bg-gold" />

        {/* Filter bar + Project grid (client component) */}
        <Suspense
          fallback={
            <div className="bg-page py-20 text-center">
              <p className="font-sans text-[15px] font-light text-text-muted">
                Loading projects...
              </p>
            </div>
          }
        >
          <ProjectBrowser
            projects={projectsResponse.content}
            filters={filters}
          />
        </Suspense>

        {/* CTA section */}
        <ProjectsCTA />
      </div>
    );
  } catch {
    return (
      <div>
        <ProjectsHero />
        <div className="h-1 bg-gold" />
        <div className="bg-page py-20 text-center">
          <p className="font-sans text-[15px] font-light text-text-muted">
            Projects are loading &mdash; please refresh in a moment.
          </p>
        </div>
      </div>
    );
  }
}

/**
 * Hero section for the projects page.
 *
 * Surface 1 background with grid pattern overlay, breadcrumb trail,
 * "OUR PORTFOLIO" section label in gold, Display XL "Projects" heading,
 * Arabic subtitle, and muted description. Matches the Projects Template.
 */
function ProjectsHero() {
  return (
    <section className="relative overflow-hidden bg-surface-1 pb-20 pt-[100px]">
      {/* Grid pattern overlay */}
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage:
            "repeating-linear-gradient(rgba(196,151,58,0.03) 0 1px, transparent 1px 72px), repeating-linear-gradient(90deg, rgba(196,151,58,0.03) 0 1px, transparent 1px 72px)",
          backgroundSize: "72px 72px",
        }}
      />

      <div className="relative mx-auto max-w-site px-[80px]">
        {/* Breadcrumb */}
        <div className="mb-6 flex items-center gap-3.5">
          <Link
            href="/"
            className="font-sans text-[8.5px] font-medium uppercase text-text-disabled transition-colors hover:text-text-muted"
            style={{ letterSpacing: "0.12em" }}
          >
            Home
          </Link>
          <span style={{ color: "rgba(196,151,58,0.4)", fontSize: "10px" }}>
            &rsaquo;
          </span>
          <span
            className="font-sans text-[8.5px] font-medium uppercase text-gold"
            style={{ letterSpacing: "0.12em" }}
          >
            Projects
          </span>
        </div>

        {/* Section label */}
        <p
          className="mb-4 font-sans text-[9.5px] font-bold uppercase text-gold"
          style={{ letterSpacing: "0.24em" }}
        >
          Our Portfolio
        </p>

        {/* Display XL heading */}
        <h1 className="mb-3 font-serif text-[72px] font-light leading-[1.1] text-text-primary">
          Projects
        </h1>

        {/* Arabic subtitle */}
        <p
          className="mb-7 font-arabic text-[22px] font-light"
          style={{ color: "rgba(196,151,58,0.5)", direction: "rtl" }}
        >
          &#1605;&#1588;&#1575;&#1585;&#1610;&#1593;&#1606;&#1575;
        </p>

        {/* Description */}
        <p className="max-w-[600px] font-sans text-[15px] font-light leading-[1.8] text-text-muted">
          A selection of ongoing and completed projects across the UAE, Saudi
          Arabia, Oman and the wider Gulf region.
        </p>
      </div>
    </section>
  );
}

/**
 * Full-width gold CTA section at the bottom of the projects page.
 *
 * Matches the Projects Template: gold background, Cormorant Garamond headline,
 * Arabic subtitle, dark "Send an Enquiry" button.
 */
function ProjectsCTA() {
  return (
    <section className="bg-gold">
      <div className="mx-auto flex max-w-site items-center justify-between px-[80px] py-20">
        <div>
          <h2 className="mb-2 font-serif text-[48px] font-light leading-[1.1] text-page">
            Start your next project with Alef.
          </h2>
          <p
            className="font-arabic text-[16px] font-light"
            style={{ color: "rgba(7,17,28,0.5)", direction: "rtl" }}
          >
            &#1575;&#1576;&#1583;&#1571; &#1605;&#1588;&#1585;&#1608;&#1593;&#1603;
            &#1575;&#1604;&#1602;&#1575;&#1583;&#1605; &#1605;&#1593;
            &#1571;&#1604;&#1601;
          </p>
        </div>
        <Link
          href="#"
          className="inline-block flex-shrink-0 bg-page px-10 py-[18px] font-sans text-[10px] font-bold uppercase text-gold transition-colors hover:bg-surface-1"
          style={{ letterSpacing: "0.14em" }}
        >
          Send an Enquiry
        </Link>
      </div>
    </section>
  );
}
