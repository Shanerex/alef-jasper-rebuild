---
feature: 011-core-marketing-pages
spec_id: "011"
phase: requirements
owner: PM
status: approved
version: "1.1"
entry_criteria:
  - business rationale captured in handoffs/1-business-to-pm.md
exit_criteria:
  - acceptance criteria are testable and id'd; architect can design from this
---

# Requirements: Core Marketing Pages (Home, About, Services, Contact)

## Problem
The site's foundational content pages exist today as a dated, repetitive PHP brochure (copyright stuck at 2014, three near-identical hero banners, thin About content, a non-functional-looking enquiry form). This is the baseline every other feature sits inside: 001-010 are the differentiators, but none of them matter if the surrounding site looks like 2014. These four pages carry the company's real content (history, leadership, specializations, contact channels) into the new build, modernized.

This feature is content migration plus layout. It is intentionally low-risk and mostly static, which is why it is bundled as one feature rather than split per page.

## Pages in scope

### Home (`/`)
- Single clear hero (replacing the three repetitive banners), with the AI concierge entry point (001) prominent.
- Sector/capability strip, featured marquee projects (pulled from 003), trust stats (005).
- One clear primary CTA path into the RFQ/concierge.

### About (`/about`)
- Company story: ALEF (Dubai, founded March 2003) and the Jasper International Engineering Consultants India delivery arm (Vasudevanallur, Tamil Nadu).
- Leadership/team section: migrate all 10 core team profiles from the prequalification document (name, role, company, contact, photo) and present them as designed cards, not a flat list. The team is expanding; profiles are managed via admin (012) and must be easy to add without a migration.
- The India delivery model explained as a selling point: purpose-built Jasper campus in Vasudevanallur with capacity for 250 staff, own accommodation (196 beds), canteen, QC and project management departments. Frame this as scale and cost advantage, consistent with the vision doc.

### Services / Specializations (`/services`)
- The specializations list reconciled from the prequalification document and live site: rebar shop drawings & BBS, general arrangement drawings, concrete works drawings, global co-ordinates / setting-out drawings, road setting out / profiles / cross sections / road marking / signage & utility services, estimation & quantity surveying, MEP drawings, as-built drawings. Reframed as outcomes rather than a bare capability list, per the original PRD direction.
- Software badges: AutoCAD, Steel PAC RCD, Steel PAC RCS, CADS RC, CADMATE, LOHA, Multi-Rc (full list from prequalification doc). Can be surfaced here or linked from 005; do not duplicate content.

### Contact (`/contact`)
- Working contact form (the current site's form looks non-functional; this one must actually submit).
- Both office locations with maps:
  - **Dubai (ALEF)**: 404 Sheikh Maktoum Building, Damascus Street, Al Qusais. Phone: +971 4 2513840 / +971 4 3434440. Email: alefllc@eim.ae
  - **India (Jasper)**: Plot No. 2/286, Near Dharani Sugars, Vasudevanallur, Tirunelveli Dist., Tamil Nadu 627 758. Phone: 0091 4636 293166. Email: jasperiec@gmail.com
- Direct channels (phone, email) alongside the AI concierge and WhatsApp (009) entry points, not instead of them.

## User stories
- As a buyer landing on the home page, I immediately understand what ALEF does and see one clear way to start an enquiry, not three repeated banners.
- As a buyer evaluating credibility, I read the company history and see the real leadership team, not a placeholder list.
- As a buyer scoping work, I understand the specializations in outcome terms ("we take your structure and return detailing-standard shop drawings") not just a service list.
- As a buyer ready to reach out directly, I can submit a working contact form or find a phone/email without hunting.

## Acceptance criteria
- [ ] F11-AC1 Home has a single hero (not repeated banners) with the concierge entry point visible above the fold.
- [ ] F11-AC2 Home surfaces featured projects (from 003) and trust stats (from 005) without duplicating their data; both pull from the same source of truth.
- [ ] F11-AC3 About migrates the company history (ALEF + Jasper India) and all 10 core team profiles from the prequalification document, with no content loss. Team profiles are API-served and admin-manageable (new profiles can be added without a code change).
- [ ] F11-AC4 About corrects the live site's known content typos when copied over (for example "Intenational" → "International", "Concorse" → "Concourse"). Note: "Cadding" is the legal company name per the trade license and must NOT be changed.
- [ ] F11-AC5 Services presents every specialization from the live site, reframed as outcomes, with nothing dropped.
- [ ] F11-AC6 Contact form actually submits (creates a lead record, same path as other lead sources) and shows a clear success/error state.
- [ ] F11-AC7 Contact shows both office locations (Dubai ALEF + India Jasper) with working maps and direct channels (phone, email) for each.
- [ ] F11-AC8 All four pages share consistent nav/header/footer and pass the same mobile and Lighthouse budgets as the rest of the site.
- [ ] F11-AC9 Copyright year and any other live dead/stale content (the current site is stuck at 2014) is corrected.

## Out of scope
- Localizing this content into Arabic (that is 008; this feature ships the English baseline 008 localizes).
- A CMS for non-technical editing (Phase 3 per the architecture's deployment ladder).
- Rewriting the specializations' technical substance; reframing presentation only, not inventing new capabilities.
