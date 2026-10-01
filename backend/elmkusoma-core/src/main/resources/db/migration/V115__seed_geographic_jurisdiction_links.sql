-- V112: Seed geographic jurisdiction links for regional governance
--
-- Regions and districts were introduced in V47, but no seed row was ever linked
-- to them: institutions.region_id / institutions.district_id and the authority
-- users' region_id / district_id stayed NULL. Every region-scoped oversight and
-- regional-admin query (findByRegionIdAndIsDeletedFalse, user jurisdiction
-- resolution) therefore returned zero rows on a fresh database.
--
-- This migration links ONLY rows whose location is stated in the seed data
-- itself (institution address in V58, authority accounts in V58). No district
-- is invented where the seed does not state one — those remain NULL and are
-- reported as real "missing geographic relationship" findings by the Regional
-- Admin data-quality checks.

-- 1. Institutions (V58 addresses)
UPDATE institutions SET region_id = '11111111-1111-1111-1111-111111111102'  -- Dar es Salaam
WHERE id = 'a0000000-0000-0000-0000-000000000002'                          -- Dar es Salaam Model School ("Dar es Salaam, Tanzania")
  AND region_id IS NULL AND is_deleted = FALSE;

UPDATE institutions SET region_id = '11111111-1111-1111-1111-111111111101'  -- Arusha
WHERE id = 'a0000000-0000-0000-0000-000000000003'                          -- Arusha Technical College ("Arusha, Tanzania")
  AND region_id IS NULL AND is_deleted = FALSE;

UPDATE institutions SET region_id = '11111111-1111-1111-1111-111111111115'  -- Mwanza
WHERE id = 'a0000000-0000-0000-0000-000000000004'                          -- Mwanza Primary Academy ("Mwanza, Tanzania")
  AND region_id IS NULL AND is_deleted = FALSE;

UPDATE institutions SET region_id = '11111111-1111-1111-1111-111111111103'  -- Dodoma
WHERE id = 'a0000000-0000-0000-0000-000000000001'                          -- ELMKUSOMA National HQ ("Dodoma, Tanzania")
  AND region_id IS NULL AND is_deleted = FALSE;

-- 2. Authority users (V58 created them without a jurisdiction, which made the
--    REGIONAL_ADMIN / DISTRICT_ADMIN roles unusable: jurisdiction resolution
--    throws "no regional jurisdiction assigned").
UPDATE users SET region_id = '11111111-1111-1111-1111-111111111102'          -- Dar es Salaam
WHERE email = 'regional@elmkusoma.go.tz' AND role = 'REGIONAL_ADMIN'
  AND region_id IS NULL AND is_deleted = FALSE;

-- District admin: seed region + Ilala district (one of the four V47 Dar es
-- Salaam districts).
UPDATE users
SET region_id   = '11111111-1111-1111-1111-111111111102',                    -- Dar es Salaam
    district_id = '22222222-2222-2222-2222-222222222201'                     -- Ilala
WHERE email = 'district@elmkusoma.go.tz' AND role = 'DISTRICT_ADMIN'
  AND region_id IS NULL AND is_deleted = FALSE;
