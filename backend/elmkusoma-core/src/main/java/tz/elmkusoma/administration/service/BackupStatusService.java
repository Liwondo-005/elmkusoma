package tz.elmkusoma.administration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.dto.BackupStatusResponse;
import tz.elmkusoma.administration.repository.PlatformConfigRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Backup & Disaster Recovery visibility (spec §58).
 * Reads ONLY real artifacts: the status file written by backup-db.sh and the
 * backup dump directory. If nothing has run, status is NEVER_RUN / UNAVAILABLE —
 * never a fabricated dashboard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BackupStatusService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private final PlatformConfigRepository configRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String[] SCRIPT_CANDIDATES = {
            "infrastructure/scripts/backup-db.sh",
            "../infrastructure/scripts/backup-db.sh",
            "../../infrastructure/scripts/backup-db.sh",
            "../../../infrastructure/scripts/backup-db.sh"
    };
    private static final String[] RESTORE_CANDIDATES = {
            "infrastructure/scripts/restore-db.sh",
            "../infrastructure/scripts/restore-db.sh",
            "../../infrastructure/scripts/restore-db.sh",
            "../../../infrastructure/scripts/restore-db.sh"
    };

    public BackupStatusResponse getBackupStatus() {
        String statusFile = configValue("ops.backup.status_file").orElse("./backups/last_backup_status.json");
        String backupDir = configValue("ops.backup.dir").orElse("./backups");

        Path statusPath = Paths.get(statusFile);
        Path dir = Paths.get(backupDir);

        BackupStatusResponse.BackupStatusResponseBuilder b = BackupStatusResponse.builder()
                .statusFilePath(statusPath.toAbsolutePath().toString())
                .backupDirectory(dir.toAbsolutePath().toString())
                .scriptPresent(findFirst(SCRIPT_CANDIDATES).isPresent())
                .restoreScriptPresent(findFirst(RESTORE_CANDIDATES).isPresent())
                .restoreProcedure("""
                    1. Stop application traffic (enable maintenance mode).
                    2. Run infrastructure/scripts/restore-db.sh <dump-file> (pg_restore --clean --if-exists).
                    3. Verify row counts and run health-check.sh.
                    4. Disable maintenance mode and verify login + dashboards.
                    5. Record the recovery event in Platform Admin audit.""");

        // status file
        if (Files.isRegularFile(statusPath)) {
            try {
                String json = Files.readString(statusPath);
                JsonNode node = objectMapper.readTree(json);
                b.status(node.path("status").asText("UNKNOWN"));
                if (node.hasNonNull("runAt")) {
                    try { b.lastRunAt(LocalDateTime.parse(node.get("runAt").asText(), TS)); }
                    catch (Exception ignored) { /* unparsable timestamp stays null */ }
                }
                if (node.hasNonNull("file")) b.lastFile(node.get("file").asText());
                if (node.hasNonNull("sizeBytes")) b.lastFileSizeBytes(node.get("sizeBytes").asLong());
                if (node.hasNonNull("integrityOk")) b.integrityOk(node.get("integrityOk").asBoolean());
            } catch (Exception e) {
                b.status("UNAVAILABLE");
                b.integrityOk(null);
                log.warn("Could not parse backup status file {}: {}", statusPath, e.getMessage());
            }
        } else {
            b.status("NEVER_RUN");
            b.integrityOk(null);
        }

        // recovery events: sibling history file if the script wrote one
        Path history = statusPath.resolveSibling("backup_history.log");
        if (Files.isRegularFile(history)) {
            try {
                List<String> lines = Files.readAllLines(history);
                b.recoveryEvents(lines.subList(Math.max(0, lines.size() - 10), lines.size()));
            } catch (Exception e) {
                log.debug("Backup history unreadable: {}", e.getMessage());
            }
        }

        // real dump inventory
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                List<Path> dumps = files
                        .filter(p -> p.getFileName().toString().endsWith(".dump"))
                        .sorted(Comparator.comparing((Path p) -> p.toFile().lastModified()).reversed())
                        .toList();
                b.backupCount(dumps.size());
                if (!dumps.isEmpty()) {
                    b.newestBackupAt(toLocal(dumps.get(0).toFile().lastModified()));
                    b.oldestBackupAt(toLocal(dumps.get(dumps.size() - 1).toFile().lastModified()));
                    BackupStatusResponse current = b.build();
                    if (current.getLastFile() == null) {
                        b.lastFile(dumps.get(0).getFileName().toString());
                    }
                }
            } catch (Exception e) {
                log.debug("Backup dir listing failed: {}", e.getMessage());
            }
        } else {
            b.backupCount(null);
        }
        return b.build();
    }

    private Optional<String> configValue(String key) {
        return configRepository.findByConfigKeyAndIsDeletedFalse(key)
                .map(e -> e.getConfigValue())
                .filter(v -> v != null && !v.isBlank());
    }

    private Optional<Path> findFirst(String[] candidates) {
        for (String c : candidates) {
            if (Files.isRegularFile(Paths.get(c))) return Optional.of(Paths.get(c));
        }
        return Optional.empty();
    }

    private LocalDateTime toLocal(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
}
