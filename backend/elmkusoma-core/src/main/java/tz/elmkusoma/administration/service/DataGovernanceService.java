package tz.elmkusoma.administration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.dto.DataQualityCheckResponse;
import tz.elmkusoma.administration.dto.RetentionStatusResponse;
import tz.elmkusoma.administration.repository.ContentReportRepository;
import tz.elmkusoma.administration.repository.PlatformConfigRepository;
import tz.elmkusoma.administration.repository.VerificationRecordRepository;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.parent.repository.SupportTicketRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Governance (spec §56) + Data Quality Center (spec §57) + audit retention (§51).
 * Every number comes from a live query. Checks that cannot run surface UNAVAILABLE.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DataGovernanceService {

    static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PlatformConfigRepository configRepository;
    private final AuditLogRepository auditLogRepository;
    private final ContentReportRepository contentReportRepository;
    private final UserRepository userRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final VerificationRecordRepository verificationRepository;

    @Transactional(readOnly = true)
    public RetentionStatusResponse retentionStatus() {
        Map<String, String> cfg = new HashMap<>();
        for (String key : List.of("data.retention.days.audit", "data.retention.days.media",
                "data.retention.days.security_events", "data.export.enabled", "data.retention.last_sweep")) {
            configRepository.findByConfigKeyAndIsDeletedFalse(key)
                    .ifPresent(e -> cfg.put(key, e.getConfigValue()));
        }
        Long archived = null;
        Long total = null;
        try {
            archived = auditLogRepository.countByArchivedAtIsNotNull();
            total = auditLogRepository.count();
        } catch (Exception e) {
            log.warn("Audit retention counts unavailable: {}", e.getMessage());
        }
        Long purged = null;
        try {
            purged = contentReportRepository.countByIsDeletedTrue();
        } catch (Exception e) {
            log.debug("Soft-deleted report count unavailable: {}", e.getMessage());
        }
        return RetentionStatusResponse.builder()
                .config(cfg)
                .lastSweep(cfg.get("data.retention.last_sweep"))
                .archivedAuditCount(archived)
                .totalAuditCount(total)
                .purgedSoftDeletedReports(purged)
                .build();
    }

    public RetentionStatusResponse runSweep(String actor) {
        int retentionDays = configRepository.findByConfigKeyAndIsDeletedFalse("data.retention.days.audit")
                .map(e -> parseIntSafe(e.getConfigValue(), 3650))
                .orElse(3650);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        long archived = 0;
        try {
            archived = auditLogRepository.archiveOlderThan(cutoff);
        } catch (Exception e) {
            log.error("Audit archive sweep failed: {}", e.getMessage());
        }

        long purgedReports = 0;
        try {
            int mediaDays = configRepository.findByConfigKeyAndIsDeletedFalse("data.retention.days.media")
                    .map(e -> parseIntSafe(e.getConfigValue(), 1825))
                    .orElse(1825);
            LocalDateTime reportCutoff = LocalDateTime.now().minusDays(mediaDays);
            purgedReports = contentReportRepository.purgeSoftDeletedOlderThan(reportCutoff);
        } catch (Exception e) {
            log.error("Soft-deleted report purge failed: {}", e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("runAt", LocalDateTime.now().format(ISO));
        result.put("actor", actor != null ? actor : "system");
        result.put("retentionDays", retentionDays);
        result.put("archivedAuditLogs", archived);
        result.put("purgedSoftDeletedReports", purgedReports);

        configRepository.findByConfigKeyAndIsDeletedFalse("data.retention.last_sweep").ifPresentOrElse(
                entry -> {
                    entry.setConfigValue(toJson(result));
                    entry.setLastModifiedBy(actor != null ? actor : "system");
                    configRepository.save(entry);
                },
                () -> log.warn("data.retention.last_sweep config row missing — sweep result not persisted")
        );
        log.info("Retention sweep by {}: archived audit={}, purged reports={}", actor, archived, purgedReports);
        return retentionStatus();
    }

    @Transactional(readOnly = true)
    public List<DataQualityCheckResponse> dataQuality() {
        List<DataQualityCheckResponse> checks = new ArrayList<>();

        try {
            long dups = userRepository.countDuplicateEmails();
            checks.add(check("Duplicate user emails", dups, "Active users sharing an email address"));
        } catch (Exception e) {
            checks.add(unavailable("Duplicate user emails", e));
        }
        try {
            long orphans = userRepository.countStudentsWithoutInstitution();
            checks.add(check("Learners without institution", orphans, "STUDENT/OTHER_LEARNER rows with no institution link"));
        } catch (Exception e) {
            checks.add(unavailable("Learners without institution", e));
        }
        try {
            long open = supportTicketRepository.countByStatusAndIsDeletedFalse("OPEN");
            checks.add(check("Open support tickets", open, "Tickets still in OPEN state"));
        } catch (Exception e) {
            checks.add(unavailable("Open support tickets", e));
        }
        try {
            long pending = verificationRepository.countByStatusAndIsDeletedFalse("PENDING");
            checks.add(check("Pending verifications", pending, "Awaiting review — workflow backlog"));
        } catch (Exception e) {
            checks.add(unavailable("Pending verifications", e));
        }
        try {
            long stale = supportTicketRepository.countByStatusAndCreatedAtBeforeAndIsDeletedFalse(
                    "OPEN", LocalDateTime.now().minusDays(7));
            checks.add(check("Tickets open > 7 days", stale, "Possible stalled cases"));
        } catch (Exception e) {
            checks.add(unavailable("Tickets open > 7 days", e));
        }
        try {
            long unres = contentReportRepository.countByStatusAndIsDeletedFalse("OPEN");
            checks.add(check("Unresolved content reports", unres, "Moderation queue depth"));
        } catch (Exception e) {
            checks.add(unavailable("Unresolved content reports", e));
        }
        return checks;
    }

    private DataQualityCheckResponse check(String name, long count, String detail) {
        return DataQualityCheckResponse.builder()
                .name(name)
                .status(count == 0 ? "OK" : "WARN")
                .count(count)
                .detail(detail)
                .build();
    }

    private DataQualityCheckResponse unavailable(String name, Exception e) {
        return DataQualityCheckResponse.builder()
                .name(name).status("UNAVAILABLE").count(null)
                .detail("Data unavailable: " + e.getMessage())
                .build();
    }

    private int parseIntSafe(String v, int def) {
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return def; }
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(e.getKey()).append("\":");
            if (e.getValue() instanceof Number n) sb.append(n);
            else sb.append('"').append(String.valueOf(e.getValue()).replace("\"", "'")).append('"');
        }
        return sb.append('}').toString();
    }
}
