import { getTrustOverview } from "@/lib/api/trust";
import { TrustLayer } from "@/components/trust/trust-layer";

/**
 * Drop-in async server wrapper: fetches the composed trust payload and renders
 * the Trust Layer. Lets F011 mount the section with zero data wiring. If F011
 * prefers a single page-level fetch, use <TrustLayer data={...}/> directly and
 * skip this wrapper. Fails soft -> renders nothing if the API is unreachable,
 * so the Home page never breaks on a trust fetch error.
 */
export async function TrustLayerSection() {
  try {
    const data = await getTrustOverview();
    return <TrustLayer data={data} />;
  } catch {
    return null;
  }
}
