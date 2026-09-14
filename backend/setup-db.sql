-- =============================================================================
-- ELMKUSOMA Database Setup
-- =============================================================================
-- Run this ONCE as PostgreSQL superuser to create the database.
--
-- Usage:  psql -U postgres -f setup-db.sql
--    or:  psql -U postgres -d postgres -f setup-db.sql
--
-- This script:
--   1. Creates the "elmkusoma" database (if not exists)
--   2. Grants all privileges to the connecting user (typically "postgres")
--
-- NOTE: The application connects as the "postgres" user by default.
--       Each developer uses their own PostgreSQL password via .env configuration.
-- =============================================================================

-- Create database (ignore error if exists)
SELECT 'CREATE DATABASE elmkusoma'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'elmkusoma')\gexec

-- Grant privileges to the connecting user
GRANT ALL PRIVILEGES ON DATABASE elmkusoma TO CURRENT_USER;

-- Connect to elmkusoma and set up schema permissions
\connect elmkusoma

GRANT ALL ON SCHEMA public TO CURRENT_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO CURRENT_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO CURRENT_USER;

\echo 'Database setup complete. You can now start the application.'
