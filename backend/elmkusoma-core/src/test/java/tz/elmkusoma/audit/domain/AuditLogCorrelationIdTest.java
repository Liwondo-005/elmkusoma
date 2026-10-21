package tz.elmkusoma.audit.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import tz.elmkusoma.config.security.RequestCorrelationFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AU-03 (Payment.md §58/§71): an audit row must carry the correlation id of the request that
 * produced it, so a support conversation can be joined to the exact line in the application
 * log.
 *
 * <p>The value is read from the MDC inside {@code @PrePersist} rather than passed in by each
 * caller, because more than a dozen services build audit rows directly through
 * {@code auditLogRepository.save(...)}. These tests pin that single choke point.</p>
 */
class AuditLogCorrelationIdTest {

    @AfterEach
    void clearMdc() {
        MDC.remove(AuditLog.CORRELATION_MDC_KEY);
    }

    @Test
    void mdcKey_MatchesTheFilterThatWritesIt() {
        // The entity reads a literal so a JPA entity need not depend on a servlet filter.
        // Renaming the key on one side alone would silently stop every audit row from
        // carrying it, so the two definitions are pinned together here.
        assertEquals(RequestCorrelationFilter.MDC_KEY, AuditLog.CORRELATION_MDC_KEY);
        assertEquals("correlationId", AuditLog.CORRELATION_MDC_KEY);
    }

    @Test
    void persisting_CarriesTheRequestCorrelationIdOntoTheRow() {
        MDC.put(AuditLog.CORRELATION_MDC_KEY, "7f3a1b2c-9d40-4e11-b6aa-0d5f2c8e1a77");

        AuditLog row = new AuditLog();
        row.onCreate();

        assertEquals("7f3a1b2c-9d40-4e11-b6aa-0d5f2c8e1a77", row.getCorrelationId());
        assertNotNull(row.getCreatedAt(), "the pre-existing createdAt stamp must still apply");
        assertEquals(Boolean.FALSE, row.getIsDeleted(), "the pre-existing default must still apply");
    }

    @Test
    void backgroundWriters_LeaveTheCorrelationIdNullRatherThanInventingOne() {
        // A scheduler, migration or import runs outside any request thread. NULL is a true
        // statement about the row; an invented or backfilled id would be a fabricated one.
        AuditLog row = new AuditLog();
        row.onCreate();

        assertNull(row.getCorrelationId());
        assertNotNull(row.getCreatedAt());
    }

    @Test
    void anExplicitlySuppliedId_IsNeverOverwritten() {
        MDC.put(AuditLog.CORRELATION_MDC_KEY, "from-the-request");

        AuditLog row = new AuditLog();
        row.setCorrelationId("supplied-by-the-caller");
        row.onCreate();

        assertEquals("supplied-by-the-caller", row.getCorrelationId());
    }

    @Test
    void anOverlongValue_IsBoundedToTheColumnSoTheInsertCannotFail() {
        // The filter already rejects anything beyond 64 characters, so this never fires in
        // practice. It exists so a rogue MDC value can never abort an audit insert and lose
        // the very row that would have explained the problem.
        MDC.put(AuditLog.CORRELATION_MDC_KEY, "x".repeat(200));

        AuditLog row = new AuditLog();
        row.onCreate();

        assertEquals(64, row.getCorrelationId().length());
        assertTrue(row.getCorrelationId().chars().allMatch(c -> c == 'x'));
    }

    @Test
    void aBlankValue_BecomesNullInsteadOfAnEmptyString() {
        MDC.put(AuditLog.CORRELATION_MDC_KEY, "     ");

        AuditLog row = new AuditLog();
        row.onCreate();

        assertNull(row.getCorrelationId());
    }
}
