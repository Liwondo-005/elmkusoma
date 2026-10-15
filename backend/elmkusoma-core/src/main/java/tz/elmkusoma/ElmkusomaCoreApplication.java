package tz.elmkusoma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

@SpringBootApplication
@EntityScan(basePackages = "tz.elmkusoma")
@EnableJpaRepositories(basePackages = "tz.elmkusoma")
@EnableJpaAuditing
@EnableScheduling
public class ElmkusomaCoreApplication {

    /**
     * Marker file that identifies a directory as the backend root. {@code ../.env} is only
     * accepted from a directory that ships one, so a stray {@code .env} in an unrelated
     * parent directory (a developer's home or desktop, say) is never loaded.
     */
    static final String BACKEND_ROOT_MARKER = ".env.example";

    /** Only the core module reads {@code .env} itself - see docs/audits/…REMEDIATION.md. */
    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(ElmkusomaCoreApplication.class, args);
    }

    /**
     * Loads the .env file into system properties before Spring Boot starts.
     * This ensures ${DB_PASSWORD} and other placeholders in application.yml
     * are resolved from the local .env file.
     *
     * <p><b>Precedence</b> (highest first): a real environment variable, then an already-set
     * system property, then a value from the file, then the default in application.yml.
     * The file only ever fills gaps - it never overwrites anything the operator set.</p>
     *
     * <p><b>Diagnostics</b> print the file path, the number of keys applied and the key
     * <em>names</em>. Values are never printed.</p>
     */
    private static void loadDotenv() {
        File envFile = findEnvFile();
        if (envFile == null) {
            System.err.println("[dotenv] No .env file found. Using environment variables or defaults.");
            return;
        }
        System.err.println("[dotenv] Loading " + envFile.getAbsolutePath());

        try {
            Loaded loaded = load(envFile, System::getenv, System::getProperty, System::setProperty);
            StringBuilder line = new StringBuilder()
                    .append("[dotenv] Loaded ").append(loaded.count()).append(" properties from .env");
            if (!loaded.keys().isEmpty()) {
                line.append(": ").append(String.join(", ", loaded.keys()));
            }
            System.err.println(line);
        } catch (IOException e) {
            System.err.println("[dotenv] Error reading .env: " + e.getMessage());
        }
    }

    /**
     * Locates the .env file for the current working directory.
     *
     * <p>Exactly two locations are considered, both anchored to the working directory:</p>
     * <ol>
     *   <li>{@code ./.env} - the documented, predictable location.</li>
     *   <li>{@code ../.env} - needed because services are launched with the module directory
     *       ({@code backend/elmkusoma-core}) as the working directory, while the file lives one
     *       level up in {@code backend/}. Accepted only when that parent directory ships a
     *       {@value #BACKEND_ROOT_MARKER}, i.e. is recognisably the backend root.</li>
     * </ol>
     *
     * <p>No directory further up the tree is ever searched, so a {@code .env} belonging to some
     * unrelated project or to the developer's home directory cannot be picked up by accident.</p>
     */
    static File findEnvFile() {
        return findEnvFile(new File("."));
    }

    /**
     * Package-visible for tests, which supply an isolated temporary directory.
     *
     * <p>The directory is canonicalised first. Production calls this with {@code new File(".")},
     * whose absolute form is {@code <working-dir>/.} - a path whose last name component is the
     * literal {@code "."}. Taking the parent of <em>that</em> string yields the working
     * directory itself instead of its parent, which silently disabled the {@code ../.env}
     * branch and left {@code backend/.env} invisible when the module directory
     * ({@code backend/elmkusoma-core}) is the working directory - the normal way the service
     * is launched. Canonicalising collapses the trailing {@code "."} so the parent lookup sees
     * the real one.</p>
     */
    static File findEnvFile(File workingDir) {
        File base;
        try {
            base = workingDir.getCanonicalFile();
        } catch (IOException e) {
            base = workingDir.getAbsoluteFile();
        }

        File local = new File(base, ".env");
        if (isUsable(local)) return local;

        File parent = base.getParentFile();
        if (parent != null) {
            File parentEnv = new File(parent, ".env");
            if (isUsable(parentEnv) && isUsable(new File(parent, BACKEND_ROOT_MARKER))) {
                return parentEnv;
            }
        }
        return null;
    }

    /**
     * Applies {@code envFile} through the supplied lookups.
     *
     * @param envFile     file to read
     * @param envLookup   resolves a real environment variable, or null when absent
     * @param propLookup  resolves an already-set system property, or null when absent
     * @param setter      receives keys that the file is allowed to define
     * @return how many keys were applied, and their names (never values)
     */
    static Loaded load(File envFile,
                       Function<String, String> envLookup,
                       Function<String, String> propLookup,
                       BiConsumer<String, String> setter) throws IOException {
        List<String> applied = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int eq = line.indexOf('=');
                if (eq <= 0) continue;

                String key = line.substring(0, eq).trim();
                String value = unquote(line.substring(eq + 1).trim());

                // Only set if not already set by system env or system property.
                if (envLookup.apply(key) == null && propLookup.apply(key) == null) {
                    setter.accept(key, value);
                    applied.add(key);
                }
            }
        }
        return new Loaded(applied.size(), Collections.unmodifiableList(applied));
    }

    private static boolean isUsable(File file) {
        return file.exists() && file.canRead() && file.isFile();
    }

    /**
     * Removes one layer of matching surrounding quotes.
     *
     * <p>A value written {@code KEY="value"} is quoted the way a shell would quote it, and
     * what the caller means is {@code value}. This is not cosmetic: the payment gateway key
     * is recognised by its {@code sk_test_}/{@code sk_live_} prefix, so a quoted key no longer
     * matches at position zero and a correctly configured sandbox deployment is rejected as
     * "not a Stakaba key" - the operator did nothing wrong and gets no useful clue why.</p>
     *
     * <p>Only a quote that wraps the <em>entire</em> value is treated as structure, and only
     * when both ends match. A quote anywhere else is data: {@code say "hello" now} and a
     * half-written {@code "unclosed} are both returned verbatim, because mangling those would
     * corrupt a real value while fixing no real problem. Interior escapes are not interpreted
     * - {@code "a\"b"} keeps its backslash - because no current value needs that and guessing
     * would be worse than passing the characters through.</p>
     */
    static String unquote(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' || first == '\'') && first == last) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /** Count and key names of a completed load. Deliberately carries no values. */
    record Loaded(int count, List<String> keys) {
    }
}
