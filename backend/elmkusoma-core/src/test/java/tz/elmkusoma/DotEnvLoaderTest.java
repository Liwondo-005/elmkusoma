package tz.elmkusoma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@code backend/.env} loader: which file it may read, and the precedence rule
 * that keeps a real environment variable ahead of the file.
 *
 * <p>Everything here uses non-secret sentinel values in an isolated temporary directory - no
 * value from the developer's real {@code backend/.env} is ever read or asserted.</p>
 */
class DotEnvLoaderTest {

    // ---------------------------------------------------------------- location

    @Test
    void findEnvFile_readsTheWorkingDirectoryFileFirst(@TempDir File dir) throws IOException {
        write(new File(dir, ".env"), "SENTINEL=from-working-dir\n");

        File found = ElmkusomaCoreApplication.findEnvFile(dir);
        assertNotNull(found);
        assertEquals(new File(dir, ".env").getAbsoluteFile(), found.getAbsoluteFile(),
                "./.env is the documented, predictable location");
    }

    @Test
    void findEnvFile_fallsBackToTheParentOnlyWhenItIsTheBackendRoot(@TempDir File parent)
            throws IOException {
        // backend/.env.example is the marker that identifies the backend root.
        write(new File(parent, ".env.example"), "# template\n");
        write(new File(parent, ".env"), "SENTINEL=from-backend-root\n");
        File module = new File(parent, "elmkusoma-core");
        assertTrue(module.mkdirs());

        File found = ElmkusomaCoreApplication.findEnvFile(module);
        assertNotNull(found, "a module working directory must still reach backend/.env");
        assertEquals(new File(parent, ".env").getAbsoluteFile(), found.getAbsoluteFile());
    }

    /**
     * The regression this fix exists for. Production calls {@code findEnvFile()} with
     * {@code new File(".")} - a path whose absolute form ends in a literal {@code "."}
     * ({@code backend/elmkusoma-core/.}). Parent-taking that string returned the working
     * directory itself, so the {@code ../.env} branch never looked at the real parent and
     * {@code backend/.env} was never loaded. Running the module directory the normal way then
     * failed at startup with an unresolvable {@code ${JWT_SECRET}}.
     *
     * <p>The other parent test above passes a clean {@code <parent>/elmkusoma-core} path, whose
     * parent resolves correctly - which is why the broken behaviour was not caught.</p>
     */
    @Test
    void findEnvFile_findsTheBackendRootWhenTheWorkingDirectoryIsGivenAsADotPath(@TempDir File parent)
            throws IOException {
        write(new File(parent, ".env.example"), "# template\n");
        write(new File(parent, ".env"), "SENTINEL=from-backend-root\n");
        File module = new File(parent, "elmkusoma-core");
        assertTrue(module.mkdirs());

        File dotPath = new File(module, ".");
        assertEquals(module.getAbsolutePath() + File.separator + ".",
                dotPath.getAbsolutePath(),
                "the precondition: this path really does end in a literal '.'");

        File found = ElmkusomaCoreApplication.findEnvFile(dotPath);
        assertNotNull(found, "../.env must be found when the working directory is given as <dir>/.");
        assertEquals(new File(parent, ".env").getAbsoluteFile(), found.getAbsoluteFile());
    }

    /**
     * The regression this guard exists for: without a marker, {@code ../.env} would walk out of
     * the project and load whatever happens to sit in an unrelated parent directory.
     */
    @Test
    void findEnvFile_refusesAParentDirectoryThatIsNotTheBackendRoot(@TempDir File parent)
            throws IOException {
        write(new File(parent, ".env"), "SENTINEL=some-other-project\n");
        File module = new File(parent, "elmkusoma-core");
        assertTrue(module.mkdirs());
        // No .env.example alongside -> not recognisably the backend root.

        assertNull(ElmkusomaCoreApplication.findEnvFile(module),
                "a stray .env in an unrelated parent directory must never be loaded");
    }

    @Test
    void findEnvFile_returnsNullWhenNothingIsPresent(@TempDir File dir) {
        assertNull(ElmkusomaCoreApplication.findEnvFile(dir));
    }

    @Test
    void findEnvFile_doesNotReadADirectoryNamedEnv(@TempDir File dir) {
        assertTrue(new File(dir, ".env").mkdirs());
        assertNull(ElmkusomaCoreApplication.findEnvFile(dir));
    }

    // -------------------------------------------------------------- precedence

    @Test
    void load_fillsGapsFromTheFile(@TempDir File dir) throws IOException {
        File env = file(dir, "A=1\nB=2\n");
        Map<String, String> props = new HashMap<>();

        ElmkusomaCoreApplication.Loaded loaded =
                ElmkusomaCoreApplication.load(env, k -> null, props::get, props::put);

        assertEquals(2, loaded.count());
        assertEquals("1", props.get("A"));
        assertEquals("2", props.get("B"));
    }

    /** The documented contract: a real environment variable always wins over the file. */
    @Test
    void load_neverOverwritesARealEnvironmentVariable(@TempDir File dir) throws IOException {
        File env = file(dir, "DB_PASSWORD=from-file\n");
        Map<String, String> props = new HashMap<>();

        ElmkusomaCoreApplication.Loaded loaded = ElmkusomaCoreApplication.load(
                env, k -> "from-process-environment", props::get, props::put);

        assertEquals(0, loaded.count(), "an env-provided key must not be re-applied");
        assertFalse(props.containsKey("DB_PASSWORD"),
                "the file must not replace a value the operator set in the environment");
    }

    @Test
    void load_neverOverwritesAnExistingSystemProperty(@TempDir File dir) throws IOException {
        File env = file(dir, "JWT_SECRET=from-file\n");
        Map<String, String> props = new HashMap<>();
        props.put("JWT_SECRET", "already-set");

        ElmkusomaCoreApplication.Loaded loaded =
                ElmkusomaCoreApplication.load(env, k -> null, props::get, props::put);

        assertEquals(0, loaded.count());
        assertEquals("already-set", props.get("JWT_SECRET"));
    }

    @Test
    void load_reportsTheKeysItContributedByName(@TempDir File dir) throws IOException {
        File env = file(dir, "SET_BY_ENV=yes\nONLY_IN_FILE=yes\n");
        Map<String, String> props = new HashMap<>();

        ElmkusomaCoreApplication.Loaded loaded = ElmkusomaCoreApplication.load(
                env, k -> "SET_BY_ENV".equals(k) ? "from-env" : null, props::get, props::put);

        assertEquals(List.of("ONLY_IN_FILE"), loaded.keys(),
                "the report must list exactly the keys the file contributed");
        assertEquals(1, loaded.count());
    }

    // ------------------------------------------------------------------ parsing

    @Test
    void load_ignoresCommentsBlanksAndMalformedLines(@TempDir File dir) throws IOException {
        File env = file(dir, "# a comment\n"
                + "\n"
                + "   \n"
                + "=no-key\n"
                + "no-equals-sign\n"
                + "GOOD=value\n");
        Map<String, String> props = new HashMap<>();

        ElmkusomaCoreApplication.Loaded loaded =
                ElmkusomaCoreApplication.load(env, k -> null, props::get, props::put);

        assertEquals(1, loaded.count());
        assertEquals("value", props.get("GOOD"));
        assertFalse(props.containsKey("no-equals-sign"));
    }

    /** Secrets and connection strings routinely contain '=' - only the first one splits. */
    @Test
    void load_splitsOnTheFirstEqualsOnly(@TempDir File dir) throws IOException {
        File env = file(dir, "CONNECTION=host=db port=5432 ssl=disable\n");
        Map<String, String> props = new HashMap<>();

        ElmkusomaCoreApplication.load(env, k -> null, props::get, props::put);

        assertEquals("host=db port=5432 ssl=disable", props.get("CONNECTION"));
    }

    @Test
    void load_appliesEachKeyOnlyOnceEvenIfRepeated(@TempDir File dir) throws IOException {
        File env = file(dir, "K=first\nK=second\n");
        Map<String, String> props = new HashMap<>();

        ElmkusomaCoreApplication.Loaded loaded =
                ElmkusomaCoreApplication.load(env, k -> null, props::get, props::put);

        // First occurrence wins: once set, the key is now a system property.
        assertEquals("first", props.get("K"));
        assertEquals(1, loaded.count());
    }

    // ------------------------------------------------- diagnostics carry no values

    @Test
    void load_resultNeverCarriesAValue(@TempDir File dir) throws IOException {
        File env = file(dir, "TOP_SECRET=super-secret-value-42\n");
        Map<String, String> props = new HashMap<>();

        ElmkusomaCoreApplication.Loaded loaded =
                ElmkusomaCoreApplication.load(env, k -> null, props::get, props::put);

        assertEquals(List.of("TOP_SECRET"), loaded.keys());
        assertFalse(loaded.toString().contains("super-secret-value-42"),
                "the load result must expose key names only, never values");
    }

    @Test
    void theMarkerThatIdentifiesTheBackendRoot() {
        assertEquals(".env.example", ElmkusomaCoreApplication.BACKEND_ROOT_MARKER);
    }

    // ------------------------------------------------------------------ helpers

    private static File file(File dir, String content) throws IOException {
        File f = new File(dir, ".env");
        write(f, content);
        return f;
    }

    private static void write(File target, String content) throws IOException {
        Files.writeString(target.toPath(), content, StandardCharsets.UTF_8);
    }
}
