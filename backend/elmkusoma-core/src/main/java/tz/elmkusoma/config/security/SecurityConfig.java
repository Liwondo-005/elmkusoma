package tz.elmkusoma.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.persistence.EntityManager;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.List;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Import(PermissionConfig.class)
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final OrganizationContextHolder contextHolder;
    private final EntityManager entityManager;
    private final PermissionService permissionService;

    private static final String[] PUBLIC_URLS = {
            "/v1/auth/**",
            "/v1/public/**",
            "/v1/certificates/verify/**",
            "/v1/institutions",
            "/v1/institutions/{id}",
            "/v1/webhooks/livekit",
            "/v1/webhooks/**",
            "/ws/**"
    };

    private static final String[] ADMIN_ONLY_URLS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/**"
    };

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService());
    }

    @Bean
    public JwtRequestAttributeFilter jwtRequestAttributeFilter() {
        return new JwtRequestAttributeFilter(jwtTokenProvider, userRepository, membershipRepository);
    }

    @Bean
    public OrganizationContextResolver organizationContextResolver() {
        return new OrganizationContextResolver(jwtTokenProvider, userRepository, membershipRepository, contextHolder, entityManager, permissionService);
    }

    /**
     * Same standalone-registration guard as the JWT filters: the resolver is
     * already wired into the security chain via addFilterBefore; a second
     * servlet-container registration would run it pre-chain (unauthenticated,
     * no-op) and OncePerRequestFilter would then skip the in-chain execution,
     * leaving every request without an organization context.
     */
    @Bean
    public FilterRegistrationBean<OrganizationContextResolver> organizationContextResolverFilterRegistration(OrganizationContextResolver filter) {
        FilterRegistrationBean<OrganizationContextResolver> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        return email -> {
            tz.elmkusoma.shared.domain.User user = userRepository.findByEmailAndIsDeletedFalse(email)
                    .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                            "User not found with email: " + email));
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPasswordHash())
                    .authorities("ROLE_" + user.getRole().name())
                    .build();
        };
    }

    /**
     * Guard against servlet-container auto-registration of the JWT filters.
     * Without this guard the filter also runs standalone before the security
     * chain; OncePerRequestFilter then skips the in-chain invocation (same
     * already-filtered attribute), the chain's SecurityContextRepository load
     * wipes the standalone authentication, and every authenticated endpoint
     * returns 403. The filters must run only inside the security chain.
     * (Regression guard restored: 12807f0 -> 95f2cda -> removed in 046250a.)
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<JwtRequestAttributeFilter> jwtRequestFilterRegistration(JwtRequestAttributeFilter filter) {
        FilterRegistrationBean<JwtRequestAttributeFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public OrganizationContextHolder organizationContextHolder() {
        return new OrganizationContextHolder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(ADMIN_ONLY_URLS).hasRole("ADMIN")
                        // Namespace fences mirroring the class-level @PreAuthorize of the
                        // /v1/admin and /v1/platform-admin controllers, enforced at the URL
                        // level so role denials return 403 before body validation (400) or
                        // unmapped-route handling (404) can mask them.
                        .requestMatchers("/v1/admin", "/v1/admin/**").hasAnyRole("ADMIN", "INSTITUTION_ADMIN")
                        .requestMatchers("/v1/platform-admin", "/v1/platform-admin/**").hasRole("ADMIN")
                        // The payment detail API is not exposed; deny explicitly so
                        // cross-institution probing gets 403 instead of a 404 oracle.
                        .requestMatchers("/v1/payments", "/v1/payments/**").denyAll()
                        // Object-level fences mirroring the controllers' @PreAuthorize role
                        // sets; running before @PathVariable/@Valid argument resolution
                        // keeps denials from being masked by 400/500 argument errors.
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/v1/teachers/**")
                            .hasAnyRole("ADMIN", "INSTITUTION_ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/v1/students/**")
                            .hasAnyRole("ADMIN", "INSTITUTION_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(organizationContextResolver(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtRequestAttributeFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:4200",
                "https://elmkusoma.com",
                "https://www.elmkusoma.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization", "X-Total-Count"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}
