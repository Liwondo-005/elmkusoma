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
    private static final String ADMIN_PASSWORD = "password";

    @Override
    public void run(ApplicationArguments args) {
        try {
            var existing = userRepository.findByEmailAndIsDeletedFalse(ADMIN_EMAIL);
            if (existing.isPresent()) {
                User user = existing.get();
                boolean needsUpdate = false;
                if (!passwordEncoder.matches(ADMIN_PASSWORD, user.getPasswordHash())) {
                    user.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
                    needsUpdate = true;
                }
                if (user.getRole() != User.Role.ADMIN) {
                    user.setRole(User.Role.ADMIN);
                    needsUpdate = true;
                }
                if (!Boolean.TRUE.equals(user.getIsActive())) {
                    user.setIsActive(true);
                    needsUpdate = true;
                }
                if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
                    user.setIsEmailVerified(true);
                    needsUpdate = true;
                }
                if (needsUpdate) {
                    userRepository.save(user);
                    log.info("Admin user updated: {}", ADMIN_EMAIL);
                }
            } else {
                User admin = User.builder()
                        .email(ADMIN_EMAIL)
                        .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
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
