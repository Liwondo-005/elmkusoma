-- Platform Admin step-up authentication (TOTP) and single-use recovery codes.
-- Secrets are stored encrypted (TOTP) or hashed (recovery codes); never plaintext.
CREATE TABLE IF NOT EXISTS mfa_factors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    factor_type VARCHAR(20) NOT NULL DEFAULT 'TOTP',
    secret_ciphertext VARCHAR(512) NOT NULL,
    label VARCHAR(255) NOT NULL DEFAULT 'Authenticator app',
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP,
    last_used_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mfa_factors_user_id ON mfa_factors(user_id);

CREATE TABLE IF NOT EXISTS recovery_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recovery_codes_user_id ON recovery_codes(user_id);
