"use client";

import type { ProjectSummary } from "@/lib/types/project";

/**
 * Horizontal prestige marquee of landmark project names for the About page (design §4).
 *
 * Data source: the same 003 featurable=true projects that the Home page uses
 * (architecture §10 Decision 4 — single source of truth, DEC-008 demo clearance).
 *
 * The array is doubled back-to-back so the CSS translateX(-50%) animation
 * loops seamlessly. Under prefers-reduced-motion the track is static and wraps.
 *
 * Self-hides entirely if projects is empty — the About page is complete without it.
 *
 * Gotcha: this is a client component because it needs to read
 * window.matchMedia for reduced-motion in older Safari. Next.js allows
 * "use client" on leaf components; the About page itself stays a server component.
 */

interface LandmarkProjectsStripProps {
  /** Featured projects from getProjects({ featurable: true }) — same 003 source as Home. */
  projects: ProjectSummary[];
}

export function LandmarkProjectsStrip({ projects }: LandmarkProjectsStripProps) {
  if (projects.length === 0) return null;

  // Double the array for seamless looping
  const doubled = [...projects, ...projects];

  return (
    <section className="relative bg-surface-1 py-20 lg:py-[120px]">
      {/* Grid pattern overlay */}
      <div
        className="pointer-events-none absolute inset-0"
        aria-hidden="true"
        style={{
          backgroundImage:
            "repeating-linear-gradient(rgba(196,151,58,0.03) 0 1px, transparent 1px 72px), repeating-linear-gradient(90deg, rgba(196,151,58,0.03) 0 1px, transparent 1px 72px)",
          backgroundSize: "72px 72px",
        }}
      />
      {/* 4px gold top rule */}
      <div className="absolute left-0 right-0 top-0 h-1 bg-gold" style={{ opacity: 0.3 }} />

      <div className="relative mx-auto max-w-site px-6 sm:px-12 lg:px-[80px]">
        {/* Section head */}
        <p
          className="mb-4 font-sans text-[10px] font-bold uppercase text-gold"
          style={{ letterSpacing: "0.22em" }}
        >
          Landmark Projects
        </p>
        <h2 className="mb-12 font-serif text-[36px] font-light text-text-primary lg:text-[48px]">
          Where our detailing stands today.
        </h2>
      </div>

      {/* Marquee strip — full-width, overflows the container */}
      <div className="relative w-full overflow-hidden">
        {/* Left fade */}
        <div
          className="pointer-events-none absolute bottom-0 left-0 top-0 z-10 w-20"
          aria-hidden="true"
          style={{
            background: "linear-gradient(to right, #0C1B2E, transparent)",
          }}
        />
        {/* Right fade */}
        <div
          className="pointer-events-none absolute bottom-0 right-0 top-0 z-10 w-20"
          aria-hidden="true"
          style={{
            background: "linear-gradient(to left, #0C1B2E, transparent)",
          }}
        />

        {/*
          Marquee track. The animation is toggled off via @media (prefers-reduced-motion)
          using a Tailwind arbitrary variant — the static fallback wraps the names.
          The -50% translateX is the correct target since the track is doubled.
        */}
        <div
          className="flex w-max motion-reduce:animate-none motion-reduce:flex-wrap motion-reduce:justify-center motion-reduce:gap-x-10 motion-reduce:gap-y-4 motion-reduce:px-6 hover:[animation-play-state:paused]"
          style={{
            animation: "landmark-scroll 40s linear infinite",
          }}
        >
          {doubled.map((project, i) => (
            <div key={`${project.slug}-${i}`} className="flex items-center">
              <span className="whitespace-nowrap px-8 font-serif text-[28px] font-light text-text-secondary lg:text-[36px]">
                {project.name}
              </span>
              {/* Gold diamond separator */}
              <span
                className="inline-block h-2 w-2 flex-shrink-0 bg-gold"
                aria-hidden="true"
                style={{
                  transform: "rotate(45deg)",
                  opacity: 0.4,
                }}
              />
            </div>
          ))}
        </div>
      </div>

      {/* Caption */}
      <div className="relative mx-auto mt-10 max-w-site px-6 text-center sm:px-12 lg:px-[80px]">
        <p className="font-sans text-[12px] font-light text-text-muted">
          &hellip;and 100+ more across six GCC countries.
        </p>
      </div>

      {/* Keyframe animation injected as a style tag */}
      <style>{`
        @keyframes landmark-scroll {
          from { transform: translateX(0); }
          to   { transform: translateX(-50%); }
        }
        @media (prefers-reduced-motion: reduce) {
          .motion-reduce\\:animate-none {
            animation: none !important;
          }
        }
      `}</style>
    </section>
  );
}
