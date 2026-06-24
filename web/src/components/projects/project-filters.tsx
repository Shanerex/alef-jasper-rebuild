"use client";

import type { ProjectFilters as ProjectFiltersType, FilterState } from "@/lib/types/project";
import { SECTOR_LABELS, STATUS_LABELS } from "@/lib/types/project";

interface ProjectFiltersProps {
  filters: ProjectFiltersType;
  value: FilterState;
  onChange: (next: FilterState) => void;
  /** Total number of visible projects, displayed as "Showing N projects". */
  projectCount: number;
}

/**
 * Multi-row filter bar with chip/toggle buttons per Dark Prestige design.
 *
 * Renders three filter dimensions as labeled chip rows:
 * - Status: "All" / Ongoing / Completed
 * - Sector: "All" / Airport / Mall-Retail / etc. (labels from SECTOR_LABELS)
 * - Country: "All" / UAE / Qatar / etc. (values from filters.countries)
 *
 * Active chip: gold background (#C4973A), dark text (#07111C).
 * Inactive chip: transparent, gold-subtle border, muted text (#7A8FA8).
 *
 * The result count ("Showing N projects") appears at the right of the
 * status row. URL query params are the source of truth for active state
 * (managed by the parent ProjectBrowser component).
 *
 * All three dimensions are AND-combined by the parent's filtering logic:
 * selecting sector=airport + country=UAE shows only airport projects in UAE.
 */
export function ProjectFilters({
  filters,
  value,
  onChange,
  projectCount,
}: ProjectFiltersProps) {
  /** Updates a single filter dimension, preserving the other dimensions. */
  function setFilter(dimension: keyof FilterState, next: string | undefined) {
    onChange({ ...value, [dimension]: next });
  }

  /** Whether any filter is active (used for the count label). */
  const hasActiveFilter = !!(value.status || value.sector || value.country);

  return (
    <div
      className="bg-page"
      style={{ borderBottom: "1px solid rgba(196,151,58,0.12)" }}
    >
      <div className="mx-auto max-w-site px-[80px] py-4">
        {/* Status row (no divider above the first row) */}
        <FilterRow label="Status" showDivider={false}>
          <FilterChip
            label="All"
            active={!value.status}
            onClick={() => setFilter("status", undefined)}
          />
          {filters.statuses.map((s) => (
            <FilterChip
              key={s}
              label={STATUS_LABELS[s] || s}
              active={value.status === s}
              onClick={() =>
                setFilter("status", value.status === s ? undefined : s)
              }
            />
          ))}

          {/* Spacer pushes count to the right */}
          <div className="flex-1" />

          {/* Project count */}
          <p
            className="font-sans text-[9px] font-light text-text-disabled"
            style={{ letterSpacing: "0.08em" }}
          >
            Showing {hasActiveFilter ? "filtered" : "all"} projects (
            {projectCount})
          </p>
        </FilterRow>

        {/* Sector row */}
        <FilterRow label="Sector">
          <FilterChip
            label="All"
            active={!value.sector}
            onClick={() => setFilter("sector", undefined)}
          />
          {filters.sectors.map((s) => (
            <FilterChip
              key={s}
              label={SECTOR_LABELS[s] || s}
              active={value.sector === s}
              onClick={() =>
                setFilter("sector", value.sector === s ? undefined : s)
              }
            />
          ))}
        </FilterRow>

        {/* Country row */}
        <FilterRow label="Country">
          <FilterChip
            label="All"
            active={!value.country}
            onClick={() => setFilter("country", undefined)}
          />
          {filters.countries.map((c) => (
            <FilterChip
              key={c}
              label={c}
              active={value.country === c}
              onClick={() =>
                setFilter("country", value.country === c ? undefined : c)
              }
            />
          ))}
        </FilterRow>
      </div>
    </div>
  );
}

interface FilterRowProps {
  /** Short label displayed at the left edge of the row (e.g. "Status"). */
  label: string;
  /** Whether to show a subtle gold divider above this row. False for the first row. */
  showDivider?: boolean;
  children: React.ReactNode;
}

/**
 * A single filter-dimension row: a gold section label on the left,
 * then the chip buttons laid out horizontally.
 *
 * Rows are separated by a subtle gold divider. The first row should
 * pass showDivider={false} to omit the top border.
 */
function FilterRow({ label, showDivider = true, children }: FilterRowProps) {
  return (
    <div
      className="flex items-center gap-1.5 py-2"
      style={showDivider ? { borderTop: "1px solid rgba(196,151,58,0.06)" } : undefined}
    >
      {/* Row label */}
      <span
        className="w-[60px] shrink-0 font-sans text-[8px] font-bold uppercase text-gold"
        style={{ letterSpacing: "0.2em" }}
      >
        {label}
      </span>

      {/* Chips */}
      {children}
    </div>
  );
}

interface FilterChipProps {
  label: string;
  active: boolean;
  onClick: () => void;
}

/**
 * Individual filter toggle chip following the Dark Prestige design.
 *
 * Active state: gold background (#C4973A) with page-dark text (#07111C).
 * Inactive state: transparent with gold-subtle border and muted text (#7A8FA8).
 * Sharp corners (no border-radius) per DESIGN_SYSTEM.md.
 */
function FilterChip({ label, active, onClick }: FilterChipProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="font-sans text-[9px] font-bold uppercase transition-colors"
      style={
        active
          ? {
              background: "#C4973A",
              padding: "8px 18px",
              letterSpacing: "0.14em",
              color: "#07111C",
            }
          : {
              background: "transparent",
              border: "1px solid rgba(196,151,58,0.2)",
              padding: "8px 18px",
              letterSpacing: "0.14em",
              color: "#7A8FA8",
            }
      }
    >
      {label}
    </button>
  );
}
