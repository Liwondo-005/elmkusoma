-- ============================================================
-- SEED FILE: Oversight test accounts
-- Password for all accounts: Admin@1234
-- ============================================================

-- National Admin (1)
INSERT INTO users (id, email, password_hash, first_name, middle_name, last_name, phone, role, is_active, is_email_verified, learning_level, created_at, updated_at, is_deleted) VALUES
('aaaa1111-1111-1111-1111-111111111101', 'national@test.com', '$2b$10$/qxUuGchfhYab1sy5rzxl.IcoL/aaGdG9IwwlmYQmKbrhrhL6n0ba', 'National', NULL, 'Admin', '+255700000001', 'NATIONAL_ADMIN', true, true, NULL, NOW(), NOW(), FALSE);

-- Regional Admins (2) - Dar es Salaam, Arusha
INSERT INTO users (id, email, password_hash, first_name, middle_name, last_name, phone, role, is_active, is_email_verified, region_id, learning_level, created_at, updated_at, is_deleted) VALUES
('aaaa1111-1111-1111-1111-111111111102', 'regional.dar@test.com', '$2b$10$/qxUuGchfhYab1sy5rzxl.IcoL/aaGdG9IwwlmYQmKbrhrhL6n0ba', 'Dar', NULL, 'Regional', '+255700000002', 'REGIONAL_ADMIN', true, true, '11111111-1111-1111-1111-111111111102', NULL, NOW(), NOW(), FALSE),
('aaaa1111-1111-1111-1111-111111111103', 'regional.aru@test.com', '$2b$10$/qxUuGchfhYab1sy5rzxl.IcoL/aaGdG9IwwlmYQmKbrhrhL6n0ba', 'Arusha', NULL, 'Regional', '+255700000003', 'REGIONAL_ADMIN', true, true, '11111111-1111-1111-1111-111111111101', NULL, NOW(), NOW(), FALSE);

-- District Admins (2) - Ilala, Arusha City
INSERT INTO users (id, email, password_hash, first_name, middle_name, last_name, phone, role, is_active, is_email_verified, district_id, learning_level, created_at, updated_at, is_deleted) VALUES
('aaaa1111-1111-1111-1111-111111111104', 'district.ila@test.com', '$2b$10$/qxUuGchfhYab1sy5rzxl.IcoL/aaGdG9IwwlmYQmKbrhrhL6n0ba', 'Ilala', NULL, 'District', '+255700000004', 'DISTRICT_ADMIN', true, true, '22222222-2222-2222-2222-222222222201', NULL, NOW(), NOW(), FALSE),
('aaaa1111-1111-1111-1111-111111111105', 'district.arc@test.com', '$2b$10$/qxUuGchfhYab1sy5rzxl.IcoL/aaGdG9IwwlmYQmKbrhrhL6n0ba', 'Arusha', NULL, 'City', '+255700000005', 'DISTRICT_ADMIN', true, true, '22222222-2222-2222-2222-222222222205', NULL, NOW(), NOW(), FALSE);

-- Test institutions linked to districts (for oversight dashboards to have data)
INSERT INTO institutions (id, name, code, type, description, address, city, region, country, phone, email, is_active, created_at, updated_at, is_deleted, region_id, district_id) VALUES
('bbbb1111-1111-1111-1111-111111111101', 'Test Primary School Ilala', 'TPS-ILA', 'PRIMARY', 'Test primary school in Ilala district', '123 Main St', 'Dar es Salaam', 'Dar es Salaam', 'Tanzania', '+255700000010', 'tpsilala@test.com', true, NOW(), NOW(), FALSE, '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222201'),
('bbbb1111-1111-1111-1111-111111111102', 'Test Secondary School Kinondoni', 'TSS-KIN', 'SECONDARY', 'Test secondary school in Kinondoni district', '456 Academy Rd', 'Dar es Salaam', 'Dar es Salaam', 'Tanzania', '+255700000011', 'tsskinondoni@test.com', true, NOW(), NOW(), FALSE, '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222202'),
('bbbb1111-1111-1111-1111-111111111103', 'Test Primary School Arusha', 'TPS-ARC', 'PRIMARY', 'Test primary school in Arusha City', '789 Education Lane', 'Arusha', 'Arusha', 'Tanzania', '+255700000012', 'tpsarusha@test.com', true, NOW(), NOW(), FALSE, '11111111-1111-1111-1111-111111111101', '22222222-2222-2222-2222-222222222205');
