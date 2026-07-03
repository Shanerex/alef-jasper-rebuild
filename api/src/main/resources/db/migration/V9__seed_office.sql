-- V9: Seed the two office locations (architecture §3.2, feature 011).
--
-- Dubai: head office with two phone lines (Operations + HO per prequalification doc).
-- India: Jasper delivery centre, single line.
-- ON CONFLICT (office_key) DO NOTHING keeps this idempotent (DEC-012).

INSERT INTO office (office_key, name, address_lines, phones, email, map_query, display_order)
VALUES (
    'dubai',
    'ALEF — Dubai (Head Office)',
    ARRAY['404 Sheikh Maktoum Building', 'Damascus Street, Al Qusais', 'PO Box 65825, Dubai, UAE'],
    ARRAY['+971 4 2513840', '+971 4 3434440'],
    'alefllc@eim.ae',
    'Sheikh Maktoum Building, Damascus Street, Al Qusais, Dubai',
    0
)
ON CONFLICT (office_key) DO NOTHING;

INSERT INTO office (office_key, name, address_lines, phones, email, map_query, display_order)
VALUES (
    'india',
    'Jasper — India (Delivery Centre)',
    ARRAY['Plot No. 2/286, Near Dharani Sugars', 'Vasudevanallur, Tirunelveli Dist.', 'Tamil Nadu 627 758, India'],
    ARRAY['0091 4636 293166'],
    'jasperiec@gmail.com',
    'Vasudevanallur, Tirunelveli District, Tamil Nadu 627758',
    1
)
ON CONFLICT (office_key) DO NOTHING;
