import Link from "next/link";

/** Section title + optional primary "New" action (design.md §B.3 header row). */
export function AdminPageHeader({
  title,
  newHref,
  newLabel,
}: {
  title: string;
  newHref?: string;
  newLabel?: string;
}) {
  return (
    <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
      <h1 className="font-serif text-[28px] font-normal text-text-primary">{title}</h1>
      {newHref && (
        <Link
          href={newHref}
          className="bg-gold px-5 py-2.5 font-sans text-[10px] font-bold uppercase tracking-[0.14em] text-page hover:bg-gold-light"
        >
          + {newLabel ?? "New"}
        </Link>
      )}
    </div>
  );
}

/** Top-of-form error summary banner (design.md §B.7). */
export function FormErrorBanner({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <div role="alert" className="mb-5 border-l-2 border-gold bg-gold-ghost px-4 py-3">
      <p className="font-sans text-[12px] font-normal text-text-primary">{message}</p>
    </div>
  );
}
