-- V83: runtime fixes discovered by the certificate-governance E2E run (ADDITIVE / NON-DESTRUCTIVE)
--
-- certificate_templates.template_html is the legacy V16 column. The current entity
-- (CertificateTemplate.htmlContent) maps html_content instead, so every INSERT omits
-- template_html and its NOT NULL constraint aborted template creation at runtime with
-- "null value in column template_html violates not-null constraint" (409) on BOTH the
-- platform governance path and the pre-existing institution create path. No code reads
-- template_html; only the constraint is relaxed, so existing rows keep their values.
ALTER TABLE certificate_templates ALTER COLUMN template_html DROP NOT NULL;
