-- Widen certificates.qr_code_url so it can hold the QR the certificate now carries.
--
-- The column was created in V81 as varchar(255) and nothing has ever written to it, because no QR
-- fits in 255 characters: an SVG data URI for a real verification link runs to a few kilobytes.
-- The field could never have been used as intended, so widening it is not changing a contract that
-- anyone depends on.
--
-- WIDENING ONLY. varchar(255) is a subset of text, so every existing value - all of them NULL -
-- remains valid and no scan of the table behaves differently. Nothing here narrows a type.
ALTER TABLE certificates ALTER COLUMN qr_code_url TYPE text;
