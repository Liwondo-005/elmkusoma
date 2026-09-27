package tz.elmkusoma.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PermissionConfig {

    @Bean
    public PermissionService permissionService() {
        return new PermissionService();
    }
}