-- V5: Seed the editorial trust facts (architecture 6, feature 005).
-- Idempotent via ON CONFLICT so it never collides with F012-created rows
-- on a long-lived local volume. Demo content cleared by DEC-008.

-- Stats (item_type = 'stat')
INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('years_in_business', 'stat', 'Years in Business', '18+', NULL, 1)
ON CONFLICT (item_key) DO NOTHING;

INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('staff_count', 'stat', 'Detailing Engineers', '120+', NULL, 2)
ON CONFLICT (item_key) DO NOTHING;

INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('monthly_steel_capacity_tonnes', 'stat', 'Monthly Steel Capacity', '5,000', 'tonnes / month', 3)
ON CONFLICT (item_key) DO NOTHING;

-- Software (item_type = 'software')
INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('software_autocad', 'software', 'AutoCAD', NULL, NULL, 1)
ON CONFLICT (item_key) DO NOTHING;

INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('software_cads_rc', 'software', 'CADS RC', NULL, NULL, 2)
ON CONFLICT (item_key) DO NOTHING;

INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('software_steelpac_rc', 'software', 'SteelPac RC', NULL, NULL, 3)
ON CONFLICT (item_key) DO NOTHING;

-- Standards (item_type = 'standard')
INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('standard_bs_8666', 'standard', 'BS 8666', NULL, NULL, 1)
ON CONFLICT (item_key) DO NOTHING;

INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('standard_aci_318', 'standard', 'ACI 318', NULL, NULL, 2)
ON CONFLICT (item_key) DO NOTHING;

INSERT INTO trust_content (item_key, item_type, label, value, unit, display_order)
VALUES ('standard_bs_en_iso_3766', 'standard', 'BS EN ISO 3766', NULL, NULL, 3)
ON CONFLICT (item_key) DO NOTHING;
