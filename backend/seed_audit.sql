INSERT INTO audit_logs (institution_id, user_id, user_email, user_role, action, entity_type, entity_id, entity_name, new_values, ip_address, user_agent, request_method, request_url, response_status, duration_ms)
VALUES ('fbd2e3e3-99df-48f3-b138-58d6f6f84103', '61fb22c6-77df-4d89-8dff-623a43c5bb74', 'admin@test.com', 'ADMIN', 'LOGIN', 'USER', '61fb22c6-77df-4d89-8dff-623a43c5bb74', 'Admin User', '{"email":"admin@test.com"}', '127.0.0.1', 'Mozilla/5.0', 'POST', '/api/v1/auth/login', 200, 128);

INSERT INTO audit_logs (institution_id, user_id, user_email, user_role, action, entity_type, entity_id, entity_name, new_values, ip_address, user_agent, request_method, request_url, response_status, duration_ms)
VALUES ('fbd2e3e3-99df-48f3-b138-58d6f6f84103', '61fb22c6-77df-4d89-8dff-623a43c5bb74', 'admin@test.com', 'ADMIN', 'CREATE', 'TEMPLATE', '54944775-9cbd-4a4c-bd30-655cde5513ce', 'School Completion Certificate', '{"name":"School Completion Certificate","type":"COMPLETION"}', '127.0.0.1', 'Mozilla/5.0', 'POST', '/api/v1/certificates/templates', 201, 156);

INSERT INTO audit_logs (institution_id, user_id, user_email, user_role, action, entity_type, entity_id, entity_name, new_values, ip_address, user_agent, request_method, request_url, response_status, duration_ms)
VALUES ('fbd2e3e3-99df-48f3-b138-58d6f6f84103', '61fb22c6-77df-4d89-8dff-623a43c5bb74', 'admin@test.com', 'ADMIN', 'CREATE', 'CERTIFICATE', 'dac40ec6-e3db-4fdb-a886-b226cc7ce333', 'Certificate of Completion', '{"title":"Certificate of Completion","studentName":"John Mwamba","type":"COMPLETION"}', '127.0.0.1', 'Mozilla/5.0', 'POST', '/api/v1/certificates/generate', 201, 215);

INSERT INTO audit_logs (institution_id, user_id, user_email, user_role, action, entity_type, entity_id, entity_name, old_values, new_values, ip_address, user_agent, request_method, request_url, response_status, duration_ms)
VALUES ('fbd2e3e3-99df-48f3-b138-58d6f6f84103', '61fb22c6-77df-4d89-8dff-623a43c5bb74', 'admin@test.com', 'ADMIN', 'ISSUE', 'CERTIFICATE', 'dac40ec6-e3db-4fdb-a886-b226cc7ce333', 'Certificate of Completion', '{"status":"DRAFT"}', '{"status":"ISSUED","verificationCode":"59561C99FB334BDF"}', '127.0.0.1', 'Mozilla/5.0', 'POST', '/api/v1/certificates/issue', 200, 98);

INSERT INTO audit_logs (institution_id, user_id, user_email, user_role, action, entity_type, entity_id, entity_name, old_values, new_values, ip_address, user_agent, request_method, request_url, response_status, duration_ms)
VALUES ('fbd2e3e3-99df-48f3-b138-58d6f6f84103', '61fb22c6-77df-4d89-8dff-623a43c5bb74', 'admin@test.com', 'ADMIN', 'UPDATE', 'SETTINGS', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'Institution Settings', '{"theme":"light"}', '{"theme":"dark"}', '127.0.0.1', 'Mozilla/5.0', 'PUT', '/api/v1/admin/settings', 200, 67);

INSERT INTO audit_logs (institution_id, user_id, user_email, user_role, action, entity_type, entity_id, entity_name, new_values, ip_address, user_agent, request_method, request_url, response_status, duration_ms)
VALUES ('fbd2e3e3-99df-48f3-b138-58d6f6f84103', '11163106-cef5-403e-9aca-d112efaf5980', 'student@test.com', 'STUDENT', 'REGISTER', 'USER', '11163106-cef5-403e-9aca-d112efaf5980', 'John Mwamba', '{"email":"student@test.com","role":"STUDENT"}', '127.0.0.1', 'Mozilla/5.0', 'POST', '/api/v1/auth/register', 201, 287);

INSERT INTO audit_logs (institution_id, user_id, user_email, user_role, action, entity_type, entity_id, entity_name, new_values, ip_address, user_agent, request_method, request_url, response_status, duration_ms)
VALUES ('fbd2e3e3-99df-48f3-b138-58d6f6f84103', '11163106-cef5-403e-9aca-d112efaf5980', 'student@test.com', 'STUDENT', 'VIEW', 'CERTIFICATE', 'dac40ec6-e3db-4fdb-a886-b226cc7ce333', 'Certificate of Completion', '{"studentName":"John Mwamba"}', '127.0.0.1', 'Mozilla/5.0', 'GET', '/api/v1/certificates', 200, 45);
