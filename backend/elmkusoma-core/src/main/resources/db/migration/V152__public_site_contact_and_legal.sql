-- Public site settings, contact enquiries, and versioned legal content.
--
-- Reuses platform_config rather than introducing a second settings store: it already
-- carries category / is_public / is_sensitive, and the admin API already masks
-- sensitive values. The rows below only seed what the public settings screens read.

-- ---------------------------------------------------------------------------
-- Public contact + social settings
-- ---------------------------------------------------------------------------
-- Only public.contact.email is seeded with a value, because that address already exists
-- in the repository and is what the site publishes today. Everything else is deliberately
-- EMPTY: the phone (+255 700 000 000) and WhatsApp (wa.me/255700000000) currently in the
-- code are obvious placeholders, and the social links are bare domain roots
-- (https://facebook.com) rather than official accounts. Inventing real-looking values
-- would be worse than showing nothing, so the UI hides anything unset.

INSERT INTO platform_config (id, created_at, is_deleted, config_key, config_value, config_type, description, category, is_sensitive, is_public)
VALUES
  (gen_random_uuid(), NOW(), false, 'public.contact.email', 'info@elmkusoma.co.tz', 'STRING',
   'Official public contact address shown on the landing page, footer and contact page', 'PUBLIC_CONTACT', false, true),
  (gen_random_uuid(), NOW(), false, 'public.contact.supportEmail', '', 'STRING',
   'Address the support team answers enquiries on', 'PUBLIC_CONTACT', false, true),
  (gen_random_uuid(), NOW(), false, 'public.contact.phone', '', 'STRING',
   'Public phone number in international format, digits and + only', 'PUBLIC_CONTACT', false, true),
  (gen_random_uuid(), NOW(), false, 'public.contact.whatsapp', '', 'STRING',
   'Support WhatsApp number as bare digits with country code, no + or spaces (e.g. 255700000000)', 'PUBLIC_CONTACT', false, true),
  (gen_random_uuid(), NOW(), false, 'public.contact.address', '', 'STRING',
   'Office address shown publicly', 'PUBLIC_CONTACT', false, true),
  (gen_random_uuid(), NOW(), false, 'public.contact.workingHours', '', 'STRING',
   'Support working hours, free text', 'PUBLIC_CONTACT', false, true),
  (gen_random_uuid(), NOW(), false, 'public.contact.responseTime', '', 'STRING',
   'Expected support response time, free text', 'PUBLIC_CONTACT', false, true),

  (gen_random_uuid(), NOW(), false, 'public.social.facebook', '', 'STRING',
   'Official Facebook page URL', 'PUBLIC_SOCIAL', false, true),
  (gen_random_uuid(), NOW(), false, 'public.social.instagram', '', 'STRING',
   'Official Instagram profile URL', 'PUBLIC_SOCIAL', false, true),
  (gen_random_uuid(), NOW(), false, 'public.social.youtube', '', 'STRING',
   'Official YouTube channel URL', 'PUBLIC_SOCIAL', false, true),
  (gen_random_uuid(), NOW(), false, 'public.social.linkedin', '', 'STRING',
   'Official LinkedIn page URL', 'PUBLIC_SOCIAL', false, true),
  (gen_random_uuid(), NOW(), false, 'public.social.tiktok', '', 'STRING',
   'Official TikTok profile URL', 'PUBLIC_SOCIAL', false, true),
  (gen_random_uuid(), NOW(), false, 'public.social.x', '', 'STRING',
   'Official X (formerly Twitter) profile URL', 'PUBLIC_SOCIAL', false, true)
ON CONFLICT (config_key) DO NOTHING;

-- Notification routing. Never public: these are internal recipients, and publishing them
-- would hand the outside world the support inbox address and an admin escalation path.
INSERT INTO platform_config (id, created_at, is_deleted, config_key, config_value, config_type, description, category, is_sensitive, is_public)
VALUES
  (gen_random_uuid(), NOW(), false, 'support.notify.email', '', 'STRING',
   'Support inbox that receives new contact enquiries', 'SUPPORT_INTERNAL', false, false),
  (gen_random_uuid(), NOW(), false, 'support.notify.adminEmail', '', 'STRING',
   'Optional Platform Admin address copied on new contact enquiries', 'SUPPORT_INTERNAL', false, false),
  (gen_random_uuid(), NOW(), false, 'support.whatsapp.enabled', 'false', 'STRING',
   'Whether to offer WhatsApp click-to-chat links on public pages', 'SUPPORT_INTERNAL', false, false)
ON CONFLICT (config_key) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Contact enquiries
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS contact_messages (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  institution_id        UUID,
  created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at            TIMESTAMP,
  created_by            VARCHAR(255),
  updated_by            VARCHAR(255),
  is_deleted            BOOLEAN NOT NULL DEFAULT false,

  -- Human-quotable reference, unique so support can cite it in an email.
  reference             VARCHAR(32)  NOT NULL,
  name                  VARCHAR(120) NOT NULL,
  email                 VARCHAR(254) NOT NULL,
  category              VARCHAR(20)  NOT NULL,
  subject               VARCHAR(160) NOT NULL,
  message               TEXT         NOT NULL,

  -- NEW | IN_PROGRESS | AWAITING_RESPONSE | RESOLVED | CLOSED
  status                VARCHAR(24)  NOT NULL DEFAULT 'NEW',
  priority              VARCHAR(10)  NOT NULL DEFAULT 'NORMAL',

  assigned_to           UUID,
  resolved_at           TIMESTAMP,
  resolved_by           UUID,

  -- Delivery truth. A row existing says nothing about whether anyone was notified.
  -- NOT_CONFIGURED | PENDING | SENT | FAILED
  notification_status   VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  notification_error    VARCHAR(500),
  notification_attempts INTEGER      NOT NULL DEFAULT 0,
  notified_at           TIMESTAMP,

  -- Optional link to an authenticated user who used the same address.
  user_id               UUID,

  -- Abuse controls. Retained for moderation and rate-limit auditing, never exposed publicly.
  ip_address            VARCHAR(64),
  user_agent            VARCHAR(500)
);

-- The support inbox filters on status and orders by newest first.
CREATE INDEX IF NOT EXISTS idx_contact_messages_status_created
    ON contact_messages (status, created_at DESC) WHERE is_deleted = false;
-- "Search by reference" is the support team's first move on an inbound enquiry.
CREATE INDEX IF NOT EXISTS idx_contact_messages_reference
    ON contact_messages (reference);
-- Ownership lookups for a signed-in user viewing their own enquiries.
CREATE INDEX IF NOT EXISTS idx_contact_messages_user
    ON contact_messages (user_id) WHERE is_deleted = false AND user_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS contact_message_replies (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  institution_id        UUID,
  created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at            TIMESTAMP,
  created_by            VARCHAR(255),
  updated_by            VARCHAR(255),
  is_deleted            BOOLEAN NOT NULL DEFAULT false,

  contact_message_id    UUID         NOT NULL REFERENCES contact_messages (id) ON DELETE CASCADE,
  author_id             UUID,
  -- Internal notes are never returned to the enquiry's author and never leave the admin surface.
  is_internal           BOOLEAN      NOT NULL DEFAULT false,
  message               TEXT         NOT NULL,
  -- Whether the reply was actually emailed to the enquirer.
  delivered             BOOLEAN      NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_contact_message_replies_message
    ON contact_message_replies (contact_message_id, created_at) WHERE is_deleted = false;

-- ---------------------------------------------------------------------------
-- Legal documents with an immutable published history
-- ---------------------------------------------------------------------------
-- Same shape as certificate_template_versions: the working copy holds the draft, and every
-- publication is archived before the working copy is allowed to move on. History is append
-- only, so a legal change is never silently overwritten and can always be traced.

CREATE TABLE IF NOT EXISTS legal_documents (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  institution_id        UUID,
  created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at            TIMESTAMP,
  created_by            VARCHAR(255),
  updated_by            VARCHAR(255),
  is_deleted            BOOLEAN NOT NULL DEFAULT false,

  -- TERMS | PRIVACY | COOKIE | SUPPORT_POLICY
  doc_type              VARCHAR(32)  NOT NULL,
  title                 VARCHAR(200) NOT NULL,
  -- Plain text by design: the backend has no HTML sanitiser available, so legal content is
  -- never accepted as HTML and can never be rendered as markup. See docs/API-CONTRACT-PUBLIC-SITE.md
  content               TEXT         NOT NULL,
  effective_date        DATE,
  -- Working-copy revision counter. Increments on every save.
  version               INTEGER      NOT NULL DEFAULT 1,
  -- Highest published version number; null until the document is published once.
  published_version     INTEGER,
  last_modified_by      VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_legal_documents_type
    ON legal_documents (doc_type) WHERE is_deleted = false;

CREATE TABLE IF NOT EXISTS legal_document_versions (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  institution_id        UUID,
  created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at            TIMESTAMP,
  created_by            VARCHAR(255),
  updated_by            VARCHAR(255),
  is_deleted            BOOLEAN NOT NULL DEFAULT false,

  legal_document_id     UUID         NOT NULL REFERENCES legal_documents (id) ON DELETE CASCADE,
  version               INTEGER      NOT NULL,
  title                 VARCHAR(200) NOT NULL,
  -- Frozen at publication. Plain text, same reasoning as the working copy.
  content               TEXT         NOT NULL,
  effective_date        DATE,
  published_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
  published_by          VARCHAR(255)
);

-- One archive row per (document, version). This is what makes "never rewrite history"
-- a database guarantee rather than a convention someone has to remember.
CREATE UNIQUE INDEX IF NOT EXISTS uq_legal_document_versions
    ON legal_document_versions (legal_document_id, version);

CREATE INDEX IF NOT EXISTS idx_legal_document_versions_doc
    ON legal_document_versions (legal_document_id, version DESC) WHERE is_deleted = false;