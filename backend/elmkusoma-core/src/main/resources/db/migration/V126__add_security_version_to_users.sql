-- Session invalidation counter for authentication recovery hardening.
-- Embedded in JWTs as the "sv" claim; enforced by JwtAuthenticationFilter and
-- JwtRequestAttributeFilter. Bumped on every password reset/change.
ALTER TABLE users ADD COLUMN IF NOT EXISTS security_version BIGINT NOT NULL DEFAULT 1;
