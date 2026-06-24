import type { Config } from "tailwindcss";

/**
 * Tailwind configuration for the ALEF site -- Dark Prestige direction.
 *
 * Palette follows DESIGN_SYSTEM.md: deep navy page background (#07111C),
 * layered dark surfaces, gold accent (#C4973A), warm cream text (#E8E0D0).
 * Fonts: Cormorant Garamond (serif headings), Montserrat (sans body/labels),
 * Noto Serif Arabic (decorative Arabic accents). Sharp corners everywhere.
 */
const config: Config = {
  content: [
    "./src/**/*.{ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        /* Background & Surfaces */
        page: "#07111C",
        surface: {
          1: "#0C1B2E",
          2: "#0E2035",
          3: "#101F32",
          4: "#122339",
        },
        footer: "#040C14",

        /* Gold Accent Palette */
        gold: {
          DEFAULT: "#C4973A",
          light: "#D4A94A",
          dark: "#A8812E",
          subtle: "rgba(196,151,58,0.2)",
          ghost: "rgba(196,151,58,0.08)",
        },

        /* Text Colors */
        "text-primary": "#E8E0D0",
        "text-secondary": "#C8C0B0",
        "text-muted": "#7A8FA8",
        "text-disabled": "#4A5B6E",
        slate: "#5A6B80",
      },
      fontFamily: {
        serif: ["'Cormorant Garamond'", "serif"],
        sans: ["'Montserrat'", "system-ui", "sans-serif"],
        arabic: ["'Noto Serif Arabic'", "serif"],
      },
      /* Sharp corners everywhere -- no border-radius */
      borderRadius: {
        none: "0",
        sm: "0",
        DEFAULT: "0",
        md: "0",
        lg: "0",
        xl: "0",
        "2xl": "0",
        "3xl": "0",
        full: "9999px",
      },
      maxWidth: {
        site: "1280px",
      },
      spacing: {
        /* Design system spacing scale aliases */
        18: "4.5rem",   /* 72px */
        22: "5.5rem",   /* 88px */
        "site-pad": "80px",
      },
    },
  },
  plugins: [],
};

export default config;
