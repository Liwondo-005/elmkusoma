-- V134: events.rescheduled_from must be uuid, not timestamp.
--
-- Event.rescheduledFrom is a java.util.UUID (the id of the event this one was
-- rescheduled from), mapped to this column. V71 declared it UUID, but V75 then
-- declared it TIMESTAMP (V77 documents the conflict), so the live schema carried
-- a timestamp. Hibernate binds the parameter with the entity's type, therefore
-- EVERY insert into events failed with
--
--   ERROR: column "rescheduled_from" is of type timestamp without time zone
--          but expression is of type uuid
--
-- which the global handler surfaced as a bare HTTP 500 ("An unexpected error
-- occurred" / "Failed to create new event") on POST /v1/events. It fired even
-- when the form left the field empty, because the typed NULL parameter is still
-- sent. Reading worked, so events appeared normal until creation was attempted.
--
-- The conversion goes through text so environments that ran V75 and stored a
-- timestamp there do not abort the migration. Such values are cleared: the
-- application type is UUID, so a timestamp in this column could never be read
-- back (it would fail deserialisation), i.e. it was already unusable data.

ALTER TABLE events
    ALTER COLUMN rescheduled_from TYPE text USING rescheduled_from::text;

UPDATE events
SET rescheduled_from = NULL
WHERE rescheduled_from IS NOT NULL
  AND rescheduled_from !~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$';

ALTER TABLE events
    ALTER COLUMN rescheduled_from TYPE uuid USING rescheduled_from::uuid;