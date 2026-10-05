package tz.elmkusoma.workers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Trust model (workers is a background consumer, not a user-facing API):
        // - Only reachable HTTP surface is public: GET /api/v1/workers/health,
        //   actuator health/info, and springdoc docs. No controller requires auth.
        // - Removed dead app.jwt.secret: nothing ever read it (no JWT filter here).
        // - anyRequest().authenticated() + httpBasic is deny-by-default for any
        //   future endpoint. No UserDetailsService/users are configured, so there
        //   are no httpBasic principals — a future authenticated endpoint would
        //   fail closed until real auth (prefer JWT like realtime/media) is added.
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/workers/health").permitAll()
                .requestMatchers("/api/v1/workers/docs/**", "/api/v1/workers/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {});

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
