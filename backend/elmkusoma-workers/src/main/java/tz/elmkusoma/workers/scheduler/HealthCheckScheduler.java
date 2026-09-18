package tz.elmkusoma.workers.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HealthCheckScheduler {

    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 60000)
    public void heartbeat() {
        log.debug("Workers heartbeat: system is running");

        try {
            rabbitTemplate.getConnectionFactory().createConnection();
            log.debug("RabbitMQ connection is healthy");
        } catch (Exception e) {
            log.warn("RabbitMQ connection check failed: {}", e.getMessage());
        }
    }
}
