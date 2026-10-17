package tz.elmkusoma.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Public live-class discovery and Platform Admin publication governance.
 *
 * <p>The property under test throughout is that a class reaches an anonymous visitor only when it
 * is both PUBLISHED and PUBLIC. Those are two separate decisions on two separate axes, and most of
 * these tests exist to pin a specific way one of them alone is not enough.</p>
 *
 * <p>Fixtures are written straight through the repository so a class can be placed in an exact
 * lifecycle state with an exact timestamp. The service refuses to create a past-dated class, which
 * is correct but would make the time-window assertions untestable through the API.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicLiveClassDiscoveryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LiveClassRepository liveClassRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private tz.elmkusoma.shared.repository.UserRepository userRepository;

    @Autowired
    private tz.elmkusoma.shared.repository.InstitutionMembershipRepository membershipRepository;

    private String platformToken;
    private String teacherToken;
    private String teacherUserId;
    private UUID institutionId;
    private Teacher seededTeacher;
    private Teacher foreignTeacher;

    @BeforeAll
    void fixtures() {
        institutionId = TestDataSeeder.INSTITUTION_ID;

        String adminEmail = "live-admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        var adminUser = userRepository.save(tz.elmkusoma.shared.domain.User.builder()
                .email(adminEmail).passwordHash("test-hash")
                .firstName("Live").lastName("Admin")
                .role(tz.elmkusoma.shared.domain.User.Role.ADMIN)
                .institutionId(institutionId).isActive(true).isEmailVerified(true).isDeleted(false)
                .build());
        membershipRepository.save(tz.elmkusoma.shared.domain.InstitutionMembership.builder()
                .userId(adminUser.getId()).institutionId(institutionId)
                .role(tz.elmkusoma.shared.domain.InstitutionMembership.Role.ADMIN)
                .isActive(true).build());
        platformToken = TestTokens.userToken(adminEmail);

        String teacherEmail = "live-teacher-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        var teacherUser = userRepository.save(tz.elmkusoma.shared.domain.User.builder()
                .email(teacherEmail).passwordHash("test-hash")
                .firstName("Live").lastName("Teacher")
                .role(tz.elmkusoma.shared.domain.User.Role.TEACHER)
                .institutionId(institutionId).isActive(true).isEmailVerified(true).isDeleted(false)
                .build());
        membershipRepository.save(tz.elmkusoma.shared.domain.InstitutionMembership.builder()
                .userId(teacherUser.getId()).institutionId(institutionId)
                .role(tz.elmkusoma.shared.domain.InstitutionMembership.Role.TEACHER)
                .isActive(true).build());
        var ownTeacher = tz.elmkusoma.teacher.domain.Teacher.builder()
                .userId(teacherUser.getId())
                .employeeNumber("EMP-" + UUID.randomUUID().toString().substring(0, 8))
                .status(tz.elmkusoma.teacher.domain.TeacherStatus.ACTIVE)
                .build();
        ownTeacher.setInstitutionId(institutionId);
        seededTeacher = teacherRepository.save(ownTeacher);
        teacherUserId = teacherUser.getId().toString();
        teacherToken = TestTokens.userToken(teacherEmail);

        var foreignUser = userRepository.save(tz.elmkusoma.shared.domain.User.builder()
                .email("live-other-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .passwordHash("test-hash")
                .firstName("Other").lastName("Teacher")
                .role(tz.elmkusoma.shared.domain.User.Role.TEACHER)
                .institutionId(institutionId).isActive(true).isEmailVerified(true).isDeleted(false)
                .build());
        var otherTeacher = tz.elmkusoma.teacher.domain.Teacher.builder()
                .userId(foreignUser.getId())
                .employeeNumber("EMP-" + UUID.randomUUID().toString().substring(0, 8))
                .status(tz.elmkusoma.teacher.domain.TeacherStatus.ACTIVE)
                .build();
        otherTeacher.setInstitutionId(institutionId);
        foreignTeacher = teacherRepository.save(otherTeacher);
    }

    @BeforeEach
    void reset() {
        liveClassRepository.deleteAll();
    }

    // ── public eligibility ───────────────────────────────────────────────────────────────

    @Test
    void nothingIsDiscoverableByDefault() throws Exception {
        // A created class is not published. Every pre-existing class looks exactly like this.
        seed("Default draft", LocalDateTime.now().plusDays(2),
                LiveClass.LiveClassStatus.SCHEDULED, "DRAFT", "INSTITUTION");

        assertEquals(0, publicCount("/v1/public/live-classes/upcoming"));
        assertEquals(0, publicCount("/v1/public/live-classes/live"));
        assertEquals(0, publicCount("/v1/public/live-classes/past"));
    }

    @Test
    void publishedButInstitutionScopedIsStillInvisible() throws Exception {
        // The central safety property: approval alone must never expose a teacher's session.
        seed("Published but private to school", LocalDateTime.now().plusDays(2),
                LiveClass.LiveClassStatus.SCHEDULED, "PUBLISHED", "INSTITUTION");

        assertEquals(0, publicCount("/v1/public/live-classes/upcoming"),
                "a PUBLISHED but INSTITUTION class must not appear on the public site");
    }

    @Test
    void publicButUnpublishedIsStillInvisible() throws Exception {
        seed("Public access but only a draft", LocalDateTime.now().plusDays(2),
                LiveClass.LiveClassStatus.SCHEDULED, "DRAFT", "PUBLIC");

        assertEquals(0, publicCount("/v1/public/live-classes/upcoming"),
                "access level alone must not publish a class");
    }

    @Test
    void publishedAndPublicUpcomingIsVisible() throws Exception {
        seed("Visible upcoming class", LocalDateTime.now().plusDays(3),
                LiveClass.LiveClassStatus.SCHEDULED, "PUBLISHED", "PUBLIC");

        assertEquals(1, publicCount("/v1/public/live-classes/upcoming"));
    }

    @Test
    void onlyGenuinelyActiveSessionsAppearLiveNow() throws Exception {
        seed("Actually running", LocalDateTime.now().minusMinutes(30),
                LiveClass.LiveClassStatus.IN_PROGRESS, "PUBLISHED", "PUBLIC");
        seed("Merely scheduled", LocalDateTime.now().plusHours(2),
                LiveClass.LiveClassStatus.SCHEDULED, "PUBLISHED", "PUBLIC");
        seed("Cancelled today", LocalDateTime.now().minusMinutes(30),
                LiveClass.LiveClassStatus.CANCELLED, "PUBLISHED", "PUBLIC");
        seed("Service degraded", LocalDateTime.now().minusMinutes(30),
                LiveClass.LiveClassStatus.SERVICE_DEGRADED, "PUBLISHED", "PUBLIC");

        assertEquals(1, publicCount("/v1/public/live-classes/live"),
                "only IN_PROGRESS qualifies; SCHEDULED, CANCELLED and SERVICE_DEGRADED must not");
    }

    @Test
    void endedSessionsLeaveLiveNowAndAppearInPast() throws Exception {
        seed("Finished class", LocalDateTime.now().minusHours(3),
                LiveClass.LiveClassStatus.ENDED, "PUBLISHED", "PUBLIC");

        assertEquals(0, publicCount("/v1/public/live-classes/live"),
                "an ended session must disappear from Live Now");
        assertEquals(1, publicCount("/v1/public/live-classes/past"));
    }

    @Test
    void cancelledClassesAreExcludedFromPast() throws Exception {
        // A cancelled session is not a completed learning event. Listing it beside real past
        // classes, with a replay affordance, would misrepresent what happened.
        seed("Cancelled class", LocalDateTime.now().minusHours(3),
                LiveClass.LiveClassStatus.CANCELLED, "PUBLISHED", "PUBLIC");

        assertEquals(0, publicCount("/v1/public/live-classes/past"));
    }

    @Test
    void softDeletedClassesAreNeverDiscoverable() throws Exception {
        LiveClass lc = seed("Archived class", LocalDateTime.now().plusDays(1),
                LiveClass.LiveClassStatus.SCHEDULED, "PUBLISHED", "PUBLIC");
        lc.setIsDeleted(true);
        liveClassRepository.save(lc);

        assertEquals(0, publicCount("/v1/public/live-classes/upcoming"));
    }

    @Test
    void archivedClassesAreNotDiscoverable() throws Exception {
        // Archiving is a soft delete plus a return to DRAFT, so it removes a class from the public
        // listing without touching the row.
        LiveClass lc = seed("Archiving hides it", LocalDateTime.now().plusDays(1),
                LiveClass.LiveClassStatus.SCHEDULED, "PUBLISHED", "PUBLIC");
        liveClassRepository.save(lc);
        assertEquals(1, publicCount("/v1/public/live-classes/upcoming"));

        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + lc.getId() + "/archive")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Term ended\"}"))
                .andExpect(status().isOk());

        assertEquals(0, publicCount("/v1/public/live-classes/upcoming"));
    }

    // ── payload hygiene ──────────────────────────────────────────────────────────────────

    @Test
    void publicPayloadNeverLeaksRecordingUrls() throws Exception {
        LiveClass lc = seed("Class with a recording", LocalDateTime.now().minusHours(2),
                LiveClass.LiveClassStatus.ENDED, "PUBLISHED", "PUBLIC");
        lc.setRecordingUrl("https://private-bucket.example/recordings/secret-token.mp4");
        liveClassRepository.save(lc);

        String body = mockMvc.perform(get("/v1/public/live-classes/past"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains("secret-token"),
                "a raw recording URL must never reach a public payload");
        assertFalse(body.contains("recordingUrl"), "the recording URL field must not exist publicly");
    }

    @Test
    void publicPayloadOmitsInternalIdentifiers() throws Exception {
        seed("Identifier check", LocalDateTime.now().plusDays(1),
                LiveClass.LiveClassStatus.SCHEDULED, "PUBLISHED", "PUBLIC");

        String body = mockMvc.perform(get("/v1/public/live-classes/upcoming"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        for (String field : List.of("teacherId", "institutionId", "classGroupId",
                "publicationStatus", "accessLevel", "reviewedBy", "subjectId")) {
            assertFalse(body.contains("\"" + field + "\""),
                    "public payload must not expose " + field);
        }
    }

    @Test
    void publicPayloadCarriesTheTimezoneAndANewFlag() throws Exception {
        LiveClass lc = seed("Timezone carried", LocalDateTime.now().plusDays(1),
                LiveClass.LiveClassStatus.SCHEDULED, "PUBLISHED", "PUBLIC");
        lc.setTimezone("Africa/Dar_es_Salaam");
        liveClassRepository.save(lc);

        mockMvc.perform(get("/v1/public/live-classes/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].timezone").value("Africa/Dar_es_Salaam"))
                // hasRecording must exist and be false rather than absent, so the client can rely
                // on the shape.
                .andExpect(jsonPath("$.data[0].hasRecording").value(false));
    }

    @Test
    void limitIsClamped() throws Exception {
        seed("Clamp check", LocalDateTime.now().plusDays(1),
                LiveClass.LiveClassStatus.SCHEDULED, "PUBLISHED", "PUBLIC");

        // A caller asking for a huge page must not get an unbounded query.
        mockMvc.perform(get("/v1/public/live-classes/upcoming?limit=100000"))
                .andExpect(status().isOk());
    }

    // ── governance authorization ──────────────────────────────────────────────────────────

    @Test
    void anonymousCannotReachGovernance() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/live-classes/governance"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + UUID.randomUUID() + "/publish"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teachersCannotPublishTheirOwnClasses() throws Exception {
        LiveClass lc = seed("Teacher self-publish attempt", LocalDateTime.now().plusDays(2),
                LiveClass.LiveClassStatus.SCHEDULED, "APPROVED", "PUBLIC");
        liveClassRepository.save(lc);

        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + lc.getId() + "/publish")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());
    }

    // ── publication workflow ─────────────────────────────────────────────────────────────

    @Test
    void fullApprovalWorkflowMakesAClassDiscoverable() throws Exception {
        LiveClass lc = seed("Full workflow", LocalDateTime.now().plusDays(2),
                LiveClass.LiveClassStatus.SCHEDULED, "DRAFT", "PUBLIC");
        liveClassRepository.save(lc);
        assertEquals(0, publicCount("/v1/public/live-classes/upcoming"));

        // Step 1: Teacher submits for approval
        mockMvc.perform(post("/v1/teachers/me/live-classes/" + lc.getId() + "/submit-for-approval")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk());

        // Step 2: Admin approves
        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + lc.getId() + "/approve")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Looks fine\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicationStatus").value("APPROVED"));

        // Approved is not published. Approval says "fit to announce", not "announce".
        assertEquals(0, publicCount("/v1/public/live-classes/upcoming"));

        // Step 3: Admin publishes
        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + lc.getId() + "/publish")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicationStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publiclyDiscoverable").value(true));

        assertEquals(1, publicCount("/v1/public/live-classes/upcoming"));
    }

    @Test
    void publishingRefusesWhenTheClassIsNotPublicScoped() throws Exception {
        LiveClass lc = seed("Publish attempt while institution-scoped", LocalDateTime.now().plusDays(2),
                LiveClass.LiveClassStatus.SCHEDULED, "APPROVED", "INSTITUTION");
        liveClassRepository.save(lc);

        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + lc.getId() + "/publish")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isConflict());

        assertEquals("APPROVED", liveClassRepository.findById(lc.getId()).orElseThrow().getPublicationStatus());
    }

    @Test
    void unpublishedClassLeavesThePublicListingImmediately() throws Exception {
        LiveClass lc = seed("Unpublish me", LocalDateTime.now().plusDays(2),
                LiveClass.LiveClassStatus.SCHEDULED, "PUBLISHED", "PUBLIC");
        liveClassRepository.save(lc);
        assertEquals(1, publicCount("/v1/public/live-classes/upcoming"));

        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + lc.getId() + "/unpublish")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Session had not started\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publiclyDiscoverable").value(false));

        assertEquals(0, publicCount("/v1/public/live-classes/upcoming"));
    }

    @Test
    void unpublishingPreservesAcademicRecords() throws Exception {
        LiveClass lc = seed("Keep my records", LocalDateTime.now().minusHours(2),
                LiveClass.LiveClassStatus.ENDED, "PUBLISHED", "PUBLIC");
        lc.setRecordingUrl("https://private-bucket.example/rec.mp4");
        lc.setEndedAt(LocalDateTime.now().minusMinutes(30));
        liveClassRepository.save(lc);
        // Compared against what was actually stored, not against "today". The seed is two hours
        // back, so between midnight and 02:00 it lands on yesterday and an equality against
        // LocalDate.now() fails for no reason but the clock - which is the kind of failure that
        // gets retried until it passes and then hides a real break. Asserting the value round-trips
        // is both deterministic and a stronger statement about archiving preserving the schedule.
        // Tolerance rather than truncation: PostgreSQL rounds the fractional seconds it is given, so
        // truncation in Java and rounding in the database can disagree by one microsecond, and
        // truncating to a coarser unit still has a boundary where they disagree. A stated
        // tolerance has no boundary to fall on.
        LocalDateTime scheduledBeforeArchive = lc.getScheduledAt();

        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + lc.getId() + "/archive")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"End of term\"}"))
                .andExpect(status().isOk());

        LiveClass after = liveClassRepository.findById(lc.getId()).orElseThrow();
        assertTrue(Boolean.TRUE.equals(after.getIsDeleted()), "archiving is a soft delete");
        assertEquals("https://private-bucket.example/rec.mp4", after.getRecordingUrl(),
                "archiving must never destroy recording metadata");
        Duration scheduleDrift = Duration.between(scheduledBeforeArchive, after.getScheduledAt());
        assertTrue(Math.abs(scheduleDrift.toNanos()) < 1_000_000L,
                "the schedule is academic record too, but it moved by " + scheduleDrift);

        assertEquals(0, publicCount("/v1/public/live-classes/past"));
    }

    @Test
    void aRunningSessionCannotBeArchived() throws Exception {
        LiveClass lc = seed("Running class", LocalDateTime.now().minusMinutes(10),
                LiveClass.LiveClassStatus.IN_PROGRESS, "PUBLISHED", "PUBLIC");
        liveClassRepository.save(lc);

        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + lc.getId() + "/archive")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"typo\"}"))
                .andExpect(status().isConflict());

        assertFalse(Boolean.TRUE.equals(liveClassRepository.findById(lc.getId()).orElseThrow().getIsDeleted()));
    }

    @Test
    void rejectionRequiresAReason() throws Exception {
        LiveClass lc = seed("Reject without reason", LocalDateTime.now().plusDays(2),
                LiveClass.LiveClassStatus.SCHEDULED, "PENDING_APPROVAL", "PUBLIC");
        liveClassRepository.save(lc);

        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + lc.getId() + "/reject")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyPendingClassesCanBeApproved() throws Exception {
        LiveClass lc = seed("Approve a draft", LocalDateTime.now().plusDays(2),
                LiveClass.LiveClassStatus.SCHEDULED, "DRAFT", "PUBLIC");
        liveClassRepository.save(lc);

        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + lc.getId() + "/approve")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"x\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelledClassCannotBePublished() throws Exception {
        LiveClass lc = seed("Publish a cancellation", LocalDateTime.now().plusDays(2),
                LiveClass.LiveClassStatus.CANCELLED, "APPROVED", "PUBLIC");
        liveClassRepository.save(lc);

        mockMvc.perform(post("/v1/platform-admin/live-classes/governance/" + lc.getId() + "/publish")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isConflict());
    }

    @Test
    void governanceQueueFiltersByPublicationState() throws Exception {
        seed("Pending one", LocalDateTime.now().plusDays(1),
                LiveClass.LiveClassStatus.SCHEDULED, "PENDING_APPROVAL", "PUBLIC");
        seed("Draft one", LocalDateTime.now().plusDays(1),
                LiveClass.LiveClassStatus.SCHEDULED, "DRAFT", "PUBLIC");

        MvcResult result = mockMvc.perform(get("/v1/platform-admin/live-classes/governance?publicationStatus=PENDING_APPROVAL")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andReturn();

        Object total = JsonPath.read(result.getResponse().getContentAsString(), "$.data.totalElements");
        assertEquals(1, total);
    }

    // ── teacher submission ────────────────────────────────────────────────────────────────

    @Test
    void teacherSubmitsOwnClassForReview() throws Exception {
        LiveClass owned = seedOwnedBy("My class", teacherUserId, LocalDateTime.now().plusDays(2));

        mockMvc.perform(post("/v1/teachers/me/live-classes/" + owned.getId() + "/submit-for-approval")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk());

        assertEquals(LiveClass.PublicationStatus.PENDING_APPROVAL.name(),
                liveClassRepository.findById(owned.getId()).orElseThrow().getPublicationStatus());
    }

    @Test
    void aTeacherCannotSubmitAnotherTeachersClass() throws Exception {
        LiveClass someoneElse = seedOwnedBy("Not yours", UUID.randomUUID().toString(),
                LocalDateTime.now().plusDays(2));

        mockMvc.perform(post("/v1/teachers/me/live-classes/" + someoneElse.getId() + "/submit-for-approval")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden());

        assertEquals(LiveClass.PublicationStatus.DRAFT.name(),
                liveClassRepository.findById(someoneElse.getId()).orElseThrow().getPublicationStatus());
    }

    @Test
    void submittingIsRefusedForAnAlreadySubmittedClass() throws Exception {
        LiveClass owned = seedOwnedBy("Submitted twice", teacherUserId, LocalDateTime.now().plusDays(2));
        owned.setPublicationStatus(LiveClass.PublicationStatus.PENDING_APPROVAL.name());
        liveClassRepository.save(owned);

        mockMvc.perform(post("/v1/teachers/me/live-classes/" + owned.getId() + "/submit-for-approval")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isConflict());
    }

    // ── cover image ───────────────────────────────────────────────────────────────────────

    @Test
    void aTeacherCanUploadACoverImage() throws Exception {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

        MvcResult result = mockMvc.perform(multipart("/v1/teachers/me/live-classes/cover")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "cover.png", "image/png", png))
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andReturn();

        String url = JsonPath.read(result.getResponse().getContentAsString(), "$.data.url");
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void aDisguisedFileIsRefusedAndOnlyTeachersMayUpload() throws Exception {
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Named png, declared png, actually HTML: only a byte check catches this.
        mockMvc.perform(multipart("/v1/teachers/me/live-classes/cover")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "cover.png", "image/png", html))
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/v1/teachers/me/live-classes/cover")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "cover.png", "image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47})))
                .andExpect(status().isForbidden());
    }

    // ── helpers ───────────────────────────────────────────────────────────────────────────

    private int publicCount(String path) throws Exception {
        String body = mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<?> items = JsonPath.read(body, "$.data");
        return items == null ? 0 : items.size();
    }

    private LiveClass seed(String title, LocalDateTime scheduledAt,
                           LiveClass.LiveClassStatus status, String publicationStatus, String accessLevel) {
        return seedOwnedByTitle(title, seededTeacher.getId(), scheduledAt, status, publicationStatus, accessLevel);
    }

    private LiveClass seedOwnedBy(String title, String teacherUserId, LocalDateTime scheduledAt) {
        return seedOwnedByTitle(title, resolveTeacherId(teacherUserId), scheduledAt,
                LiveClass.LiveClassStatus.SCHEDULED,
                LiveClass.PublicationStatus.DRAFT.name(),
                LiveClass.AccessLevel.INSTITUTION.name());
    }

    private UUID resolveTeacherId(String teacherUserId) {
        try {
            return teacherRepository
                    .findFirstByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID.fromString(teacherUserId))
                    .orElseThrow(() -> new IllegalArgumentException("unknown teacher user"))
                    .getId();
        } catch (IllegalArgumentException ex) {
            // A deliberately unknown owner, used to model "someone else's class".
            return foreignTeacher.getId();
        }
    }

    private LiveClass seedOwnedByTitle(String title, UUID teacherEntityId, LocalDateTime scheduledAt,
                                       LiveClass.LiveClassStatus status,
                                       String publicationStatus, String accessLevel) {
        LiveClass lc = LiveClass.builder()
                .title(title)
                .description("Description for " + title)
                .scheduledAt(scheduledAt)
                .durationMinutes(60)
                .status(status.name())
                .publicationStatus(publicationStatus)
                .accessLevel(accessLevel)
                .timezone("Africa/Dar_es_Salaam")
                .startedAt(status == LiveClass.LiveClassStatus.SCHEDULED ? null : scheduledAt)
                .maxParticipants(50)
                .recordingEnabled(false)
                .build();
        lc.setInstitutionId(institutionId);
        lc.setTeacherId(teacherEntityId);
        return liveClassRepository.save(lc);
    }
}