import type { ReactNode } from "react";

/** One column definition: a header label and a per-row render function. */
export interface ListColumn<T> {
  header: string;
  render: (row: T) => ReactNode;
  /** Card view: hide this column's label (e.g. the primary field, used as the card heading). */
  primary?: boolean;
}

/**
 * Responsive record list (design.md §B.3, §B.10, F12-AC27).
 *
 * At `md` and up: a dense table. Below `md`: each row becomes a stacked card
 * (heading = the `primary` column, remaining columns as labelled lines,
 * actions as full-width buttons at the card foot). Same data, same columns
 * config, two renderings -- no separate API call or DTO shape (architecture
 * §2.7: "a presentation switch").
 */
export function ResponsiveList<T>({
  columns,
  rows,
  keyFn,
  actions,
  emptyMessage,
}: {
  columns: ListColumn<T>[];
  rows: T[];
  keyFn: (row: T) => string | number;
  actions: (row: T) => ReactNode;
  emptyMessage: string;
}) {
  if (rows.length === 0) {
    return (
      <div className="border border-gold-subtle bg-surface-2 px-6 py-10 text-center">
        <p className="font-sans text-[13px] font-light text-text-muted">{emptyMessage}</p>
      </div>
    );
  }

  const primaryColumn = columns.find((c) => c.primary) ?? columns[0];
  const secondaryColumns = columns.filter((c) => c !== primaryColumn);

  return (
    <>
      {/* Desktop table (md and up) */}
      <table className="hidden w-full border-collapse md:table">
        <thead>
          <tr className="border-b border-gold-subtle text-left">
            {columns.map((col) => (
              <th
                key={col.header}
                className="px-4 py-3 font-sans text-[10px] font-bold uppercase tracking-[0.12em] text-gold"
              >
                {col.header}
              </th>
            ))}
            <th className="px-4 py-3 text-right font-sans text-[10px] font-bold uppercase tracking-[0.12em] text-gold">
              Actions
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={keyFn(row)} className="border-b border-gold-subtle/40">
              {columns.map((col) => (
                <td key={col.header} className="px-4 py-3 font-sans text-[13px] font-light text-text-primary">
                  {col.render(row)}
                </td>
              ))}
              <td className="px-4 py-3 text-right">
                <div className="flex justify-end gap-3">{actions(row)}</div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Mobile stacked cards (below md) */}
      <div className="flex flex-col gap-3 md:hidden">
        {rows.map((row) => (
          <div key={keyFn(row)} className="border border-gold-subtle bg-surface-2 p-4">
            <div className="mb-2 font-serif text-[18px] font-normal text-text-primary">
              {primaryColumn.render(row)}
            </div>
            <dl className="mb-3 flex flex-col gap-1">
              {secondaryColumns.map((col) => (
                <div key={col.header} className="flex justify-between gap-3">
                  <dt className="font-sans text-[10px] font-bold uppercase tracking-[0.1em] text-text-muted">
                    {col.header}
                  </dt>
                  <dd className="font-sans text-[12px] font-light text-text-primary">{col.render(row)}</dd>
                </div>
              ))}
            </dl>
            <div className="flex flex-wrap gap-3 border-t border-gold-subtle pt-3">{actions(row)}</div>
          </div>
        ))}
      </div>
    </>
  );
}
