package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.exception.GlobalExceptionHandler;
import tz.elmkusoma.liveclass.controller.StudentLiveClassController;
import tz.elmkusoma.liveclass.service.LiveClassRecordingAccessService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A learner must only be shown the live sessions they are actually entitled to attend.
 *
 * <p>The listing used to filter on {@code institutionId} and a time window alone, which made the
 * whole school's timetable readable to any learner in it. Institution is a tenant boundary, not an
 * audience. These tests pin the audience rule on every student-facing route - the main listing,
 * the upcoming list and the shared live-now badge - because fixing one screen while leaving the
 * sibling API routes open would be no fix at all.</p>
 *
 * <p>The audience decision itself is deliberately exercised for real: {@link LiveClassRecordingAccessService}
 * is constructed as the production bean and only its persistence primitives are stubbed, so what is
 * asserted here is the genuine join-time rule rather than a mocked stand-in.</p>
 */
class StudentLiveClassAudienceSecurityTest {

    private UUID institutionMine;
    private UUID institutionForeign;
    private UUID classMine;
    private UUID classOther;
    private UUID learnerId;
    private String learnerEmail;

    private LiveClassRepository liveClassRepository;
    private InstitutionMembershipRepository membershipRepository;
    private ClassAccessGuard classAccessGuard;
    private UserRepository userRepository;
    private MockMvc mockMvc;

    private LiveClass sessionOwnClass;
    private LiveClass sessionOtherClass;
    private LiveClass sessionInstitutionWide;
    private LiveClass sessionForeign;
    private LiveClass sessionLiveNow;

    @BeforeEach
    void setUp() {
        institutionMine = UUID.randomUUID();
        institutionForeign = UUID.randomUUID();
        classMine = UUID.randomUUID();
        classOther = UUID.randomUUID();
        learnerId = UUID.randomUUID();
        learnerEmail = "learner@darms.edu.tz";

        liveClassRepository = mock(LiveClassRepository.class);
        TeacherRepository teacherRepository = mock(TeacherRepository.class);
        SubjectRepository subjectRepository = mock(SubjectRepository.class);
        membershipRepository = mock(InstitutionMembershipRepository.class);
        classAccessGuard = mock(ClassAccessGuard.class);
        userRepository = mock(UserRepository.class);

        // The learner is placed in classMine only - never in classOther.
        when(classAccessGuard.isLearnerInClass(learnerEmail, classMine)).thenReturn(true);
        when(classAccessGuard.isLearnerInClass(learnerEmail, classOther)).thenReturn(false);
        when(membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(learnerId, institutionMine))
                .thenReturn(true);
        when(membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(learnerId, institutionForeign))
                .thenReturn(false);

        User learner = new User();
        learner.setEmail(learnerEmail);
        when(userRepository.findById(learnerId)).thenReturn(Optional.of(learner));

        sessionOwnClass = liveClass(institutionMine, classMine, "Algebra live", "SCHEDULED", 1);
        sessionOtherClass = liveClass(institutionMine, classOther, "Biology live", "SCHEDULED", 1);
        sessionInstitutionWide = liveClass(institutionMine, null, "Assembly live", "SCHEDULED", 1);
        sessionForeign = liveClass(institutionForeign, null, "Foreign school lesson", "SCHEDULED", 1);
        sessionLiveNow = liveClass(institutionMine, classOther, "Chemistry in progress", "IN_PROGRESS", 0);

        List<LiveClass> inMyTenant = List.of(
                sessionOwnClass, sessionOtherClass, sessionInstitutionWide, sessionForeign, sessionLiveNow);
        // Even if a query handed back a row from another tenant, the filter must still refuse it.
        when(liveClassRepository.findByInstitutionIdAndScheduledAtBetween(
                eq(institutionMine), any(), any())).thenReturn(inMyTenant);
        when(liveClassRepository.findByInstitutionIdAndScheduledAtBetween(
                eq(institutionForeign), any(), any())).thenReturn(List.of(sessionForeign));

        // Real production wiring: the controller under test drives the real audience service.
        LiveClassRecordingAccessService recordingAccess = new LiveClassRecordingAccessService(
                liveClassRepository, teacherRepository, membershipRepository, userRepository, classAccessGuard);
        StudentLiveClassController controller = new StudentLiveClassController(
                liveClassRepository, teacherRepository, userRepository, subjectRepository, recordingAccess);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private LiveClass liveClass(UUID institutionId, UUID classGroupId, String title,
                                String status, int daysFromNow) {
        LiveClass liveClass = new LiveClass();
        liveClass.setId(UUID.randomUUID());
        liveClass.setInstitutionId(institutionId);
        liveClass.setClassGroupId(classGroupId);
        liveClass.setTitle(title);
        liveClass.setStatus(status);
        liveClass.setScheduledAt(LocalDateTime.now().plusDays(daysFromNow));
        liveClass.setIsDeleted(false);
        return liveClass;
    }

    private String listingAs(String role, UUID institution) throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/student/live-classes")
                        .requestAttr("userId", learnerId)
                        .requestAttr("userRole", role)
                        .requestAttr("institutionId", institution))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    // ---- 1. a learner sees the session targeted at their own class -------------------------

    @Test
    void learnerSeesTheSessionTargetedAtTheirOwnClass() throws Exception {
        String body = listingAs("STUDENT", institutionMine);

        assertTrue(body.contains("Algebra live"),
                "a learner placed in the target class must be shown that session, otherwise the "
                        + "listing hides sessions they are entitled to attend");
        assertTrue(body.contains("Assembly live"),
                "an institution-wide session must remain visible through the existing "
                        + "institution-wide policy");
    }

    // ---- 2. a learner in a different class at the same institution sees nothing else --------

    @Test
    void learnerDoesNotSeeAnotherClassesSessionAtTheSameInstitution() throws Exception {
        String body = listingAs("STUDENT", institutionMine);

        assertFalse(body.contains("Biology live"),
                "class membership is the audience: a learner in another class of the same school "
                        + "must not be shown this session");
        assertFalse(body.contains("Chemistry in progress"),
                "the live-now badge must not leak a class-scoped session either - it is served from "
                        + "the same repository query and had no audience filter at all");
    }

    // ---- 3. a learner from another institution sees nothing --------------------------------

    @Test
    void learnerDoesNotSeeASessionFromAnotherInstitution() throws Exception {
        String body = listingAs("STUDENT", institutionMine);

        assertFalse(body.contains("Foreign school lesson"),
                "the tenant boundary must hold even if the repository returns a foreign row");
    }

    @Test
    void forgingTheTenantAttributeDoesNotUnlockAnotherInstitutionsSessions() throws Exception {
        // A raw API caller only ever carries the institution their membership resolved to; prove
        // that pointing it somewhere else yields nothing rather than the other tenant's timetable.
        String body = listingAs("STUDENT", institutionForeign);

        assertFalse(body.contains("Foreign school lesson"),
                "an active member of one institution must not be read into another by supplying "
                        + "that institution as the tenant attribute");
    }

    // ---- 4. a learner without valid placement sees no class-scoped session ------------------

    @Test
    void learnerWithoutValidPlacementSeesNoClassScopedSession() throws Exception {
        // Membership is intact but the learner holds no placement in either class.
        when(classAccessGuard.isLearnerInClass(learnerEmail, classMine)).thenReturn(false);

        MvcResult result = mockMvc.perform(get("/v1/student/live-classes")
                        .requestAttr("userId", learnerId)
                        .requestAttr("userRole", "STUDENT")
                        .requestAttr("institutionId", institutionMine))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andReturn();
        String body = result.getResponse().getContentAsString();

        assertFalse(body.contains("Algebra live"),
                "a learner with no valid placement must not be entitled to a class-scoped session");
        assertTrue(body.contains("Assembly live"),
                "the institution-wide session does not depend on placement and must stay visible");
    }

    // ---- 5. institution-wide sessions keep the existing institution-wide policy -------------

    @Test
    void institutionWideSessionStaysVisibleToAnActiveMemberOfTheInstitution() throws Exception {
        String body = listingAs("STUDENT", institutionMine);

        assertTrue(body.contains("Assembly live"),
                "requiring class membership for a session that declares no class would make an "
                        + "institution-wide session unreachable");
    }

    // ---- 6. tampering with query parameters cannot widen access ----------------------------

    @Test
    void tamperedPaginationCannotWidenOrCrashTheListing() throws Exception {
        // Negative bounds previously reached Stream.limit directly and blew up as a 500.
        String negative = mockMvc.perform(get("/v1/student/live-classes")
                        .requestAttr("userId", learnerId)
                        .requestAttr("userRole", "STUDENT")
                        .requestAttr("institutionId", institutionMine)
                        .param("page", "-5")
                        .param("size", "-999"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(negative.contains("Biology live"),
                "negating pagination must not become a way around the audience filter");
        assertTrue(negative.contains("Algebra live"),
                "pagination tampering must not hide the sessions the learner is entitled to");

        // A runaway size must not pull the whole institution into one response.
        String huge = mockMvc.perform(get("/v1/student/live-classes")
                        .requestAttr("userId", learnerId)
                        .requestAttr("userRole", "STUDENT")
                        .requestAttr("institutionId", institutionMine)
                        .param("page", "0")
                        .param("size", "100000"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(huge.contains("Biology live"),
                "an oversized page must still be audience-filtered before it is returned");
    }

    // ---- 7. the sibling API routes enforce the same rule, not just the first screen --------

    @Test
    void upcomingEndpointAppliesTheSameAudienceRule() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/student/live-classes/upcoming")
                        .requestAttr("userId", learnerId)
                        .requestAttr("userRole", "STUDENT")
                        .requestAttr("institutionId", institutionMine))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();

        assertTrue(body.contains("Algebra live"));
        assertTrue(body.contains("Assembly live"));
        assertFalse(body.contains("Biology live"),
                "/upcoming is served from the same repository query and must apply the same rule");
        assertFalse(body.contains("Foreign school lesson"),
                "/upcoming must not cross the tenant boundary either");
    }

    @Test
    void liveNowNarrowsLearnersButLeavesStaffReachIntact() throws Exception {
        String asLearner = mockMvc.perform(get("/v1/student/live-classes/live-now")
                        .requestAttr("userId", learnerId)
                        .requestAttr("userRole", "STUDENT")
                        .requestAttr("institutionId", institutionMine))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(asLearner.contains("Chemistry in progress"),
                "the shared live-now badge must not show a learner a class they are not in");

        // Teachers and admins share this route for their own dashboards; their reach is unchanged.
        for (String staffRole : List.of("TEACHER", "INSTITUTION_ADMIN")) {
            String asStaff = mockMvc.perform(get("/v1/student/live-classes/live-now")
                            .requestAttr("userId", learnerId)
                            .requestAttr("userRole", staffRole)
                            .requestAttr("institutionId", institutionMine))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertTrue(asStaff.contains("Chemistry in progress"),
                    "narrowing learners must not break " + staffRole + " access on the shared route");
        }
    }

    // ---- 8. calling the API directly (bypassing any screen) still gets filtered -------------

    @Test
    void directApiCallStillReturnsOnlySessionsTheLearnerMayAttend() throws Exception {
        // No UI involved: the raw endpoint is the enforcement point.
        mockMvc.perform(get("/v1/student/live-classes")
                        .requestAttr("userId", learnerId)
                        .requestAttr("userRole", "STUDENT")
                        .requestAttr("institutionId", institutionMine))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.success").value(true));

        // Knowing that another class's session exists must not make it retrievable.
        MvcResult result = mockMvc.perform(get("/v1/student/live-classes")
                        .requestAttr("userId", learnerId)
                        .requestAttr("userRole", "STUDENT")
                        .requestAttr("institutionId", institutionMine))
                .andReturn();
        String body = result.getResponse().getContentAsString();

        assertTrue(body.indexOf("Algebra live") >= 0);
        assertTrue(body.indexOf("Assembly live") >= 0);
        assertTrue(body.indexOf("Biology live") < 0,
                "the audience decision belongs on the API, not in the screen that renders it");
    }
}
