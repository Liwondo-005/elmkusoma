-- RENUMBERED ON MERGE: this file is V162 on origin/main and V166 here.
--
-- The certificate branch independently claimed V161-V164, and a shared database has already
-- applied those under our names and checksums, so the incoming side moved instead: ours keep
-- the versions the database recorded, these apply fresh afterwards. V141 keeps its original
-- number because spring.flyway.out-of-order is enabled and it is meant to sort below V156.
-- V162: persist "a stop was requested for the current capture" on the class row.
--
-- Why a column: `live_classes.recording_url` holds the `egress:<id>` marker for BOTH states
-- that look identical to a reader - a capture that is still running, and a capture whose stop
-- was accepted but whose file has not finished uploading yet. Host-state therefore had to
-- guess from `status`, so a teacher who pressed Stop and reloaded while the class was still
-- live was told `recordingActive: true` and offered a Stop button for a job LiveKit had
-- already finished.
--
-- The flag is written only when a stop request has actually been ACCEPTED (StopEgress
-- succeeded, or LiveKit reported the job already final). It is reset whenever a new
-- `egress:<id>` marker is written, and it is only ever READ while that marker is present, so
-- a value left behind after publication can never influence a later recording.
--
-- Additive, NOT NULL with a DEFAULT, so every existing row (and every raw INSERT that does
-- not name the column, e.g. the test seeder) keeps working unchanged.
ALTER TABLE live_classes
    ADD COLUMN IF NOT EXISTS recording_stop_requested BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN live_classes.recording_stop_requested IS
    'TRUE once a stop request for the current egress:<id> marker was accepted. Read only while the marker is present: it separates "capture running" from "capture stopping, file still finalising".';
