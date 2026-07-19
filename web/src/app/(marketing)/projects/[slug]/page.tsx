import { notFound } from "next/navigation";
import { getProject } from "@/lib/api/projects";
import { ProjectDetail } from "@/components/projects/project-detail";
import type { Metadata } from "next";

/**
 * Project detail page -- clean slug URL, independently indexable (F3-AC2, F3-AC3).
 *
 * Server Component. Fetches the project by slug and renders the full detail.
 *
 * Rendering strategy: force-dynamic instead of ISR (revalidate=60).
 * The architecture spec (4.2) prescribes SSG+ISR with generateStaticParams,
 * but in the Docker Compose setup the web image is built before the api
 * container is running. ISR and generateStaticParams would attempt API
 * fetches at build time and fail, breaking the Docker build. force-dynamic
 * defers all fetches to request time, which is correct for the local-first
 * constraint (ARCHITECTURE.md constraint 1). If the deployment model changes
 * such that the API is available at build time, switch to
 * `export const revalidate = 60`, export generateStaticParams, and remove
 * this directive.
 *
 * Generates per-project metadata for SEO: title, description, Open Graph image.
 * Dark Prestige styling applied via the ProjectDetail component.
 */
export const dynamic = "force-dynamic";

interface PageProps {
  params: Promise<{ slug: string }>;
}

/**
 * Generates per-project metadata for SEO and Open Graph sharing.
 * Uses the project description and image when available.
 */
export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { slug } = await params;
  const project = await getProject(slug);

  if (!project) {
    return { title: "Project Not Found" };
  }

  return {
    title: project.name,
    description:
      project.description ||
      `${project.name} - ${project.sector} project in ${project.country}`,
    openGraph: {
      title: project.name,
      description:
        project.description ||
        `${project.name} - rebar detailing project in ${project.country}`,
      ...(project.image ? { images: [{ url: project.image }] } : {}),
    },
  };
}

export default async function ProjectDetailPage({ params }: PageProps) {
  const { slug } = await params;
  const project = await getProject(slug);

  if (!project) {
    notFound();
  }

  return (
    <div className="bg-page py-16">
      <div className="mx-auto max-w-site px-[80px]">
        <ProjectDetail project={project} />
      </div>
    </div>
  );
}
