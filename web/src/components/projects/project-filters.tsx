"use client";

import type { ProjectFilters as ProjectFiltersType, FilterState } from "@/lib/types/project";
import { SECTOR_LABELS, STATUS_LABELS } from "@/lib/types/project";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface ProjectFiltersProps {
  filters: ProjectFiltersType;
  value: FilterState;
  onChange: (next: FilterState) => void;
  /** Total number of visible projects, displayed as "Showing N projects". */
  projectCount: number;
}

/**
 * Uniform dropdown filter bar for the portfolio page.
 *
 * All three filter dimensions (status, sector, country) use the same
 * select dropdown pattern for visual consistency. Dropdowns scale
 * gracefully as options grow — country is data-derived and unbounded.
 *
 * All three dimensions are AND-combined by the parent's filtering logic.
 * URL query params are the source of truth (managed by ProjectBrowser).
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

  const hasActiveFilter = !!(value.status || value.sector || value.country);

  return (
    <div
      className="bg-page"
      style={{ borderBottom: "1px solid rgba(196,151,58,0.12)" }}
    >
      <div className="mx-auto max-w-site px-[80px] py-4">
        <div className="flex items-center gap-4">
          {/* Status dropdown */}
          <FilterSelect
            label="Status"
            value={value.status}
            options={filters.statuses.map((s) => ({
              value: s,
              label: STATUS_LABELS[s] || s,
            }))}
            onChange={(v) => setFilter("status", v)}
          />

          {/* Sector dropdown */}
          <FilterSelect
            label="Sector"
            value={value.sector}
            options={filters.sectors.map((s) => ({
              value: s,
              label: SECTOR_LABELS[s] || s,
            }))}
            onChange={(v) => setFilter("sector", v)}
          />

          {/* Country dropdown */}
          <FilterSelect
            label="Country"
            value={value.country}
            options={filters.countries.map((c) => ({
              value: c,
              label: c,
            }))}
            onChange={(v) => setFilter("country", v)}
          />

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
        </div>
      </div>
    </div>
  );
}

interface FilterSelectProps {
  /** Placeholder label shown when no value is selected (e.g. "Sector"). */
  label: string;
  value: string | undefined;
  options: { value: string; label: string }[];
  onChange: (value: string | undefined) => void;
}

/**
 * Dark Prestige select dropdown for high-cardinality filter dimensions.
 *
 * Scales gracefully as options grow (unlike chip rows). Uses the shadcn
 * Select primitive already styled for dark surfaces and gold accents.
 * Selecting "all" clears the filter.
 */
function FilterSelect({ label, value, options, onChange }: FilterSelectProps) {
  return (
    <Select
      value={value ?? "__all__"}
      onValueChange={(v) => onChange(v === "__all__" ? undefined : v)}
    >
      <SelectTrigger className="h-auto w-auto gap-2 bg-transparent px-4 py-2 font-sans text-[9px] font-bold uppercase"
        style={{
          letterSpacing: "0.14em",
          border: value
            ? "1px solid #C4973A"
            : "1px solid rgba(196,151,58,0.2)",
          color: value ? "#C4973A" : "#7A8FA8",
        }}
      >
        <SelectValue placeholder={label}>
          {value
            ? options.find((o) => o.value === value)?.label ?? value
            : label}
        </SelectValue>
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="__all__" className="font-sans text-[11px]">
          All {label}s
        </SelectItem>
        {options.map((opt) => (
          <SelectItem
            key={opt.value}
            value={opt.value}
            className="font-sans text-[11px]"
          >
            {opt.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

