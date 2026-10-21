package tz.elmkusoma.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teacher formal-verification gate: a provisionally verified teacher may manage their
 * profile, qualifications and draft content, but backend authorization blocks every
 * publish/delivery operation until a Platform Admin grants formal verification — and a
 * suspension blocks immediately while leaving login and profile access intact.
 * Existing published content stays visible to learners (block new actions only).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TeacherVerificationGateSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private InstitutionMembershipRepository membershipRepository;
    @Autowired private tz.elmkusoma.course.repository.LiveClassRepository liveClassRepository;

    private String platformToken;
    private UUID institutionId;

    @BeforeAll
    void fixtures() {
        institutionId = TestDataSeeder.INSTITUTION_ID;
        // Platform-level ADMIN (distinct from the seeded INSTITUTION_ADMIN used elsewhere).
        String adminEmail = "tv-admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        var adminUser = userRepository.save(tz.elmkusoma.shared.domain.User.builder()
                .email(adminEmail).passwordHash("test-hash")
                .firstName("TV").lastName("Admin")
                .role(tz.elmkusoma.shared.domain.User.Role.ADMIN)
                .institutionId(institutionId).isActive(true).isEmailVerified(true).isDeleted(false)
                .build());
        platformToken = TestTokens.userToken(adminEmail);
    }

    private record TeacherFixture(String email, String token, UUID userId) {}

    /** A fresh teacher user: no verification record exists, so their state is PROVISIONAL. */
    private TeacherFixture newTeacher(String prefix, UUID affiliatedInstitutionId) {
        String email = "tv-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        var user = userRepository.save(tz.elmkusoma.shared.domain.User.builder()
                .email(email).passwordHash("test-hash")
                .firstName("TV").lastName("Teacher")
                .role(tz.elmkusoma.shared.domain.User.Role.TEACHER)
                .institutionId(affiliatedInstitutionId)
                .isActive(true).isEmailVerified(true).isDeleted(false)
                .build());
        if (affiliatedInstitutionId != null) {
            membershipRepository.save(tz.elmkusoma.shared.domain.InstitutionMembership.builder()
                    .userId(user.getId()).institutionId(affiliatedInstitutionId)
                    .role(tz.elmkusoma.shared.domain.InstitutionMembership.Role.TEACHER)
                    .isActive(true).build());
        }
        return new TeacherFixture(email, TestTokens.userToken(email), user.getId());
    }

    /** Drive the real workflow: self-service submission + Platform Admin approval. */
    private void submitAndApprove(TeacherFixture teacher, String serviceType, String documentType)
            throws Exception {
        MvcResult submitted = mockMvc.perform(post("/v1/teachers/me/verification/submit")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceTypes\":[\"" + serviceType + "\"],"
                                + "\"documents\":[{\"type\":\"" + documentType
                                + "\",\"url\":\"https://docs.test/cert.pdf\"}]}"))
                .andExpect(status().isCreated())
                .andReturn();
        String recordId = JsonPath.read(submitted.getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(put("/v1/platform-admin/verifications/" + recordId + "/review")
                        .param("status", "APPROVED").param("notes", "Documents verified")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk());
    }

    // ── requirement: provisional teacher MUST NOT publish services ────────────────────

    @Test
    void provisionalTeacherCannotPublishOffering_butMaySaveDrafts() throws Exception {
        TeacherFixture teacher = newTeacher("off", institutionId);

        mockMvc.perform(post("/v1/teachers/me/offerings")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Provisional offering\",\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/v1/teachers/me/offerings")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Draft offering\"}"))
                .andExpect(status().isCreated());

        // An independent (school-less) teacher hits the same gate without any institution context.
        TeacherFixture independent = newTeacher("off-ind", null);
        mockMvc.perform(post("/v1/teachers/me/offerings")
                        .header("Authorization", "Bearer " + independent.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Independent offering\",\"independent\":true,"
                                + "\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void provisionalTeacherCannotPublishOrGoLiveWithEvent() throws Exception {
        TeacherFixture teacher = newTeacher("evt", institutionId);
        String startsAt = LocalDateTime.now().plusDays(7).withNano(0).toString();

        mockMvc.perform(post("/v1/events")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Published event\",\"eventType\":\"LECTURE\","
                                + "\"startsAt\":\"" + startsAt + "\",\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isForbidden());

        MvcResult created = mockMvc.perform(post("/v1/events")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Draft event\",\"eventType\":\"LECTURE\","
                                + "\"startsAt\":\"" + startsAt + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String eventId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        // Neither publishing nor going live is reachable for a provisional teacher.
        mockMvc.perform(post("/v1/events/" + eventId + "/publish")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/events/" + eventId + "/start-live")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void liveClassStart_requiresPlatformApprovalAndVerifiedTeacher() throws Exception {
        // Provisional teacher: creation (draft) is fine, starting is refused outright.
        TeacherFixture provisional = newTeacher("lc", institutionId);
        MvcResult created = mockMvc.perform(post("/v1/teachers/me/live-classes")
                        .header("Authorization", "Bearer " + provisional.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Provisional class\",\"scheduledAt\":\""
                                + LocalDateTime.now().plusHours(7).withNano(0)
                                + "\",\"durationMinutes\":30}"))
                .andExpect(status().isCreated())
                .andReturn();
        String provisionalClassId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(post("/v1/teachers/me/live-classes/" + provisionalClassId + "/start")
                        .header("Authorization", "Bearer " + provisional.token())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden());

        // Formally verified teacher (seeded fixture), but the class has no platform approval:
        // the delivery gate refuses an unpublished class even for an approved teacher.
        MvcResult approvedClass = mockMvc.perform(post("/v1/teachers/me/live-classes")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Approved teacher draft class\",\"scheduledAt\":\""
                                + LocalDateTime.now().plusHours(15).withNano(0)
                                + "\",\"durationMinutes\":30}"))
                .andExpect(status().isCreated())
                .andReturn();
        String classId = JsonPath.read(approvedClass.getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(post("/v1/teachers/me/live-classes/" + classId + "/start")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden());

        // After platform approval (governance step), the verified teacher can start.
        liveClassRepository.findById(UUID.fromString(classId)).ifPresent(lc -> {
            lc.setPublicationStatus("PUBLISHED");
            liveClassRepository.save(lc);
        });
        mockMvc.perform(post("/v1/teachers/me/live-classes/" + classId + "/start")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void provisionalTeacherCanManageProfileQualificationsAndDraftContent() throws Exception {
        // Independent teacher: no institution context anywhere.
        TeacherFixture independent = newTeacher("qual", null);

        mockMvc.perform(get("/v1/teachers/me/profile")
                        .header("Authorization", "Bearer " + independent.token()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/v1/teachers/me/profile")
                        .header("Authorization", "Bearer " + independent.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"Independent teacher bio\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/teachers/me/qualifications")
                        .header("Authorization", "Bearer " + independent.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(post("/v1/teachers/me/qualifications")
                        .header("Authorization", "Bearer " + independent.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qualificationName\":\"B.Ed\",\"institutionName\":\"UDOM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.qualificationName").value("B.Ed"));

        // Verification view is readable while provisional and carries the document matrix.
        mockMvc.perform(get("/v1/teachers/me/verification")
                        .header("Authorization", "Bearer " + independent.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectiveStatus").value("PROVISIONAL"))
                .andExpect(jsonPath("$.data.formallyVerified").value(false))
                .andExpect(jsonPath("$.data.requiredDocumentsByServiceType['LIVE_CLASS']").exists());

        // Lesson and video publish endpoints are gated before anything else runs.
        // (Affiliated teacher: these routes require the institution request attribute.)
        TeacherFixture affiliated = newTeacher("res", institutionId);
        mockMvc.perform(post("/v1/learning/lessons/" + UUID.randomUUID() + "/publish")
                        .header("Authorization", "Bearer " + affiliated.token())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/video-tutorials/" + UUID.randomUUID() + "/publish")
                        .header("Authorization", "Bearer " + affiliated.token())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden());

        // Resource PUBLIC visibility is a publish action; anything less is free.
        mockMvc.perform(post("/v1/resources")
                        .header("Authorization", "Bearer " + affiliated.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Public resource\",\"resourceType\":\"PDF\","
                                + "\"storageUrl\":\"https://storage.test/x.pdf\",\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/resources")
                        .header("Authorization", "Bearer " + affiliated.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Draft resource\",\"resourceType\":\"PDF\","
                                + "\"storageUrl\":\"https://storage.test/x.pdf\"}"))
                .andExpect(status().isCreated());
    }

    // ── requirement: documents by category + Platform Admin is the only approver ──────

    @Test
    void submission_validatesDocumentMatrix_andOnlyPlatformAdminReviews() throws Exception {
        TeacherFixture teacher = newTeacher("sub", institutionId);

        // Preliminary automated check: required documents depend on the service category.
        mockMvc.perform(post("/v1/teachers/me/verification/submit")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceTypes\":[\"LIVE_CLASS\"],\"documents\":["
                                + "{\"type\":\"TEACHING_QUALIFICATION\",\"url\":\"https://docs.test/q.pdf\"}]}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/v1/teachers/me/verification/submit")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceTypes\":[\"NOT_A_SERVICE\"],\"documents\":["
                                + "{\"type\":\"TEACHING_QUALIFICATION\",\"url\":\"https://docs.test/q.pdf\"}]}"))
                .andExpect(status().isBadRequest());

        MvcResult submitted = mockMvc.perform(post("/v1/teachers/me/verification/submit")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceTypes\":[\"LIVE_CLASS\"],\"notes\":\"First application\","
                                + "\"documents\":["
                                + "{\"type\":\"TEACHING_QUALIFICATION\",\"url\":\"https://docs.test/q.pdf\"},"
                                + "{\"type\":\"CERTIFICATE\",\"url\":\"https://docs.test/c.pdf\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        String recordId = JsonPath.read(submitted.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(get("/v1/teachers/me/verification")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectiveStatus").value("PENDING"));

        // A second application while one is pending is refused (existing governance rule).
        mockMvc.perform(post("/v1/teachers/me/verification/submit")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceTypes\":[\"COURSE\"],\"documents\":["
                                + "{\"type\":\"TEACHING_QUALIFICATION\",\"url\":\"https://docs.test/q2.pdf\"}]}"))
                .andExpect(status().isConflict());

        // Neither the teacher themselves nor an institution admin may grant verification.
        mockMvc.perform(put("/v1/platform-admin/verifications/" + recordId + "/review")
                        .param("status", "APPROVED")
                        .header("Authorization", "Bearer " + teacher.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/v1/platform-admin/verifications/" + recordId + "/review")
                        .param("status", "APPROVED")
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isForbidden());

        // The Platform Admin approves — and only now does publishing unlock.
        mockMvc.perform(put("/v1/platform-admin/verifications/" + recordId + "/review")
                        .param("status", "APPROVED").param("notes", "Documents verified")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/v1/teachers/me/verification")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectiveStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.formallyVerified").value(true));

        mockMvc.perform(post("/v1/teachers/me/offerings")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Now publishable\",\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isCreated());
    }

    // ── requirement: suspension blocks backend immediately, login/profile stay ────────

    @Test
    void suspension_blocksPublishingImmediately_butKeepsLoginAndProfile() throws Exception {
        TeacherFixture teacher = newTeacher("sus", institutionId);
        submitAndApprove(teacher, "COURSE", "TEACHING_QUALIFICATION");

        mockMvc.perform(post("/v1/platform-admin/teachers/" + teacher.userId() + "/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Credential under review\"}")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        // The same, still-valid JWT is blocked from publishing — enforcement is per request.
        MvcResult blocked = mockMvc.perform(post("/v1/teachers/me/offerings")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Blocked while suspended\",\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isForbidden())
                .andReturn();
        assertTrue(blocked.getResponse().getContentAsString().contains("suspended"),
                "the refusal must name the suspension so the teacher knows why");

        // Login and profile access survive; drafts remain saveable.
        mockMvc.perform(get("/v1/teachers/me/profile")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/v1/teachers/me/offerings")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Draft while suspended\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/v1/teachers/me/verification")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectiveStatus").value("SUSPENDED"));

        // Suspension is not repeatable, and only a suspended verification can be reactivated.
        mockMvc.perform(post("/v1/platform-admin/teachers/" + teacher.userId() + "/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Again\"}")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/v1/platform-admin/teachers/" + teacher.userId() + "/reactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Review completed\"}")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(post("/v1/teachers/me/offerings")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Publishable again\",\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isCreated());

        // Governance preconditions: only verified teachers get suspended/reactivated.
        TeacherFixture provisional = newTeacher("sus-pre", institutionId);
        mockMvc.perform(post("/v1/platform-admin/teachers/" + provisional.userId() + "/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Too early\"}")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isConflict());
    }

    // ── requirement: block only new actions — legacy published content stays visible ──

    @Test
    void existingPublishedContent_staysVisibleToLearners_whileNewPublishesAreBlocked() throws Exception {
        TeacherFixture teacher = newTeacher("legacy", institutionId);
        submitAndApprove(teacher, "COURSE", "TEACHING_QUALIFICATION");

        MvcResult published = mockMvc.perform(post("/v1/teachers/me/offerings")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Legacy published offering\",\"independent\":true,"
                                + "\"visibility\":\"PUBLIC\",\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String offeringId = JsonPath.read(published.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(post("/v1/platform-admin/teachers/" + teacher.userId() + "/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Post-publication review\"}")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk());

        // Learner access to already-published content is untouched (no retroactive unpublish)…
        mockMvc.perform(get("/v1/learner/offerings/" + offeringId)
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk());

        // …while every new publish action by the suspended teacher is refused.
        mockMvc.perform(post("/v1/teachers/me/offerings")
                        .header("Authorization", "Bearer " + teacher.token())
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New publish attempt\",\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isForbidden());
    }
}
