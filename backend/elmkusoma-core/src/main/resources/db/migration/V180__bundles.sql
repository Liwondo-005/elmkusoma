-- V180: bundles / packages.
--
-- A bundle is one purchasable thing that grants several services. Nothing existed for
-- this: no bundle or package concept appears anywhere in the backend, so this is a
-- genuinely new domain rather than an unfinished one.
--
-- Deliberate design decisions, each of which prevents a specific wrong outcome:
--
--  * NO PRICE COLUMN. A bundle is just another service type. Its price is resolved
--    through PricingService.resolvePrice(serviceType) exactly like any other
--    product, so pricing governance keeps applying to bundles. Storing a price here
--    would quietly create a second, ungoverned pricing path.
--
--  * status lives on the bundle and is enforced at purchase time, not just hidden in a
--    listing. A DRAFT or SUSPENDED bundle must refuse the sale outright; hiding it
--    from a menu is presentation, not a rule.
--
--  * access_days carries the duration so the granted entitlements know how long they
--    run. It is snapshotted onto the payment at initiation like any other duration, so
--    a later edit here cannot restate what someone already paid for.
--
--  * items are ordered and unique per bundle+service, so a bundle cannot silently
--    contain the same service twice and grant it twice.

CREATE TABLE IF NOT EXISTS bundles (
    id              uuid PRIMARY KEY,
    institution_id  uuid,
    name            varchar(200) NOT NULL,
    description     text,
    -- The pricing key. Resolved through the pricing service, never hard-coded here.
    service_type    varchar(100) NOT NULL,
    access_days     integer,
    status          varchar(32) NOT NULL DEFAULT 'DRAFT',
    created_at      timestamp   NOT NULL,
    updated_at      timestamp,
    created_by      varchar(255),
    updated_by      varchar(255),
    is_deleted      boolean     NOT NULL DEFAULT false,
    CONSTRAINT bundles_status_ck CHECK (status IN ('DRAFT', 'PUBLISHED', 'SUSPENDED')),
    -- A published bundle must be purchasable, which means it needs a duration to
    -- grant. Publishing without one is refused rather than selling access with an
    -- unknown end date.
    CONSTRAINT bundles_published_duration_ck
        CHECK (status <> 'PUBLISHED' OR (access_days IS NOT NULL AND access_days > 0))
);

CREATE INDEX IF NOT EXISTS idx_bundles_institution
    ON bundles (institution_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_bundles_service_type
    ON bundles (service_type)
    WHERE is_deleted = false AND status = 'PUBLISHED';

CREATE TABLE IF NOT EXISTS bundle_items (
    id           uuid PRIMARY KEY,
    bundle_id    uuid        NOT NULL REFERENCES bundles (id),
    -- Each item names a real service. service_id is what the entitlement grant keys on,
    -- so an item pointing at nothing would grant access to nothing.
    service_type varchar(100) NOT NULL,
    service_id   uuid        NOT NULL,
    position     integer     NOT NULL DEFAULT 0,
    is_deleted   boolean     NOT NULL DEFAULT false,
    CONSTRAINT bundle_items_unique_service UNIQUE (bundle_id, service_type, service_id)
);

CREATE INDEX IF NOT EXISTS idx_bundle_items_bundle
    ON bundle_items (bundle_id)
    WHERE is_deleted = false;