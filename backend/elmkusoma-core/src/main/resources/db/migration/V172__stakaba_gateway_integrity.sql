-- V172: Stakaba mobile-money gateway integrity.
--
-- Additive only. Supports the outbound Stakaba collection adapter
-- (PaymentGatewayService) without altering any existing column:
--
--   * A Stakaba internalReference (e.g. "M_X7Y8Z9W0") may be bound to AT MOST
--     one live payment. The application already refuses a reference reused on
--     another payment at the webhook/status layer; this partial unique index
--     is the storage-layer backstop so a race or a future code path cannot
--     double-bind one gateway transaction and grant two entitlements.
--     Scoped to provider = 'stakaba' so historical 'manual' references (which
--     were never guaranteed globally unique) are untouched and existing rows
--     cannot make this migration fail.
--
--   * A lookup index for the inbound webhook path, which resolves an event to
--     a local payment by provider_reference before any state change.
--
-- No data is rewritten. Every existing row satisfies both rules.

CREATE UNIQUE INDEX IF NOT EXISTS uniq_payments_stakaba_reference
    ON payments (provider_reference)
    WHERE provider = 'stakaba' AND provider_reference IS NOT NULL AND is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_payments_provider_reference
    ON payments (provider_reference)
    WHERE provider_reference IS NOT NULL AND is_deleted = false;
