-- V125: Align legacy subject education levels with the authoritative EducationLevel stages.
--
-- Context (§16 seed/data consistency):
--   V58 seeded subjects with legacy values 'O_LEVEL' and 'A_LEVEL'
--   (V58:56-59 Dar es Salaam Model School O_LEVEL, V58:60 Arusha Technical College A_LEVEL,
--   V58:61 Mwanza Primary Academy O_LEVEL). Those legacy stage names predate the canonical
--   EducationLevel set (NURSERY, PRIMARY, SECONDARY, VETA, COLLEGE, UNIVERSITY); with
--   @Enumerated(EnumType.STRING) they previously made JPA hydration fail (subject endpoints 500).
--   The enum now also accepts O_LEVEL/A_LEVEL for hydration tolerance, and this fix-forward data
--   migration normalizes the seeded rows to the canonical stage authoritative for their OWNING
--   INSTITUTION.
--
-- Strategy: fix-forward data migration (V58 is immutable; editing it would break checksums).
--   Each legacy row is mapped to the level authoritative for its OWNING INSTITUTION:
--     - institution type COLLEGE/UNIVERSITY/VOCATIONAL(VETA) -> that level;
--     - school-type institutions -> nursery/primary by institution name, otherwise SECONDARY
--       (O-Level and A-Level are both secondary stages of the Tanzanian 2-7-3-4 system);
--     - orphaned institution reference -> SECONDARY (prefer a readable row over a hydration failure).
--   No rows are deleted; only education_level is rewritten, and only for the two legacy values.

UPDATE subjects s
SET education_level = CASE
        WHEN (SELECT i.type FROM institutions i WHERE i.id = s.institution_id) = 'COLLEGE' THEN 'COLLEGE'
        WHEN (SELECT i.type FROM institutions i WHERE i.id = s.institution_id) = 'UNIVERSITY' THEN 'UNIVERSITY'
        WHEN (SELECT i.type FROM institutions i WHERE i.id = s.institution_id) IN ('VOCATIONAL', 'VETA') THEN 'VETA'
        WHEN (SELECT i.name FROM institutions i WHERE i.id = s.institution_id) ILIKE '%nursery%' THEN 'NURSERY'
        WHEN (SELECT i.name FROM institutions i WHERE i.id = s.institution_id) ILIKE '%primary%' THEN 'PRIMARY'
        ELSE 'SECONDARY'
    END,
    updated_at = NOW()
WHERE s.education_level IN ('O_LEVEL', 'A_LEVEL');
