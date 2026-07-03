-- V7: Seed the 10 core leadership profiles (architecture §4.2, feature 011).
--
-- Source: ALEF Prequalification Document (round 2 correction; was 8 in round 1).
-- Primary email only per §3.1 (secondary personal addresses dropped for the public card).
-- photo is NULL until photo assets exist; TeamCard degrades to a serif monogram.
-- ON CONFLICT (name, role) DO NOTHING keeps this idempotent on long-lived local volumes
-- (DEC-012 idempotent-seed discipline).

INSERT INTO team (name, role, company, email, photo, display_order)
VALUES
    ('Ali Bin Beyat',      'Sponsor / Chairman',              'ALEF',          'alibeyat@alef-jasper.com',     NULL, 1),
    ('K. Jeyaraman',       'Managing Director',               'ALEF & JASPER', 'jeyaraman@alef-jasper.com',    NULL, 2),
    ('J. Sunitha',         'Executive Director',              'JASPER',         'sunitha@alef-jasper.com',      NULL, 3),
    ('M.D. Dinu',          'Operations Manager',              'ALEF',           'dinu@alef-jasper.com',         NULL, 4),
    ('Lakshmipathy Rao',   'Senior Technical Manager',        'ALEF',           'pathy.alef@gmail.com',         NULL, 5),
    ('Namasivayam',        'Administration Manager',          'JASPER',         'siva@alef-jasper.com',         NULL, 6),
    ('Tamilmani',          'Projects Manager',                'JASPER',         'tamil@alef-jasper.com',        NULL, 7),
    ('Syed Oli Masood',    'Project Manager',                 'JASPER',         'masood@alef-jasper.com',       NULL, 8),
    ('Sankar',             'Technical Manager',               'JASPER',         'sankar@alef-jasper.com',       NULL, 9),
    ('Varun Pillai',       'Business Development Manager',    'ALEF',           'varun.sales@alef-jasper.com',  NULL, 10)
ON CONFLICT (name, role) DO NOTHING;
