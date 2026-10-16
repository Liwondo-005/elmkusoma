-- V173: Event monetisation - a paid event may only be registered for against a confirmed payment.
--
-- Additive only. Nothing existing is altered, renamed or rewritten, so applying it over live data
-- cannot lose a registration.
--
-- WHY
--
-- Events carried a boolean is_free and a capacity, but no price and no link to the payment that
-- took the money. That means a paid event and a free event were indistinguishable to the payment
-- domain: nothing tied an attendee's seat to a confirmed transaction, nothing on the seat could be
-- reconciled against the ledger, and nothing recorded how much was collected for an event. The
-- financial record and the attendance record could therefore disagree with no way to tell which
-- was right.
--
-- WHAT
--
-- events.ticket_price / ticket_currency
--     The price of one seat. NULL means the event's existing is_free flag governs it and nothing
--     is charged; a value means each confirmed seat costs that amount. Price is never derived
--     from the client and never recomputed at registration - the stored value is the one that is
--     charged and the one that appears on the receipt.
--
-- event_registrations.payment_id
--     The payment that paid for this seat. Kept as a plain uuid rather than a foreign key for the
--     same reason the rest of the payment domain does: it records a relationship without adding a
--     hard constraint that would make a refund or an archival job fail on an ordering assumption.
--
-- event_registrations.amount_paid
--     What was actually taken for this seat at the moment the seat was confirmed, snapshotted.
--     A later price change must not restate what a particular attendee paid.
--
-- event_registrations.seats
--     How many seats this registration holds, for an organiser buying several at once. Defaults
--     to 1 so every existing registration keeps its current meaning. Capacity is then checked
--     against the SUM of seats rather than a row count, which is what makes a group booking
--     actually consume the seats it bought.
--
-- ADDITIVE GUARD
--
-- chk_event_registration_seats_positive stops a zero or negative seat count, which would
-- otherwise let a group booking consume no capacity at all while still being counted as a
-- registration.
--
-- No existing row is modified. Every current registration defaults to one seat and no payment,
-- which is exactly what it meant before this migration.

ALTER TABLE events
    ADD COLUMN IF NOT EXISTS ticket_price NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS ticket_currency VARCHAR(10);

ALTER TABLE event_registrations
    ADD COLUMN IF NOT EXISTS payment_id UUID,
    ADD COLUMN IF NOT EXISTS amount_paid NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS seats INTEGER NOT NULL DEFAULT 1;

ALTER TABLE event_registrations
    DROP CONSTRAINT IF EXISTS chk_event_registration_seats_positive;

ALTER TABLE event_registrations
    ADD CONSTRAINT chk_event_registration_seats_positive CHECK (seats > 0);

CREATE INDEX IF NOT EXISTS idx_event_registrations_payment
    ON event_registrations (payment_id)
    WHERE payment_id IS NOT NULL AND is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_events_paid
    ON events (institution_id, starts_at)
    WHERE is_free = false AND is_deleted = false;