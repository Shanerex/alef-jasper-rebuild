import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

/**
 * Root layout for the ALEF site -- Dark Prestige direction.
 *
 * Loads Cormorant Garamond (serif headings), Montserrat (sans body/labels),
 * and Noto Serif Arabic (decorative Arabic accents) from Google Fonts.
 * Provides the sticky navigation and footer per DESIGN_SYSTEM.md:
 * deep navy background, gold accents, 72px nav height, 1280px max-width.
 */
export const metadata: Metadata = {
  title: {
    template: "%s | ALEF Architectural & Cadding Services",
    default: "ALEF Architectural & Cadding Services",
  },
  description:
    "GCC rebar detailing and structural drafting consultancy. Portfolio of airport, infrastructure, hospitality, and residential projects across the Gulf region.",
};

/** Navigation link items for the main nav bar.
 *  About / Services / Contact are wired to their 011 routes.
 *  Samples stays # until feature 004 ships.
 *  "Enquire Now" CTA points to /contact until 001 rewires it to the concierge. */
const NAV_LINKS = [
  { href: "/about",    label: "About" },
  { href: "/services", label: "Services" },
  { href: "/projects", label: "Projects" },
  { href: "#",         label: "Samples" },
  { href: "/contact",  label: "Contact" },
] as const;

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link
          rel="preconnect"
          href="https://fonts.gstatic.com"
          crossOrigin="anonymous"
        />
        <link
          href="https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,300;0,400;0,500;0,600;1,300;1,400&family=Montserrat:wght@300;400;500;600;700&family=Noto+Serif+Arabic:wght@300;400;500;600&display=swap"
          rel="stylesheet"
        />
      </head>
      <body className="min-h-screen bg-page font-sans">
        {/* Navigation */}
        <SiteNav />

        {/* Main content */}
        <main>{children}</main>

        {/* Footer */}
        <SiteFooter />
      </body>
    </html>
  );
}

/**
 * Sticky site navigation matching the Dark Prestige design reference.
 *
 * Height 72px, page-bg background, gold-subtle bottom border.
 * Logo: "ALEF" in Cormorant Garamond gold with Arabic "alif" beside it.
 * Nav links: Montserrat uppercase muted, CTA gold button on the right.
 */
function SiteNav() {
  return (
    <nav
      className="sticky top-0 z-50 bg-page"
      style={{ borderBottom: "1px solid rgba(196,151,58,0.22)" }}
    >
      <div className="mx-auto flex h-[72px] max-w-site items-center justify-between px-[80px]">
        {/* Logo block */}
        <Link href="/" className="flex flex-col">
          <div className="flex items-baseline gap-2.5">
            <span
              className="font-serif text-[26px] font-semibold text-gold"
              style={{ letterSpacing: "0.08em" }}
            >
              ALEF
            </span>
            <span
              className="font-arabic text-[16px]"
              style={{ color: "rgba(196,151,58,0.4)" }}
            >
              &#1571;&#1604;&#1601;
            </span>
          </div>
          <span
            className="font-sans text-[7.5px] font-light uppercase text-text-disabled"
            style={{ letterSpacing: "0.14em", marginTop: "1px" }}
          >
            Architectural &amp; Cadding Services
          </span>
        </Link>

        {/* Nav links + CTA */}
        <div className="flex items-center gap-9">
          {NAV_LINKS.map((link) => (
            <Link
              key={link.label}
              href={link.href}
              className="font-sans text-[9.5px] font-medium uppercase text-text-muted transition-colors hover:text-text-primary"
              style={{ letterSpacing: "0.12em" }}
            >
              {link.label}
            </Link>
          ))}
          {/* "Enquire Now" → /contact until 001 wires the concierge (F11-AC8) */}
          <Link
            href="/contact"
            className="inline-block bg-gold px-[26px] py-[11px] font-sans text-[9px] font-bold uppercase text-page transition-colors hover:bg-gold-light"
            style={{ letterSpacing: "0.14em" }}
          >
            Enquire Now
          </Link>
        </div>
      </div>
    </nav>
  );
}

/**
 * Site footer matching the Dark Prestige design reference.
 *
 * Minimal footer with ALEF logo in reduced gold and copyright line.
 * Background #040C14 with gold-subtle top border.
 */
function SiteFooter() {
  return (
    <footer
      className="bg-footer"
      style={{ borderTop: "1px solid rgba(196,151,58,0.15)" }}
    >
      <div className="mx-auto flex max-w-site items-center justify-between px-[80px] py-10">
        <span
          className="font-serif text-[20px] font-semibold"
          style={{ letterSpacing: "0.08em", color: "rgba(196,151,58,0.4)" }}
        >
          ALEF
        </span>
        {/* Dynamic year (F11-AC9) — never goes stale. "ALEF" and "Cadding" are correct. */}
        <p className="font-sans text-[10px] font-light" style={{ color: "#2A3B4C" }}>
          &copy; {new Date().getFullYear()} ALEF Architectural &amp; Cadding Services LLC &middot; Dubai, UAE
        </p>
      </div>
    </footer>
  );
}
