package com.alef.api.lead.vocabulary;

/**
 * Stable string constants for the lead.source column (architecture §4.4, feature 011).
 *
 * source is stored as TEXT (not an enum) per DEC-009 so values added by future
 * features (e.g. 'concierge' from feature 001) read back from the database
 * without a redeploy. These constants provide compile-time safety for the values
 * this feature writes without closing the set to future extension.
 */
public final class LeadSource {

    /** Source value written by the contact form (POST /api/leads, feature 011). */
    public static final String CONTACT_FORM = "contact_form";

    private LeadSource() {
        // Utility class; not instantiable.
    }
}
