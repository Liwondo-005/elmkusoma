-- V117: complete the jurisdiction linkage for the seeded institutions and
-- authority accounts (Nationaladmin.md §5/§8).
--
-- The regions/districts tables have been populated since V47, but no prior
-- migration ever assigned them to institutions or to the regional/district
-- admin accounts — leaving jurisdiction-scoped oversight with an empty scope
-- and the authority accounts unauthorized for any region or district.
-- This is an additive data migration (no schema change, no rewrite of old
-- migrations); it only links existing rows to other existing rows by their
-- stable codes/emails.
--
-- Geography follows the V58 seed addresses and converges with V115's
-- (region-level) assignments; this migration additionally resolves the
-- district level, which V115 deliberately leaves NULL.
--
-- Districts exist (V47) only for Arusha, Dar es Salaam and Dodoma, so
-- Mwanza Primary Academy receives a region but no district — a genuine gap
-- that the data-quality check "institutions-missing-geo" will keep reporting.

-- 1. Institutions → region/district (matched on the stable institution code).
UPDATE institutions
SET region_id   = (SELECT id FROM regions WHERE code = 'DOD' AND is_deleted = false),
    district_id = (SELECT id FROM districts WHERE code = 'DOC' AND is_deleted = false),
    updated_at  = NOW()
WHERE code = 'ELMKUSOMA-HQ' AND is_deleted = false;

UPDATE institutions
SET region_id   = (SELECT id FROM regions WHERE code = 'DAR' AND is_deleted = false),
    district_id = (SELECT id FROM districts WHERE code = 'ILA' AND is_deleted = false),
    updated_at  = NOW()
WHERE code = 'DMS-001' AND is_deleted = false;

UPDATE institutions
SET region_id   = (SELECT id FROM regions WHERE code = 'ARU' AND is_deleted = false),
    district_id = (SELECT id FROM districts WHERE code = 'ARC' AND is_deleted = false),
    updated_at  = NOW()
WHERE code = 'ATC-001' AND is_deleted = false;

UPDATE institutions
SET region_id   = (SELECT id FROM regions WHERE code = 'MWZ' AND is_deleted = false),
    district_id = NULL,
    updated_at  = NOW()
WHERE code = 'MPA-001' AND is_deleted = false;

-- 2. Authority accounts → their own jurisdiction (matched on email).
--    Jurisdiction belongs to the role, not to the attached institution.
--    Unconditional on the jurisdiction columns: a previously linked account
--    must converge to the canonical seed geography (V115 set region-level
--    links for regional@/district@ where they were still NULL).
UPDATE users
SET region_id  = (SELECT id FROM regions WHERE code = 'DAR' AND is_deleted = false),
    updated_at = NOW()
WHERE email = 'regional@elmkusoma.go.tz' AND is_deleted = false;

UPDATE users
SET region_id  = (SELECT id FROM regions WHERE code = 'DAR' AND is_deleted = false),
    updated_at = NOW()
WHERE email = 'regional1@elmkusoma.go.tz' AND is_deleted = false;

UPDATE users
SET region_id   = (SELECT id FROM regions WHERE code = 'DAR' AND is_deleted = false),
    district_id = (SELECT id FROM districts WHERE code = 'ILA' AND is_deleted = false),
    updated_at  = NOW()
WHERE email = 'district@elmkusoma.go.tz' AND is_deleted = false;
