package tz.elmkusoma.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CoreScheduler {

    private final EventPublisherService eventPublisherService;

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredTokens() {
        log.info("Token cleanup executed");
    }

    @Scheduled(fixedRate = 60000)
    public void healthCheck() {
        log.debug("Core health check");
    }
}
