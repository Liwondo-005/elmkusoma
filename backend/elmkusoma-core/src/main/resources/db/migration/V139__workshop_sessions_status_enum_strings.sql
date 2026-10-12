-- V127: Align workshop_sessions enum columns with the JPA mappings.
-- V57 created status/workshop_type as smallint (legacy ordinal mapping) with
-- ordinal-range CHECKs, but the entity maps both with
-- @Enumerated(EnumType.STRING) and the service writes name() values
-- ('SCHEDULED'..., 'WORKSHOP'...), so every INSERT failed with
-- "column ... is of type smallint but expression is of type character varying".
-- The legacy CHECKs also block ALTER TYPE (they re-validate as
-- varchar >= integer), so they are dropped with the ordinal mapping.
-- The table has no rows (verified at time of writing), so no data is lost.

ALTER TABLE workshop_sessions
    DROP CONSTRAINT IF EXISTS workshop_sessions_status_check;

ALTER TABLE workshop_sessions
    DROP CONSTRAINT IF EXISTS workshop_sessions_workshop_type_check;

ALTER TABLE workshop_sessions
    ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

ALTER TABLE workshop_sessions
    ALTER COLUMN workshop_type TYPE VARCHAR(30) USING workshop_type::text;
