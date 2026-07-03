import Link from "next/link";
import { ProjectGrid } from "@/components/projects/project-grid";
import type { ProjectSummary } from "@/lib/types/project";

/**
 * Featured projects section on the Home page (design §1.2, F11-AC2).
 *
 * Wraps 003's ProjectGrid unchanged — no new card, no duplicated data. This
 * component is only the section framing (label + heading + grid + ghost link).
 * If projects is empty, ProjectGrid renders its own "No projects" message and
 * the section head still renders.
 */

interface FeaturedProjectsProps {
  /** Featured project summaries from getProjects({ featurable: true }). */
  projects: ProjectSummary[];
}

export function FeaturedProjects({ projects }: FeaturedProjectsProps) {
  return (
    <section className="bg-page py-20 lg:py-[120px]">
      <div className="mx-auto max-w-site px-6 sm:px-12 lg:px-[80px]">
        {/* Section head */}
        <div className="mb-10">
          <p
            className="mb-4 font-sans text-[10px] font-bold uppercase text-gold"
            style={{ letterSpacing: "0.22em" }}
          >
            Selected Work
          </p>
          <h2 className="mb-3 font-serif text-[36px] font-light leading-[1.2] text-text-primary lg:text-[48px]">
            Projects that define the skyline.
          </h2>
          <p
            className="font-arabic text-[20px] font-light"
            style={{ color: "rgba(196,151,58,0.5)", direction: "rtl" }}
            aria-hidden="true"
          >
            &#1605;&#1588;&#1575;&#1585;&#1610;&#1593;&#1606;&#1575;
          </p>
        </div>

        {/* 003 ProjectGrid — reused verbatim (F11-AC2) */}
        <ProjectGrid projects={projects} />

        {/* Ghost link to full portfolio */}
        <div className="mt-12 text-right">
          <Link
            href="/projects"
            className="font-sans text-[10px] font-bold uppercase text-gold transition-colors hover:text-gold-light"
            style={{ letterSpacing: "0.14em" }}
          >
            View All Projects &rarr;
          </Link>
        </div>
      </div>
    </section>
  );
}
