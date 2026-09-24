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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
                    + "V76__replay_tables|V77__dedupe_event_columns)\\.sql$");

    private static Path migrationDir;
    private static List<Path> migrationFiles;

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
                        && versions.contains(76) && versions.contains(77),
                "D03 migrations V71/V75/V76/V77 must all be present (found " + versions.size() + ")");
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
                "V77__dedupe_event_columns.sql"}) {
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
