import type { TeamMember } from "@/lib/types/team";
import { resolveUploadUrl } from "@/lib/utils";

/**
 * Team member card (design §5, F11-AC3).
 *
 * Surface 2, sharp corners, 2px gold top border — mirrors ProjectCard structure:
 * photo area → gold border → content block.
 *
 * Photo present: renders the image with lazy loading and hover-scale.
 * Photo null (all current profiles): renders a dignified serif monogram
 * placeholder on Surface 3 — NOT the project-hatch diagonal used in portfolio cards.
 *
 * Nullable fields (company, email) are omitted entirely when absent so the card
 * never shows blank gaps (graceful degradation, F11-AC3).
 */

interface TeamCardProps {
  member: TeamMember;
}

/** Extracts the display monogram from a name string.
 *  "K. Jeyaraman" → "KJ"; "Namasivayam" → "N"; "J. Sunitha" → "JS". */
function getMonogram(name: string): string {
  const tokens = name
    .split(/[\s.]+/)
    .filter((t) => t.length > 0 && /[A-Za-z]/.test(t[0]));
  if (tokens.length === 0) return "?";
  if (tokens.length === 1) return tokens[0][0].toUpperCase();
  return (tokens[0][0] + tokens[tokens.length - 1][0]).toUpperCase();
}

export function TeamCard({ member }: TeamCardProps) {
  const { name, role, company, email, photo } = member;
  const monogram = getMonogram(name);

  return (
    <div
      className="group bg-surface-2 transition-colors duration-300"
      style={{ borderTop: "2px solid #C4973A" }}
    >
      {/* Photo area — fixed 280px height */}
      <div
        className="h-[280px] w-full overflow-hidden"
        style={{ backgroundColor: "#0E2035" }}
      >
        {photo ? (
          <img
            src={resolveUploadUrl(photo) ?? undefined}
            alt={name}
            className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-[1.02]"
            loading="lazy"
          />
        ) : (
          /* Monogram placeholder — serif initials on Surface 3 + gold-ghost tint */
          <div
            className="flex h-full w-full items-center justify-center"
            style={{
              backgroundColor: "#101F32",
              backgroundImage:
                "radial-gradient(circle at center, rgba(196,151,58,0.08) 0%, transparent 70%)",
            }}
          >
            <span
              className="font-serif text-[48px] font-light text-gold"
              style={{ opacity: 0.4 }}
              aria-hidden="true"
            >
              {monogram}
            </span>
          </div>
        )}
      </div>

      {/* Content block */}
      <div
        className="transition-colors duration-300 group-hover:bg-surface-4"
        style={{ padding: "28px 32px" }}
      >
        {/* Role eyebrow — matches ProjectCard location line */}
        <p
          className="mb-2 font-sans text-[8px] font-bold uppercase text-gold"
          style={{ letterSpacing: "0.14em" }}
        >
          {role}
        </p>

        {/* Name */}
        <h3 className="mb-2 font-serif text-[24px] font-normal leading-[1.2] text-text-primary">
          {name}
        </h3>

        {/* Company (nullable) */}
        {company && (
          <p
            className="mb-2 font-sans text-[10px] font-bold uppercase text-text-muted"
            style={{ letterSpacing: "0.12em" }}
          >
            {company}
          </p>
        )}

        {/* Email (nullable) */}
        {email && (
          <a
            href={`mailto:${email}`}
            className="font-sans text-[11px] font-normal text-text-muted transition-colors hover:text-gold"
          >
            <span className="mr-1 text-gold">&rarr;</span>
            {email}
          </a>
        )}
      </div>
    </div>
  );
}
