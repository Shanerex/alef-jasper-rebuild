import { TeamCard } from "@/components/team/team-card";
import type { TeamMember } from "@/lib/types/team";

/**
 * Responsive grid of TeamCards for the About page (design §1.3, F11-AC3).
 *
 * 3 columns desktop / 2 tablet / 1 mobile, 24px gap. Flows for any count of
 * members — never hardcoded to 10. The team is expanding (DEC-020 requirement:
 * new profiles addable via a row insert, no code change).
 *
 * Empty/degraded: renders a muted message if members is empty, matching the
 * projects-page degradation copy pattern. The About page try/catch also guards
 * a cold API independently.
 */

interface TeamGridProps {
  /** Ordered team members from getTeam(). Already sorted by display_order server-side. */
  members: TeamMember[];
}

export function TeamGrid({ members }: TeamGridProps) {
  return (
    <section className="bg-page py-20 lg:py-[120px]">
      <div className="mx-auto max-w-site px-6 sm:px-12 lg:px-[80px]">
        {/* Section head */}
        <div className="mb-10">
          <p
            className="mb-4 font-sans text-[10px] font-bold uppercase text-gold"
            style={{ letterSpacing: "0.22em" }}
          >
            Leadership
          </p>
          <h2 className="font-serif text-[36px] font-light text-text-primary lg:text-[48px]">
            The people behind the drawings.
          </h2>
        </div>

        {members.length === 0 ? (
          <p className="font-sans text-[15px] font-light text-text-muted">
            Leadership profiles are loading &mdash; please refresh in a moment.
          </p>
        ) : (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {members.map((member) => (
              // Natural key: name + role (unique per architecture §4.1 UNIQUE constraint)
              <TeamCard key={`${member.name}|${member.role}`} member={member} />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
