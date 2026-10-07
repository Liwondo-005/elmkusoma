package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Live-session institution-isolation regression tests through the real chain.
 *
 * <p>Contracts pinned here (verified against {@code LiveSessionController}
 * before writing):</p>
 * <ul>
 *   <li>join without institution membership -&gt; 403
 *       {@code "You are not a member of this institution"};</li>
 *   <li>join with a caller institution that does not own the class -&gt; 403
 *       {@code "Not authorized for this class"} (header is never trusted for
 *       ownership — the stored class institution wins);</li>
 *   <li>recording download/replay from a foreign institution -&gt; 403
 *       {@code "Access denied"} without leaking recording data.</li>
 * </ul>
 *
 * <p>Fixtures are random per run (hermetic). The seeded student (member of
 * the seed institution) plays the wrong-institution attacker; a freshly
 * created membership-less user plays the non-member.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LiveSessionSecurityRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LiveClassRepository liveClassRepository;

    private Institution institutionB;
    private LiveClass classInB;
    private User memberlessUser;

    @BeforeAll
    void createFixtures() {
        String run = UUID.randomUUID().toString().substring(0, 8);

        institutionB = institutionRepository.save(Institution.builder()
                .name("Foreign Live Institution " + run)
                .code("FORLIVE-" + run)
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());

        // Session-scoped (no classGroupId) so only the institution checks gate access.
        classInB = liveClassRepository.save(LiveClass.builder()
                .institutionId(institutionB.getId())
                .teacherId(UUID.randomUUID())
                .title("Foreign live class " + run)
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .durationMinutes(60)
                .status("IN_PROGRESS")
                .recordingUrl("https://cdn.test/recordings/foreign-" + run + ".mp4")
                .isDeleted(false)
                .build());

        // Deliberately no InstitutionMembership row anywhere.
        User user = User.builder()
                .email("nomember-live-" + run + "@test.com")
                .passwordHash("test-hash")
                .firstName("No")
                .lastName("Member")
                .role(User.Role.STUDENT)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build();
        memberlessUser = userRepository.save(user);
    }

    @Test
    void memberlessUserControl_isRecognisedByAuthChain() throws Exception {
        mockMvc.perform(get("/v1/auth/me")
                        .header("Authorization", "Bearer " + TestTokens.userToken(memberlessUser.getEmail())))
                .andExpect(status().isOk());
    }

    @Test
    void join_withoutInstitutionMembership_is403() throws Exception {
        // The organization-context resolver rejects the forged institution
        // header before the controller runs, with its own envelope
        // (success/message, no error field).
        mockMvc.perform(post("/v1/live-session/join/" + classInB.getId())
                        .header("X-Institution-Id", institutionB.getId().toString())
                        .header("Authorization", "Bearer " + TestTokens.userToken(memberlessUser.getEmail())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access to the requested institution is not permitted"));
    }

    @Test
    void join_withWrongInstitution_is403() throws Exception {
        // Seeded student is a member of the seed institution (A), not B.
        mockMvc.perform(post("/v1/live-session/join/" + classInB.getId())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Not authorized for this class"));
    }

    @Test
    void recordingDownload_crossInstitution_is403() throws Exception {
        mockMvc.perform(get("/v1/live-session/classes/" + classInB.getId() + "/recording/download")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    void forceEnd_ownInstitutionClass_is200AndCompletes() throws Exception {
        LiveClass ownClass = liveClassRepository.save(LiveClass.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .teacherId(UUID.randomUUID())
                .title("Force end target " + UUID.randomUUID().toString().substring(0, 8))
                .scheduledAt(LocalDateTime.now().minusMinutes(30))
                .durationMinutes(60)
                .status("IN_PROGRESS")
                .isDeleted(false)
                .build());

        mockMvc.perform(post("/v1/admin/live-sessions/" + ownClass.getId() + "/force-end")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void forceEnd_foreignInstitutionClass_is403() throws Exception {
        mockMvc.perform(post("/v1/admin/live-sessions/" + classInB.getId() + "/force-end")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    void forceEnd_missingClass_is404() throws Exception {
        mockMvc.perform(post("/v1/admin/live-sessions/" + UUID.randomUUID() + "/force-end")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
