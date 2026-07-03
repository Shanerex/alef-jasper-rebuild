/**
 * TypeScript types for the POST /api/leads contact form path (architecture §3.4, feature 011).
 */

/**
 * Payload sent to POST /api/leads from the ContactForm.
 *
 * source and created_at are never sent from the client — LeadService sets them
 * server-side. website is the honeypot field; a real human leaves it blank.
 */
export interface LeadPayload {
  name: string;
  email: string;
  phone: string;
  company: string;
  message: string;
  /** Honeypot field. Always empty for real users; bots may fill it. */
  website: string;
}

/**
 * Confirmation returned by POST /api/leads on 201 Created.
 *
 * id is the database surrogate key (informational; not shown to the user).
 * status is always "received" — the frontend uses it to drive the success panel.
 * When the honeypot is triggered, the API still returns 201 with id=-1 so bots
 * cannot detect the trap by inspecting the response.
 */
export interface LeadResponse {
  id: number;
  status: string;
}
