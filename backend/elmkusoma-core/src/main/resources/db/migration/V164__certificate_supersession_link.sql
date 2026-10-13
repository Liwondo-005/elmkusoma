-- Record which certificate replaced which.
--
-- The problem this solves
-- -----------------------
-- A certificate is revoked so it can be re-issued - that is the documented remedy in V162, and
-- "Revoke it first if it must be replaced" is what the service tells an operator who tries to
-- issue a duplicate. But the record of that was only ever implicit: two rows with the same
-- student, template, type and title, one REVOKED and one ISSUED, and nothing saying they are the
-- same award at different points in time.
--
-- That gap is visible from outside. Someone holding the revoked copy scans its QR and is told
-- "revoked" - which is true but not useful, because it does not tell them that a valid
-- replacement exists or where to find it. The issuer knows. The holder does not.
--
-- A link, not a status
-- --------------------
-- This deliberately does NOT add SUPERSEDED to certificates.status.
--
-- The stored lifecycle is documented as one state machine with exactly one field
-- (see Certificate.CertificateStatus). Adding a sixth value would break that, and worse, it
-- would have to be reconciled with REVOKED - a certificate is both revoked AND superseded in the
-- normal re-issue flow, so a single status column could not hold the fact without losing one of
-- them.
--
-- So: status keeps meaning what it means, and supersession is recorded alongside it as a
-- reference to the certificate that took its place. Two facts, two fields, no new state.
--
-- The public verification status DOES gain a SUPERSEDED value, but that is the presentation
-- layer, which already has a value (EXPIRED) that exists in no stored enum. See
-- CertificateService.verifyCertificate.
--
-- Why the link points at the newest, not the immediate successor
-- -------------------------------------------------------------
-- Every certificate for the award scope is pointed at the certificate that replaced it in the
-- most recent issuance. Were it only the immediate successor, a certificate superseded twice
-- would need a chain walked at read time to answer "what is current?", and the anonymous
-- verification endpoint is exactly where you do not want a loop.
--
-- Only REVOKED and ISSUED predecessors are linked. A DRAFT or PENDING_APPROVAL row was never
-- released to anyone, so nothing can be holding a copy of it and there is nothing to replace.

ALTER TABLE certificates ADD COLUMN IF NOT EXISTS superseded_by_certificate_id uuid;
ALTER TABLE certificates ADD COLUMN IF NOT EXISTS superseded_at timestamptz;

-- Self-reference. Enforces that the id we store is a real certificate: without it a typo or a
-- race that deletes the successor would leave an orphan pointer that the verification page would
-- follow into nothing.
ALTER TABLE certificates
    ADD CONSTRAINT fk_certificates_superseded_by
    FOREIGN KEY (superseded_by_certificate_id) REFERENCES certificates (id);

-- The verification endpoint resolves a superseded certificate to its replacement by serial. It
-- also lets an operator list everything a single issuance replaced.
CREATE INDEX IF NOT EXISTS idx_certificates_superseded_by
    ON certificates (superseded_by_certificate_id)
    WHERE superseded_by_certificate_id IS NOT NULL;
