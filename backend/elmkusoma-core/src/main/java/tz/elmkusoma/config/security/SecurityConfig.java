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

    // These filters are declared as beans AND added to the SecurityFilterChain.
    // Spring Boot would otherwise also auto-register them as standalone servlet
    // filters, which run before the chain and cause OncePerRequestFilter to skip
    // the in-chain copies — leaving SecurityContextHolder wiped by the chain's
    // SecurityContextHolderFilter and producing 403 on every authenticated
    // endpoint (regression of 12807f0). Disable the standalone registrations so
    // they only run inside the chain where the security context persists.
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<JwtRequestAttributeFilter> jwtRequestFilterRegistration(JwtRequestAttributeFilter filter) {
        FilterRegistrationBean<JwtRequestAttributeFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<OrganizationContextResolver> organizationContextResolverRegistration(OrganizationContextResolver resolver) {
        FilterRegistrationBean<OrganizationContextResolver> registration = new FilterRegistrationBean<>(resolver);
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
