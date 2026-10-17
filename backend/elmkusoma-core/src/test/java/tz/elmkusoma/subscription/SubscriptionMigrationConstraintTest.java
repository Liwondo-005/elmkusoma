package tz.elmkusoma.subscription;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Exercises the real V177 migration against a real PostgreSQL server.
 *
 * <p>This exists because a Hibernate-generated schema is not evidence. The
 * exclusion constraint in V177 is hand-written SQL, and {@code ddl-auto=create}
 * would happily build a {@code subscriptions} table with no constraint at all -
 * so a passing entity test proves nothing about whether overlapping paid periods
 * can actually be stored. Only a real migrated database answers that.</p>
 *
 * <p>The database is created from scratch and dropped again, and the test refuses
 * to run against anything but a disposable name. There is no code path here that
 * can reach the development or test databases.</p>
 */
class SubscriptionMigrationConstraintTest {

    /**
     * Hard allowlist. These are the only databases this test may ever touch, and
     * none of them is a developer or shared environment.
     */
    private static final Set<String> DISPOSABLE_DATABASES = Set.of("elmkusoma_iso_phasec");

    private static String databaseName;
    private static String jdbcUrl;
    private static String username;
    private static String password;
    private static String adminUrl;

    @BeforeAll
    static void applyRealMigrationChain() throws Exception {
        databaseName = System.getProperty("phasec.db", "elmkusoma_iso_phasec");
        if (!DISPOSABLE_DATABASES.contains(databaseName)) {
            fail("Refusing to migrate " + databaseName + ": it is not an approved disposable database");
        }
        String host = System.getProperty("phasec.host", "localhost");
        String port = System.getProperty("phasec.port", "5432");
        username = System.getProperty("phasec.user", "postgres");
        password = System.getenv("DB_PASSWORD");

        adminUrl = "jdbc:postgresql://" + host + ":" + port + "/postgres";
        jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;

        try (Connection admin = DriverManager.getConnection(adminUrl, username, password);
             Statement st = admin.createStatement()) {
            st.execute("DROP DATABASE IF EXISTS " + databaseName);
            st.execute("CREATE DATABASE " + databaseName);
        }

        // The real chain, through V177, with no Hibernate involved at all.
// V141 interpolates ${demoSeedDeactivate}; the application supplies it, so this
// test must too. "true" mirrors production, which never ships the demo logins.
Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .outOfOrder(true)
                .placeholders(java.util.Map.of("demoSeedDeactivate", "true"))
                .load();
        flyway.migrate();
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /** Inserts one ACTIVE period and returns its id. */
    private UUID insertPeriod(Connection c, UUID payer, String serviceType, UUID serviceId,
                              String periodStart, String periodEnd) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO subscriptions (id, student_id, payer_id, service_type, service_id, "
                        + "payment_id, status, period_start, period_end, created_at, is_deleted) "
                        + "VALUES (?,?,?,?,?,?,'ACTIVE',?::timestamp,?::timestamp, now(), false)")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, UUID.randomUUID());
            ps.setObject(3, payer);
            ps.setString(4, serviceType);
            ps.setObject(5, serviceId);
            ps.setObject(6, UUID.randomUUID());
            ps.setString(7, periodStart);
            ps.setString(8, periodEnd);
            ps.executeUpdate();
        }
        try (PreparedStatement back = c.prepareStatement(
                "SELECT id FROM subscriptions WHERE payer_id=? AND period_start=?::timestamp")) {
            back.setObject(1, payer);
            back.setString(2, periodStart);
            try (ResultSet rs = back.executeQuery()) {
                rs.next();
                return rs.getObject(1, UUID.class);
            }
        }
    }

    private void assertRejected(String why, SqlInsert insert) {
        SQLException ex = assertThrows(SQLException.class, insert::execute,
                "the database must refuse: " + why);
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("exclusion")
                        || ex.getMessage().toLowerCase(Locale.ROOT).contains("conflict")
                        || ex.getSQLState().startsWith("23"),
                "expected a constraint violation for " + why + " but got: " + ex.getMessage());
    }

    @Test
    void theCommittedV174SourceMatchesTheChecksumAlreadyAppliedInTheSharedDatabase() throws Exception {
        // V174 was applied to the shared development database before its source was committed to
        // Git, so there was a period where the database had executed a migration the repository did
        // not contain. If the committed file were not the file that was applied, every environment
        // would fail Flyway validation on V174, and it would only surface at startup.
        //
        // 797143152 is the checksum recorded for V174 in elmkusoma's flyway_schema_history. The
        // chain is applied here from the repository's own files with no Hibernate involved, so
        // comparing the two proves the committed source is the source that was applied.
        int applied;
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT checksum FROM flyway_schema_history WHERE version = '174'");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next(), "V174 must be recorded in the chain");
            applied = rs.getInt(1);
        }
        assertEquals(797143152, applied,
                "the committed V174 source no longer matches the checksum already applied in "
                        + "elmkusoma. Either the file was edited after being applied, or the "
                        + "recorded checksum belongs to different SQL. Do not edit an applied "
                        + "migration - recover the original source instead.");
    }

    @Test
    @DisplayName("V180 applies through the real Flyway chain")
    void theRealMigrationChainCompletes() throws Exception {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(
                "SELECT version FROM flyway_schema_history WHERE success "
                        + "ORDER BY installed_rank DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "the chain must have recorded something");
                assertEquals("180", rs.getString(1),
                        "the chain must reach V180, not stop earlier or skip it");
            }
    }

    @Test
    @DisplayName("btree_gist is installed by the migration under the test role")
    void btreeGistIsAvailableAndCreated() throws Exception {
        try (Connection c = open(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT count(*) FROM pg_extension WHERE extname='btree_gist'")) {
            rs.next();
            assertEquals(1, rs.getInt(1), "V177 depends on btree_gist and must install it");
        }
    }

    @Test
    @DisplayName("Adjacent half-open periods are accepted - this is what makes renewal work")
    void adjacentPeriodsAreAccepted() throws Exception {
        UUID payer = UUID.randomUUID();
        try (Connection c = open()) {
            c.setAutoCommit(false);
            insertPeriod(c, payer, "PREMIUM_ACCESS", null, "2026-01-01 00:00", "2026-02-01 00:00");
            // New period starts exactly where the first ended: [t1,t2) and [t2,t3).
            insertPeriod(c, payer, "PREMIUM_ACCESS", null, "2026-02-01 00:00", "2026-03-01 00:00");
            c.commit();
        }
    }

    @Test
    @DisplayName("Overlapping ACTIVE periods for the same payer and service are rejected")
    void overlappingPeriodsAreRejected() throws Exception {
        UUID payer = UUID.randomUUID();
        try (Connection c = open()) {
            c.setAutoCommit(false);
            insertPeriod(c, payer, "PREMIUM_ACCESS", null, "2026-01-01 00:00", "2026-03-01 00:00");
            assertRejected("a second period overlapping the first",
                    () -> insertPeriod(c, payer, "PREMIUM_ACCESS", null,
                            "2026-02-01 00:00", "2026-04-01 00:00"));
            c.rollback();
        }
    }

    @Test
    @DisplayName("Different payers and different services are independent")
    void differentPayersAndServicesDoNotCollide() throws Exception {
        UUID payer = UUID.randomUUID();
        UUID otherPayer = UUID.randomUUID();
        UUID serviceA = UUID.randomUUID();
        UUID serviceB = UUID.randomUUID();
        try (Connection c = open()) {
            c.setAutoCommit(false);
            insertPeriod(c, payer, "PREMIUM_ACCESS", serviceA, "2026-01-01 00:00", "2026-03-01 00:00");
            insertPeriod(c, payer, "PREMIUM_ACCESS", serviceB, "2026-01-01 00:00", "2026-03-01 00:00");
            insertPeriod(c, otherPayer, "PREMIUM_ACCESS", serviceA, "2026-01-01 00:00", "2026-03-01 00:00");
            c.commit();
        }
    }

    @Test
    @DisplayName("NULL service_id collides via the sentinel, instead of being silently distinct")
    void nullServiceIdCollidesWithAnotherNullServiceId() throws Exception {
        UUID payer = UUID.randomUUID();
        try (Connection c = open()) {
            c.setAutoCommit(false);
            insertPeriod(c, payer, "PREMIUM_ACCESS", null, "2026-01-01 00:00", "2026-03-01 00:00");
            // Without COALESCE this would silently succeed, because PostgreSQL treats
            // every NULL as distinct and the constraint would never fire.
            assertRejected("two overlapping tiers with no service_id",
                    () -> insertPeriod(c, payer, "PREMIUM_ACCESS", null,
                            "2026-02-01 00:00", "2026-04-01 00:00"));
            c.rollback();
        }
    }

    @Test
    @DisplayName("A duplicated payment_id is rejected, so a repeated callback cannot double-pay")
    void duplicatePaymentIdIsRejected() throws Exception {
        UUID payer = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        try (Connection c = open()) {
            c.setAutoCommit(false);
            insertWithPayment(c, payer, paymentId, "2026-01-01 00:00", "2026-02-01 00:00");
            assertRejected("the same payment opening a second period",
                    () -> insertWithPayment(c, payer, paymentId,
                            "2026-02-01 00:00", "2026-03-01 00:00"));
            c.rollback();
        }
    }

    private void insertWithPayment(Connection c, UUID payer, UUID paymentId,
                                   String start, String end) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO subscriptions (id, student_id, payer_id, service_type, service_id, "
                        + "payment_id, status, period_start, period_end, created_at, is_deleted) "
                        + "VALUES (?,?,?,?,?,?,'ACTIVE',?::timestamp,?::timestamp, now(), false)")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, UUID.randomUUID());
            ps.setObject(3, payer);
            ps.setString(4, "PREMIUM_ACCESS");
            ps.setObject(5, null);
            ps.setObject(6, paymentId);
            ps.setString(7, start);
            ps.setString(8, end);
            ps.executeUpdate();
        }
    }

    @Test
    @DisplayName("Two simultaneous renewals cannot both write the same period")
    void concurrentRenewalsAreSerialisedByTheDatabase() throws Exception {
        UUID payer = UUID.randomUUID();
        String anchor = "2026-01-01 00:00";
        try (Connection setup = open()) {
            setup.setAutoCommit(false);
            // The already-paid period both renewals will chain after.
            insertPeriod(setup, payer, "PREMIUM_ACCESS", null, anchor, "2026-02-01 00:00");
            setup.commit();
        }

        int threads = 2;
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(threads);
        java.util.concurrent.CountDownLatch go = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger accepted = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger refused = new java.util.concurrent.atomic.AtomicInteger();
        java.util.List<Thread> workers = new java.util.ArrayList<>();

        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                ready.countDown();
                try {
                    go.await();
                    // Both renewals compute the same chained start, so both would insert
                    // [2026-02-01, 2026-03-01). Exactly one may win.
                    try (Connection c = open()) {
                        c.setAutoCommit(false);
                        insertPeriod(c, payer, "PREMIUM_ACCESS", null,
                                "2026-02-01 00:00", "2026-03-01 00:00");
                        c.commit();
                        accepted.incrementAndGet();
                    } catch (SQLException e) {
                        refused.incrementAndGet();
                    }
                } catch (Exception ignored) {
                    // Counted as a refusal; the assertion below is what matters.
                }
            });
            workers.add(t);
            t.start();
        }

        ready.await();
        go.countDown();
        for (Thread t : workers) {
            t.join(30_000);
        }

        assertEquals(1, accepted.get(),
                "exactly one concurrent renewal may create the next period");
        assertEquals(threads - 1, refused.get(),
                "the loser must be refused by the exclusion constraint, not silently written");
    }

    @FunctionalInterface
    private interface SqlInsert {
        void execute() throws SQLException;
    }
}