import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import type { ProjectDetail as ProjectDetailType } from "@/lib/types/project";
import { SECTOR_LABELS, STATUS_LABELS } from "@/lib/types/project";

interface ProjectDetailProps {
  project: ProjectDetailType;
}

/**
 * Detail page body for a single project (F3-AC3) in Dark Prestige style.
 *
 * Renders the full project record as a structured data sheet:
 * - Breadcrumb back link
 * - Large hero image on dark surface
 * - Display L title in Cormorant Garamond with status badge and location in gold
 * - Description in Montserrat body text
 * - Credit fields in a structured data layout on dark surface
 * - Scope as gold-outlined badges
 *
 * Only renders fields that are present (graceful degradation for nullable
 * credits per architecture 4.2).
 */
export function ProjectDetail({ project }: ProjectDetailProps) {
  const isOngoing = project.status === "ongoing";

  return (
    <article>
      {/* Breadcrumb / back link */}
      <nav className="mb-10 flex items-center gap-3.5">
        <Link
          href="/projects"
          className="font-sans text-[8.5px] font-medium uppercase text-text-disabled transition-colors hover:text-text-muted"
          style={{ letterSpacing: "0.12em" }}
        >
          &larr; Back to Projects
        </Link>
        <span style={{ color: "rgba(196,151,58,0.4)", fontSize: "10px" }}>
          &rsaquo;
        </span>
        <span
          className="font-sans text-[8.5px] font-medium uppercase text-gold"
          style={{ letterSpacing: "0.12em" }}
        >
          {project.name}
        </span>
      </nav>

      {/* Hero image */}
      {project.image ? (
        <div className="relative mb-12 aspect-[21/9] w-full overflow-hidden bg-surface-2">
          <img
            src={project.image}
            alt={project.name}
            className="h-full w-full object-cover"
          />
        </div>
      ) : (
        <div
          className="relative mb-12 aspect-[21/9] w-full overflow-hidden"
          style={{
            background:
              "repeating-linear-gradient(135deg, #0A1828 0, #0A1828 12px, #0E2035 12px, #0E2035 24px)",
          }}
        />
      )}

      {/* Title block */}
      <div className="mb-8 space-y-4">
        {/* Location in gold */}
        {project.location && (
          <p
            className="font-sans text-[9px] font-bold uppercase text-gold"
            style={{ letterSpacing: "0.14em" }}
          >
            {project.location}
          </p>
        )}

        {/* Title */}
        <h1 className="font-serif text-[48px] font-light leading-[1.15] text-text-primary">
          {project.name}
        </h1>

        {/* Badges */}
        <div className="flex flex-wrap gap-2">
          <Badge variant={isOngoing ? "accent" : "outline"}>
            {STATUS_LABELS[project.status] || project.status}
          </Badge>
          <Badge variant="default">
            {SECTOR_LABELS[project.sector] || project.sector}
          </Badge>
          <Badge variant="secondary">{project.country}</Badge>
        </div>
      </div>

      {/* Description */}
      {project.description && (
        <div className="mb-12 max-w-3xl">
          <p className="font-sans text-[15px] font-light leading-[1.8] text-text-muted">
            {project.description}
          </p>
        </div>
      )}

      {/* Divider */}
      <div
        className="mb-10 h-[1px] w-full"
        style={{ background: "rgba(196,151,58,0.15)" }}
      />

      {/* Credit fields -- structured data sheet on dark surface */}
      <div className="mb-12 grid grid-cols-1 gap-0.5 sm:grid-cols-2 lg:grid-cols-4">
        {project.mainContractor && (
          <CreditField label="Main Contractor" value={project.mainContractor} />
        )}
        {project.client && (
          <CreditField label="Client / Developer" value={project.client} />
        )}
        {project.consultant && (
          <CreditField label="Consultant" value={project.consultant} />
        )}
        {project.location && (
          <CreditField label="Location" value={project.location} />
        )}
      </div>

      {/* Scope tags */}
      {project.scope.length > 0 && (
        <div className="space-y-4">
          <h2
            className="font-sans text-[8.5px] font-bold uppercase text-gold"
            style={{ letterSpacing: "0.2em" }}
          >
            Scope of Work
          </h2>
          <div className="flex flex-wrap gap-2">
            {project.scope.map((s) => (
              <Badge key={s} variant="outline">
                {s}
              </Badge>
            ))}
          </div>
        </div>
      )}
    </article>
  );
}

interface CreditFieldProps {
  label: string;
  value: string;
}

/**
 * Renders a single credit field as a label/value pair on a dark surface card.
 *
 * Styled like a project data sheet entry: small uppercase gold label,
 * secondary text value, on Surface 2 background with gold accent rule.
 */
function CreditField({ label, value }: CreditFieldProps) {
  return (
    <div className="bg-surface-2 p-6">
      <div className="mb-3 h-[1px] w-5 bg-gold" />
      <dt
        className="mb-1.5 font-sans text-[9px] font-bold uppercase text-gold"
        style={{ letterSpacing: "0.1em" }}
      >
        {label}
      </dt>
      <dd className="font-sans text-[13px] font-light leading-[1.7] text-text-secondary">
        {value}
      </dd>
    </div>
  );
}
