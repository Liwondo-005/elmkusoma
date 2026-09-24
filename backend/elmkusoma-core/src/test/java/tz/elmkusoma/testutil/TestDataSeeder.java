package tz.elmkusoma.testutil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Seeds fixed test users, memberships, institution, and a live class for @SpringBootTest suites.
 * Runs once per Spring context (idempotent).
 * Uses JdbcTemplate inserts for pre-assigned UUID ids (repository.save/persist mis-handle @GeneratedValue UUIDs).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TestDataSeeder implements ApplicationRunner {

    public static final UUID INSTITUTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    public static final UUID TEACHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000015");
    public static final UUID STUDENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    public static final UUID LEARNER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");
    public static final UUID OTHER_STUDENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000040");
    public static final UUID CLASS_ID = UUID.fromString("00000000-0000-0000-0000-000000000050");

    private final InstitutionMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedInstitution();
        seedUser(TestTokens.ADMIN_EMAIL, ADMIN_USER_ID, User.Role.INSTITUTION_ADMIN, InstitutionMembership.Role.ADMIN);
        seedUser(TestTokens.TEACHER_EMAIL, TEACHER_USER_ID, User.Role.TEACHER, InstitutionMembership.Role.TEACHER);
        seedUser(TestTokens.STUDENT_EMAIL, STUDENT_USER_ID, User.Role.STUDENT, InstitutionMembership.Role.STUDENT);
        seedUser(TestTokens.LEARNER_EMAIL, LEARNER_USER_ID, User.Role.OTHER_LEARNER, InstitutionMembership.Role.STUDENT);
        seedUser(TestTokens.OTHER_STUDENT_EMAIL, OTHER_STUDENT_USER_ID, User.Role.STUDENT, InstitutionMembership.Role.STUDENT);
        seedLiveClass();
        log.info("TestDataSeeder completed");
    }

    private void seedInstitution() {
        if (exists("SELECT COUNT(*) FROM institutions WHERE id = ?", INSTITUTION_ID)) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO institutions (id, name, code, type, country, is_active, status, is_deleted, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                INSTITUTION_ID, "Test Institution", "TEST001", "SECONDARY", "Tanzania",
                true, "ACTIVE", false, LocalDateTime.now());
    }

    private void seedUser(String email, UUID id, User.Role role, InstitutionMembership.Role membershipRole) {
        if (!exists("SELECT COUNT(*) FROM users WHERE id = ?", id)) {
            jdbcTemplate.update(
                    "INSERT INTO users (id, institution_id, created_at, is_deleted, email, password_hash, "
                            + "first_name, last_name, role, is_active, is_email_verified) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    id, INSTITUTION_ID, LocalDateTime.now(), false,
                    email, passwordEncoder.encode("password"),
                    "Test", role.name(), role.name(),
                    true, true);
        }

        boolean hasMembership = membershipRepository
                .existsByUserIdAndInstitutionIdAndIsActiveTrue(id, INSTITUTION_ID);
        if (!hasMembership) {
            membershipRepository.save(InstitutionMembership.builder()
                    .userId(id)
                    .institutionId(INSTITUTION_ID)
                    .role(membershipRole)
                    .isActive(true)
                    .build());
        }
    }

    private void seedLiveClass() {
        if (exists("SELECT COUNT(*) FROM live_classes WHERE id = ?", CLASS_ID)) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO live_classes (id, institution_id, created_at, is_deleted, teacher_id, title, "
                        + "description, scheduled_at, duration_minutes, status, max_participants, "
                        + "recording_enabled, session_type, timezone, is_recurring, lobby_enabled) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                CLASS_ID, INSTITUTION_ID, LocalDateTime.now(), false,
                TEACHER_USER_ID, "Test Live Class", "Seeded for integration tests",
                LocalDateTime.now().plusHours(1), 60, "IN_PROGRESS", 100,
                true, "LECTURE", "Africa/Dar_es_Salaam", false, false);
    }

    private boolean exists(String sql, Object arg) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, arg);
        return count != null && count > 0;
    }
}
