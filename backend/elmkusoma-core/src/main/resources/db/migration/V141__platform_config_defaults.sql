-- Restore the column defaults that V63 declared for platform_config.
--
-- Version numbering. This is placed at V141, a gap in an otherwise contiguous 1..160
-- sequence, and that is deliberate and load-bearing:
--
--   * It MUST be below 156. V156 is the migration this exists to unblock, and Flyway
--     applies pending migrations in ascending version order, so a plain V161 would sort
--     AFTER V156 and could never help it. 128-130 and 140-141 are the only free integers
--     below 156; 141 is the highest, closest to the migration it protects.
--   * It MUST be an integer version. FlywayMigrationValidationTest enforces the naming
--     convention ^V(\d+)__description\.sql and parses versions with Integer.parseInt,
--     so a dotted "V155.1" insert is not permitted here.
--   * `spring.flyway.out-of-order: true` is already set in application.yml, so this lower
--     version still applies to a database that has already migrated past 141. Without
--     that setting this migration would be silently skipped on every existing database.
--   * 141 is confirmed absent both from this repository and from flyway_schema_history,
--     so it collides with nothing.
--
-- Why this exists.
--
-- V63__platform_admin_governance_tables.sql creates the table with a default on every
-- NOT NULL column it declares:
--     id           UUID        NOT NULL DEFAULT gen_random_uuid()
--     config_type  VARCHAR(30) NOT NULL DEFAULT 'STRING'
--     is_sensitive BOOLEAN     NOT NULL DEFAULT FALSE
--     is_public    BOOLEAN     NOT NULL DEFAULT FALSE
--     created_at   TIMESTAMP   NOT NULL DEFAULT NOW()
--     is_deleted   BOOLEAN     NOT NULL DEFAULT FALSE
--
-- V156__news_articles.sql seeds a row supplying only
--     (config_key, config_value, config_type, description, category, is_sensitive, is_public)
-- and relies on those defaults for everything else - `id`, `created_at` and `is_deleted`
-- in particular. Every other platform_config insert in this repository (V63, V69, V70,
-- V152, V158) supplies its columns explicitly, so V156 is the only one that depends on
-- the defaults - and V156 cannot be edited, because it has already succeeded on databases
-- where the defaults were present and changing it would invalidate their checksums.
-- (V142 also omits them, but its insert is guarded by WHERE ${demoSeedDeactivate}, which
-- is "false", so it never inserts a row and the missing defaults have never mattered.)
--
-- On a database built by Flyway from scratch, V63 ran, the defaults exist, and this
-- migration changes nothing.
--
-- On a database whose history seeded V48-V66 as "pre-existing" migrations (see the
-- validate-on-migrate note in application.yml), V63's CREATE TABLE never executed,
-- because platform_config already existed - created by Hibernate DDL, which emits
--     created_at timestamp(6) NOT NULL,
--     is_deleted boolean NOT NULL,
-- with no defaults. On such a database V156 fails with
--     ERROR: null value in column "id" of relation "platform_config" violates not-null constraint
-- and, because validate-on-migrate is enabled, the whole application refuses to boot.
--
-- All six V63-declared defaults are restored together rather than only the three V156
-- needs today, so the next migration to rely on V63's declared shape does not fail the
-- same way one column at a time. Setting a default is additive and idempotent: it touches
-- no existing row, no constraint and no index, and on a healthy database it simply
-- re-asserts what is already there.
--
-- Note the same drift affects other tables from the seeded range (platform_notifications
-- .id, for example); only platform_config is repaired here, because it is the one an
-- executed migration actually depends on. Fixing the rest would be speculative.

ALTER TABLE platform_config ALTER COLUMN id           SET DEFAULT gen_random_uuid();
ALTER TABLE platform_config ALTER COLUMN config_type  SET DEFAULT 'STRING';
ALTER TABLE platform_config ALTER COLUMN is_sensitive SET DEFAULT FALSE;
ALTER TABLE platform_config ALTER COLUMN is_public    SET DEFAULT FALSE;
ALTER TABLE platform_config ALTER COLUMN created_at   SET DEFAULT NOW();
ALTER TABLE platform_config ALTER COLUMN is_deleted   SET DEFAULT FALSE;
