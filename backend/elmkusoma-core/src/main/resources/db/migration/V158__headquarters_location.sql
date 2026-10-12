-- ELMKUSOMA Headquarters location settings.
--
-- Reuses platform_config, exactly like V152/V153 did for contact and social settings. No
-- second settings store, no new endpoint shape: the same allow-list in PublicSiteSettings
-- governs these keys, and the same admin config API edits them.
--
-- WHAT IS SEEDED, AND WHAT IS DELIBERATELY NOT
--
-- Only the campus-level facts the owner supplied are seeded:
--   name      ELMKUSOMA Headquarters
--   campus    University of Dar es Salaam (UDSM)
--   city      Dar es Salaam
--   country   Tanzania
--
-- The precise-location keys are seeded EMPTY and stay that way until the owner verifies them:
--   building, address, mapUrl, directionsUrl, hours
--
-- Why empty rather than "helpfully" filled in. UDSM is a multi-campus university and its public
-- sources give the mailing address (P.O. Box 35091) and campus names, but nothing identifies
-- which building or office ELMKUSOMA occupies. Seeding a guessed building, street address or map
-- pin would publish a confident-looking falsehood on the marketing site, and a wrong pin sends
-- a real visitor to the wrong place. The public endpoints omit blank values, so the page shows
-- the campus-level location honestly and simply renders no "View on Map" / "Get Directions"
-- action until a real destination is configured.
--
-- The keys exist from the start, so the Platform Admin screen can offer the fields immediately
-- and saving them later needs no further migration.

INSERT INTO platform_config (id, created_at, is_deleted, config_key, config_value, config_type, description, category, is_sensitive, is_public)
VALUES
  (gen_random_uuid(), NOW(), false, 'public.location.name', 'ELMKUSOMA Headquarters', 'STRING',
   'Headquarters section heading shown on the public landing page', 'PUBLIC_LOCATION', false, true),
  (gen_random_uuid(), NOW(), false, 'public.location.campus', 'University of Dar es Salaam (UDSM)', 'STRING',
   'University or campus the headquarters is located at', 'PUBLIC_LOCATION', false, true),
  (gen_random_uuid(), NOW(), false, 'public.location.city', 'Dar es Salaam', 'STRING',
   'City of the headquarters location', 'PUBLIC_LOCATION', false, true),
  (gen_random_uuid(), NOW(), false, 'public.location.country', 'Tanzania', 'STRING',
   'Country of the headquarters location', 'PUBLIC_LOCATION', false, true),
  (gen_random_uuid(), NOW(), false, 'public.location.building', '', 'STRING',
   'Exact building or office at the headquarters. Optional until verified.', 'PUBLIC_LOCATION', false, true),
  (gen_random_uuid(), NOW(), false, 'public.location.address', '', 'STRING',
   'Exact public street address. Optional until verified.', 'PUBLIC_LOCATION', false, true),
  (gen_random_uuid(), NOW(), false, 'public.location.mapUrl', '', 'STRING',
   'Verified map destination URL for the headquarters. Optional; the map action is hidden when empty.', 'PUBLIC_LOCATION', false, true),
  (gen_random_uuid(), NOW(), false, 'public.location.directionsUrl', '', 'STRING',
   'Verified turn-by-turn directions URL, if different from the map destination. Optional.', 'PUBLIC_LOCATION', false, true),
  (gen_random_uuid(), NOW(), false, 'public.location.description', 'Our headquarters are based at the University of Dar es Salaam (UDSM) in Dar es Salaam, Tanzania.', 'STRING',
   'Short public description of the headquarters location', 'PUBLIC_LOCATION', false, true),
  (gen_random_uuid(), NOW(), false, 'public.location.hours', '', 'STRING',
   'Headquarters opening hours, free text. Optional; only shown when supplied and approved.', 'PUBLIC_LOCATION', false, true)
ON CONFLICT (config_key) DO NOTHING;
