package tz.elmkusoma.audit.service;

import java.util.UUID;

/**
 * The institution id used to record platform-scoped activity.
 *
 * <p>{@code audit_logs.institution_id} is NOT NULL: the audit trail is partitioned by institution
 * so a school can read its own history and no one else's. That makes it impossible to record an
 * event that genuinely belongs to no school.</p>
 *
 * <p>Some events really are platform-scoped rather than tenant-scoped. The clearest case is a
 * failed public certificate verification: the code did not match any certificate, so there is no
 * institution to attribute it to, yet the attempt is precisely the kind of event an operator needs
 * to see. Passing null there does not record it platform-wide - it violates the constraint, the
 * insert fails, and the failure is swallowed, so the event is lost.</p>
 *
 * <p>This is the same sentinel {@code PlatformAdminService} already uses, lifted to shared
 * constants so the two cannot drift. It resolves to the real "ELMKUSOMA National HQ" institution
 * row, so these entries are readable in the normal audit UI rather than hidden.</p>
 */
public final class PlatformAuditScope {

    /** Institution id of ELMKUSOMA National HQ, used as the platform audit scope. */
    public static final UUID PLATFORM_INSTITUTION_ID =
            UUID.fromString("a0000000-0000-0000-0000-000000000001");

    private PlatformAuditScope() {
    }
}
