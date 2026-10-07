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
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.domain.EventRegistration;
import tz.elmkusoma.event.repository.EventRegistrationRepository;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.identity.repository.TokenCleanupRepository;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.learner.service.NotificationService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CoreScheduler {

    private static final DateTimeFormatter HB = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Reminder offsets in minutes before event start (§18/§19). */
    private static final Map<String, Long> REMINDER_OFFSETS_MINUTES = Map.of(
            "EVENT_REMINDER_24H", 24L * 60,
            "EVENT_REMINDER_1H", 60L,
            "EVENT_REMINDER_15M", 15L);
    /** How long after each offset a due reminder is still sent (scheduler lag tolerance). */
    private static final long REMINDER_GRACE_MINUTES = 15;

    private final EventPublisherService eventPublisherService;
    private final AdminDelegationRepository delegationRepository;
    private final PlatformConfigRepository configRepository;
    private final DataGovernanceService dataGovernanceService;
    private final PlatformAdminService platformAdminService;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final LearnerNotificationRepository learnerNotificationRepository;
    private final TokenCleanupRepository tokenCleanupRepository;
    private final NotificationService notificationService;
    private final tz.elmkusoma.oversight.service.OversightAnnouncementService oversightAnnouncementService;
    private final tz.elmkusoma.course.service.LiveClassService liveClassService;

    /**
     * Authoritative live-class expiry. A started session ends when its scheduled start
     * plus configured duration has elapsed, so a class can never stay LIVE forever
     * because a teacher closed the tab or a client never sent the end call. The sweep
     * runs the same completion path as a manual end: attendance finalised,
     * certificates issued, LiveKit egress stopped and the replay created.
     */
    @Scheduled(fixedRate = 60000, initialDelay = 30000)
    public void endExpiredLiveClasses() {
        try {
            int ended = liveClassService.endExpiredSessions();
            if (ended > 0) {
                log.info("Auto-ended {} expired live class session(s)", ended);
            }
        } catch (Exception e) {
            log.warn("Live class expiry sweep failed: {}", e.getMessage());
        }
    }

    /** Nationaladmin.md §23 — publish scheduled jurisdictional announcements. */
    @Scheduled(fixedRate = 60000)
    public void publishDueAnnouncements() {
        try {
            int published = oversightAnnouncementService.publishDueAnnouncements();
            if (published > 0) {
                log.info("Published {} due jurisdictional announcement(s)", published);
            }
        } catch (Exception e) {
            log.warn("Announcement publish sweep failed: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            // Retain recently consumed/expired rows briefly for forensics, then purge.
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
            long purged = 0;
            purged += tokenCleanupRepository.deleteUsedOrExpiredResetTokens(weekAgo);
            purged += tokenCleanupRepository.deleteUsedOrExpiredEmailTokens(weekAgo);
            purged += tokenCleanupRepository.deleteUsedOrExpiredCodes(dayAgo);
            purged += tokenCleanupRepository.deleteExpiredRevocations(LocalDateTime.now());
            log.info("Token cleanup executed, purged {} rows", purged);
        } catch (Exception e) {
            log.warn("Token cleanup failed: {}", e.getMessage());
        }
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

    /**
     * §18/§19: notifies registrants about events starting in 24h / 1h / 15m.
     * Idempotent: skips any (user, event, offset) that already has a reminder
     * notification, so re-runs never spam. Uses the existing NotificationService.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void sendEventStartReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Event> upcoming = eventRepository.findEventsStartingWithin(now, now.plusHours(24));
            int sent = 0;
            for (Event event : upcoming) {
                if (event.getStartsAt() == null || Boolean.TRUE.equals(event.getIsDeleted())) {
                    continue;
                }
                long minutesUntilStart = ChronoUnit.MINUTES.between(now, event.getStartsAt());
                for (Map.Entry<String, Long> offset : REMINDER_OFFSETS_MINUTES.entrySet()) {
                    String type = offset.getKey();
                    long minutes = offset.getValue();
                    boolean due = minutesUntilStart <= minutes
                            && minutesUntilStart >= minutes - REMINDER_GRACE_MINUTES;
                    if (!due) {
                        continue;
                    }
                    sent += sendReminderToRegistrants(event, type, minutes);
                }
            }
            if (sent > 0) {
                log.info("Sent {} event start reminders", sent);
            }
        } catch (Exception e) {
            log.error("Event start reminders failed: {}", e.getMessage());
        }
    }

    private int sendReminderToRegistrants(Event event, String notificationType, long minutesBefore) {
        int sent = 0;
        List<EventRegistration> regs = registrationRepository.findByEventIdAndIsDeletedFalse(event.getId());
        for (EventRegistration reg : regs) {
            if ("CANCELLED".equals(reg.getStatus())) {
                continue;
            }
            try {
                if (learnerNotificationRepository.existsByUserIdAndTargetIdAndNotificationTypeAndIsDeletedFalse(
                        reg.getUserId(), event.getId(), notificationType)) {
                    continue; // idempotent — already reminded for this offset
                }
                String humanOffset = minutesBefore >= 60
                        ? (minutesBefore / 60) + " hour(s)"
                        : minutesBefore + " minutes";
                notificationService.notifyUser(reg.getUserId(),
                        "Reminder: " + event.getTitle() + " starts in " + humanOffset,
                        "\"" + event.getTitle() + "\" starts at " + event.getStartsAt()
                                + (event.getTimezone() != null ? " (" + event.getTimezone() + ")" : "")
                                + ". See your registered events to join.",
                        notificationType, "event", event.getId());
                sent++;
            } catch (Exception e) {
                log.warn("Reminder failed for event {} user {}: {}",
                        event.getId(), reg.getUserId(), e.getMessage());
            }
        }
        return sent;
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
