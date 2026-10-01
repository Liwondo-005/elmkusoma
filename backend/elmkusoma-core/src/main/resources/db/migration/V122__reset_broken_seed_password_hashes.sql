-- V114: repair the placeholder password hashes of the V58 seed accounts.
--
-- Twelve rows were seeded with a well-known placeholder BCrypt hash that does
-- not match any of the documented seed credentials, so those accounts (two
-- institution admins, the district admin, a regional admin, teachers,
-- students, learners and a parent) could never log in. The working seed
-- convention — used by national@elmkusoma.go.tz and admin@elmkusoma.go.tz —
-- is the BCrypt hash of the documented seed password "password"; this
-- additive data migration aligns only the rows still carrying the broken
-- placeholder with that convention. No schema change, no rewrite of old
-- migrations, no effect on accounts whose hashes were set later.

UPDATE users
SET password_hash = '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2',
    updated_at    = NOW()
WHERE password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
  AND is_deleted = false;
