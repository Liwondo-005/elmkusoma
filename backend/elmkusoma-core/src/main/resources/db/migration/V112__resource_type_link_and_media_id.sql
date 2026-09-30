-- Resource Management & Student Access — additive schema alignment.
--
-- 1) The legacy CHECK created by V90 (DOCUMENT/VIDEO/IMAGE/AUDIO/LINK/OTHER)
--    rejects values that the Resource.ResourceType domain enum has shipped for
--    a long time (EXTERNAL_LINK, PDF, PRESENTATION, SPREADSHEET, LIVE_RECORDING,
--    ARCHIVE), so URL-based resource creation failed at the database.  The
--    original LINK value is preserved alongside EXTERNAL_LINK (both are URL
--    resources).  The resources table currently holds no rows, and no row is
--    deleted or rewritten here.
ALTER TABLE resources DROP CONSTRAINT IF EXISTS resources_resource_type_check;
ALTER TABLE resources ADD CONSTRAINT resources_resource_type_check
    CHECK (resource_type IN ('PDF', 'DOCUMENT', 'PRESENTATION', 'SPREADSHEET',
                             'IMAGE', 'VIDEO', 'AUDIO', 'EXTERNAL_LINK', 'LINK',
                             'LIVE_RECORDING', 'ARCHIVE', 'OTHER'));

-- 2) URL-based types (LINK, EXTERNAL_LINK) must carry an external URL; every
--    other type must carry stored bytes.  Extends V90's chk_resource_file /
--    chk_resource_external semantics to include LINK.
ALTER TABLE resources DROP CONSTRAINT IF EXISTS chk_resource_file;
ALTER TABLE resources ADD CONSTRAINT chk_resource_file
    CHECK (resource_type IN ('EXTERNAL_LINK', 'LINK') OR storage_url IS NOT NULL);

ALTER TABLE resources DROP CONSTRAINT IF EXISTS chk_resource_external;
ALTER TABLE resources ADD CONSTRAINT chk_resource_external
    CHECK (resource_type NOT IN ('EXTERNAL_LINK', 'LINK') OR external_url IS NOT NULL);

-- 3) Stable linkage from a resource to the byte object held by the existing
--    media service (media_files.id, same database).  Additive column; existing
--    rows (none today) and legacy URL-only resources keep working without it.
ALTER TABLE resources ADD COLUMN IF NOT EXISTS media_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_resources_media_id ON resources (media_id);
