-- V2: Idempotent demo seed for the project portfolio (DEC-012).
-- Uses ON CONFLICT (slug) DO NOTHING so admin-created rows are never overwritten.
-- 15 representative GCC construction projects across sectors, countries, and statuses.
-- created_at and updated_at take their DB defaults and are omitted from the INSERT.

INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable)
VALUES (
    'doha-metro-gold-line',
    'Doha Metro - Gold Line',
    'infrastructure_rail',
    'Qatar',
    'completed',
    '/img/projects/doha-metro-gold-line.jpg',
    'Rebar detailing and bar bending schedule preparation for multiple elevated and underground stations on the Gold Line of the Doha Metro. Scope included complex post-tensioned elements, deep foundation cages, and precast segment connections.',
    'Qatar Rail',
    'Qatar Railways Company',
    'AECOM',
    'Doha, Qatar',
    ARRAY['rebar', 'BBS', 'GA'],
    TRUE
) ON CONFLICT (slug) DO NOTHING;

INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable)
VALUES (
    'abu-dhabi-international-airport-midfield',
    'Abu Dhabi International Airport - Midfield Terminal',
    'airport',
    'UAE',
    'completed',
    '/img/projects/abu-dhabi-airport-midfield.jpg',
    'Full rebar detailing package for the iconic Midfield Terminal Building at Abu Dhabi International Airport. Over 180,000 tonnes of reinforcement steel across the terminal superstructure, foundations, and airside facilities.',
    'Arabtec-TAV JV',
    'Abu Dhabi Airports Company',
    'KPF / Arup',
    'Abu Dhabi, UAE',
    ARRAY['rebar', 'BBS', 'GA', 'QS'],
    TRUE
) ON CONFLICT (slug) DO NOTHING;

INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable)
VALUES (
    'riyadh-metro-line-3',
    'Riyadh Metro - Line 3 (Orange Line)',
    'infrastructure_rail',
    'Saudi Arabia',
    'completed',
    '/img/projects/riyadh-metro-line-3.jpg',
    'Rebar detailing for elevated viaduct sections and underground stations on Line 3 of the Riyadh Metro project. Coordinated with precast yard for segment reinforcement layouts.',
    'Salini Impregilo - Samsung JV',
    'Arriyadh Development Authority',
    'Bechtel',
    'Riyadh, Saudi Arabia',
    ARRAY['rebar', 'BBS'],
    TRUE
) ON CONFLICT (slug) DO NOTHING;

INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable)
VALUES (
    'mall-of-qatar',
    'Mall of Qatar',
    'mall_retail',
    'Qatar',
    'completed',
    '/img/projects/mall-of-qatar.jpg',
    'Complete rebar detailing and BBS for the Mall of Qatar, a 500,000 sqm retail destination featuring a hypermarket, entertainment zone, and multi-level parking structures. Included complex transfer beams and post-tensioned slabs.',
    'UrbaCon Trading & Contracting',
    'Mall of Qatar WLL',
    'Chapman Taylor',
    'Doha, Qatar',
    ARRAY['rebar', 'BBS', 'GA'],
    TRUE
) ON CONFLICT (slug) DO NOTHING;

INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable)
VALUES (
    'jw-marriott-muscat',
    'JW Marriott Hotel Muscat',
    'hotel_hospitality',
    'Oman',
    'completed',
    '/img/projects/jw-marriott-muscat.jpg',
    'Rebar detailing for the 304-room JW Marriott luxury hotel in Muscat. Scope covered raft foundations, basement retaining walls, and the tower superstructure including a rooftop pool deck.',
    'Aluminum Insulation & Contracting Co.',
    'Marriott International',
    'KEO International Consultants',
    'Muscat, Oman',
    ARRAY['rebar', 'BBS'],
    FALSE
) ON CONFLICT (slug) DO NOTHING;

INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable)
VALUES (
    'bahrain-international-airport-modernisation',
    'Bahrain International Airport - Modernisation',
    'airport',
    'Bahrain',
    'completed',
    '/img/projects/bahrain-airport-modernisation.jpg',
    'Rebar detailing and general arrangement drawings for the new passenger terminal and associated structures at Bahrain International Airport. Included large-span roof trusses and complex curtain wall support steelwork.',
    'TAV Construction',
    'Ministry of Transportation and Telecommunications',
    'Dar Al-Handasah',
    'Muharraq, Bahrain',
    ARRAY['rebar', 'BBS', 'GA'],
    TRUE
) ON CONFLICT (slug) DO NOTHING;

INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable)
VALUES (
    'al-maryah-island-towers',
    'Al Maryah Island Mixed-Use Towers',
    'residential',
    'UAE',
    'completed',
    '/img/projects/al-maryah-island-towers.jpg',
    'Rebar detailing for twin 45-storey residential towers on Al Maryah Island, Abu Dhabi. Scope included deep piled foundations, transfer plate structures, and core wall reinforcement for wind resistance.',
    'Al Futtaim Carillion',
    'Mubadala Investment Company',
    'Atkins',
    'Abu Dhabi, UAE',
    ARRAY['rebar', 'BBS', 'QS'],
    FALSE
) ON CONFLICT (slug) DO NOTHING;

INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable)
VALUES (
    'national-museum-of-qatar',
    'National Museum of Qatar',
    'leisure_museum',
    'Qatar',
    'completed',
    '/img/projects/national-museum-qatar.jpg',
    'Rebar detailing support for the National Museum of Qatar designed by Jean Nouvel. Complex interlocking disc geometry required bespoke reinforcement layouts and extensive 3D coordination with the structural steel frame.',
    'Hyundai Engineering & Construction',
    'Qatar Museums Authority',
    'Jean Nouvel Ateliers',
    'Doha, Qatar',
    ARRAY['rebar', 'BBS', 'GA'],
    TRUE
) ON CONFLICT (slug) DO NOTHING;

INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable)
VALUES (
    'king-abdullah-financial-district-tower',
    'King Abdullah Financial District - Office Tower',
    'residential',
    'Saudi Arabia',
    'completed',
    '/img/projects/kafd-office-tower.jpg',
    'Rebar detailing for a 40-storey office tower within the King Abdullah Financial District. High-strength reinforcement in the core walls and outrigger system required close coordination with the structural engineer.',
    'Saudi Binladin Group',
    'Rayadah Investment Company',
    'Henning Larsen Architects',
    'Riyadh, Saudi Arabia',
    ARRAY['rebar', 'BBS'],
    FALSE
) ON CONFLICT (slug) DO NOTHING;

INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable)
VALUES (
    'muscat-grand-mall-expansion',
    'Muscat Grand Mall Expansion',
    'mall_retail',
    'Oman',
    'completed',
    '/img/projects/muscat-grand-mall.jpg',
    'Rebar detailing and as-built drawings for the expansion wing of Muscat Grand Mall. Included a new multi-storey car park, retail podium, and connection bridge to the existing structure.',
    'Galfar Engineering & Contracting',
    'Muscat Grand Mall LLC',
    'Arup',
    'Muscat, Oman',
    ARRAY['rebar', 'BBS', 'as-built'],
    FALSE
) ON CONFLICT (slug) DO NOTHING;

INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable)
VALUES (
    'lusail-light-rail-transit',
    'Lusail Light Rail Transit',
    'infrastructure_rail',
    'Qatar',
    'completed',
    '/img/projects/lusail-lrt.jpg',
    'Rebar detailing for elevated guideway sections and four underground stations of the Lusail LRT system. Work included precast segmental viaduct reinforcement and cast-in-place station boxes.',
    'QDVC (Qatari Diar Vinci Construction)',
    'Qatar Rail',
    'Egis Rail',
    'Lusail, Qatar',
    ARRAY['rebar', 'BBS', 'GA'],
    TRUE
) ON CONFLICT (slug) DO NOTHING;

INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable)
VALUES (
    'dubai-creek-harbour-residential',
    'Dubai Creek Harbour - Residential Towers',
    'residential',
    'UAE',
    'ongoing',
    '/img/projects/dubai-creek-harbour.jpg',
    'Ongoing rebar detailing for a cluster of residential towers at Dubai Creek Harbour. The phased delivery covers raft foundations through to roof-level mechanical floors, with BIM coordination for MEP penetrations.',
    'Aldar Properties',
    'Emaar Properties',
    'WSP',
    'Dubai, UAE',
    ARRAY['rebar', 'BBS', 'MEP'],
    FALSE
) ON CONFLICT (slug) DO NOTHING;

INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable)
VALUES (
    'red-sea-international-airport',
    'Red Sea International Airport',
    'airport',
    'Saudi Arabia',
    'ongoing',
    '/img/projects/red-sea-airport.jpg',
    'Rebar detailing for the new Red Sea International Airport terminal and airside infrastructure. The project is part of Saudi Arabia''s Vision 2030 tourism mega-projects and features a dramatic canopy roof structure.',
    'NESMA & Partners',
    'Red Sea Global',
    'Foster + Partners',
    'Tabuk, Saudi Arabia',
    ARRAY['rebar', 'BBS', 'GA', 'QS'],
    TRUE
) ON CONFLICT (slug) DO NOTHING;

INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable)
VALUES (
    'diriyah-gate-hotel-quarter',
    'Diriyah Gate - Hotel & Heritage Quarter',
    'hotel_hospitality',
    'Saudi Arabia',
    'ongoing',
    '/img/projects/diriyah-gate-hotel.jpg',
    'Rebar detailing for multiple boutique hotels and heritage restoration structures within the Diriyah Gate development. Sensitive context required careful foundation design to preserve adjacent UNESCO heritage structures.',
    'Salini Impregilo',
    'Diriyah Gate Development Authority',
    'Omrania',
    'Diriyah, Saudi Arabia',
    ARRAY['rebar', 'BBS'],
    FALSE
) ON CONFLICT (slug) DO NOTHING;

INSERT INTO project (slug, name, sector, country, status, image, description, main_contractor, client, consultant, location, scope, featurable)
VALUES (
    'bahrain-bay-waterfront-promenade',
    'Bahrain Bay Waterfront Promenade',
    'leisure_museum',
    'Bahrain',
    'ongoing',
    '/img/projects/bahrain-bay-promenade.jpg',
    'Rebar detailing for the public waterfront promenade and leisure pavilions at Bahrain Bay. Marine environment required special attention to cover, crack width control, and corrosion-resistant reinforcement specifications.',
    'Cebarco Bahrain',
    'Bahrain Bay Development',
    'SOM',
    'Manama, Bahrain',
    ARRAY['rebar', 'BBS', 'as-built'],
    FALSE
) ON CONFLICT (slug) DO NOTHING;
