package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Real backup visibility (spec §58). Any field that cannot be verified from the
 * backup directory or status file is null and surfaced as "unavailable" in the UI —
 * never fabricated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupStatusResponse {
    private String status;              // SUCCESS | FAILURE | NEVER_RUN | UNAVAILABLE
    private LocalDateTime lastRunAt;
    private String lastFile;
    private Long lastFileSizeBytes;
    private Boolean integrityOk;        // pg_dump exit code recorded by script
    private Integer backupCount;        // real file count in backup dir (null if dir missing)
    private LocalDateTime newestBackupAt;
    private LocalDateTime oldestBackupAt;
    private String backupDirectory;
    private String statusFilePath;
    private Boolean scriptPresent;      // backup-db.sh exists
    private Boolean restoreScriptPresent;
    private String restoreProcedure;    // documented restoration steps
    private List<String> recoveryEvents; // recent status-file history lines if present
}
