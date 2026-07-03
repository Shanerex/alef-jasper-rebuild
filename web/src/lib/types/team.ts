/**
 * TypeScript mirrors of the GET /api/team DTO (architecture §3.1, feature 011).
 *
 * company, email, and photo are nullable: a team card degrades gracefully by
 * omitting absent fields (design §5 graceful-degradation contract).
 */

/** One leadership profile from the team roster. */
export interface TeamMember {
  name: string;
  role: string;
  company: string | null;
  email: string | null;
  /** Public-relative image path, e.g. "/img/team/k-jeyaraman.jpg". Null until assets exist. */
  photo: string | null;
}

/** Envelope returned by GET /api/team. */
export interface TeamResponse {
  members: TeamMember[];
}
