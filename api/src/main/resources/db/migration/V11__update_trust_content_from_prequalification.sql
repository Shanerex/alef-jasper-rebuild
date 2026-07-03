-- V11: Correct and expand trust_content from the ALEF Prequalification Document.
-- Supersedes the stale V5 values without editing V5 (Flyway checksums applied migrations).
-- Stats corrected: years (18+ → 23), workstations (120+ → 250+), steel capacity (5,000 → 70,000 t/mo).
-- Stats added: projects completed (102+), major clients (40+), GCC countries (6).
-- Software corrected: "SteelPac RC" → "Steel PAC RCS"; added RCD, CADMATE, LOHA, Multi-Rc (3 → 7 tools).
-- Demo content cleared by DEC-008; see feature 011 architecture §9 for the full rationale.

-- ── Correct stale stats ────────────────────────────────────────────────────

UPDATE trust_content
SET    value      = '23',
       updated_at = now()
WHERE  item_key   = 'years_in_business';

UPDATE trust_content
SET    label      = 'Workstations',
       value      = '250+',
       updated_at = now()
WHERE  item_key   = 'staff_count';

UPDATE trust_content
SET    value      = '70,000',
       updated_at = now()
WHERE  item_key   = 'monthly_steel_capacity_tonnes';

-- ── Add new stats ──────────────────────────────────────────────────────────
-- When the project catalogue is fully seeded (CLAUDE.md TODO: ~38 total, 15 seeded),
-- these headline counts should be derived live from the project table rather than
-- kept as static rows. Until then, the prequalification figures are the source of truth.

INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('projects_completed', 'stat', 'Projects Completed', '102+', 'across GCC', 4)
ON CONFLICT (item_key) DO NOTHING;

INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('contractor_clients', 'stat', 'Major Clients', '40+', NULL, 5)
ON CONFLICT (item_key) DO NOTHING;

INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('gcc_countries', 'stat', 'GCC Countries Served', '6', NULL, 6)
ON CONFLICT (item_key) DO NOTHING;

-- ── Correct and expand software list (3 → 7) ─────────────────────────────
-- The prequalification doc distinguishes Steel PAC RCD (drawings) and RCS (schedules).
-- Update the existing generic label; the item_key is stable and not used by the API.

UPDATE trust_content
SET    label      = 'Steel PAC RCS',
       display_order = 3,
       updated_at = now()
WHERE  item_key   = 'software_steelpac_rc';

INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('software_steelpac_rcd', 'software', 'Steel PAC RCD', NULL, NULL, 4)
ON CONFLICT (item_key) DO NOTHING;

INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('software_cadmate', 'software', 'CADMATE', NULL, NULL, 5)
ON CONFLICT (item_key) DO NOTHING;

INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('software_loha', 'software', 'LOHA', NULL, NULL, 6)
ON CONFLICT (item_key) DO NOTHING;

INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('software_multi_rc', 'software', 'Multi-Rc', NULL, NULL, 7)
ON CONFLICT (item_key) DO NOTHING;
