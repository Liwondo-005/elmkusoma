-- RENUMBERED ON MERGE: this file is V163 on origin/main and V167 here.
--
-- The certificate branch independently claimed V161-V164, and a shared database has already
-- applied those under our names and checksums, so the incoming side moved instead: ours keep
-- the versions the database recorded, these apply fresh afterwards. V141 keeps its original
-- number because spring.flyway.out-of-order is enabled and it is meant to sort below V156.
-- Communication Centre idempotency: safe retry for conversation creation and message submission.
--
-- A double-click, a flaky mobile connection, or a client timeout used to create a second ticket
-- or a second message every time. The application-only "does this already exist" check cannot
-- survive two requests racing, so the guarantee lives in a UNIQUE constraint instead: only one
-- row per (actor, key) can ever be committed.
--
-- Scope. The key is namespaced by the actor, never global. Two people whose browsers happen to
-- mint the same UUID must not collide, so the actor is part of the identity. Operation is
-- deliberately NOT part of it: a key is a token the client owns, so reusing it for a different
-- operation is a client bug rather than a fresh claim, and is refused instead of quietly
-- opening a second row under the same name.
--
-- Anonymous submission. The public contact form has no authenticated actor, so it is scoped by
-- the caller's address instead. That is the strongest identity available without inventing a
-- user per anonymous visitor, and it is the same scoping the form's rate limit already uses.
--
-- Conflicting reuse. A stored request_hash pins the payload the key was first used with. The
-- same key replayed with a different body is rejected rather than silently returning a result
-- computed from the wrong content.
--
-- Only a hash is kept. The request body is never persisted here: a contact enquiry or a support
-- message may contain personal data, and a retry ledger is not a place to retain a second copy
-- of it. A hash answers "is this the same request?" without holding the content.
--
-- Retention. Rows carry expires_at and are swept by the scheduler, so the ledger cannot grow
-- without bound. The window only has to outlast the retry horizon of a client, not forever.

CREATE TABLE IF NOT EXISTS communication_idempotency_keys (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  -- BaseEntity columns, kept so the entity matches the schema exactly.
  institution_id      UUID,
  updated_at          TIMESTAMP,
  created_by          VARCHAR(255),
  updated_by          VARCHAR(255),
  is_deleted          BOOLEAN NOT NULL DEFAULT false,

  -- The authenticated user's id, or "ip:<address>" for an anonymous submission.
  actor_key           VARCHAR(64)  NOT NULL,

  -- TICKET_CREATE | TICKET_MESSAGE | ENQUIRY_REPLY | ENQUIRY_CREATE
  operation           VARCHAR(32)  NOT NULL,

  idempotency_key     VARCHAR(100) NOT NULL,

  -- SHA-256 of the canonical request payload. Never the payload itself.
  request_hash        VARCHAR(64)  NOT NULL,

  -- What the first successful run produced, so a retry returns the original result rather
  -- than an empty success. NULL until the work has actually committed.
  resource_id         UUID,
  resource_text       VARCHAR(64),

  -- LIFECYCLE: a claimed key with no result is a crashed request; it is deleted, not honoured.
  completed_at        TIMESTAMP,

  created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
  expires_at          TIMESTAMP NOT NULL,

  -- The concurrency guarantee. An application-level pre-check cannot provide this: two
  -- simultaneous retries both pass a SELECT and both insert without it.
  CONSTRAINT uq_comm_idem_actor_key UNIQUE (actor_key, idempotency_key)
);

-- Retention sweep reads by expiry.
CREATE INDEX IF NOT EXISTS idx_comm_idem_expires
  ON communication_idempotency_keys (expires_at)
  WHERE completed_at IS NOT NULL;
