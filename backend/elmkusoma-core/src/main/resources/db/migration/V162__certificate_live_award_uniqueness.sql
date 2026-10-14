-- Prevent a second live certificate for an award a student already holds.
--
-- Why this is a database constraint and not only a service check
-- ------------------------------------------------------------
-- CertificateService refuses a duplicate award at issuance, but that check reads before it writes.
-- Two operators issuing the same award at the same moment both read "no existing certificate", both
-- proceed, and both commit. Nothing in the service layer can prevent that: it is a check-then-act
-- race, and the window is real on any system with concurrent requests.
--
-- Only a uniqueness rule in the database closes that window, because the database is what actually
-- arbitrates the two writes.
--
-- Why a PARTIAL unique index rather than an ordinary constraint
-- -----------------------------------------------------------
-- Two exclusions, both deliberate:
--
--   1. Revoked certificates are excluded. If an award was issued in error and then withdrawn,
--      re-issuing it is the remedy. A blanket unique constraint would make the mistake permanent:
--      the operator would have to delete or edit the revoked row to fix it, destroying the record
--      of what went wrong.
--
--   2. Soft-deleted rows are excluded, for the same reason. Soft deletion here is archival, not a
--      statement that the certificate never existed.
--
-- Both exclusions are what make "revoke and re-issue" work while still preventing two live copies.
--
-- Note on title
-- ------------
-- The uniqueness key includes the certificate title. That is what distinguishes "participation in
-- session A" from "participation in session B" when both use the same template, so a student can
-- legitimately hold several participation certificates. It also means the guard is only as good as
-- the title: two genuinely different awards given the identical title on the same template would
-- collide. That is the same identity the rest of the certificate code uses, so the guard and the
-- rest of the system agree on what an award is.

CREATE UNIQUE INDEX IF NOT EXISTS ux_certificates_live_award
    ON certificates (student_id, template_id, certificate_type, title)
    WHERE is_deleted = false AND status <> 'REVOKED';

-- The issuance-time lookup that backs the service-level check uses this index.
CREATE INDEX IF NOT EXISTS idx_certificates_award_lookup
    ON certificates (student_id, template_id, certificate_type)
    WHERE is_deleted = false AND status <> 'REVOKED';
