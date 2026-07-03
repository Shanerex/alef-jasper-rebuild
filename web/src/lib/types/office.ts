/**
 * TypeScript mirrors of the GET /api/offices DTO (architecture §3.2, feature 011).
 *
 * addressLines and phones are arrays to match the database columns: Dubai has
 * two phone numbers; both offices have multi-line addresses.
 * email and mapQuery are nullable; the card omits the email link and skips
 * the map iframe when these are absent (design §1.5 graceful-degradation).
 */

/** One office location from the offices endpoint. */
export interface Office {
  /** Stable machine key: "dubai" | "india". */
  key: string;
  name: string;
  /** Ordered address presentation lines — map over these to render multi-line address. */
  addressLines: string[];
  /** One or more phone numbers (Dubai has two). */
  phones: string[];
  email: string | null;
  /** Plaintext location string for the keyless Google Maps embed q= parameter. Null omits the map. */
  mapQuery: string | null;
}

/** Envelope returned by GET /api/offices. */
export interface OfficesResponse {
  offices: Office[];
}
