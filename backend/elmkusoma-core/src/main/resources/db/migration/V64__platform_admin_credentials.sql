-- V64: Add a plain ADMIN (platform admin) user for full platform access
-- Password: 'password' (BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy)

INSERT INTO users (id, institution_id, email, password_hash, first_name, last_name, role, is_active, is_email_verified, is_deleted, created_at, updated_at)
VALUES
  ('b0000000-0000-0000-0000-000000000099', 'a0000000-0000-0000-0000-000000000001', 'admin@elmkusoma.go.tz', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Platform', 'Admin', 'ADMIN', true, true, false, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
  SET role = 'ADMIN', is_active = true, is_email_verified = true, updated_at = NOW();

INSERT INTO institution_memberships (user_id, institution_id, role, is_active, created_at, updated_at)
VALUES
  ('b0000000-0000-0000-0000-000000000099', 'a0000000-0000-0000-0000-000000000001', 'ADMIN', true, NOW(), NOW())
ON CONFLICT (user_id, institution_id) DO UPDATE
  SET role = 'ADMIN', is_active = true, updated_at = NOW();
