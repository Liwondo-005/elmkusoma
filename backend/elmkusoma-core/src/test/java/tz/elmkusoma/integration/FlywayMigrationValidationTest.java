package tz.elmkusoma.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * §84 — Flyway migration validation without mutating the shared test database
 * (spring.flyway.enabled=false under application-test.properties; Hibernate uses
 * ddl-auto=create-drop). Validates the classpath migration set the same way a
 * Flyway resolve/validate pass would: naming convention, unique monotonic versions,
 * required D03 changesets present, applied-migration overlap (V71/V75) is
 * IF-NOT-EXISTS idempotent, and V77 is a comment-only no-op.
 */
class FlywayMigrationValidationTest {

    private static final Pattern MIGRATION_NAME = Pattern.compile("^V(\\d+)__(.+)\\.sql$");
    private static final Pattern REQUIRED = Pattern.compile(
            "^(V71__event_d03_extended_columns|V75__events_entity_columns|"
                    + "V76__replay_tables|V77__dedupe_event_columns|V78__replay_caption_url)\\.sql$");

    private static Path migrationDir;
    private static List<Path> migrationFiles;

    /** Migration file:line of the last declaration per "table.column" (shared helper output). */
    private Map<String, String> declaredIn;

    @BeforeAll
    static void locateMigrations() throws Exception {
        URL url = FlywayMigrationValidationTest.class.getClassLoader().getResource("db/migration");
        if (url != null && "file".equals(url.getProtocol())) {
            migrationDir = Path.of(url.toURI());
        } else {
            migrationDir = Path.of("src/main/resources/db/migration");
        }
        assertTrue(Files.isDirectory(migrationDir), "migration directory must exist: " + migrationDir);
        try (Stream<Path> stream = Files.list(migrationDir)) {
            migrationFiles = stream
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList();
        }
    }

    @Test
    void migrations_FollowFlywayNamingConvention() {
        assertFalse(migrationFiles.isEmpty(), "no migrations found under " + migrationDir);
        for (Path file : migrationFiles) {
            String name = file.getFileName().toString();
            assertTrue(MIGRATION_NAME.matcher(name).matches(),
                    "migration must match V<version>__description.sql: " + name);
        }
    }

    @Test
    void migrationVersions_AreUniqueAndMonotonic() {
        Set<Integer> versions = new TreeSet<>();
        for (Path file : migrationFiles) {
            Matcher m = MIGRATION_NAME.matcher(file.getFileName().toString());
            assertTrue(m.find(), "bad name: " + file);
            int version = Integer.parseInt(m.group(1));
            assertTrue(versions.add(version), "duplicate migration version V" + version);
        }
        List<Integer> sorted = new ArrayList<>(versions);
        for (int i = 1; i < sorted.size(); i++) {
            assertTrue(sorted.get(i) > sorted.get(i - 1),
                    "versions must be strictly increasing: V" + sorted.get(i - 1)
                            + " then V" + sorted.get(i));
        }
        assertTrue(versions.contains(71) && versions.contains(75)
                        && versions.contains(76) && versions.contains(77) && versions.contains(78),
                "D03 migrations V71/V75/V76/V77/V78 must all be present (found " + versions.size() + ")");
    }

    @Test
    void requiredD03Migrations_Present() {
        Set<String> names = new HashSet<>();
        for (Path file : migrationFiles) {
            names.add(file.getFileName().toString());
        }
        for (String required : new String[]{
                "V71__event_d03_extended_columns.sql",
                "V75__events_entity_columns.sql",
                "V76__replay_tables.sql",
                "V77__dedupe_event_columns.sql",
                "V78__replay_caption_url.sql"}) {
            assertTrue(names.contains(required), "missing required migration: " + required);
        }
    }

    @Test
    void appliedOverlap_V71AndV75_IsIdempotent() throws IOException {
        for (String name : new String[]{
                "V71__event_d03_extended_columns.sql",
                "V75__events_entity_columns.sql"}) {
            Path file = migrationDir.resolve(name);
            assertTrue(Files.exists(file), "missing " + name);
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                if (trimmed.toUpperCase().startsWith("ALTER TABLE")
                        && trimmed.toUpperCase().contains("ADD COLUMN")) {
                    assertTrue(trimmed.toUpperCase().contains("IF NOT EXISTS"),
                            name + " must use ADD COLUMN IF NOT EXISTS (idempotent overlap): " + trimmed);
                }
                if (trimmed.toUpperCase().startsWith("CREATE TABLE")
                        || trimmed.toUpperCase().startsWith("CREATE INDEX")) {
                    assertTrue(trimmed.toUpperCase().contains("IF NOT EXISTS"),
                            name + " must use IF NOT EXISTS: " + trimmed);
                }
            }
        }
    }

    @Test
    void v77_DedupeDecision_IsCommentOnlyNoOp() throws IOException {
        Path v77 = migrationDir.resolve("V77__dedupe_event_columns.sql");
        assertTrue(Files.exists(v77), "V77__dedupe_event_columns.sql must exist (§83)");
        String content = Files.readString(v77, StandardCharsets.UTF_8);
        assertTrue(content.contains("V71") && content.contains("V75"),
                "V77 must document the V71/V75 overlap");

        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            String upper = trimmed.toUpperCase();
            assertFalse(upper.startsWith("ALTER ") || upper.startsWith("CREATE ")
                            || upper.startsWith("DROP ") || upper.startsWith("TRUNCATE ")
                            || upper.startsWith("INSERT ") || upper.startsWith("UPDATE ")
                            || upper.startsWith("DELETE "),
                    "V77 must not mutate schema/data (comment-only no-op): " + trimmed);
        }
    }

    /**
     * BaseEntity.createdBy / updatedBy are Strings that AuditListener fills with
     * authentication.getName(), and the rest of the codebase reads them as such
     * (userRepository.findByEmailAndIsDeletedFalse). If a table's audited actor
     * columns end up as a non-character type, every insert against that table
     * aborts with "column ... is of type <t> but expression is of type character
     * varying" and the user only sees a bare HTTP 500 - which is exactly what
     * saving a course from /dashboard/admin/courses produced until V128.
     *
     * The check is timeline-aware: migrations are replayed in version order and
     * only the *effective* type counts, because this drift has been introduced
     * and then corrected repeatedly (V81, V91, V118, V127, V128).
     *
     * It cannot be caught by a persistence test because the test profile runs
     * with spring.flyway.enabled=false and ddl-auto=create-drop, so Hibernate
     * builds the schema from the entity and never sees the migration's type.
     */
    @Test
    void auditedActorColumns_AreCharacterTypesInMigrations() throws IOException {
        Map<String, String> effectiveType = effectiveColumnTypes(Set.of("created_by", "updated_by"));

        List<String> offenders = new ArrayList<>();
        for (Map.Entry<String, String> entry : effectiveType.entrySet()) {
            String type = entry.getValue();
            boolean character = type.startsWith("character") || type.startsWith("varchar")
                    || type.startsWith("text") || type.startsWith("citext");
            if (!character) {
                offenders.add(entry.getKey() + " = " + type
                        + " (last declared at " + declaredIn.get(entry.getKey()) + ")");
            }
        }
        assertTrue(offenders.isEmpty(),
                "audited actor columns (BaseEntity.createdBy/updatedBy are Strings filled with "
                        + "authentication.getName()) must resolve to a character type after all migrations run, "
                        + "otherwise every audited insert fails with a bare HTTP 500. Offenders: " + offenders);
    }

    /**
     * Event.rescheduledFrom / organizerId / institutionId are java.util.UUID. The
     * same mechanism (typed parameters, bare HTTP 500 on write) applies when the
     * migration leaves such a column as a timestamp: V71 declared
     * events.rescheduled_from UUID and V75 re-declared it TIMESTAMP, so every
     * POST /v1/events failed with
     * "column rescheduled_from is of type timestamp without time zone but
     * expression is of type uuid" until V129 corrected it. The failure fires even
     * when the field is left empty, because the typed NULL parameter is still sent.
     */
    @Test
    void uuidReferenceColumns_ResolveToUuidInMigrations() throws IOException {
        Set<String> watched = Set.of("rescheduled_from", "organizer_id", "institution_id");
        Map<String, String> effectiveType = effectiveColumnTypes(watched);

        List<String> offenders = new ArrayList<>();
        for (Map.Entry<String, String> entry : effectiveType.entrySet()) {
            // Only the events.* references are UUID-typed on the entity side.
            if (!entry.getKey().startsWith("events.")) {
                continue;
            }
            if (!watched.contains(entry.getKey().substring("events.".length()))) {
                continue;
            }
            if (!entry.getValue().startsWith("uuid")) {
                offenders.add(entry.getKey() + " = " + entry.getValue()
                        + " (last declared at " + declaredIn.get(entry.getKey())
                        + ") but the entity maps it as java.util.UUID");
            }
        }
        assertTrue(offenders.isEmpty(),
                "events columns mapped as java.util.UUID on the entity must resolve to uuid after all "
                        + "migrations run, otherwise every insert into events fails with a bare HTTP 500. "
                        + "Offenders: " + offenders);
    }

    /**
     * live_classes.started_at is the authoritative expiry baseline (V135). The unit tests run
     * with spring.flyway.enabled=false and ddl-auto=create-drop, so Hibernate builds the schema
     * from the entity and the migration can silently go missing while every test still passes -
     * the exact failure mode that produced bare HTTP 500s on courses.created_by and
     * events.rescheduled_from. If the migration is dropped or mistyped, every startSession and
     * expiry sweep fails at runtime against a real database. This replays the migrations to
     * prove the column exists, is a timestamp, and stays nullable (SCHEDULED and legacy active
     * rows have no actual start).
     */
    @Test
    void liveClassesStartedAt_IsDeclaredAsNullableTimestampInMigrations() throws IOException {
        Pattern addColumn = ADD_COLUMN;

        String declaredType = null;
        String declaredIn = null;
        boolean seenNotNull = false;

        List<Path> ordered = migrationFiles.stream()
                .sorted(Comparator.comparingInt(FlywayMigrationValidationTest::versionOf))
                .toList();

        for (Path file : ordered) {
            // Collapse each file into whitespace-normalised statements: an
            // "ALTER TABLE live_classes" header and its "ADD COLUMN ..." body are
            // conventionally written on separate lines, which a line-by-line scan misses.
            StringBuilder normalized = new StringBuilder();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                normalized.append(trimmed).append(' ');
            }
            String body = normalized.toString();

            Matcher added = addColumn.matcher(body);
            if (added.find()
                    && unquote(added.group(1)).equalsIgnoreCase("live_classes")
                    && added.group(2).equalsIgnoreCase("started_at")) {
                declaredType = normalizeDeclaredType(added.group(3));
                declaredIn = file.getFileName().toString();
                // Scope NOT NULL detection to this statement, not the whole file.
                int stmtEnd = body.indexOf(';', added.start());
                String statement = stmtEnd < 0 ? body.substring(added.start())
                        : body.substring(added.start(), stmtEnd);
                seenNotNull = statement.toUpperCase().contains("NOT NULL");
            }
        }

        assertNotNull(declaredType,
                "live_classes.started_at must be added by a Flyway migration. LiveClass maps "
                        + "@Column(name = \"started_at\"), so without the column every startSession "
                        + "and every expiry sweep fails against a real database.");
        assertTrue(declaredType.startsWith("timestamp"),
                "live_classes.started_at must be a timestamp to match LocalDateTime, but is \""
                        + declaredType + "\" (declared in " + declaredIn + ")");
        assertFalse(seenNotNull,
                "live_classes.started_at must stay nullable: SCHEDULED classes have not started and "
                        + "active rows created before V135 have no actual start (" + declaredIn + ")");
    }

    /**
     * V136 makes Department -> Programme a real foreign key and adds course_modules.module_code.
 * Both are mapped on entities, and the test profile builds its schema with Hibernate instead
 * of Flyway, so a missing or mistyped migration would only surface at runtime against a real
 * database - the exact class of bare-500 defect this suite exists to prevent. Replays the
 * migrations to prove the columns exist and that department_id is uuid-typed, matching the
 * UUID it references on departments(id).
 */
    /**
     * V156 creates news_articles. Every column NewsArticle maps is asserted here, because the test
     * profile builds its schema with Hibernate (ddl-auto=create-drop) and never runs Flyway: a
     * column present on the entity but missing from the migration passes the entire Spring test
     * suite and then fails as a bare HTTP 500 against a real database, which is exactly the class
     * of defect this suite exists to catch.
     *
     * <p>Also pins the NOT NULL columns. A migration that omits a nullability constraint still
     * lets Hibernate create it in tests, so the mismatch would not surface until production.</p>
     */
    @Test
    void newsArticleColumns_AreDeclaredInMigrations() throws IOException {
        Set<String> columnNames = Set.of(
                "title", "summary", "body", "slug", "category", "cover_image_url", "author_name",
                "status", "published_at", "scheduled_at", "expires_at", "is_featured",
                "priority", "sort_order", "last_modified_by", "published_by");

        Map<String, String> types = effectiveColumnTypes(columnNames);

        List<String> missing = columnNames.stream()
                .map(name -> "news_articles." + name.toLowerCase(java.util.Locale.ROOT))
                .filter(key -> !types.containsKey(key))
                .sorted()
                .toList();
        assertTrue(missing.isEmpty(),
                "NewsArticle maps these columns but V156 does not declare them, so publishing an "
                        + "article would fail against a real database: " + missing);

        // published_at is what the NEW window is measured against. If a future edit widened it to
        // text, every comparison against it would silently become a string comparison.
        String publishedAt = types.get("news_articles.published_at");
        assertTrue(publishedAt != null && publishedAt.startsWith("timestamp"),
                "news_articles.published_at must be a timestamp but was declared as \""
                        + publishedAt + "\" at " + declaredIn.get("news_articles.published_at"));

        assertTrue(types.get("news_articles.is_featured").startsWith("boolean"),
                "is_featured is a boolean on the entity; a migration that declared it as text "
                        + "would still pass Hibernate's generated schema in tests");
    }

    /**
     * V157 adds the publication/access columns that make a live class publicly discoverable.
     *
     * <p>The same reasoning as the news assertion applies, and it matters more here: the test
     * profile builds live_classes from the entity with ddl-auto=create-drop, so a migration that
     * omitted any of these columns would leave the entire Spring suite green and then fail as a
     * bare 500 against a real database the first time a class was submitted, approved or
     * published.</p>
     *
     * <p>Also pins the dark-by-default constraint. Both publication_status and access_level are
     * NOT NULL with a default that keeps pre-existing rows invisible. A future edit that relaxed
     * either to nullable would silently let a row with no publication decision appear in a public
     * query, which is the exact failure the two-axis design exists to prevent.</p>
     */
    @Test
    void liveClassPublicationColumns_AreDeclaredInMigrations() throws IOException {
        Set<String> columnNames = Set.of(
                "publication_status", "access_level", "cover_image_url", "ended_at",
                "submitted_for_review_at", "published_at", "reviewed_by", "reviewed_at",
                "review_note");

        // V157 adds these with ALTER TABLE ... ADD COLUMN rather than CREATE TABLE, so this is
        // read with declaredColumns, not effectiveColumnTypes.
        Map<String, String> types = declaredColumns(
                columnNames.stream().map(n -> "live_classes." + n).collect(java.util.stream.Collectors.toSet()));

        List<String> missing = columnNames.stream()
                .map(name -> "live_classes." + name.toLowerCase(java.util.Locale.ROOT))
                .filter(key -> !types.containsKey(key))
                .sorted()
                .toList();
        assertTrue(missing.isEmpty(),
                "LiveClass maps these columns but V157 does not declare them, so the publication "
                        + "workflow would fail against a real database: " + missing);

        // Timestamps drive Past Classes and the review queue. Widening one to text would make every
        // comparison silently lexicographic rather than temporal.
        for (String temporal : List.of("ended_at", "submitted_for_review_at", "published_at",
                "reviewed_at")) {
            String type = types.get("live_classes." + temporal);
            assertTrue(type != null && type.startsWith("timestamp"),
                    "live_classes." + temporal + " must be a timestamp but was declared as \""
                            + type + "\"");
        }

        assertTrue(types.get("live_classes.publication_status").startsWith("character varying")
                        || types.get("live_classes.publication_status").startsWith("varchar"),
                "publication_status must be a varchar, but is \""
                        + types.get("live_classes.publication_status") + "\"");

        assertTrue(types.get("live_classes.review_note").startsWith("character varying")
                        || types.get("live_classes.review_note").startsWith("varchar"),
                "review_note must be a varchar so a note cannot be silently truncated, but is \""
                        + types.get("live_classes.review_note") + "\"");

        // cover_image_url carries a URL, so a length cap matters; an unbounded varchar would let a
        // caller store an arbitrarily large value in a column every public card reads.
        assertTrue(types.get("live_classes.cover_image_url").contains("1000"),
                "cover_image_url must stay length-bounded (VARCHAR(1000)) but is \""
                        + types.get("live_classes.cover_image_url") + "\"");
    }

    /**
     * V157's dark-by-default guarantee is a property of the column definitions, not of any Java
     * code, so it is asserted here against the migration text.
     *
     * <p>A row with publication_status NULL would be excluded by the {@code = 'PUBLISHED'}
     * predicate rather than exposed, so the NULL case is not itself a leak. The leak this guards
     * against is a DEFAULT that ever changes from 'DRAFT'/'INSTITUTION' to something permissive,
     * which would promote every pre-existing row on the next migration that re-added the column.
     */
    @Test
    void liveClassPublicationColumns_AreDarkByDefault() throws IOException {
        String migration = readMigration("V157__live_class_public_discovery.sql");

        assertTrue(migration.contains("publication_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'"),
                "publication_status must default to DRAFT so every pre-existing class stays "
                        + "invisible; found: "
                        + firstLineContaining(migration, "publication_status VARCHAR"));

        assertTrue(migration.contains("access_level VARCHAR(30) NOT NULL DEFAULT 'INSTITUTION'"),
                "access_level must default to INSTITUTION; found: "
                        + firstLineContaining(migration, "access_level VARCHAR"));
    }

    /**
     * V162 makes duplicate live awards impossible. The index must stay PARTIAL: an unconditional
     * unique index would permanently block re-issuing an award after a mistake, because the revoked
     * row would still occupy the key and the operator would have to destroy the record of the
     * error to fix it.
     */
    @Test
    void liveAwardUniqueness_IsPartialSoRevokedCertificatesDoNotBlockReissue() throws IOException {
        String migration = readMigration("V162__certificate_live_award_uniqueness.sql");

        assertTrue(migration.contains("ux_certificates_live_award"),
                "V162 must prevent two live certificates for one award");
        assertTrue(migration.contains("WHERE is_deleted = false AND status <> 'REVOKED'"),
                "the live-award index must exclude revoked and soft-deleted rows, otherwise a "
                        + "withdrawn award could never be re-issued");
        // The service-level check alone is a check-then-act race; only the database arbitrates two
        // concurrent issuances.
        assertTrue(migration.contains("CREATE UNIQUE INDEX"),
                "V162 must use a UNIQUE index: an application-level check cannot survive two "
                        + "concurrent issuances both reading 'no existing certificate'");
    }

    /**
     * qr_code_url held a QR image as of V163, and it was created as varchar(255) in V81 - a width
     * no real QR fits, which is why nothing had ever written to it. Widening to text is the only
     * direction this column may move: shrinking it would truncate a stored image at read time into
     * a data URI that no parser will accept, and the certificate would silently lose its code.
     */
    @Test
    void certificateQrCodeUrl_OnlyEverWidens() throws IOException {
        String migration = readMigration(
                "V163__certificate_qr_code_url_holds_a_data_uri.sql");

        assertTrue(migration.contains("qr_code_url"),
                "V163 must retype certificates.qr_code_url");
        assertTrue(migration.matches("(?s).*ALTER COLUMN qr_code_url TYPE text;.*"),
                "V163 must widen qr_code_url to text so a stored QR is not truncated");
        assertFalse(migration.matches("(?s).*qr_code_url TYPE character varying.*"),
                "V163 must not re-widen to a length-bounded type: a data URI of any fixed length "
                        + "is one longer value away from being cut in half");
        // Dropping and re-adding the column would discard whatever is in it.
        assertFalse(migration.matches("(?s).*DROP COLUMN qr_code_url.*"),
                "V163 must alter the column in place, not drop and recreate it");
    }

    /**
     * Reads a migration by file name from the located migration directory.
     */
    private String readMigration(String fileName) throws IOException {
        Path path = migrationDir.resolve(fileName);
        assertTrue(Files.exists(path), "migration must exist: " + fileName);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String firstLineContaining(String text, String needle) {
        for (String line : text.split("\\R")) {
            if (line.contains(needle)) {
                return line.trim();
            }
        }
        return "(not found)";
    }

    /**
     * V156 adds media_assets.class_group_id, the class target that makes a live class
     * recording appear in the Media Library. MediaAsset maps @Column(name = "class_group_id")
     * and the test profile builds its schema from the entity, so a missing migration would
     * only surface at runtime against a real database.
     */
    @Test
    void mediaAssetsClassGroup_IsDeclaredInMigrations() throws IOException {
        Map<String, String> declared = declaredColumns(Set.of("media_assets.class_group_id"));

        assertTrue(declared.containsKey("media_assets.class_group_id"),
                "MediaAsset maps @Column(name = \"class_group_id\") but no migration declares it, "
                        + "so saving a recording would fail against a real database");
        assertTrue(declared.get("media_assets.class_group_id").startsWith("uuid"),
                "media_assets.class_group_id must be uuid to reference class_groups(id), but is \""
                        + declared.get("media_assets.class_group_id") + "\"");
    }

    /**
     * V136 makes Department -> Programme a real foreign key and adds course_modules.module_code.
     * Both are mapped on entities, and the test profile builds its schema with Hibernate instead
     * of Flyway, so a missing or mistyped migration would only surface at runtime against a real
     * database - the exact class of bare-500 defect this suite exists to prevent. Replays the
     * migrations to prove the columns exist and that department_id is uuid-typed, matching the
     * UUID it references on departments(id).
     */
    @Test
    void departmentProgrammeAndModuleColumns_AreDeclaredInMigrations() throws IOException {
        Set<String> wanted = Set.of(
                "programmes.department_id",
                "course_modules.module_code");

        Map<String, String> declared = declaredColumns(wanted);

        List<String> missing = new ArrayList<>();
        for (String column : wanted) {
            if (!declared.containsKey(column)) {
                missing.add(column);
            }
        }
        assertTrue(missing.isEmpty(),
                "these columns are mapped on entities but no migration declares them, so every "
                        + "programme/module query would fail against a real database: " + missing);

        assertTrue(declared.get("programmes.department_id").startsWith("uuid"),
                "programmes.department_id must be uuid to reference departments(id), but is \""
                        + declared.get("programmes.department_id") + "\"");
        assertTrue(declared.get("course_modules.module_code").startsWith("character varying")
                        || declared.get("course_modules.module_code").startsWith("varchar"),
                "course_modules.module_code must be a varchar, but is \""
                        + declared.get("course_modules.module_code") + "\"");
    }

    /**
     * V161 adds the approval tables and the template-family column. The approval gate is only real
     * if the tables exist: a missing migration would leave issuance referencing entities that do
     * not exist on a real database while the whole Hibernate-generated test schema passed.
     *
     * <p>Also pins the additive lifecycle values. {@code certificates.status} is an unconstrained
     * varchar, so nothing would stop a future edit from dropping one of them and breaking every
     * certificate already stored in that state.</p>
     */
    @Test
    void certificateApprovalAndFamilyColumns_AreDeclaredInMigrations() throws IOException {
        // template_family is added to an existing table, so the ADD COLUMN helper can see it.
        Map<String, String> added = declaredColumns(Set.of("certificate_templates.template_family"));
        assertTrue("varchar(30)".equals(added.get("certificate_templates.template_family")),
                "certificate_templates.template_family must be varchar(30) but is \""
                        + added.get("certificate_templates.template_family") + "\"");

        // The approval tables are created wholesale, so they are checked as table definitions.
        String migration = readMigration("V161__certificate_approvals_and_template_families.sql");
        for (String column : List.of(
                "certificate_id", "stage", "decision_status", "actor_user_id",
                "template_version", "decided_at")) {
            assertTrue(migration.contains(column),
                    "certificate_approvals." + column + " is mapped by the approval service but V161 does not declare it");
        }
        for (String column : List.of("certificate_id", "full_name", "signature_image")) {
            assertTrue(migration.contains(column),
                    "certificate_signatory_snapshots." + column
                            + " is mapped by the approval service but V161 does not declare it");
        }
        assertTrue(migration.contains("template_family"),
                "certificate_templates.template_family is mapped by the template entity but V161 does not declare it");
    }

    /**
     * V164 records which certificate replaced which, so a holder of a withdrawn copy can be told
     * that a valid replacement exists and which one it is.
     *
     * <p>Both columns are mapped by the entity. Without the migration the whole approval and QR
     * feature set still passes against the Hibernate-generated test schema, while a real database -
     * which runs ddl-auto=none - fails on every supersession with an unknown-column error.</p>
     *
     * <p>Also pins the self-referencing foreign key. The pointer is only worth having if it cannot
     * name a certificate that is not there; a plain uuid column would accept any 16 bytes.</p>
     */
    @Test
    void certificateSupersessionColumns_AreDeclaredInMigrations() throws IOException {
        Map<String, String> added = declaredColumns(Set.of(
                "certificates.superseded_by_certificate_id",
                "certificates.superseded_at"));
        assertTrue("uuid".equals(added.get("certificates.superseded_by_certificate_id")),
                "certificates.superseded_by_certificate_id must be uuid but is \""
                        + added.get("certificates.superseded_by_certificate_id") + "\"");
        assertTrue("timestamptz".equals(added.get("certificates.superseded_at")),
                "certificates.superseded_at must be timestamptz but is \""
                        + added.get("certificates.superseded_at") + "\"");

        String migration = readMigration("V164__certificate_supersession_link.sql");
        assertTrue(migration.contains("fk_certificates_superseded_by"),
                "V164 must name its self-reference so it can be found and, if ever needed, dropped");
        assertTrue(migration.contains("REFERENCES certificates"),
                "V164 must point superseded_by_certificate_id at certificates(id); an unconstrained "
                        + "uuid would accept an id that does not exist and strand the verification "
                        + "page on a pointer to nothing");
        assertTrue(migration.contains("idx_certificates_superseded_by"),
                "the verification lookup resolves a supersession by this column and needs the index");
    }

    /**
     * V161 must not narrow {@code certificates.status}. The live column is varchar(255); shrinking
     * it to today's longest status name would silently truncate every future lifecycle value
     * someone adds, and the damage would only show up later as an unreadable status on a stored
     * certificate.
     *
     * <p>16 is not arbitrary: {@code PENDING_APPROVAL} is exactly 16 characters (7 + 1 + 8). The
     * guard has to reject varchar(15) and accept varchar(16).</p>
     */
    @Test
    void certificateStatus_IsNeverNarrowed() throws IOException {
        String migration = readMigration("V161__certificate_approvals_and_template_families.sql");

        assertFalse(migration.contains("ALTER COLUMN status TYPE"),
                "V161 must not retype certificates.status; narrowing a lifecycle column truncates "
                        + "future status values");
        assertTrue(migration.contains("character_maximum_length"),
                "V161 should assert the existing status width instead of replacing it");

        Map<String, String> status = declaredColumns(Set.of("certificates.status"));
        // The guard threshold must equal the real length of the longest status value.
        int pendingApproval = "PENDING_APPROVAL".length();
        assertEquals(16, pendingApproval,
                "PENDING_APPROVAL length changed; update the V161 width guard to match");
        assertTrue(migration.contains("< " + pendingApproval),
                "V161 width guard must reject any width below " + pendingApproval);
    }

    /**
     * The partial index is what makes re-approval after rejection possible while still preventing
     * two live decisions for one stage. Asserting only that the index exists would not catch it
     * being redefined as unconditional.
     */
    @Test
    void certificateApprovals_ConstrainOneCurrentDecisionPerStage() throws IOException {
        String migration = readMigration("V161__certificate_approvals_and_template_families.sql");

        assertTrue(migration.contains("certificate_approvals"),
                "V161 must create the approval table");
        // The partial unique index is what stops a second live decision appearing for a stage.
        assertTrue(migration.contains("ux_certificate_approvals_current_stage"),
                "V161 must enforce one current decision per (certificate, stage)");
        // It must be partial. An unconditional index would forbid a REJECTED row coexisting with a
        // later re-submitted one, which is the exact case the workflow exists to support.
        assertTrue(migration.contains("WHERE is_deleted = false AND decision_status IN ('PENDING', 'APPROVED')"),
                "ux_certificate_approvals_current_stage must be a PARTIAL index covering only current "
                        + "PENDING/APPROVED decisions, so a refusal can stay on record alongside a re-approval");
        assertTrue(migration.contains("FOREIGN KEY (certificate_id) REFERENCES certificates (id)"),
                "approvals must reference the certificate they belong to");
    }

    /**
     * Column type per "table.column" for columns added via ALTER TABLE ADD COLUMN, last
     * declaration winning. Does not see CREATE TABLE columns.
     */
    private Map<String, String> declaredColumns(Set<String> wanted) throws IOException {
        Map<String, String> declared = new LinkedHashMap<>();
        List<Path> ordered = migrationFiles.stream()
                .sorted(Comparator.comparingInt(FlywayMigrationValidationTest::versionOf))
                .toList();

        for (Path file : ordered) {
            StringBuilder normalized = new StringBuilder();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                normalized.append(trimmed).append(' ');
            }
            Matcher matcher = ADD_COLUMN.matcher(normalized.toString());
            while (matcher.find()) {
                String key = unquote(matcher.group(1)).toLowerCase() + "." + matcher.group(2).toLowerCase();
                if (wanted.contains(key)) {
                    declared.put(key, normalizeDeclaredType(matcher.group(3)));
                }
            }
        }
        return declared;
    }

    /**
     * ADD COLUMN with an optional length ("VARCHAR(50)") and a trailing explicit NULL.
     * The type group is lazy and stops at a column constraint or the statement end, so the
     * captured text can carry a trailing "null" that has to be stripped before comparing.
     */
    private static final Pattern ADD_COLUMN = Pattern.compile(
            "ALTER\\s+TABLE\\s+([A-Za-z0-9_\\\".]+)\\s+ADD\\s+COLUMN\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?"
                    + "([A-Za-z0-9_]+)\\s+([A-Za-z][A-Za-z0-9_ ]*?(?:\\([0-9, ]+\\))?[A-Za-z0-9_ ]*?)"
                    + "(\\s+NOT\\s+NULL|\\s*;|\\s+DEFAULT|\\s+REFERENCES|\\s+PRIMARY|\\s+UNIQUE|$)",
            Pattern.CASE_INSENSITIVE);

    private static String normalizeDeclaredType(String rawType) {
        String type = rawType.trim().toLowerCase().replaceAll("\\s+", " ");
        // An explicit "NULL" column constraint is not part of the type.
        if (type.endsWith(" null")) {
            type = type.substring(0, type.length() - " null".length()).trim();
        }
        return type;
    }

    /** Effective (post-all-migrations) type per "table.column", last declaration winning. */
    private Map<String, String> effectiveColumnTypes(Set<String> watchedColumns) throws IOException {
        Pattern createTable = Pattern.compile(
                "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([A-Za-z0-9_\\\".]+)\\s*\\(",
                Pattern.CASE_INSENSITIVE);
        Pattern columnLine = Pattern.compile(
                "^(" + String.join("|", watchedColumns.stream().sorted().toList()) + ")\\s+([A-Za-z][A-Za-z0-9_ ]*)",
                Pattern.CASE_INSENSITIVE);
        Pattern alterColumn = Pattern.compile(
                "ALTER\\s+TABLE\\s+([A-Za-z0-9_\\\".]+)\\s+ALTER\\s+COLUMN\\s+("
                        + String.join("|", watchedColumns.stream().sorted().toList())
                        + ")\\s+TYPE\\s+([A-Za-z][A-Za-z0-9_ ]*)",
                Pattern.CASE_INSENSITIVE);

        Map<String, String> effectiveType = new LinkedHashMap<>();
        declaredIn = new LinkedHashMap<>();

        List<Path> ordered = migrationFiles.stream()
                .sorted(Comparator.comparingInt(FlywayMigrationValidationTest::versionOf))
                .toList();

        for (Path file : ordered) {
            String currentTable = null;
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                String upper = line.toUpperCase();
                // Constraint declarations reference a column without typing it.
                if (upper.contains("FOREIGN KEY") || upper.contains("REFERENCES")) {
                    continue;
                }
                Matcher created = createTable.matcher(line);
                if (created.find()) {
                    currentTable = unquote(created.group(1));
                    continue;
                }
                if (currentTable != null && upper.startsWith(")")) {
                    currentTable = null;
                }
                Matcher altered = alterColumn.matcher(line);
                if (altered.find()) {
                    String key = unquote(altered.group(1)).toLowerCase() + "." + altered.group(2).toLowerCase();
                    effectiveType.put(key, altered.group(3).trim().toLowerCase());
                    declaredIn.put(key, file.getFileName() + ":" + (i + 1));
                    continue;
                }
                if (currentTable != null) {
                    Matcher column = columnLine.matcher(line);
                    if (column.find()) {
                        String key = currentTable.toLowerCase() + "." + column.group(1).toLowerCase();
                        effectiveType.put(key, column.group(2).trim().toLowerCase());
                        declaredIn.put(key, file.getFileName() + ":" + (i + 1));
                    }
                }
            }
        }
        return effectiveType;
    }

    private static String unquote(String identifier) {
        String cleaned = identifier.trim().replace("\"", "");
        return cleaned;
    }

    private static int versionOf(Path path) {
        Matcher matcher = MIGRATION_NAME.matcher(path.getFileName().toString());
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : Integer.MAX_VALUE;
    }

    @Test
    void migrationFiles_AreNonEmpty() throws IOException {
        for (Path file : migrationFiles) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            boolean hasContent = content.lines().map(String::trim)
                    .anyMatch(l -> !l.isEmpty());
            assertTrue(hasContent, "migration file is empty: " + file.getFileName());
        }
    }

    @Test
    void migrationSet_IsDiscoverableOnClasspath() throws URISyntaxException {
        URL url = getClass().getClassLoader().getResource("db/migration");
        if (url == null) {
            fail("db/migration not on classpath");
            return;
        }
        if ("file".equals(url.getProtocol())) {
            Path dir = Path.of(url.toURI());
            assertTrue(Files.isDirectory(dir));
        }
        // jar-packaged case: resource exists (URL non-null) which is sufficient for Flyway resolve
    }

    @Test
    void v77_ReadableViaClasspathStream() throws IOException {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V77__dedupe_event_columns.sql")) {
            assertTrue(in != null, "V77 must be loadable from classpath (Flyway resolve)");
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(content.contains("comment-only") || content.contains("no-op"),
                    "V77 must state it is a comment-only no-op");
        }
    }

    @Test
    void migrationFiles_SortedByNumericVersion_MatchLexicalEdgeCases() {
        // Guard against V9 sorting after V10 in naive lexical order: numeric parse must dominate.
        List<Integer> versions = new ArrayList<>();
        for (Path file : migrationFiles) {
            Matcher m = MIGRATION_NAME.matcher(file.getFileName().toString());
            assertTrue(m.find());
            versions.add(Integer.parseInt(m.group(1)));
        }
        List<Integer> numericSorted = versions.stream().sorted(Comparator.naturalOrder()).toList();
        assertTrue(versions.equals(numericSorted) || new TreeSet<>(versions).size() == versions.size(),
                "versions must parse to a duplicate-free set");
    }
}
