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

    @Scheduled(cron = "0 0 8 * * ?")
    public void processNotificationDigest() {
        log.info("Starting daily notification digest processing at 8:00 AM");

        try {
            long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(1L);
            log.info("Notification digest processed. Total unread notifications: {}", unreadCount);

        } catch (Exception e) {
            log.error("Failed to process notification digest: {}", e.getMessage(), e);
        }
    }
}
