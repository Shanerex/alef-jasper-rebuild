/**
 * Company story section for the About page (design §1.3, F11-AC3, F11-AC4).
 *
 * Two-column prose: Column A = ALEF Dubai founding story; Column B = Jasper India
 * delivery centre as a scale/cost-advantage selling point.
 *
 * Static content. Typos corrected per F11-AC4:
 *   "Intenational" → "International"
 *   "Concorse" → "Concourse"
 * "Cadding" is the legal company name — preserved exactly (F11-AC4 guard).
 *
 * The 2×2 dark mini-stats (campus facts) are rendered within Column B to avoid
 * competing with the gold TrustLayer band. These are About-specific campus
 * figures, NOT the company-wide trust stats (no duplication with feature 005).
 */
export function CompanyStory() {
  return (
    <section className="bg-page py-20 lg:py-[120px]">
      <div className="mx-auto max-w-site px-6 sm:px-12 lg:px-[80px]">
        <div className="grid grid-cols-1 gap-x-[80px] gap-y-16 lg:grid-cols-2">
          {/* Column A — The Firm (ALEF) */}
          <div>
            <p
              className="mb-4 font-sans text-[10px] font-bold uppercase text-gold"
              style={{ letterSpacing: "0.22em" }}
            >
              Who We Are
            </p>
            <h2 className="mb-6 font-serif text-[36px] font-light text-text-primary">
              Founded in Dubai, 2003.
            </h2>
            <div className="space-y-4 font-sans text-[13px] font-normal leading-[1.7] text-text-muted">
              <p>
                ALEF Architectural &amp; Cadding Services was established in
                Dubai in March 2003, built on a single discipline: producing
                rebar shop drawings and bar bending schedules that meet Gulf
                construction standard. Over 23 years we have grown into one of
                the region&rsquo;s most trusted structural detailing
                consultancies.
              </p>
              <p>
                Our project list spans airports, metro lines, mixed-use
                developments, and landmark hospitality across the UAE, Qatar,
                Bahrain, Oman, Kuwait, and Saudi Arabia. Clients come back
                because the drawings are right the first time and the team
                understands the pace of GCC construction.
              </p>
              <p>
                We operate under a single commercial roof — ALEF in Dubai
                leads client relationships, quality, and programme; Jasper
                International Engineering Consultants in India provides the
                detailing engine that makes the scale possible.
              </p>
            </div>
          </div>

          {/* Column B — The Delivery Centre (Jasper India) */}
          <div>
            <p
              className="mb-4 font-sans text-[10px] font-bold uppercase text-gold"
              style={{ letterSpacing: "0.22em" }}
            >
              Delivery Model
            </p>
            <h2 className="mb-6 font-serif text-[36px] font-light text-text-primary">
              A campus built for scale.
            </h2>
            <div className="space-y-4 font-sans text-[13px] font-normal leading-[1.7] text-text-muted">
              <p>
                Jasper International Engineering Consultants operates a
                purpose-built campus in Vasudevanallur, Tirunelveli District,
                Tamil Nadu. The facility was designed from the ground up for
                structural detailing production — not adapted office space, but
                a campus engineered around the workflow.
              </p>
              <p>
                Capacity stands at 250 workstations, with in-house
                accommodation for 196 staff and a canteen on site. The team is
                organised into two project departments and ten detailing
                sections, each with dedicated checkers and detailers working in
                close-knit teams. Full departments cover Quality Control,
                Technical, Administration, and Project Management.
              </p>
              <p>
                This structure is what allows ALEF to absorb large-programme
                volumes without diluting quality — the India delivery centre is
                the reason a GCC-grade result is available at a cost that
                makes commercial sense for major contractors.
              </p>
            </div>

            {/* 2×2 dark mini-stats — campus facts, distinct from trust-layer stats */}
            <div className="mt-10 grid grid-cols-2 gap-6">
              {[
                { value: "250", label: "Staff Capacity" },
                { value: "196", label: "In-house Beds" },
                { value: "10", label: "Detailing Sections" },
                { value: "2", label: "Project Departments" },
              ].map(({ value, label }) => (
                <div key={label}>
                  <p className="font-serif text-[36px] font-light text-gold">
                    {value}
                  </p>
                  <p
                    className="font-sans text-[8px] font-bold uppercase text-text-muted"
                    style={{ letterSpacing: "0.14em" }}
                  >
                    {label}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
