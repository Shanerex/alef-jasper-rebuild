import Link from "next/link";
import type { ProjectSummary } from "@/lib/types/project";
import { STATUS_LABELS } from "@/lib/types/project";
import { resolveUploadUrl } from "@/lib/utils";

interface ProjectCardProps {
  project: ProjectSummary;
  /** When true, renders as a large featured card (spans 2 grid columns). */
  featured?: boolean;
}

/**
 * Renders a single project card for the portfolio grid.
 *
 * Dark Prestige style per DESIGN_SYSTEM.md and the Projects Template:
 * - Card surface #0E2035, no border-radius
 * - Image area with status badge overlay
 * - 2px gold top border between image and content
 * - Location in gold uppercase, Cormorant Garamond title, description in slate
 * - "Read More" ghost link in gold
 *
 * The `featured` prop increases image height and title size for the
 * first card in the grid that spans 2 columns.
 */
export function ProjectCard({ project, featured = false }: ProjectCardProps) {
  const imageHeight = featured ? "h-[360px]" : "h-[280px]";
  const titleSize = featured ? "text-[28px]" : "text-[24px]";

  return (
    <Link
      href={`/projects/${project.slug}`}
      className="group block transition-colors"
    >
      {/* Image area with status badge */}
      <div className={`relative ${imageHeight} w-full overflow-hidden bg-surface-2`}>
        {project.image ? (
          <img
            src={resolveUploadUrl(project.image) ?? undefined}
            alt={project.name}
            className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-[1.02]"
            loading="lazy"
          />
        ) : (
          <div
            className="flex h-full w-full items-center justify-center"
            style={{
              background:
                "repeating-linear-gradient(135deg, #0A1828 0, #0A1828 12px, #0E2035 12px, #0E2035 24px)",
            }}
          >
            <span
              className="font-mono text-[9px] uppercase"
              style={{
                color: "rgba(196,151,58,0.25)",
                letterSpacing: "0.1em",
              }}
            >
              project photograph
            </span>
          </div>
        )}

        {/* Status badge overlay */}
        <StatusBadge status={project.status} />
      </div>

      {/* Gold top border */}
      <div className="border-t-2 border-gold bg-surface-2 px-8 pb-7 pt-7">
        {/* Location */}
        {project.location && (
          <p
            className="mb-2 font-sans text-[8px] font-bold uppercase text-gold"
            style={{ letterSpacing: "0.14em" }}
          >
            {project.location}
          </p>
        )}

        {/* Title */}
        <h3
          className={`font-serif ${titleSize} font-normal leading-[1.2] text-text-primary mb-2.5`}
        >
          {project.name}
        </h3>

        {/* Description placeholder -- uses location or sector as fallback */}
        <p className="mb-4 font-sans text-[12px] font-light leading-[1.7] text-slate line-clamp-3">
          {project.location
            ? `${STATUS_LABELS[project.status] || project.status} project in ${project.location}.`
            : `${STATUS_LABELS[project.status] || project.status} ${project.sector} project in ${project.country}.`}
        </p>

        {/* Read More link */}
        <span
          className="font-sans text-[8.5px] font-bold uppercase text-gold transition-colors group-hover:text-gold-light"
          style={{ letterSpacing: "0.12em" }}
        >
          Read More &rarr;
        </span>
      </div>
    </Link>
  );
}

/**
 * Status badge overlaid on the project image.
 *
 * Ongoing: gold filled background with dark text.
 * Completed: dark surface with gold border and gold text.
 */
function StatusBadge({ status }: { status: string }) {
  const label = STATUS_LABELS[status] || status;
  const isOngoing = status === "ongoing";

  return (
    <div
      className="absolute left-5 top-5"
      style={
        isOngoing
          ? { background: "#C4973A", padding: "6px 14px" }
          : {
              background: "#0E2035",
              border: "1px solid rgba(196,151,58,0.3)",
              padding: "6px 14px",
            }
      }
    >
      <span
        className="font-sans text-[7.5px] font-bold uppercase"
        style={{
          letterSpacing: "0.14em",
          color: isOngoing ? "#07111C" : "#C4973A",
        }}
      >
        {label}
      </span>
    </div>
  );
}
