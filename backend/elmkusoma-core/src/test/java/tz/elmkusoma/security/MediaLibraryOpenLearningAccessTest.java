package tz.elmkusoma.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.liveclass.domain.MediaAsset;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.domain.TeacherStatus;
import tz.elmkusoma.teacher.repository.TeacherRepository;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Media-library and recording authorization for open learning — permitted AND denied
 * access, through the real filter chain (JwtAuthenticationFilter → JwtRequestAttributeFilter
 * → OrganizationContextResolver → method security).
 *
 * <p>The defect this class pins fixed: the media library was strictly tenant-scoped, so a
 * recording of a genuine open learning session (PUBLISHED + PUBLIC + no declared class
 * scope) was unreachable from any other school and from institution-less learners, and the
 * library endpoints rejected accounts that claimed no tenant at all (400 on the required
 * institution attribute / 403 on the membership guard). The rule under test is the same one
 * {@code LiveClassRecordingAccessService} already enforces for per-class replay — listing,
 * detail, download-url and per-source playback must all decide alike:</p>
 *
 * <ul>
 *   <li>A published open session's playable recording is discoverable and reachable by a
 *       learner of another school and by a learner with no school at all.</li>
 *   <li>School-private recordings stay inside their audience: foreign-school and
 *       institution-less readers get the same "not found" a missing id gets.</li>
 *   <li>Unpublished, failed or deleted material of an open session is never advertised.</li>
 *   <li>The teacher who owns their own material reaches it without any tenant claim
 *       (independent-teacher management), and an institution admin keeps whole-library
 *       access inside their own tenant while no longer reading another tenant by id.</li>
 *   <li>A forged institution header remains refused outright.</li>
 * </ul>
 *
 * <p>All fixtures are created fresh per test with a random run suffix and removed again in
 * {@code @AfterEach}, so no row here can outlive its test and no assertion depends on how
 * much other test data happens to share the database. Assertions are title-based, never
 * length-based.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class MediaLibraryOpenLearningAccessTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private InstitutionRepository institutionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InstitutionMembershipRepository membershipRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private LiveClassRepository liveClassRepository;
    @Autowired
    private LiveClassParticipantRepository participantRepository;
    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    private String run;

    private Institution instA;
    private Institution instB;
    private User teacherA;
    private User teacherInd;
    private User studentA;
    private User studentB;
    private User learnerInd;
    private User instAdminB;
    private Teacher teacherARow;
    private Teacher teacherIndRow;

    private LiveClass openClass;
    private LiveClass privateClass;
    private LiveClass draftClass;
    private UUID privateClassGroupId;

    private String openTitle;
    private String failedOpenTitle;
    private String draftTitle;
    private String privateTitle;
    private String ownedTitle;
    private String tenantBFailedTitle;

    // Only this test's own rows — removed in FK-safe order, nothing pre-existing touched.
    private final List<MediaAsset> assets = new ArrayList<>();
    private final List<LiveClassParticipant> participants = new ArrayList<>();
    private final List<LiveClass> classes = new ArrayList<>();
    private final List<InstitutionMembership> memberships = new ArrayList<>();
    private final List<Teacher> teachers = new ArrayList<>();
    private final List<User> users = new ArrayList<>();
    private final List<Institution> institutions = new ArrayList<>();

    @BeforeEach
    void setUp() {
        run = UUID.randomUUID().toString().substring(0, 8);
        assets.clear();
        participants.clear();
        classes.clear();
        memberships.clear();
        teachers.clear();
        users.clear();
        institutions.clear();

        instA = saveInstitution("MediaOpen-A " + run);
        instB = saveInstitution("MediaOpen-B " + run);

        teacherA = saveUser("mediaopen-teacher-a-" + run + "@test.com", User.Role.TEACHER, instA);
        teacherARow = saveTeacherRow(teacherA, "MEDIALIB-TA-" + run, instA);
        teacherInd = saveIndependentUser("mediaopen-teacher-ind-" + run + "@test.com", User.Role.TEACHER);
        teacherIndRow = saveTeacherRow(teacherInd, "MEDIALIB-TI-" + run, null);
        studentA = saveUser("mediaopen-student-a-" + run + "@test.com", User.Role.STUDENT, instA);
        studentB = saveUser("mediaopen-student-b-" + run + "@test.com", User.Role.STUDENT, instB);
        instAdminB = saveUser("mediaopen-admin-b-" + run + "@test.com", User.Role.INSTITUTION_ADMIN, instB);
        learnerInd = saveIndependentUser("mediaopen-learner-ind-" + run + "@test.com", User.Role.OTHER_LEARNER);

        // A genuine open learning session of school A: PUBLISHED + PUBLIC + no class scope.
        openClass = saveClass("Open session " + run, instA, teacherARow, "PUBLIC", "PUBLISHED", null);
        // A school-private recording session of school A, with a class audience.
        privateClassGroupId = UUID.randomUUID();
        privateClass = saveClass("Private session " + run, instA, teacherARow,
                "INSTITUTION", "PUBLISHED", privateClassGroupId);
        // PUBLIC but not published: must not be open to anyone.
        draftClass = saveClass("Draft session " + run, instA, teacherARow, "PUBLIC", "DRAFT", null);

        openTitle = "Open recording " + run;
        failedOpenTitle = "Open failed capture " + run;
        draftTitle = "Draft open recording " + run;
        privateTitle = "Private class recording " + run;
        ownedTitle = "Independent owned media " + run;
        tenantBFailedTitle = "Tenant-B failed capture " + run;

        assets.add(saveRecording(openTitle, openClass.getId(), null, "READY", teacherA.getId(), instA));
        assets.add(saveRecording(failedOpenTitle, openClass.getId(), null, "FAILED", teacherA.getId(), instA));
        assets.add(saveRecording(draftTitle, draftClass.getId(), null, "READY", teacherA.getId(), instA));
        assets.add(saveRecording(privateTitle, privateClass.getId(), privateClassGroupId,
                "READY", teacherA.getId(), instA));
        // The independent teacher's own material: no institution anywhere on the row.
        assets.add(saveRecording(ownedTitle, UUID.randomUUID(), null, "READY",
                teacherInd.getId(), null));

        // School A's learner attended the private session — entitled the old way, unchanged.
        participants.add(participantRepository.saveAndFlush(LiveClassParticipant.builder()
                .liveClassId(privateClass.getId())
                .userId(studentA.getId())
                .role("LEARNER")
                .joinedAt(LocalDateTime.now().minusHours(2))
                .isDeleted(false)
                .build()));

        // School B's own (failed) capture: its institution admin must keep diagnosing it.
        tenantBFailedTitle = "Tenant-B failed capture " + run;
        assets.add(saveRecording(tenantBFailedTitle, UUID.randomUUID(), null, "FAILED",
                teacherA.getId(), instB));
    }

    @AfterEach
    void cleanUpOwnRowsOnly() {
        if (!assets.isEmpty()) mediaAssetRepository.deleteAll(assets);
        if (!participants.isEmpty()) participantRepository.deleteAll(participants);
        if (!classes.isEmpty()) liveClassRepository.deleteAll(classes);
        if (!memberships.isEmpty()) membershipRepository.deleteAll(memberships);
        if (!teachers.isEmpty()) teacherRepository.deleteAll(teachers);
        if (!users.isEmpty()) userRepository.deleteAll(users);
        if (!institutions.isEmpty()) institutionRepository.deleteAll(institutions);
    }

    // ── permitted: discovery and access across institutions and without a school ───────────

    /** The defect itself: another school's learner discovers the published open recording. */
    @Test
    void schoolLearnerOfAnotherSchool_seesTheOpenRecording_butNotTheOtherSchoolsPrivateOne()
            throws Exception {
        String body = listBody(token(studentB), instB.getId().toString());

        assertTrue(body.contains(openTitle),
                "a published open session's recording must be discoverable from another school: " + body);
        assertTrue(!body.contains(privateTitle),
                "a school-private recording must stay out of a foreign school's library: " + body);
        assertTrue(!body.contains(failedOpenTitle),
                "a failed capture must never be advertised to any learner: " + body);
        assertTrue(!body.contains(draftTitle),
                "an unpublished session's recording must never be advertised: " + body);
    }

    /** A learner with no school and no enrollment discovers the same open recording. */
    @Test
    void independentLearner_seesTheOpenRecordingWithoutAnySchoolOrEnrollment() throws Exception {
        String body = listBody(token(learnerInd), null);

        assertTrue(body.contains(openTitle),
                "an institution-less learner must discover published open recordings: " + body);
        assertTrue(!body.contains(privateTitle),
                "no school-private recording may leak to an institution-less learner: " + body);
        assertTrue(!body.contains(ownedTitle),
                "another teacher's own material must not leak to a learner: " + body);
    }

    /** Detail and download-url of the open recording answer every eligible reader. */
    @Test
    void openRecording_reachableByDetailAndDownload_forSchoolAndIndependentReaders()
            throws Exception {
        for (String[] reader : new String[][] {
                {token(studentB), instB.getId().toString()},
                {token(learnerInd), null},
        }) {
            mockMvc.perform(getWithInstitution("/v1/media/" + assetId(openTitle), reader[1], reader[0]))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(assetId(openTitle).toString()))
                    .andExpect(content().string(not(containsString("cdn.test"))));

            mockMvc.perform(getWithInstitution(
                            "/v1/media/" + assetId(openTitle) + "/download-url", reader[1], reader[0]))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.assetId").value(assetId(openTitle).toString()))
                    .andExpect(content().string(not(containsString("cdn.test"))));
        }
    }

    /** The per-source recording listing applies the same open-session rule as the library. */
    @Test
    void openSession_recordingsBySource_reachableFromEverySchoolAndWithoutASchool()
            throws Exception {
        String foreign = mockMvc.perform(getWithInstitution(
                        "/v1/media/recordings/LIVE_CLASS/" + openClass.getId(),
                        instB.getId().toString(), token(studentB)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(foreign.contains(openTitle), "foreign school must list the open recording: " + foreign);
        assertTrue(!foreign.contains(failedOpenTitle),
                "the failed capture must stay filtered even inside an open session: " + foreign);

        String independent = mockMvc.perform(get("/v1/media/recordings/LIVE_CLASS/" + openClass.getId())
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(independent.contains(openTitle),
                "an institution-less learner must reach the open session's recordings: " + independent);
        assertTrue(!independent.contains(failedOpenTitle),
                "the failed capture must stay filtered for institution-less readers too: " + independent);
    }

    /** An independent teacher manages their own material with no school attached. */
    @Test
    void independentTeacher_reachesTheirOwnMaterialWithoutAnySchool() throws Exception {
        String my = mockMvc.perform(get("/v1/media/my").header("Authorization", token(teacherInd)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(my.contains(ownedTitle),
                "an independent teacher's own media must list without a tenant: " + my);

        String list = listBody(token(teacherInd), null);
        assertTrue(list.contains(ownedTitle),
                "an independent teacher's own media must appear in the library list: " + list);
        assertTrue(list.contains(openTitle),
                "an independent teacher's library also carries published open recordings: " + list);

        mockMvc.perform(get("/v1/media/" + assetId(ownedTitle))
                        .header("Authorization", token(teacherInd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(assetId(ownedTitle).toString()));
    }

    // ── denied: private, unpublished and foreign material stays closed ─────────────────────

    /** Private recordings answer "not found" to a foreign school and to a school-less reader. */
    @Test
    void privateRecording_deniedByDetailAndDownload_forForeignAndIndependentReaders()
            throws Exception {
        UUID privateId = assetId(privateTitle);

        mockMvc.perform(getWithInstitution("/v1/media/" + privateId,
                        instB.getId().toString(), token(studentB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(getWithInstitution("/v1/media/" + privateId + "/download-url",
                        instB.getId().toString(), token(studentB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/media/" + privateId).header("Authorization", token(learnerInd)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/media/" + privateId + "/download-url")
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isNotFound());
    }

    /** The private session's source and class selectors reveal nothing to outsiders. */
    @Test
    void privateSession_recordingsBySourceAndClass_revealNothingToOutsiders() throws Exception {
        String foreign = mockMvc.perform(getWithInstitution(
                        "/v1/media/recordings/LIVE_CLASS/" + privateClass.getId(),
                        instB.getId().toString(), token(studentB)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(!foreign.contains(privateTitle),
                "another school must not see this session's recordings: " + foreign);

        // An account with no tenant is not refused outright anymore (it used to 400/403 on
        // the required institution attribute) — it simply receives nothing it is entitled to.
        mockMvc.perform(get("/v1/media/recordings/LIVE_CLASS/" + privateClass.getId())
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(privateTitle))));

        mockMvc.perform(get("/v1/media/class/" + privateClassGroupId)
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(privateTitle))));

        mockMvc.perform(get("/v1/media/search").param("q", "Private")
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(privateTitle))));
    }

    /** Publication state and capture outcome gate the open branch itself. */
    @Test
    void unpublishedAndFailedRecordings_ofOpenSessions_areNeverExposed() throws Exception {
        mockMvc.perform(get("/v1/media/" + assetId(draftTitle))
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/media/" + assetId(failedOpenTitle))
                        .header("Authorization", token(learnerInd)))
                .andExpect(status().isNotFound());
        mockMvc.perform(getWithInstitution("/v1/media/" + assetId(failedOpenTitle),
                        instB.getId().toString(), token(studentB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(getWithInstitution("/v1/media/" + assetId(draftTitle),
                        instB.getId().toString(), token(studentB)))
                .andExpect(status().isNotFound());
    }

    // ── no regression on the paths that were already correct ───────────────────────────────

    /** Attendance still grants the private recording inside the owning school. */
    @Test
    void ownSchoolLearner_stillSeesThePrivateRecordingTheyAttended() throws Exception {
        mockMvc.perform(getWithInstitution("/v1/media/" + assetId(privateTitle),
                        instA.getId().toString(), token(studentA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(assetId(privateTitle).toString()));

        String body = listBody(token(studentA), instA.getId().toString());
        assertTrue(body.contains(privateTitle),
                "the attending learner's own school library must keep the private recording: " + body);
        assertTrue(body.contains(openTitle),
                "the owning school sees its own open recording through the same union: " + body);
    }

    /** Institution admins keep whole-library access inside their own tenant — and only there. */
    @Test
    void institutionAdmin_keepsOwnLibraryButCannotFetchAnotherTenantsAssetById() throws Exception {
        // Own tenant: even a FAILED capture stays diagnosable for staff (unchanged).
        mockMvc.perform(getWithInstitution("/v1/media/" + assetId(tenantBFailedTitle),
                        instB.getId().toString(), token(instAdminB)))
                .andExpect(status().isOk());

        // Another tenant by id: closed (this used to answer 200 through a blanket bypass).
        mockMvc.perform(getWithInstitution("/v1/media/" + assetId(privateTitle),
                        instB.getId().toString(), token(instAdminB)))
                .andExpect(status().isNotFound());

        // The open recording is open to everyone anyway — by the open rule, not by tenant.
        mockMvc.perform(getWithInstitution("/v1/media/" + assetId(openTitle),
                        instB.getId().toString(), token(instAdminB)))
                .andExpect(status().isOk());
    }

    /** A forged institution header is still refused outright, before any controller logic. */
    @Test
    void forgedInstitutionHeader_remainsRefused() throws Exception {
        mockMvc.perform(get("/v1/media")
                        .header("Authorization", token(studentA))
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ── fixtures (created fresh per test, removed in @AfterEach) ───────────────────────────

    private MockHttpServletRequestBuilder getWithInstitution(
            String path, String institutionId, String bearer) {
        MockHttpServletRequestBuilder request = get(path).header("Authorization", bearer);
        if (institutionId != null) {
            request.header("X-Institution-Id", institutionId);
        }
        return request;
    }

    private String listBody(String bearer, String institutionId) throws Exception {
        MvcResult result = mockMvc.perform(getWithInstitution("/v1/media", institutionId, bearer))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    /** Id of this test's own fixture row, found through the repository — never hardcoded. */
    private UUID assetId(String title) {
        return mediaAssetRepository.findAll().stream()
                .filter(a -> title.equals(a.getTitle()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("fixture asset missing: " + title))
                .getId();
    }

    private LiveClass saveClass(String title, Institution institution, Teacher teacherRow,
                                String accessLevel, String publicationStatus, UUID classGroupId) {
        LiveClass liveClass = liveClassRepository.saveAndFlush(LiveClass.builder()
                .teacherId(teacherRow.getId())
                .title(title)
                .scheduledAt(LocalDateTime.now().minusHours(2))
                .durationMinutes(60)
                .status("COMPLETED")
                .accessLevel(accessLevel)
                .publicationStatus(publicationStatus)
                .classGroupId(classGroupId)
                .institutionId(institution.getId())
                .build());
        classes.add(liveClass);
        return liveClass;
    }

    private MediaAsset saveRecording(String title, UUID sourceId, UUID classGroupId,
                                     String status, UUID ownerUserId, Institution institution) {
        MediaAsset asset = MediaAsset.builder()
                .title(title)
                .description("Media open learning fixture - " + title)
                .mediaType("RECORDING")
                .fileUrl("https://cdn.test/" + UUID.randomUUID() + ".mp4")
                .status(status)
                .visibility("INSTITUTION")
                .sourceType("LIVE_CLASS")
                .sourceId(sourceId)
                .classGroupId(classGroupId)
                .teacherId(ownerUserId)
                .build();
        if (institution != null) {
            asset.setInstitutionId(institution.getId());
        }
        MediaAsset saved = mediaAssetRepository.saveAndFlush(asset);
        return saved;
    }

    private Institution saveInstitution(String name) {
        Institution saved = institutionRepository.save(Institution.builder()
                .name(name)
                .code("MOPEN-" + UUID.randomUUID().toString().substring(0, 8))
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());
        institutions.add(saved);
        return saved;
    }

    private User saveUser(String email, User.Role role, Institution institution) {
        User user = userRepository.save(newUser(email, role, institution.getId()));
        users.add(user);
        InstitutionMembership membership = membershipRepository.save(InstitutionMembership.builder()
                .userId(user.getId())
                .institutionId(institution.getId())
                .role(membershipRole(role))
                .isActive(true)
                .build());
        memberships.add(membership);
        return user;
    }

    /** An unaffiliated account: no institution on the user and no membership row at all. */
    private User saveIndependentUser(String email, User.Role role) {
        User user = userRepository.save(newUser(email, role, null));
        users.add(user);
        return user;
    }

    private User newUser(String email, User.Role role, UUID institutionId) {
        User user = User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("MediaOpen")
                .lastName("Tester")
                .role(role)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build();
        user.setInstitutionId(institutionId);
        return user;
    }

    private Teacher saveTeacherRow(User user, String employeeNumber, Institution institution) {
        Teacher teacherRow = Teacher.builder()
                .userId(user.getId())
                .employeeNumber(employeeNumber)
                .status(TeacherStatus.ACTIVE)
                .build();
        teacherRow.setInstitutionId(institution != null ? institution.getId() : null);
        Teacher saved = teacherRepository.save(teacherRow);
        teachers.add(saved);
        return saved;
    }

    private InstitutionMembership.Role membershipRole(User.Role role) {
        return switch (role) {
            case TEACHER -> InstitutionMembership.Role.TEACHER;
            case INSTITUTION_ADMIN, ADMIN -> InstitutionMembership.Role.ADMIN;
            default -> InstitutionMembership.Role.STUDENT;
        };
    }

    /** TestTokens emits the raw JWT; the filter only accepts {@code Authorization: Bearer <jwt>}. */
    private String token(User user) {
        return "Bearer " + TestTokens.userToken(user.getEmail());
    }
}
