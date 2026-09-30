-- V113: allow the mandated resource processing lifecycle.
--
-- chk_resource_file (originally added by V90, re-declared by V112 with the
-- 12-value type list) required storage_url to be present at INSERT time.
-- That made the required processing states impossible:
--   * PROCESSING rows exist before the upload finishes (storage_url is NULL
--     by design), and
--   * FAILED rows must persist (storage unavailable / extraction failed) so
--     the failure stays visible and retryable — also with NULL storage_url.
--
-- The rule is relaxed ONLY for those two non-terminal states. READY file
-- resources still REQUIRE storage_url, and link types behave exactly as
-- before. This constraint is strictly looser than the previous one, so every
-- pre-existing row remains valid — no data rewrite, no version rewritten.
ALTER TABLE resources DROP CONSTRAINT IF EXISTS chk_resource_file;

ALTER TABLE resources ADD CONSTRAINT chk_resource_file CHECK (
        resource_type IN ('EXTERNAL_LINK', 'LINK')
        OR storage_url IS NOT NULL
        OR processing_status IN ('PROCESSING', 'FAILED')
    );
