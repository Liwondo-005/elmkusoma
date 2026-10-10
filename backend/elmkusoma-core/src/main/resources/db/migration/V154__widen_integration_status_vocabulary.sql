-- Widens the integration status vocabulary.
--
-- Email health now distinguishes NOT_CONFIGURED / CONFIGURED_UNVERIFIED / FAILED rather than
-- collapsing to CONFIGURED whenever a host string is present. "CONFIGURED_UNVERIFIED" is 21
-- characters and integration_status.config_status is VARCHAR(20), so the honest value could
-- not be stored - the column would have rejected it and turned a truthful status into a 500.
--
-- Widening a column is additive and safe for existing rows: no data is rewritten, and the
-- previous values remain valid subsets.

ALTER TABLE integration_status
    ALTER COLUMN config_status TYPE VARCHAR(32);

ALTER TABLE integration_status
    ALTER COLUMN connection_status TYPE VARCHAR(32);