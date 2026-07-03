/**
 * Keyless Google Maps iframe for an office card (architecture §10 Decision 3, design §1.5).
 *
 * No API key, no paid SDK. The mapQuery string is passed as the q= parameter
 * to the Google Maps embed URL. If mapQuery is null, renders nothing — the
 * surrounding OfficeCard still shows the full address and phone, satisfying
 * F11-AC7 through the address text alone (the map is decorative, not structural).
 *
 * Gotcha: the iframe may fail to load on a cold local demo with no internet
 * access. This is intentional — the card is complete without it.
 */

interface ContactMapProps {
  /** Plaintext location string fed to the keyless embed q= parameter. Nullable. */
  mapQuery: string | null;
  /** Office name for the iframe title (accessibility). */
  name: string;
}

export function ContactMap({ mapQuery, name }: ContactMapProps) {
  if (!mapQuery) return null;

  const src = `https://maps.google.com/maps?q=${encodeURIComponent(mapQuery)}&output=embed`;

  return (
    <iframe
      src={src}
      title={`Map: ${name}`}
      loading="lazy"
      width="100%"
      height="220"
      style={{
        border: "1px solid rgba(196,151,58,0.2)",
        display: "block",
      }}
      aria-label={`Map showing the location of ${name}`}
    />
  );
}
