import type { Metadata } from "next";
import "./globals.css";

/**
 * Root layout for the ALEF site -- Dark Prestige direction.
 *
 * Loads Cormorant Garamond (serif headings), Montserrat (sans body/labels),
 * and Noto Serif Arabic (decorative Arabic accents) from Google Fonts, and
 * sets the shared html/body shell. Deliberately carries no nav/footer chrome
 * of its own -- that lives in `(marketing)/layout.tsx` so `/admin/**`, which
 * sits outside that route group, never inherits the public site's nav/footer
 * (design.md §B.1; F12 bug-fix pass, QA concern #6).
 */
export const metadata: Metadata = {
  title: {
    template: "%s | ALEF Architectural & Cadding Services",
    default: "ALEF Architectural & Cadding Services",
  },
  description:
    "GCC rebar detailing and structural drafting consultancy. Portfolio of airport, infrastructure, hospitality, and residential projects across the Gulf region.",
};

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
      <body className="min-h-screen bg-page font-sans">{children}</body>
    </html>
  );
}
