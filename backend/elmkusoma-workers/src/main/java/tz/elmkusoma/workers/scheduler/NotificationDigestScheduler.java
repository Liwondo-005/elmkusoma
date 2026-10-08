package tz.elmkusoma.workers.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tz.elmkusoma.workers.repository.WorkerNotificationRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDigestScheduler {

    private final WorkerNotificationRepository notificationRepository;

    /**
     * Audit B-06: this digest was hardcoded to user id 1, which under core's UUID key space
     * matches no account, so the job always reported 0. It now aggregates across all recipients,
     * which is what a platform-wide digest means.
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void processNotificationDigest() {
        log.info("Starting daily notification digest processing at 8:00 AM");

        try {
            long totalUnread = notificationRepository.countByIsReadFalse();
            log.info("Notification digest processed. Total unread notifications: {}", totalUnread);

        } catch (Exception e) {
            log.error("Failed to process notification digest: {}", e.getMessage(), e);
        }
    }
}
