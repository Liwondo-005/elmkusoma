package tz.elmkusoma.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class FlywayConfig {

    public FlywayConfig() {
        log.info("Flyway migrations are executed by Spring Boot auto-configuration (spring.flyway.enabled)");
    }
}
