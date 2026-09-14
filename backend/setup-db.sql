-- ELMKUSOMA Database Setup
-- Run this as PostgreSQL superuser (postgres) to create the app database.
-- Usage: psql -U postgres -f setup-db.sql
--
-- All developers use the same database: elmkusoma
-- All developers use the same PostgreSQL user: postgres
-- Only the password differs per developer (set in .env)

-- Create database (ignore error if exists)
SELECT 'CREATE DATABASE elmkusoma'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'elmkusoma')\gexec

-- Grant all privileges to postgres
GRANT ALL PRIVILEGES ON DATABASE elmkusoma TO postgres;

-- Connect to elmkusoma and set up schema permissions
\connect elmkusoma

GRANT ALL ON SCHEMA public TO postgres;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO postgres;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO postgres;

\echo 'Database setup complete. Configure your .env with your local PostgreSQL password.'
