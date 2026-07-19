"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { listAdminProjects } from "@/lib/api/admin-projects";
import { listAdminTeam } from "@/lib/api/admin-team";
import { getAdminTrustOverview } from "@/lib/api/admin-trust";
import { listAdminSamples } from "@/lib/api/admin-samples";

/** Dashboard tile: a resource name, its live count, and a link to the list. */
interface Tile {
  label: string;
  href: string;
  count: number | null;
}

/**
 * Admin dashboard (design.md §B.1 "Dashboard -- links + counts").
 * Fetches each resource's current count so staff land on a useful overview,
 * not an empty shell.
 */
export default function AdminDashboardPage() {
  const [tiles, setTiles] = useState<Tile[]>([
    { label: "Projects", href: "/admin/projects", count: null },
    { label: "Team", href: "/admin/team", count: null },
    { label: "Trust Content", href: "/admin/trust", count: null },
    { label: "Samples", href: "/admin/samples", count: null },
  ]);

  useEffect(() => {
    listAdminProjects(0, 1)
      .then((r) => setTile("Projects", r.totalElements))
      .catch(() => setTile("Projects", 0));
    listAdminTeam()
      .then((r) => setTile("Team", r.length))
      .catch(() => setTile("Team", 0));
    getAdminTrustOverview()
      .then((r) => setTile("Trust Content", r.stats.length + r.software.length + r.standards.length))
      .catch(() => setTile("Trust Content", 0));
    listAdminSamples()
      .then((r) => setTile("Samples", r.length))
      .catch(() => setTile("Samples", 0));
    // eslint-disable-next-line react-hooks/exhaustive-deps -- run once on mount
  }, []);

  function setTile(label: string, count: number) {
    setTiles((prev) => prev.map((t) => (t.label === label ? { ...t, count } : t)));
  }

  return (
    <div>
      <h1 className="mb-6 font-serif text-[28px] font-normal text-text-primary">Dashboard</h1>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {tiles.map((tile) => (
          <Link
            key={tile.label}
            href={tile.href}
            className="border border-gold-subtle bg-surface-2 p-6 hover:border-gold"
          >
            <p className="mb-2 font-sans text-[10px] font-bold uppercase tracking-[0.14em] text-gold">
              {tile.label}
            </p>
            <p className="font-serif text-[36px] font-light text-text-primary">
              {tile.count === null ? "—" : tile.count}
            </p>
          </Link>
        ))}
      </div>
    </div>
  );
}
