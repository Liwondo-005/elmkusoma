-- Public news & announcements.
--
-- A genuinely new capability. The audit found no news, article, notice or CMS table, entity,
-- service or endpoint anywhere in the schema or the Java sources, and no slug handling at all.
--
-- This is deliberately NOT a widening of the existing `announcements` table. That table is an
-- internal, authenticated, institution-scoped notice feed for learners and teachers: it has no
-- slug, no summary, no cover image, no featured flag and no expiry, and its rows are addressed
-- by audience region/district rather than being public editorial content. Merging public
-- marketing content into it would force both to compromise on visibility rules.
--
-- Follows the same working-copy discipline as legal_documents from V152: `status` and
-- `published_at` are the only things the public endpoint reads, and the public query enforces
-- them server-side rather than trusting the client to filter.
--
-- `body` is PLAIN TEXT, matching the decision already taken for legal documents. This build has
-- no HTML sanitiser dependency, so rather than accept markup and hope a filter catches it,
-- markup is refused outright and the client renders body text as React text nodes. Stored XSS
-- is structurally impossible rather than filtered.

CREATE TABLE IF NOT EXISTS news_articles (
  id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  institution_id     UUID,
  created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at         TIMESTAMP,
  created_by         VARCHAR(255),
  updated_by         VARCHAR(255),
  is_deleted         BOOLEAN NOT NULL DEFAULT false,

  title              VARCHAR(200) NOT NULL,
  summary            VARCHAR(500) NOT NULL,
  body               TEXT         NOT NULL,

  -- Human-quotable URL segment. Unique among live rows so /news/{slug} is unambiguous.
  slug               VARCHAR(160) NOT NULL,

  category           VARCHAR(40),
  cover_image_url    VARCHAR(1000),
  author_name        VARCHAR(160),

  -- DRAFT | PUBLISHED | ARCHIVED. Only PUBLISHED is ever returned by a public endpoint.
  status             VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

  -- Set by the server on publish, never accepted from a client. A fabricated publishedAt in a
  -- request body cannot make an article "new", because this column is what NEW is measured from.
  published_at       TIMESTAMP,

  -- Publish now, but not before this instant. A scheduled article is not public until it is due.
  scheduled_at       TIMESTAMP,

  -- Once passed, the article stops appearing publicly without anyone having to unpublish it.
  expires_at         TIMESTAMP,

  -- A deliberate editorial choice. Never derived from recency: a brand new article is not
  -- automatically featured, which is the whole point of keeping the two flags separate.
  is_featured        BOOLEAN NOT NULL DEFAULT false,

  -- NORMAL | IMPORTANT | URGENT. Importance is independent of both recency and featured.
  priority           VARCHAR(20) NOT NULL DEFAULT 'NORMAL',

  -- Optional manual ordering inside a featured/priority band. Ties fall through to published_at
  -- and then id, so the listing order is fully deterministic.
  sort_order         INTEGER NOT NULL DEFAULT 0,

  last_modified_by   VARCHAR(255),
  published_by       VARCHAR(255)
);

-- Unique among rows that have not been soft-deleted. A partial unique index rather than a table
-- constraint because soft-deleted rows keep their slug for audit purposes and must not block a
-- future article from reusing it.
CREATE UNIQUE INDEX IF NOT EXISTS uq_news_articles_slug
    ON news_articles (slug) WHERE is_deleted = false;

-- Serves the public listing: status + published_at DESC, which is the secondary sort key.
CREATE INDEX IF NOT EXISTS idx_news_articles_public
    ON news_articles (status, published_at DESC) WHERE is_deleted = false;

-- Serves "is anything publishable yet" checks and the NEW-window scan without a full scan.
CREATE INDEX IF NOT EXISTS idx_news_articles_published_at
    ON news_articles (published_at DESC) WHERE is_deleted = false AND status = 'PUBLISHED';

-- Serves admin search/filter on status.
CREATE INDEX IF NOT EXISTS idx_news_articles_status
    ON news_articles (status) WHERE is_deleted = false;

-- The NEW window is configuration rather than a constant, so an administrator can change how
-- long the NEW badge lasts without a code change or a redeploy. Seeded to 7 days.
--
-- Kept off the public settings endpoint: it is internal behaviour, not site configuration a
-- visitor should be able to read or edit, and public.site-settings returns an explicit key
-- allow-list regardless of is_public.
INSERT INTO platform_config
    (config_key, config_value, config_type, description, category, is_sensitive, is_public)
VALUES
    ('news.newWindowDays', '7', 'INTEGER',
     'How many days after publication a news article is shown with the NEW indicator. Set to 0 to disable the badge entirely.',
     'news', false, false)
ON CONFLICT (config_key) DO UPDATE
    SET config_value = EXCLUDED.config_value,
        description  = EXCLUDED.description,
        category     = EXCLUDED.category,
        updated_at   = NOW();