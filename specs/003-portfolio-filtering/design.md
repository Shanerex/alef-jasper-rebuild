---
feature: 003-portfolio-filtering
spec_id: "003"
phase: design
owner: Architect
status: approved
version: "1.0"
entry_criteria:
  - upstream phase approved
exit_criteria:
  - Architect has produced this file and a human has approved it
---

# Design: Portfolio with Filtering

This design bridges the approved architecture (`architecture.md`) to implementation. It names concrete
classes, files, packages, and components, and defines signatures and type shapes. It contains no
implementation code (no method bodies, no full JSX, no SQL beyond the DDL the architecture already
fixed). Everything here is consistent with `architecture.md` and DEC-009 through DEC-012.

---

## 1. API Layer (Spring Boot)

### 1.1 Package structure

Base package: `com.alef.api.portfolio`. Standard Spring layering, one package per responsibility:

```
com.alef.api.portfolio
├── controller   // ProjectController
├── service      // ProjectService, ProjectFilterQuery, ProjectMapper
├── repository   // ProjectRepository, ProjectSpecifications
├── entity       // ProjectEntity, StringArrayConverter (if converter route chosen)
├── dto          // ProjectSummaryDto, ProjectDetailDto, ProjectFiltersDto, PagedResponse<T>
├── vocabulary   // Sector, ProjectStatus (validated app-layer enums per DEC-009)
└── error        // InvalidFilterValueException, ProjectNotFoundException, PortfolioExceptionHandler
```

Cross-cutting note: `PortfolioExceptionHandler` is a `@RestControllerAdvice` scoped to this feature's
exceptions. If the project already has a global exception handler, these handlers fold into it instead;
the Dev should prefer an existing global advice over introducing a parallel one. Either way the mapping
is: `InvalidFilterValueException -> 400`, `ProjectNotFoundException -> 404`.

### 1.2 Entity: `ProjectEntity`

Maps the `project` DDL (architecture 2.1) to a JPA entity. Table name `project`.

| Field            | Java type      | Mapping notes |
|------------------|----------------|---------------|
| `id`             | `Long`         | `@Id @GeneratedValue(strategy = IDENTITY)` — `BIGINT GENERATED ALWAYS AS IDENTITY`. Internal only, never serialized to a public DTO. |
| `slug`           | `String`       | `@Column(nullable = false, unique = true)`. Public routing key. |
| `name`           | `String`       | `@Column(nullable = false)`. |
| `sector`         | `String`       | `@Column(nullable = false)`. Stored as TEXT (DEC-009); validated via the `Sector` vocabulary, not a JPA enum, so unseen values from 012 do not break reads. |
| `country`        | `String`       | `@Column(nullable = false)`. Display name (e.g. "UAE"). |
| `status`         | `String`       | `@Column(nullable = false)`. Stored as TEXT; validated via `ProjectStatus` vocabulary. |
| `image`          | `String`       | nullable. |
| `description`    | `String`       | nullable, `@Column(columnDefinition = "text")`. |
| `mainContractor` | `String`       | `@Column(name = "main_contractor")`, nullable. |
| `client`         | `String`       | nullable. |
| `consultant`     | `String`       | nullable. |
| `location`       | `String`       | nullable. |
| `scope`          | `List<String>` | Postgres `TEXT[]` (DEC-010). Map via `@JdbcTypeCode(SqlTypes.ARRAY) @Column(columnDefinition = "text[]")` (Hibernate 6 native array support, preferred), **or** a `StringArrayConverter implements AttributeConverter<List<String>, String[]>` if the array codec needs explicit control. Dev picks one; the `@JdbcTypeCode` route is preferred for less custom code. |
| `featurable`     | `boolean`      | `@Column(nullable = false)`, defaults FALSE at DB level. |
| `createdAt`      | `Instant`      | `@Column(name = "created_at", nullable = false, updatable = false)`. DB default `now()`; read-only on the entity. Not serialized to any public DTO. |
| `updatedAt`      | `Instant`      | `@Column(name = "updated_at", nullable = false)`. Not serialized publicly; exists for the 012 edit path. |

Notes:
- `sector` and `status` are deliberately `String`, NOT JPA-mapped enums. Per DEC-009 the DB is the
  loose store and the app is the validator; binding a JPA enum here would reintroduce the rigidity
  DEC-009 rejected and would throw on any value 012 adds before a redeploy.
- No JPA lifecycle write hooks are needed in this read-only feature; timestamps are DB-defaulted.

### 1.3 DTOs

All DTOs are immutable Java `record`s. Field naming is camelCase; JSON keys match the architecture's
sample responses (3.1, 3.2, 3.3).

**`ProjectSummaryDto`** (list/marquee projection, DEC-011 — card fields only):
```
record ProjectSummaryDto(
    String slug,
    String name,
    String sector,
    String country,
    String status,
    String image,
    String location,
    boolean featurable
)
```

**`ProjectDetailDto`** (full public record; omits id/createdAt/updatedAt per architecture 3.2):
```
record ProjectDetailDto(
    String slug,
    String name,
    String sector,
    String country,
    String status,
    String image,
    String description,
    String mainContractor,
    String client,
    String consultant,
    String location,
    List<String> scope,
    boolean featurable
)
```

**`ProjectFiltersDto`** (filter-options discoverability, architecture 3.3):
```
record ProjectFiltersDto(
    List<String> sectors,    // fixed vocabulary, Sector order
    List<String> statuses,   // fixed vocabulary, ProjectStatus order
    List<String> countries   // SELECT DISTINCT country ... ORDER BY country
)
```

**`PagedResponse<T>`** (explicit envelope, architecture 3.1):
```
record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
)
```

Decision (not prescribed verbatim by the architecture, but implied by its sample shape): use this
custom `PagedResponse<T>` envelope, NOT Spring's `Page<T>` serialized directly. Spring's default
`Page` serialization emits a different, unstable shape (`pageable`, `sort`, `numberOfElements`, etc.)
and Spring itself warns that shape is not a stable API contract. The architecture's 3.1 sample fixes
exactly `content / page / size / totalElements / totalPages`, so the service maps the repository
`Page<...>` into `PagedResponse<ProjectSummaryDto>` before returning. A small
`PagedResponse.from(Page<T>)` static factory keeps the mapping in one place.

### 1.4 Repository: `ProjectRepository`

`interface ProjectRepository extends JpaRepository<ProjectEntity, Long>, JpaSpecificationExecutor<ProjectEntity>`.

Method/query approach:

- **Filtered list** — use `JpaSpecificationExecutor` with a `ProjectSpecifications` helper that composes
  optional `sector`, `country`, `status`, and `featurable` predicates (AND semantics, architecture 3.1).
  Specification is chosen over a multi-arg `@Query` because all four filters are independently optional;
  a Specification composes only the predicates actually supplied without a combinatorial set of query
  methods or nullable-parameter `WHERE (:sector IS NULL OR ...)` boilerplate.
  Called as `findAll(Specification<ProjectEntity>, Pageable)` -> `Page<ProjectEntity>`.
- **Detail by slug** — derived query: `Optional<ProjectEntity> findBySlug(String slug)`.
- **Distinct countries** — `@Query("SELECT DISTINCT p.country FROM ProjectEntity p ORDER BY p.country")`
  exposed as `List<String> findDistinctCountries()`.

`ProjectSpecifications` (in the `repository` package) — static factory methods returning
`Specification<ProjectEntity>`:
```
static Specification<ProjectEntity> hasSector(String sector)         // null -> no predicate
static Specification<ProjectEntity> hasCountry(String country)       // null -> no predicate
static Specification<ProjectEntity> hasStatus(String status)         // null -> no predicate
static Specification<ProjectEntity> isFeaturable(Boolean featurable) // null -> no predicate
```

### 1.5 Service: `ProjectService`

Thin orchestration. Validates the constrained-vocabulary filters (sector, status) before querying,
maps entities to DTOs, and assembles the paged envelope. `country` is NOT validated (architecture:
unknown country -> empty result, not an error).

```
class ProjectService {

    /** Lists projects matching optional filters; validates sector/status against the vocabulary. */
    PagedResponse<ProjectSummaryDto> listProjects(ProjectFilterQuery query, Pageable pageable);

    /** Returns the full public record for a slug, or throws ProjectNotFoundException. */
    ProjectDetailDto getProjectBySlug(String slug);

    /** Returns the fixed sector/status vocabularies plus the data-derived distinct countries. */
    ProjectFiltersDto getFilters();
}
```

`ProjectFilterQuery` is a small carrier record `(String sector, String country, String status,
Boolean featurable)` to avoid a long controller-to-service parameter list. Mapping entity -> DTO lives
in a package-private `ProjectMapper` (plain static methods, no MapStruct dependency required for two
shapes; Dev may use MapStruct if the project already depends on it).

**Validation placement:** sector/status vocabulary validation happens in `ProjectService`. The service
is the authoritative guard so the rule holds regardless of caller. On an invalid value the service
throws `InvalidFilterValueException` (-> 400). On unknown slug `getProjectBySlug` throws
`ProjectNotFoundException` (-> 404).

### 1.6 Controller: `ProjectController`

`@RestController @RequestMapping("/api/projects")`. All endpoints public, read-only.

```
/** GET /api/projects — filterable, paginated summary list. */
@GetMapping
PagedResponse<ProjectSummaryDto> listProjects(
    @RequestParam(required = false) String sector,
    @RequestParam(required = false) String country,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) Boolean featurable,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "50") int size
);

/** GET /api/projects/{slug} — full public detail record. */
@GetMapping("/{slug}")
ProjectDetailDto getProject(@PathVariable String slug);

/** GET /api/projects/filters — sector/status vocabularies + distinct countries. */
@GetMapping("/filters")
ProjectFiltersDto getFilters();
```

`size` is capped to a max (e.g. 100, architecture 3.1) inside the controller/service before building
the `Pageable`. The controller delegates straight to `ProjectService`; it owns no business logic.

**Where validation lives:** unknown `sector`/`status` -> `400` and unknown `slug` -> `404` are produced
by exceptions thrown in `ProjectService` and translated by `PortfolioExceptionHandler`
(`@RestControllerAdvice`). The vocabulary itself lives in the `vocabulary` package (1.7), so the
controller, service, and `/filters` endpoint all reference one source of truth.

### 1.7 Enum / vocabulary classes

Per DEC-009 the constrained sets are app-layer owned and served by `/api/projects/filters`. Model them
as Java enums in the `vocabulary` package, each enum carrying its wire value:

- **`Sector`** — `AIRPORT("airport")`, `MALL_RETAIL("mall_retail")`, `HOTEL_HOSPITALITY("hotel_hospitality")`,
  `RESIDENTIAL("residential")`, `INFRASTRUCTURE_RAIL("infrastructure_rail")`, `LEISURE_MUSEUM("leisure_museum")`.
- **`ProjectStatus`** — `ONGOING("ongoing")`, `COMPLETED("completed")`.

Each enum provides:
```
String wireValue();                       // the lowercase token used in API/DB
static boolean isValid(String value);     // membership check for filter validation
static List<String> wireValues();         // ordered list for ProjectFiltersDto
```

Rationale for enum-over-config: the set is small, requirement-fixed, and compile-time-known for the
public read path; an enum gives type safety and a single ordered source for the `/filters` response.
This does NOT contradict DEC-009 — DEC-009 forbids a *Postgres* ENUM/CHECK (rigid, migration-per-value)
and mandates app-layer validation; a Java enum is the app-layer validator. 012 extends the vocabulary
by editing this enum and redeploying the API (no DB migration), exactly as DEC-009 intends. The
`ProjectEntity.sector/status` fields stay `String` so a value present in data but not yet in the enum
still reads back without throwing.

---

## 2. Database Migrations (Flyway)

Per DEC-012: versioned schema migration plus a separate idempotent SQL seed. Location:
`api/src/main/resources/db/migration/`. Naming follows Flyway's `V<n>__<description>.sql` convention.

| File | Purpose |
|------|---------|
| `V1__create_project_table.sql` | The `CREATE TABLE project (...)` DDL plus the four indexes exactly as fixed in architecture 2.1 (id IDENTITY PK, slug UNIQUE, TEXT[] scope, featurable, timestamps, B-tree indexes on sector/country/status, partial index on `featurable = TRUE`). No additional columns. |
| `V2__seed_projects.sql` | Idempotent demo seed. Structure only: a sequence of `INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable) VALUES (...) ON CONFLICT (slug) DO NOTHING;`. The 38 curated rows are a content task for implementation (architecture 5), not enumerated here. `created_at`/`updated_at` are omitted from the INSERT and take their DB defaults. |

Notes:
- `V1`/`V2` numbering assumes this feature owns the first migrations in the `api` module. If an earlier
  baseline migration already exists (e.g. an infra/bootstrap `V1`), the Dev shifts these to the next
  free version numbers and keeps the create-before-seed order. The version order create -> seed is the
  invariant, not the literal `1`/`2`.
- Seed idempotency via `ON CONFLICT (slug) DO NOTHING` (DEC-012) ensures no collision with 012's
  admin-created rows on a long-lived local volume.

---

## 3. Web Layer (Next.js + TypeScript)

App Router. Paths assume a `web/` module root.

### 3.1 Page structure

**`web/app/projects/page.tsx`** — filterable list page.
- Server Component. Fetches `getProjects()` (full summary catalogue, one page, `size=50`) and
  `getProjectFilters()` at render time via the API client (3.4).
- Rendering: ISR. Export `export const revalidate = 60;` (architecture 4.1) so 012 admin edits surface
  within the window without a redeploy.
- Passes the fetched summary list + filter options down to a client child (`ProjectBrowser`) which
  performs the client-side filtering. The server component itself stays non-interactive (good for SEO:
  crawlers get fully rendered cards).

**`web/app/projects/[slug]/page.tsx`** — detail page.
- Server Component. Fetches `getProject(slug)`.
- `generateStaticParams()` — pre-renders known slugs from `getProjects()` (maps summaries to `{ slug }`).
- ISR fallback: `export const revalidate = 60;` and `export const dynamicParams = true;` so slugs added
  after build (by 012) render on first request (blocking fallback) rather than 404.
- `generateMetadata({ params })` — per-project `<title>`, meta description (from `description`), and
  Open Graph image (from `image`), satisfying F3-AC2 SEO and LEARNING-001 (clean slug URLs, no opaque ids).
- Unknown slug: the API client surfaces a 404 -> the page calls Next's `notFound()`.

### 3.2 Components

All in `web/components/projects/`. Props mirror the API DTO types (3.5).

| Component | File | Props | Responsibility |
|-----------|------|-------|----------------|
| `ProjectCard` | `project-card.tsx` | `{ project: ProjectSummary }` | Renders one card (image, name, sector/country/status badges, location), links to `/projects/{slug}`. Presentational, server-renderable. |
| `ProjectGrid` | `project-grid.tsx` | `{ projects: ProjectSummary[] }` | Responsive grid of `ProjectCard`. Receives the already-filtered list. Presentational. |
| `ProjectFilters` | `project-filters.tsx` | `{ filters: ProjectFilters; value: FilterState; onChange: (next: FilterState) => void }` | Client Component. Sector/country/status controls (chips or selects). Reads option lists from `filters`; reflects/raises selection. Display labels (human-readable sector names) are resolved here from the wire tokens. |
| `ProjectDetail` | `project-detail.tsx` | `{ project: ProjectDetail }` | Detail page body: image, description, credit fields, scope, status. Graceful field-level degradation — renders only fields present (nullable credits, architecture 4.2). Presentational. |
| `ProjectBrowser` | `project-browser.tsx` | `{ projects: ProjectSummary[]; filters: ProjectFilters }` | Client Component. Owns filter state (3.3), composes `ProjectFilters` + `ProjectGrid`. The single client boundary on the list page, keeping the list page's server component a thin data-fetch shell. |

### 3.3 Filter state management

URL search params are the source of truth so filtered views are shareable/bookmarkable
(architecture 4.1). Approach: Next.js `useSearchParams` + `useRouter`/`usePathname` to read and write
the query string inside `ProjectBrowser`. (`nuqs` is an acceptable type-safe alternative if the project
already adopts it; default is the built-in Next hooks to avoid a new dependency.)

State shape (mirrors the URL):
```
type FilterState = {
  sector?: Sector;     // single-select, wire token
  country?: string;    // single-select, exact data value
  status?: ProjectStatus;
};
```

Flow: URL query params -> parsed into `FilterState` on render -> `ProjectFilters` mutates it via
`onChange` -> `ProjectBrowser` pushes the new query string (`router.replace`, shallow) AND applies the
predicate to the in-memory `projects` array -> `ProjectGrid` re-renders the filtered subset. AND
semantics across the three dimensions, matching the API contract. No round-trip: filtering is purely
client-side over the already-loaded catalogue (architecture 4.1). Empty `FilterState` shows all.

### 3.4 API client

`web/lib/api/projects.ts` — typed fetch wrappers over the `api` service. Base URL from an env var
(e.g. `process.env.NEXT_PUBLIC_API_BASE_URL` for client-reachable calls, or a server-only base for
server-component fetches; Dev wires per the project's existing env convention). Each function uses
`fetch` with Next's `revalidate` cache option aligned to the page's ISR window.

```
/** Fetches the summary catalogue (optionally filtered/paged). */
getProjects(params?: ProjectQuery): Promise<PagedResponse<ProjectSummary>>;

/** Fetches one project's full detail by slug; throws-to-notFound on 404. */
getProject(slug: string): Promise<ProjectDetail>;

/** Fetches the sector/status/country filter vocabularies. */
getProjectFilters(): Promise<ProjectFilters>;
```

`ProjectQuery` = `{ sector?: string; country?: string; status?: string; featurable?: boolean; page?: number; size?: number }`.
For v1 the list page calls `getProjects()` with no filter params (load-all, filter client-side); the
filter params exist on the client so the same function serves 011's `featurable=true` marquee fetch and
the future server-side-filtering growth path with no signature change.

### 3.5 Types

`web/lib/types/project.ts` — TypeScript mirrors of the API DTOs:

```
type Sector = "airport" | "mall_retail" | "hotel_hospitality" | "residential" | "infrastructure_rail" | "leisure_museum";
type ProjectStatus = "ongoing" | "completed";

interface ProjectSummary {
  slug: string; name: string; sector: string; country: string; status: string;
  image: string | null; location: string | null; featurable: boolean;
}
interface ProjectDetail {
  slug: string; name: string; sector: string; country: string; status: string;
  image: string | null; description: string | null; mainContractor: string | null;
  client: string | null; consultant: string | null; location: string | null;
  scope: string[]; featurable: boolean;
}
interface ProjectFilters { sectors: string[]; statuses: string[]; countries: string[]; }
interface PagedResponse<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; }
```

`Sector`/`ProjectStatus` literal unions give compile-time safety on the web side; they are the TS
counterpart of the backend `vocabulary` enums. DTO `sector`/`status` fields are typed as `string` at
the boundary (narrowed where safe) so an unknown token returned by `/filters` or a record still renders.

---

## 4. File Tree Summary

New files introduced by this feature (Dev implementation checklist):

```
api/
└── src/main/
    ├── java/com/alef/api/portfolio/
    │   ├── controller/ProjectController.java
    │   ├── service/
    │   │   ├── ProjectService.java
    │   │   ├── ProjectFilterQuery.java
    │   │   └── ProjectMapper.java
    │   ├── repository/
    │   │   ├── ProjectRepository.java
    │   │   └── ProjectSpecifications.java
    │   ├── entity/
    │   │   ├── ProjectEntity.java
    │   │   └── StringArrayConverter.java        // only if the AttributeConverter route is chosen
    │   ├── dto/
    │   │   ├── ProjectSummaryDto.java
    │   │   ├── ProjectDetailDto.java
    │   │   ├── ProjectFiltersDto.java
    │   │   └── PagedResponse.java
    │   ├── vocabulary/
    │   │   ├── Sector.java
    │   │   └── ProjectStatus.java
    │   └── error/
    │       ├── InvalidFilterValueException.java
    │       ├── ProjectNotFoundException.java
    │       └── PortfolioExceptionHandler.java   // or fold into an existing global advice
    └── resources/db/migration/
        ├── V1__create_project_table.sql
        └── V2__seed_projects.sql

web/
├── app/projects/
│   ├── page.tsx
│   └── [slug]/page.tsx
├── components/projects/
│   ├── project-card.tsx
│   ├── project-grid.tsx
│   ├── project-filters.tsx
│   ├── project-detail.tsx
│   └── project-browser.tsx
└── lib/
    ├── api/projects.ts
    └── types/project.ts
```

---

## 5. Visual Direction

This is a structural engineering / rebar detailing consultancy — not a SaaS product or a trendy startup.
The visual language should convey **precision, scale, and trust**. Use shadcn/ui primitives with Tailwind
but tailor them to the construction industry context.

### Palette and tone
- Dark, grounded palette: deep navy or charcoal as the primary, with a warm accent (amber/gold or
  construction-orange) for CTAs and active filter states. White/light gray backgrounds for content areas.
- Avoid playful or rounded aesthetics. Prefer sharp corners, clean grid lines, and generous whitespace
  that suggests technical precision.
- Typography: strong, legible headings (a sans-serif with weight — Inter or similar). Body text
  readable at mobile size. Numbers and stats should feel bold and authoritative.

### Project cards (`ProjectCard`)
- Full-bleed project image as the card hero (construction site / structural photos).
- Sector, country, and status shown as compact, muted badges (not colorful pills — think engineering
  drawing labels).
- Project name in a strong heading. Location as secondary text.
- Card hover: subtle lift or border accent, not an animation-heavy effect.

### Filter bar (`ProjectFilters`)
- Horizontal filter bar above the grid. Sector/country/status as select dropdowns or a chip-toggle row.
- Active filters clearly distinguishable (accent color). A "Clear all" affordance when any filter is set.
- Compact and functional — this is a tool, not a decorative element.

### Project grid (`ProjectGrid`)
- Responsive: 3 columns on desktop, 2 on tablet, 1 on mobile.
- Consistent card height with image aspect ratio locked (e.g. 16:9 or 4:3).
- Grid gap that gives each project breathing room without feeling sparse.

### Detail page (`ProjectDetail`)
- Large hero image at top (full width or inset with contained max-width).
- Project name as an H1 with sector/status badges beside it.
- Description as body text, well-spaced.
- Credit fields (main contractor, client, consultant, location) in a structured info grid or
  definition list — like a project data sheet. Only render fields that are present.
- Scope shown as a row of badges or tags.
- A back-to-portfolio link/breadcrumb at top.

### Overall feel
Think: the portfolio section of a top-tier engineering firm's website. Professional, image-led,
information-dense but not cluttered. Every element earns its place. The portfolio is this company's
strongest sales tool — it should feel like opening a project dossier, not browsing a blog.

---

## 6. Acceptance Criteria Traceability

| AC | Design element |
|----|----------------|
| **F3-AC1** Filter by sector, country, status | `ProjectFilters` + `ProjectBrowser` client filtering (3.2/3.3); `GET /api/projects?sector=&country=&status=` via `ProjectController.listProjects` + `ProjectSpecifications` (1.4/1.6); vocabularies served by `GET /api/projects/filters` (`Sector`, `ProjectStatus`, distinct countries — 1.7). Invalid sector/status -> 400 via `InvalidFilterValueException`. |
| **F3-AC2** Clean slug URL, independently indexable | `web/app/projects/[slug]/page.tsx` with `generateStaticParams` + `generateMetadata` (3.1); `GET /api/projects/{slug}` keyed on `slug` (`findBySlug`, 1.4); slug `NOT NULL UNIQUE` in `V1` DDL. Unknown slug -> 404 / `notFound()`. |
| **F3-AC3** Detail shows image, description, main contractor, client, consultant, location, scope, status | `ProjectDetailDto` (1.3) carries all fields incl. `scope` (TEXT[] -> `List<String>`); `ProjectDetail` component renders them with graceful degradation (3.2); `ProjectEntity` maps nullable credit columns (1.2). |
| **F3-AC4** Marquee projects featurable on home | `featurable` column + partial index (`V1`); `ProjectEntity.featurable`; carried in `ProjectSummaryDto`; `GET /api/projects?featurable=true` via `ProjectSpecifications.isFeaturable` (1.4) — consumed by 011, no extra endpoint needed. |
