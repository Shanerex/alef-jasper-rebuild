---
feature: 003-portfolio-filtering
spec_id: "003"
phase: requirements
owner: PM
status: approved
version: "1.0"
entry_criteria:
  - business rationale captured
exit_criteria:
  - testable criteria; content migration scoped
---

# Requirements: Portfolio with Filtering

## Problem
The ~38-project portfolio is the strongest asset and the current site shows it as a flat truncated grid. Present it properly, filterable, every project indexable.

## User stories
- As a buyer, I filter projects by sector, country, and status.
- As a buyer, I open a project and see who built it and ALEF's role.
- As a search engine, I can index each project at a clean URL.

## Acceptance criteria
- [ ] F3-AC1 Filter by sector (airport, mall/retail, hotel/hospitality, residential, infrastructure/rail, leisure/museum), country, and status (ongoing/completed).
- [ ] F3-AC2 Each project has a clean slug URL and is independently indexable.
- [ ] F3-AC3 Detail shows image, description, main contractor, client, consultant, location, scope, status.
- [ ] F3-AC4 Marquee projects are featurable on the home page.

## Out of scope
- User accounts or saved projects.
