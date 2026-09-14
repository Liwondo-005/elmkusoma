-- ELMKUSOMA Database Setup
-- Run this as PostgreSQL superuser (postgres) to create the app database and user.
-- Usage: psql -U postgres -f setup-db.sql

-- Create application user (ignore error if exists)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'elmkusoma') THEN
        CREATE ROLE elmkusoma WITH LOGIN PASSWORD 'changeme' CREATEDB;
    END IF;
END
$$;

-- Create database (ignore error if exists)
SELECT 'CREATE DATABASE elmkusoma OWNER elmkusoma'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'elmkusoma')\gexec

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE elmkusoma TO elmkusoma;
ALTER DATABASE elmkusoma OWNER TO elmkusoma;

-- Connect to elmkusoma and set up schema permissions
\connect elmkusoma

GRANT ALL ON SCHEMA public TO elmkusoma;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO elmkusoma;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO elmkusoma;

\echo 'Database setup complete. elmkusoma user can now run the application.'
