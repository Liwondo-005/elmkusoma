package tz.elmkusoma.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    private static final UUID ADMIN_ID = UUID.fromString("b0000000-0000-0000-0000-000000000099");
    private static final UUID HQ_INSTITUTION_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final String ADMIN_EMAIL = "admin@elmkusoma.go.tz";

    @org.springframework.beans.factory.annotation.Value("${app.security.admin-bootstrap.enabled:false}")
    private boolean bootstrapEnabled;

    @org.springframework.beans.factory.annotation.Value("${ADMIN_BOOTSTRAP_PASSWORD:}")
    private String bootstrapPassword;

    @Override
    public void run(ApplicationArguments args) {
        // Security: bootstrap is opt-in and create-only. It NEVER resets the
        // password, role, or status of an existing account, so a restart can
        // no longer silently restore a default privileged credential.
        if (!bootstrapEnabled) {
            log.info("Admin bootstrap disabled; existing admin account left untouched");
            return;
        }
        if (bootstrapPassword == null || bootstrapPassword.isBlank()) {
            throw new IllegalStateException(
                    "Admin bootstrap is enabled but ADMIN_BOOTSTRAP_PASSWORD is not set; refusing to start");
        }
        try {
            var existing = userRepository.findByEmailAndIsDeletedFalse(ADMIN_EMAIL);
            if (existing.isPresent()) {
                log.info("Admin account already exists; bootstrap makes no credential changes: {}", ADMIN_EMAIL);
            } else {
                User admin = User.builder()
                        .email(ADMIN_EMAIL)
                        .passwordHash(passwordEncoder.encode(bootstrapPassword))
                        .firstName("Platform")
                        .lastName("Admin")
                        .role(User.Role.ADMIN)
                        .isActive(true)
                        .isEmailVerified(true)
                        .build();
                admin.setInstitutionId(HQ_INSTITUTION_ID);
                admin = userRepository.save(admin);
                log.info("Admin user created: {} ({})", ADMIN_EMAIL, admin.getId());
            }

            User adminUser = userRepository.findByEmailAndIsDeletedFalse(ADMIN_EMAIL)
                    .orElseThrow(() -> new IllegalStateException("Admin user missing after ensure"));
            UUID adminId = adminUser.getId() != null ? adminUser.getId() : ADMIN_ID;

            boolean membershipExists = membershipRepository
                    .existsByUserIdAndInstitutionIdAndIsActiveTrue(adminId, HQ_INSTITUTION_ID);
            if (!membershipExists) {
                tz.elmkusoma.shared.domain.InstitutionMembership membership =
                        tz.elmkusoma.shared.domain.InstitutionMembership.builder()
                                .userId(adminId)
                                .institutionId(HQ_INSTITUTION_ID)
                                .role(tz.elmkusoma.shared.domain.InstitutionMembership.Role.ADMIN)
                                .isActive(true)
                                .build();
                membershipRepository.save(membership);
                log.info("Admin institution membership created");
            }
        } catch (Exception e) {
            log.error("Failed to ensure admin user exists: {}", e.getMessage());
        }
    }
}
