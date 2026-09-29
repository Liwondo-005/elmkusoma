-- Add institution_id column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS institution_id UUID;

-- Add index for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_institution_id ON users(institution_id);
