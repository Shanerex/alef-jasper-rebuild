# Handoff: Core Marketing Pages

| Field | Value |
|---|---|
| Feature # | 011 |
| From / To | Business -> PM |
| Status | ready |
| Spec | specs/011-core-marketing-pages/requirements.md |

## Context
Everything else in this rebuild (the concierge, the portfolio, the trust layer) sits inside these four pages. They are the baseline credibility layer: a 2014-looking shell undermines even a great concierge sitting on top of it. This is content migration and layout, not a new capability, which is why risk is low and it is bundled as one feature.

## What was done
- Identified the four in-scope pages from the live site's actual structure and content (company history, 8 leadership profiles, the specializations list, the contact form/map).
- Flagged the known content defects to fix during migration: three repetitive hero banners, stale 2014 copyright, content typos ("Intenational", "Concorse", "Cadding"), and a contact form that looks non-functional.

## What the receiving role must do
1. Confirm acceptance criteria are testable (done, see requirements.md).
2. Hand to Architect: these pages should be the first content built, since 003 (portfolio) and 005 (trust layer) feed data into Home, and the concierge (001) needs an entry point placed here.

## Read first
- docs/knowledge/ARCHITECTURE.md (shared nav/layout constraints, content migration notes)
- specs/003-portfolio-filtering/requirements.md (Home pulls featured projects from here)
- specs/005-trust-layer/requirements.md (Home pulls stats from here)

## Do not touch
- Do not duplicate project or stats data on Home; reference the same source of truth as 003 and 005.
- Do not invent new specializations; reframe presentation only.
