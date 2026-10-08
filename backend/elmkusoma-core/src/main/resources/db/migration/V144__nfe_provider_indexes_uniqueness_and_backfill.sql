-- V130: NFE provider schema hardening (audit Phase 2 -- B-19, B-17)
--
-- The 8 nfe_* tables had ZERO secondary indexes and ZERO uniqueness constraints, so every
-- provider read was a sequential scan on (institution_id, is_deleted) and the certificate
-- verification endpoint could be broken by a duplicate verification code.
--
-- Additive only: indexes + unique partial indexes. No row is modified or deleted.

-- ---------------------------------------------------------------------------
-- B-19: index every predicate the provider repositories actually use
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_nfe_providers_institution
    ON nfe_education_providers (institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_programs_institution
    ON nfe_programs (institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_programs_provider
    ON nfe_programs (provider_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_sessions_institution
    ON nfe_sessions (institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_sessions_provider
    ON nfe_sessions (provider_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_sessions_program
    ON nfe_sessions (program_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_materials_institution
    ON nfe_materials (institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_materials_provider
    ON nfe_materials (provider_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_materials_program
    ON nfe_materials (program_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_learners_institution
    ON nfe_learners (institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_learners_provider
    ON nfe_learners (provider_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_learners_user
    ON nfe_learners (user_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_assessments_institution
    ON nfe_assessments (institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_assessments_provider
    ON nfe_assessments (provider_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_assessments_program
    ON nfe_assessments (program_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_attendance_institution
    ON nfe_attendance (institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_attendance_provider
    ON nfe_attendance (provider_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_attendance_session
    ON nfe_attendance (session_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_attendance_learner
    ON nfe_attendance (learner_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_certificates_institution
    ON nfe_certificates (institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_certificates_provider
    ON nfe_certificates (provider_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_certificates_learner
    ON nfe_certificates (learner_id) WHERE is_deleted = false;

-- Dashboard counters sort/aggregate by creation time.
CREATE INDEX IF NOT EXISTS idx_nfe_programs_created
    ON nfe_programs (institution_id, created_at DESC) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_sessions_status
    ON nfe_sessions (institution_id, status) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nfe_assessments_published
    ON nfe_assessments (provider_id, is_published) WHERE is_deleted = false;

-- ---------------------------------------------------------------------------
-- B-17: uniqueness invariants that the application layer used to (and could not) enforce.
--
-- A duplicate verification_code made Optional<NfeCertificate> findByVerificationCode throw
-- IncorrectResultSizeDataAccessException -> 500 on the public verification endpoint.
-- A duplicate provider name raced past the app-level existsByName check.
-- Partial indexes so soft-deleted rows never collide with live ones.
-- ---------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS ux_nfe_providers_name
    ON nfe_education_providers (institution_id, lower(name)) WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS ux_nfe_certificates_verification_code
    ON nfe_certificates (institution_id, verification_code)
    WHERE is_deleted = false AND verification_code IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_nfe_certificates_serial
    ON nfe_certificates (institution_id, serial_number)
    WHERE is_deleted = false AND serial_number IS NOT NULL;

-- ---------------------------------------------------------------------------
-- Dedup pre-pass: if pre-existing rows would make the unique indexes above fail, keep the
-- oldest and null out the duplicate's human-facing identifiers (never delete a row).
-- ---------------------------------------------------------------------------
UPDATE nfe_certificates c
   SET verification_code = NULL, updated_at = NOW()
 WHERE c.verification_code IS NOT NULL
   AND c.is_deleted = false
   AND EXISTS (SELECT 1 FROM nfe_certificates d
                WHERE d.institution_id = c.institution_id
                  AND d.verification_code = c.verification_code
                  AND d.is_deleted = false
                  AND (d.created_at, d.id) < (c.created_at, c.id));

UPDATE nfe_certificates c
   SET serial_number = NULL, updated_at = NOW()
 WHERE c.serial_number IS NOT NULL
   AND c.is_deleted = false
   AND EXISTS (SELECT 1 FROM nfe_certificates d
                WHERE d.institution_id = c.institution_id
                  AND d.serial_number = c.serial_number
                  AND d.is_deleted = false
                  AND (d.created_at, d.id) < (c.created_at, c.id));

-- B-01 (Phase 2): backfill the provider record for every provider-type institution that has
-- none. Without this the entire provider workspace is inert ("No education provider exists").
INSERT INTO nfe_education_providers (
        id, institution_id, name, provider_type, is_active, is_verified, is_deleted,
        created_at, updated_at, created_by)
SELECT gen_random_uuid(), i.id, i.name,
       -- Institution.InstitutionType -> EducationProvider.ProviderType. These are two
       -- DIFFERENT vocabularies; the CHECK constraint on nfe_education_providers.provider_type
       -- accepts only ORGANIZATION, COMPANY, GOVERNMENT, RELIGIOUS, TRAINING, INDIVIDUAL.
       CASE
           WHEN i.type = 'TRAINING_PROVIDER' THEN 'TRAINING'
           WHEN i.type = 'COMPANY'           THEN 'COMPANY'
           WHEN i.type = 'GOVERNMENT'        THEN 'GOVERNMENT'
           ELSE 'ORGANIZATION'
       END,
       true,
       -- Approved institutions start verified; everything else stays pending review.
       CASE WHEN i.status = 'ACTIVE' THEN true ELSE false END,
       false, NOW(), NOW(), 'V130-backfill'
  FROM institutions i
 WHERE i.is_deleted = false
   AND i.type IN ('TRAINING_PROVIDER','PROFESSIONAL_BODY','COMPANY','NGO',
                  'GOVERNMENT','CONTENT_PROVIDER','EVENT_PROVIDER')
   AND NOT EXISTS (SELECT 1 FROM nfe_education_providers p
                    WHERE p.institution_id = i.id AND p.is_deleted = false);