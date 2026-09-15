package tz.elmkusoma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@SpringBootApplication
@EnableJpaAuditing
public class ElmkusomaCoreApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(ElmkusomaCoreApplication.class, args);
    }

    /**
     * Loads .env file into system properties before Spring Boot starts.
     * This ensures ${DB_PASSWORD} and other placeholders in application.yml
     * are resolved from the local .env file.
     */
    private static void loadDotenv() {
        File envFile = findEnvFile();
        if (envFile == null) {
            System.err.println("[dotenv] No .env file found. Using environment variables or defaults.");
            return;
        }
        System.err.println("[dotenv] Loading " + envFile.getAbsolutePath());

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int eq = line.indexOf('=');
                if (eq <= 0) continue;

                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();

                // Only set if not already set by system env or system property
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                    count++;
                }
            }
            System.err.println("[dotenv] Loaded " + count + " properties from .env");
        } catch (IOException e) {
            System.err.println("[dotenv] Error reading .env: " + e.getMessage());
        }
    }

    private static File findEnvFile() {
        File local = new File(".env");
        if (local.exists() && local.canRead()) return local;

        File parent = new File("../.env");
        if (parent.exists() && parent.canRead()) return parent;

        return null;
    }
}
