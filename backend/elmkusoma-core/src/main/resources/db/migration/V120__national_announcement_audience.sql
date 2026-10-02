-- National/regional/district announcements (Nationaladmin.md §23).
-- Additive only: existing institution/class announcements keep working
-- (audience_type NULL = legacy institution-scoped announcement).

ALTER TABLE announcements ADD COLUMN IF NOT EXISTS audience_type VARCHAR(32);
ALTER TABLE announcements ADD COLUMN IF NOT EXISTS audience_region_id UUID;
ALTER TABLE announcements ADD COLUMN IF NOT EXISTS audience_district_id UUID;
ALTER TABLE announcements ADD COLUMN IF NOT EXISTS status VARCHAR(16) DEFAULT 'PUBLISHED';
ALTER TABLE announcements ADD COLUMN IF NOT EXISTS scheduled_at TIMESTAMP;
ALTER TABLE announcements ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_announcements_audience_type ON announcements(audience_type) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_announcements_audience_region ON announcements(audience_region_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_announcements_audience_district ON announcements(audience_district_id) WHERE is_deleted = false;
