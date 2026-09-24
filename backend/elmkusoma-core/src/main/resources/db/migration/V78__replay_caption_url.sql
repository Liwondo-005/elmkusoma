-- V78: optional caption/transcript URL on replays (§78 accessibility)
ALTER TABLE replays ADD COLUMN IF NOT EXISTS caption_url VARCHAR(1024);
