package tz.elmkusoma.administration.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tz.elmkusoma.administration.service.RegionalAdminService;

/**
 * Materialises due scheduled reports (PROMPT §45) through the shared Spring
 * scheduler already configured for the platform — no separate queue, no
 * synthetic data: every run snapshots real oversight figures inside the
 * report owner's jurisdiction.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegionalReportScheduler {

    private final RegionalAdminService regionalAdminService;

    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void runDueScheduledReports() {
        try {
            int executed = regionalAdminService.runDueScheduledReports();
            if (executed > 0) {
                log.info("Executed {} due scheduled report(s)", executed);
            }
        } catch (Exception ex) {
            log.warn("Scheduled report sweep failed: {}", ex.getMessage());
        }
    }
}
