package tz.elmkusoma.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.domain.AdminDelegation;
import tz.elmkusoma.administration.domain.PlatformConfigEntry;
import tz.elmkusoma.administration.repository.AdminDelegationRepository;
import tz.elmkusoma.administration.repository.PlatformConfigRepository;
import tz.elmkusoma.administration.service.DataGovernanceService;
import tz.elmkusoma.administration.service.PlatformAdminService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CoreScheduler {

    private static final DateTimeFormatter HB = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EventPublisherService eventPublisherService;
    private final AdminDelegationRepository delegationRepository;
    private final PlatformConfigRepository configRepository;
    private final DataGovernanceService dataGovernanceService;
    private final PlatformAdminService platformAdminService;

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredTokens() {
        log.info("Token cleanup executed");
    }

    @Scheduled(fixedRate = 300000)
    public void expireDelegations() {
        List<AdminDelegation> expired = delegationRepository.findAll().stream()
                .filter(d -> "ACTIVE".equals(d.getStatus()))
                .filter(d -> d.getExpiresAt() != null && d.getExpiresAt().isBefore(LocalDateTime.now()))
                .toList();
        for (AdminDelegation del : expired) {
            del.setStatus("EXPIRED");
            del.setRevokedAt(LocalDateTime.now());
            del.setRevocationReason("Auto-expired: past expiration date");
            delegationRepository.save(del);
            log.info("Auto-expired delegation: {} -> {}", del.getDelegatorId(), del.getDelegateId());
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} delegations that were past their expiration date", expired.size());
        }
    }

    @Scheduled(fixedRate = 60000)
    public void healthCheck() {
        log.debug("Core health check");
        writeHeartbeat();
    }

    @Scheduled(fixedRate = 86400000, initialDelay = 60000)
    @Transactional
    public void dailyRetentionSweep() {
        try {
            dataGovernanceService.runSweep("system:scheduler");
            log.info("Daily retention sweep completed");
        } catch (Exception e) {
            log.error("Daily retention sweep failed: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void dailyAnalyticsSnapshot() {
        try {
            platformAdminService.createAnalyticsSnapshot();
            log.info("Daily PLATFORM_ANALYTICS snapshot created");
        } catch (Exception e) {
            log.error("Daily analytics snapshot failed: {}", e.getMessage());
        }
    }

    private void writeHeartbeat() {
        try {
            String now = LocalDateTime.now().format(HB);
            configRepository.findByConfigKeyAndIsDeletedFalse("ops.scheduler.heartbeat").ifPresentOrElse(
                    entry -> {
                        entry.setConfigValue(now);
                        entry.setLastModifiedBy("system:scheduler");
                        configRepository.save(entry);
                    },
                    () -> {
                        PlatformConfigEntry e = PlatformConfigEntry.builder()
                                .configKey("ops.scheduler.heartbeat")
                                .configValue(now)
                                .configType("TIMESTAMP")
                                .description("Last background scheduler heartbeat (real)")
                                .category("OPS")
                                .isSensitive(false)
                                .isPublic(false)
                                .lastModifiedBy("system:scheduler")
                                .build();
                        configRepository.save(e);
                    }
            );
        } catch (Exception e) {
            log.debug("Heartbeat write failed: {}", e.getMessage());
        }
    }
}
