INSERT INTO security_events (id, institution_id, user_id, event_type, severity, description, ip_address, user_agent, resolved, created_at)
VALUES
(gen_random_uuid(), 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', NULL, 'FAILED_LOGIN', 'MEDIUM', 'Multiple failed login attempts detected from IP 192.168.1.100', '192.168.1.100', 'Mozilla/5.0', false, NOW() - interval '3 hours'),
(gen_random_uuid(), 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', NULL, 'UNAUTHORIZED_ACCESS', 'HIGH', 'Attempted access to admin endpoints without proper role', '10.0.0.55', 'curl/7.81', false, NOW() - interval '1 hour'),
(gen_random_uuid(), 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', '11163106-cef5-403e-9aca-d112efaf5980', 'PASSWORD_CHANGE', 'LOW', 'Student password changed successfully', '127.0.0.1', 'Mozilla/5.0', true, NOW() - interval '30 minutes');
