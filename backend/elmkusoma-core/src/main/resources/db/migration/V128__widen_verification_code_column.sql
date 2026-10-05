-- Verification codes are stored as SHA-256 hex (64 chars), not plaintext.
ALTER TABLE verification_codes ALTER COLUMN code TYPE VARCHAR(64);
