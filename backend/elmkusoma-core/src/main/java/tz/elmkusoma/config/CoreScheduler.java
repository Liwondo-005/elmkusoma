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
import java.util.UUID;

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
    private final tz.elmkusoma.course.repository.LiveClassRepository liveClassRepository;
    private final tz.elmkusoma.teacher.repository.TeacherRepository teacherRepository;
    private final tz.elmkusoma.shared.repository.InstitutionMembershipRepository membershipRepository;

    /**
     * Authoritative live-class expiry. A started session ends when its ACTUAL start
     * (startedAt) plus configured duration has elapsed, so a class can never stay LIVE forever
     * because a teacher closed the tab or a client never sent the end call - and a late start
     * still gets the full duration it was configured for. The sweep runs the same completion
     * path as a manual end: attendance finalised, certificates issued, LiveKit egress stopped
     * and the replay created.
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
     * Live class start reminders, mirroring the existing event reminders above.
     *
     * <p>Events had a reminder sweep but live classes did not, so a learner who booked a session
     * had to remember to open the app: the "upcoming" list is the only thing that would have told
     * them. Live classes have no per-learner registration table, so the audience is the
     * institution's students (the same audience the class was announced to via
     * {@code notifyInstitutionStudentsExcluding}), with the hosting teacher excluded.
     *
     * <p>Idempotent exactly like the event path: the existing notification lookup on
     * (user, target, type) means a re-run never re-sends an offset that already went out.
     */
    @Scheduled(fixedRate = 300000, initialDelay = 45000)
    @Transactional
    public void sendLiveClassStartReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<tz.elmkusoma.course.domain.LiveClass> startingSoon =
                    liveClassRepository.findByStatusInAndIsDeletedFalse(List.of("SCHEDULED"));
            int sent = 0;
            for (tz.elmkusoma.course.domain.LiveClass liveClass : startingSoon) {
                if (liveClass.getScheduledAt() == null || liveClass.getInstitutionId() == null) {
                    continue;
                }
                long minutesUntilStart = ChronoUnit.MINUTES.between(now, liveClass.getScheduledAt());
                for (Map.Entry<String, Long> offset : REMINDER_OFFSETS_MINUTES.entrySet()) {
                    String type = offset.getKey();
                    long minutes = offset.getValue();
                    boolean due = minutesUntilStart <= minutes
                            && minutesUntilStart >= minutes - REMINDER_GRACE_MINUTES;
                    if (!due) {
                        continue;
                    }
                    sent += sendLiveClassReminder(liveClass, type, minutes);
                }
            }
            if (sent > 0) {
                log.info("Sent {} live class start reminders", sent);
            }
        } catch (Exception e) {
            log.error("Live class start reminders failed: {}", e.getMessage());
        }
    }

    /**
     * Sends one live-class reminder offset to the institution's students, one learner at a time.
     *
     * <p>Deliberately not the bulk {@code notifyInstitutionStudentsExcluding} used elsewhere: the
     * event reminder path above is idempotent through a per-learner
     * (user, target, notificationType) existence check, and a live-class offset needs the same
     * guard so the 5-minute sweep cannot re-notify every student. Delivery, persistence,
     * preference filtering and the unread badge still all go through the existing
     * NotificationService.
     */
    private int sendLiveClassReminder(tz.elmkusoma.course.domain.LiveClass liveClass,
                                     String notificationType, long minutesBefore) {
        String humanOffset = minutesBefore >= 60
                ? (minutesBefore / 60) + " hour(s)"
                : minutesBefore + " minutes";
        try {
            UUID teacherUserId = liveClass.getTeacherId() == null ? null
                    : teacherRepository.findById(liveClass.getTeacherId())
                            .map(tz.elmkusoma.teacher.domain.Teacher::getUserId).orElse(null);

            List<tz.elmkusoma.shared.domain.InstitutionMembership> members = membershipRepository
                    .findByInstitutionIdAndIsActiveTrue(liveClass.getInstitutionId());
            int sent = 0;
            for (tz.elmkusoma.shared.domain.InstitutionMembership member : members) {
                // OTHER_LEARNER is the non-Student learner seat in this product, and the
                // discovery endpoint already admits both roles, so a reminder that skipped it
                // would leave exactly the people who can see the class uninformed.
                if (member.getRole() != tz.elmkusoma.shared.domain.InstitutionMembership.Role.STUDENT
                        && member.getRole() != tz.elmkusoma.shared.domain.InstitutionMembership.Role.OTHER_LEARNER) {
                    continue;
                }
                if (teacherUserId != null && teacherUserId.equals(member.getUserId())) {
                    continue; // the host does not need a reminder about their own class
                }
                if (learnerNotificationRepository
                        .existsByUserIdAndTargetIdAndNotificationTypeAndIsDeletedFalse(
                                member.getUserId(), liveClass.getId(), notificationType)) {
                    continue; // already reminded for this offset
                }
                notificationService.notifyUser(
                        member.getUserId(),
                        "Live class starting soon: " + liveClass.getTitle(),
                        "\"" + liveClass.getTitle() + "\" starts in " + humanOffset + " ("
                                + liveClass.getScheduledAt() + "). Open Live Classes to join.",
                        notificationType, "live_class", liveClass.getId());
                sent++;
            }
            return sent;
        } catch (Exception e) {
            log.warn("Live class reminder failed for class {}: {}", liveClass.getId(), e.getMessage());
            return 0;
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
