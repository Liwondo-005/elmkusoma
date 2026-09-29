-- Additive only: declares which live source a teacher intends to broadcast from
-- (browser/laptop camera, phone, USB camera, professional camera via capture card,
-- OBS, hardware encoder, studio, other). The laptop camera is one source among
-- many; every pre-existing row keeps working as a browser-sourced session.
ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS broadcast_source VARCHAR(30) DEFAULT 'BROWSER';

-- Backfill so reads never see NULL and the JPA default matches stored data.
UPDATE live_classes SET broadcast_source = 'BROWSER' WHERE broadcast_source IS NULL;

COMMENT ON COLUMN live_classes.broadcast_source IS
    'Declared broadcast source: BROWSER, MOBILE, USB_CAMERA, PROFESSIONAL_CAMERA, OBS, ENCODER, STUDIO, OTHER';
