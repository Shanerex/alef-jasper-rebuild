---
feature: 003-portfolio-filtering
spec_id: "003"
phase: implementation
owner: Dev
status: approved
version: "1.0"
entry_criteria:
  - upstream phase approved
exit_criteria:
  - Dev has produced this file and a human has approved it
---

# Implementation: 003 Portfolio with Filtering

## Summary

Feature 003 is the first feature built on the ALEF rebuild. It required scaffolding both the `api/` (Spring Boot) and `web/` (Next.js) projects from scratch, then implementing the full read-only project portfolio: a filterable list at `/projects` and an independently indexable detail page at `/projects/{slug}`.

### What was built

1. **API scaffold** -- Spring Boot 3.4.1 project with Maven, Spring Web, JPA, Postgres, Flyway, Spring AI Ollama starter (dependency only), Dockerfile (multi-stage), application.yml with api/worker profiles, Maven wrapper.

2. **Portfolio API** -- Three read-only REST endpoints under `/api/projects`:
   - `GET /api/projects` -- filterable, paginated summary list (F3-AC1)
   - `GET /api/projects/{slug}` -- full project detail (F3-AC2, F3-AC3)
   - `GET /api/projects/filters` -- vocabulary discoverability (DEC-009)

3. **Flyway migrations** -- `V1__create_project_table.sql` (exact DDL from architecture 2.1) and `V2__seed_projects.sql` (15 idempotent seed projects across 6 sectors, 5 countries, 2 statuses, 8 featurable).

4. **Web scaffold** -- Next.js 15 with App Router, TypeScript, Tailwind CSS (construction-firm palette), shadcn/ui primitives (Badge, Button, Select, Separator), Dockerfile (multi-stage standalone).

5. **Portfolio web** -- ISR server-rendered pages at `/projects` and `/projects/[slug]` with client-side filtering via URL search params, per-project SEO metadata, and graceful degradation for nullable fields.

6. **Tests** -- 18 tests (5 controller HTTP contract, 7 service unit, 3+3 vocabulary). All green.

## File tree

```
api/
  .mvn/wrapper/maven-wrapper.properties
  Dockerfile
  mvnw
  pom.xml
  src/main/java/com/alef/api/
    AlefApiApplication.java
    portfolio/
      controller/ProjectController.java
      dto/PagedResponse.java
      dto/ProjectDetailDto.java
      dto/ProjectFiltersDto.java
      dto/ProjectSummaryDto.java
      entity/ProjectEntity.java
      error/InvalidFilterValueException.java
      error/PortfolioExceptionHandler.java
      error/ProjectNotFoundException.java
      repository/ProjectRepository.java
      repository/ProjectSpecifications.java
      service/ProjectFilterQuery.java
      service/ProjectMapper.java
      service/ProjectService.java
      vocabulary/ProjectStatus.java
      vocabulary/Sector.java
  src/main/resources/
    application.yml
    db/migration/
      V1__create_project_table.sql
      V2__seed_projects.sql
  src/test/java/com/alef/api/portfolio/
    controller/ProjectControllerTest.java
    service/ProjectServiceTest.java
    vocabulary/ProjectStatusTest.java
    vocabulary/SectorTest.java
  src/test/resources/
    application-test.yml

web/
  Dockerfile
  next.config.ts
  package.json
  postcss.config.mjs
  tailwind.config.ts
  tsconfig.json
  public/img/projects/           (placeholder directory for project images)
  src/
    app/
      globals.css
      layout.tsx
      page.tsx
      projects/
        page.tsx
        [slug]/page.tsx
    components/
      projects/
        project-browser.tsx
        project-card.tsx
        project-detail.tsx
        project-filters.tsx
        project-grid.tsx
      ui/
        badge.tsx
        button.tsx
        select.tsx
        separator.tsx
    lib/
      api/projects.ts
      types/project.ts
      utils.ts
```

## QA bug fixes (2026-06-24)

### BUG-1 (Major): Added sector and country filter controls to ProjectFilters

The `ProjectFilters` component originally rendered only status toggle chips. QA correctly identified that F3-AC1 requires filtering by sector, country, AND status. The fix adds three labeled filter rows (Status / Sector / Country), each with an "All" chip plus one chip per vocabulary option. Uses `SECTOR_LABELS` for human-readable sector names and raw country strings from `filters.countries`. The existing `FilterChip` component was reused with identical Dark Prestige styling. No backend or type changes were needed -- the data plumbing in `ProjectBrowser` was already complete.

### BUG-2/3 (Minor): Documented force-dynamic as deliberate deviation

Both page files used `export const dynamic = "force-dynamic"` instead of the architecture-prescribed `export const revalidate = 60`. This is a deliberate accommodation for the Docker Compose build order: the web image builds before the api container starts, so build-time API fetches would fail. Added comprehensive doc comments to both page files explaining the constraint, the tradeoff, and the migration path if the deployment model changes. Logged as PITFALL-007 in LEARNINGS.md.

## Deviations from design spec

1. **Java version: 17 instead of 21.** The local JDK is 17 and the pom.xml targets Java 17 for local compilation. The Dockerfile uses JDK 21, so Docker builds use the intended version. This is a local-dev accommodation, not a design deviation.

2. **Spring Boot 3.4.1 instead of "Spring Boot 4".** Spring Boot 4 is not yet released. 3.4.1 is the latest stable release and supports all required features (Spring AI milestone, JPA, Flyway, Postgres arrays via Hibernate 6). The architecture says "Spring Boot 4" aspirationally; 3.4.x is the correct current version.

3. **15 seed projects instead of 38.** The design says ~38 hand-curated projects but notes the content is an implementation task. 15 representative projects cover all 6 sectors, all 5 countries, both statuses, and both featurable values. The remaining projects can be added in a subsequent content pass.

4. **`@JdbcTypeCode(SqlTypes.ARRAY)` route chosen** over the `StringArrayConverter` alternative, as the design preferred. No `StringArrayConverter.java` file was created.

5. **ProjectEntity no-arg constructor is public** (not protected) to allow test setup from other packages. Standard JPA practice.

## How to run locally

```bash
# Start infrastructure (Postgres, Qdrant, Redis, Ollama)
make infra-up

# Build and start all services
make up

# Or manually:
docker compose up --build
```

The API is available at `http://localhost:8080/api/projects`.
The web UI is available at `http://localhost:3000/projects`.

### Running tests

```bash
# API tests (requires local JDK 17+)
cd api && ./mvnw test

# Web type-check
cd web && npx tsc --noEmit
```

## Seed data summary

15 projects seeded:
- **Sectors:** airport (3), mall_retail (2), hotel_hospitality (2), residential (3), infrastructure_rail (3), leisure_museum (2)
- **Countries:** UAE (3), Qatar (4), Saudi Arabia (4), Oman (2), Bahrain (2)
- **Statuses:** completed (11), ongoing (4)
- **Featurable:** true (8), false (7)
