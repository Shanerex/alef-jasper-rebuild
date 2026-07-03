import { ProjectCard } from "@/components/projects/project-card";
import type { ProjectSummary } from "@/lib/types/project";

interface ProjectGridProps {
  projects: ProjectSummary[];
}

/**
 * Responsive grid of ProjectCards in the Dark Prestige layout.
 *
 * 3-column grid on desktop, 2 on tablet, 1 on mobile, with 24px gaps.
 * The first project card spans 2 columns and renders with the `featured`
 * prop for a larger image and title, matching the Projects Template.
 * Max-width 1280px, 80px side padding per DESIGN_SYSTEM.md.
 */
export function ProjectGrid({ projects }: ProjectGridProps) {
  if (projects.length === 0) {
    return (
      <div className="flex items-center justify-center py-20">
        <p className="font-sans text-[15px] font-light text-text-muted">
          No projects match the selected filters.
        </p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
      {projects.map((project, index) => (
        <div
          key={project.slug}
          className={index === 0 ? "sm:col-span-2" : ""}
        >
          <ProjectCard project={project} featured={index === 0} />
        </div>
      ))}
    </div>
  );
}
