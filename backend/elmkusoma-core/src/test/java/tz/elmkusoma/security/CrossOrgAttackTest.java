package tz.elmkusoma.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class CrossOrgAttackTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private tz.elmkusoma.certificate.repository.CertificateRepository certificateRepository;

    @Autowired
    private tz.elmkusoma.course.repository.CourseRepository courseRepository;

    @Autowired
    private tz.elmkusoma.shared.repository.InstitutionRepository institutionRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void eventIdManipulation_CrossInstitution_Returns403() throws Exception {
        String adminToken = TestTokens.adminToken();
        String otherStudentToken = TestTokens.otherStudentToken();

        // Admin creates event in institution A
        String eventId = "00000000-0000-0000-0000-000000000099";

        // Other student from institution B tries to access
        mockMvc.perform(get("/v1/events/" + eventId)
                .header("Authorization", "Bearer " + otherStudentToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void institutionIdManipulation_CrossOrg_Returns403() throws Exception {
        String adminToken = TestTokens.adminToken();

        // Try to access another institution's data
        mockMvc.perform(get("/v1/admin/people")
                .header("Authorization", "Bearer " + adminToken)
                .header("X-Institution-Id", "11111111-1111-1111-1111-111111111111"))
            .andExpect(status().isForbidden());
    }

    @Test
    void courseIdManipulation_CrossInstitution_Returns403() throws Exception {
        String studentToken = TestTokens.studentToken();

        mockMvc.perform(get("/v1/courses/00000000-0000-0000-0000-000000000088")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void paymentIdManipulation_CrossInstitution_Returns403() throws Exception {
        String adminToken = TestTokens.adminToken();

        mockMvc.perform(get("/v1/payments/00000000-0000-0000-0000-000000000077")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void certificateIdManipulation_CrossInstitution_Returns403() throws Exception {
        String adminToken = TestTokens.adminToken();

        // /v1/certificates/verify/** is intentionally public (permitAll), so the
        // tenant-isolation check lives on the authenticated detail endpoint:
        // seed a certificate that belongs to a DIFFERENT institution than the
        // admin token's (…0001) and expect 403 on /v1/certificates/{id}.
        tz.elmkusoma.certificate.domain.Certificate foreign =
                tz.elmkusoma.certificate.domain.Certificate.builder()
                        .templateId(java.util.UUID.randomUUID())
                        .studentId(java.util.UUID.randomUUID())
                        .issuedBy(java.util.UUID.randomUUID())
                        .serialNumber("XORG-TEST-066")
                        .certificateType(tz.elmkusoma.certificate.domain.Certificate.CertificateType.COMPLETION)
                        .title("Cross-org certificate")
                        .studentName("Other Institution Student")
                        .completionDate(java.time.LocalDate.now())
                        .issueDate(java.time.LocalDateTime.now())
                        .verificationCode("xorg-verify-066")
                        .status(tz.elmkusoma.certificate.domain.Certificate.CertificateStatus.ISSUED)
                        .build();
        foreign.setInstitutionId(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"));
        foreign = certificateRepository.save(foreign);

        mockMvc.perform(get("/v1/certificates/" + foreign.getId())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void fileIdManipulation_CrossInstitution_Returns403() throws Exception {
        String studentToken = TestTokens.studentToken();

        mockMvc.perform(get("/v1/media/00000000-0000-0000-0000-000000000055")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void exportIdManipulation_CrossInstitution_Returns403() throws Exception {
        String adminToken = TestTokens.adminToken();

        mockMvc.perform(get("/v1/admin/export")
                .header("Authorization", "Bearer " + adminToken)
                .param("entityType", "users")
                .header("X-Institution-Id", "22222222-2222-2222-2222-222222222222"))
            .andExpect(status().isForbidden());
    }

    // ── learner course scoping (cross-institution regression, Phase 9) ──
    //
    // Client-supplied course/live-class ids on /v1/learner/** must resolve
    // inside the caller's institution or fail with 404 (no existence oracle).
    // Own-institution controls prove the guard does not break valid access.

    @Test
    void learnerCourseDetailManipulation_CrossInstitution_Returns404() throws Exception {
        String studentToken = TestTokens.studentToken();

        tz.elmkusoma.course.domain.Course own = courseRepository.save(
                tz.elmkusoma.course.domain.Course.builder()
                        .institutionId(tz.elmkusoma.testutil.TestDataSeeder.INSTITUTION_ID)
                        .title("XOrg own course")
                        .level("SECONDARY")
                        .isPublished(true)
                        .isFeatured(false)
                        .isDeleted(false)
                        .build());
        mockMvc.perform(get("/v1/learner/courses/" + own.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        tz.elmkusoma.course.domain.Course foreign = courseRepository.save(
                tz.elmkusoma.course.domain.Course.builder()
                        .institutionId(saveForeignInstitution())
                        .title("XOrg foreign course")
                        .level("SECONDARY")
                        .isPublished(true)
                        .isFeatured(false)
                        .isDeleted(false)
                        .build());
        mockMvc.perform(get("/v1/learner/courses/" + foreign.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void learnerCourseSubResourcesManipulation_CrossInstitution_Returns404() throws Exception {
        String studentToken = TestTokens.studentToken();
        UUID foreignCourse = courseRepository.save(
                tz.elmkusoma.course.domain.Course.builder()
                        .institutionId(saveForeignInstitution())
                        .title("XOrg foreign course subs")
                        .level("SECONDARY")
                        .isPublished(true)
                        .isFeatured(false)
                        .isDeleted(false)
                        .build()).getId();

        mockMvc.perform(get("/v1/learner/courses/" + foreignCourse + "/modules")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/learner/courses/" + foreignCourse + "/lessons")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/learner/courses/" + foreignCourse + "/related")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/learner/me/progress/" + foreignCourse)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void learnerEnrollmentManipulation_CrossInstitution_Returns404() throws Exception {
        String studentToken = TestTokens.studentToken();
        UUID foreignCourse = courseRepository.save(
                tz.elmkusoma.course.domain.Course.builder()
                        .institutionId(saveForeignInstitution())
                        .title("XOrg foreign course enroll")
                        .level("SECONDARY")
                        .isPublished(true)
                        .isFeatured(false)
                        .isDeleted(false)
                        .build()).getId();

        mockMvc.perform(post("/v1/learner/me/enrollments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + foreignCourse + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void learnerLiveClassManipulation_CrossInstitution_Returns404() throws Exception {
        String studentToken = TestTokens.studentToken();
        UUID foreignLiveClass = saveForeignLiveClass(saveForeignInstitution());

        mockMvc.perform(get("/v1/learner/live-classes/" + foreignLiveClass + "/participants")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/learner/live-classes/" + foreignLiveClass + "/related")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/v1/learner/live-classes/" + foreignLiveClass + "/join")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    private UUID saveForeignInstitution() {
        tz.elmkusoma.shared.domain.Institution foreign =
                institutionRepository.save(tz.elmkusoma.shared.domain.Institution.builder()
                        .name("XOrg Attack Institution " + UUID.randomUUID().toString().substring(0, 8))
                        .code("XORG-" + UUID.randomUUID().toString().substring(0, 8))
                        .type(tz.elmkusoma.shared.domain.Institution.InstitutionType.SECONDARY)
                        .country("Tanzania")
                        .isActive(true)
                        .isDeleted(false)
                        .build());
        return foreign.getId();
    }

    private UUID saveForeignLiveClass(UUID foreignInstitutionId) {
        UUID liveClassId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO live_classes (id, institution_id, created_at, is_deleted, teacher_id, title, "
                        + "description, scheduled_at, duration_minutes, status, max_participants, "
                        + "recording_enabled, session_type, timezone, is_recurring, lobby_enabled) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                liveClassId, foreignInstitutionId, java.time.LocalDateTime.now(), false,
                tz.elmkusoma.testutil.TestDataSeeder.TEACHER_USER_ID, "XOrg foreign live class",
                "Cross-org isolation probe",
                java.time.LocalDateTime.now().plusHours(1), 60, "SCHEDULED", 50,
                false, "LECTURE", "Africa/Dar_es_Salaam", false, false);
        return liveClassId;
    }
}