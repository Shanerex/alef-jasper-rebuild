import { ContactMap } from "@/components/contact/contact-map";
import type { Office } from "@/lib/types/office";

/**
 * Office location card (design §1.5, F11-AC7).
 *
 * Surface 2, 2px gold top border — same structure as ProjectCard and TeamCard.
 * Multi-line address rendered by mapping over addressLines array.
 * Phones rendered as tel: links (Dubai has two; India has one).
 * Email rendered as mailto: link when non-null.
 * Map rendered by ContactMap (keyless iframe, decorative — card is complete without it).
 */

interface OfficeCardProps {
  office: Office;
}

export function OfficeCard({ office }: OfficeCardProps) {
  const { name, addressLines, phones, email, mapQuery } = office;

  return (
    <div
      className="bg-surface-2"
      style={{ borderTop: "2px solid #C4973A" }}
    >
      <div className="p-6">
        {/* Office name */}
        <h3 className="mb-4 font-serif text-[26px] font-normal text-text-primary">
          {name}
        </h3>

        {/* Address lines */}
        <div className="mb-4">
          {addressLines.map((line) => (
            <p
              key={line}
              className="font-sans text-[13px] font-normal leading-[1.7] text-text-muted"
            >
              {line}
            </p>
          ))}
        </div>

        {/* Divider */}
        <div
          className="my-4 h-[1px]"
          style={{ backgroundColor: "rgba(196,151,58,0.15)" }}
        />

        {/* Phones */}
        {phones.length > 0 && (
          <div className="mb-3">
            <p
              className="mb-1 font-sans text-[8.5px] font-bold uppercase text-gold"
              style={{ letterSpacing: "0.2em" }}
            >
              Phone
            </p>
            {phones.map((phone) => (
              <a
                key={phone}
                href={`tel:${phone.replace(/\s/g, "")}`}
                className="block font-sans text-[13px] font-normal text-text-secondary transition-colors hover:text-gold"
              >
                {phone}
              </a>
            ))}
          </div>
        )}

        {/* Email */}
        {email && (
          <div className="mb-6">
            <p
              className="mb-1 font-sans text-[8.5px] font-bold uppercase text-gold"
              style={{ letterSpacing: "0.2em" }}
            >
              Email
            </p>
            <a
              href={`mailto:${email}`}
              className="font-sans text-[13px] font-normal text-text-secondary transition-colors hover:text-gold"
            >
              {email}
            </a>
          </div>
        )}

        {/* Map (decorative; card is complete without it) */}
        <ContactMap mapQuery={mapQuery} name={name} />
      </div>
    </div>
  );
}
