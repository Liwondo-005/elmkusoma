package tz.elmkusoma.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.flywaydb.core.Flyway;

@Configuration
@Slf4j
public class FlywayConfig {

    @Bean
    public FlywayMigrationInitializer flywayInitializer() {
        log.info("Flyway migrations are managed externally. Schema is controlled by Hibernate ddl-auto.");
        return new FlywayMigrationInitializer(Flyway.configure().load()) {
            @Override
            public void afterPropertiesSet() {
                log.info("Flyway migration check skipped — using ddl-auto.");
            }
        };
    }
}
