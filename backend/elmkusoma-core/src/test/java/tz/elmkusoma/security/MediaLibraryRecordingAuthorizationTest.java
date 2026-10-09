package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.domain.MediaAsset;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.testutil.TestTokens;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Media Library authorization for live class recordings.
 *
 * <p>{@code GET /v1/media} allowed the STUDENT role but only checked institution membership,
 * so any student in a tenant could list, open and download every other class's recordings.
 * That was latent while the library had no RECORDING rows; publishing recordings arms it.
 * These tests pin the rule that now matches the Replay Library: a learner may only reach a
 * recording of a session they took part in (a live class participant row).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class MediaLibraryRecordingAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private InstitutionRepository institutionRepository;
    @Autowired private MediaAssetRepository mediaAssetRepository;
    @Autowired private LiveClassParticipantRepository participantRepository;
    @Autowired private InstitutionMembershipRepository membershipRepository;

    private User outsider;
    private Institution institution;
    private UUID liveClassId;

    @BeforeEach
    void setUp() {
        String run = UUID.randomUUID().toString().substring(0, 8);
        institution = institutionRepository.save(Institution.builder()
                .name("MediaAuthz " + run)
                .code("MA-" + run)
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());

        outsider = saveLearner("media-outsider-" + run + "@test.com", institution.getId());
        liveClassId = UUID.randomUUID();
    }

    private User saveLearner(String email, UUID institutionId) {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("Media")
                .lastName("Learner")
                .role(User.Role.STUDENT)
                .institutionId(institutionId)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        membershipRepository.save(InstitutionMembership.builder()
                .userId(user.getId())
                .institutionId(institutionId)
                .role(InstitutionMembership.Role.STUDENT)
                .isActive(true)
                .build());
        return user;
    }

    private MediaAsset saveRecording(String title) {
        MediaAsset asset = MediaAsset.builder()
                .title(title)
                .description("Live class recording - " + title)
                .mediaType("RECORDING")
                .fileUrl("https://cdn.test/recording.mp4")
                .status("READY")
                .visibility("INSTITUTION")
                .sourceType("LIVE_CLASS")
                .sourceId(liveClassId)
                .classGroupId(UUID.randomUUID())
                // A different teacher owns this recording.
                .teacherId(UUID.randomUUID())
                .build();
        asset.setInstitutionId(institution.getId());
        return mediaAssetRepository.saveAndFlush(asset);
    }

    @Test
    void learnerWithoutParticipation_cannotSeeRecordingInList() throws Exception {
        saveRecording("Hidden From Learner");

        String body = mockMvc.perform(get("/v1/media")
                        .header("Authorization", "Bearer " + TestTokens.userToken(outsider.getEmail()))
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // The asset belongs to another teacher's class and the caller never participated.
        if (body.contains("Hidden From Learner")) {
            throw new AssertionError("institution membership alone exposed another class's recording");
        }
    }

    @Test
    void learnerWithoutParticipation_cannotOpenRecordingDetail() throws Exception {
        MediaAsset recording = saveRecording("Hidden Detail");

        mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(outsider.getEmail()))
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isNotFound());
    }

    /** The download path must not be weaker than the detail path. */
    @Test
    void learnerWithoutParticipation_cannotGetDownloadUrl() throws Exception {
        MediaAsset recording = saveRecording("Hidden Download");

        mockMvc.perform(get("/v1/media/" + recording.getId() + "/download-url")
                        .header("Authorization", "Bearer " + TestTokens.userToken(outsider.getEmail()))
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void learnerWithoutParticipation_cannotEnumerateRecordingsByLiveClassId() throws Exception {
        saveRecording("Hidden By Origin");

        mockMvc.perform(get("/v1/media/recordings/LIVE_CLASS/" + liveClassId)
                        .header("Authorization", "Bearer " + TestTokens.userToken(outsider.getEmail()))
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void learnerCannotEnumerateRecordingsByClassGroupId() throws Exception {
        MediaAsset recording = saveRecording("Hidden By Class");
        UUID classGroupId = recording.getClassGroupId();

        mockMvc.perform(get("/v1/media/class/" + classGroupId)
                        .header("Authorization", "Bearer " + TestTokens.userToken(outsider.getEmail()))
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * The positive half of the entitlement rule: a learner who DID take part in the recorded
 * *LiveClass* must be able to reach the recording. Without this the suite would happily pass
 * with every recording denied to everyone, including the students it is meant to serve.
 */
    @Test
    void learnerWhoParticipated_canOpenTheirRecording() throws Exception {
        MediaAsset recording = saveRecording("Entitled Recording");

        participantRepository.saveAndFlush(LiveClassParticipant.builder()
                .liveClassId(liveClassId)
                .userId(outsider.getId())
                .role("LEARNER")
                .joinedAt(java.time.LocalDateTime.now())
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(outsider.getEmail()))
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/media/" + recording.getId() + "/download-url")
                        .header("Authorization", "Bearer " + TestTokens.userToken(outsider.getEmail()))
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isOk());

        String list = mockMvc.perform(get("/v1/media")
                        .header("Authorization", "Bearer " + TestTokens.userToken(outsider.getEmail()))
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        if (!list.contains("Entitled Recording")) {
            throw new AssertionError("an entitled participant must see their own recording in the list");
        }
    }

    /**
     * The teacher who recorded it keeps access without being a participant row, so a session
     * that never had a participant (or whose rows expired) is still reviewable by its teacher.
     */
    @Test
    void owningTeacher_canOpenRecordingWithoutParticipationRow() throws Exception {
        MediaAsset recording = saveRecording("Teacher Owned Recording");

        User teacher = userRepository.save(User.builder()
                .email("media-teacher-" + UUID.randomUUID() + "@test.com")
                .passwordHash("test-hash")
                .firstName("Media")
                .lastName("Teacher")
                .role(User.Role.TEACHER)
                .institutionId(institution.getId())
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        // media_assets.teacher_id stores a users.id, so the asset must point at the saved user.
        recording.setTeacherId(teacher.getId());
        mediaAssetRepository.saveAndFlush(recording);
        membershipRepository.save(InstitutionMembership.builder()
                .userId(teacher.getId())
                .institutionId(institution.getId())
                .role(InstitutionMembership.Role.TEACHER)
                .isActive(true)
                .build());

        mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(teacher.getEmail()))
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequest_isDenied() throws Exception {
        mockMvc.perform(get("/v1/media")
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().is4xxClientError());
    }

    /** Case F: cross-institution access is denied before entitlement is even considered. */
    @Test
    void foreignInstitutionRequest_isDenied() throws Exception {
        MediaAsset recording = saveRecording("Foreign Tenant Recording");

        mockMvc.perform(get("/v1/media/" + recording.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(outsider.getEmail()))
                        .header("X-Institution-Id", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    /** Non-recording media must stay visible to ordinary learners (no regression). */
    @Test
    void nonRecordingAsset_remainsVisibleToLearner() throws Exception {
        MediaAsset pdf = MediaAsset.builder()
                .title("Shared Handbook")
                .mediaType("DOCUMENT")
                .fileUrl("https://cdn.test/handbook.pdf")
                .status("READY")
                .visibility("INSTITUTION")
                .teacherId(UUID.randomUUID())
                .build();
        pdf.setInstitutionId(institution.getId());
        mediaAssetRepository.saveAndFlush(pdf);

        mockMvc.perform(get("/v1/media/" + pdf.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(outsider.getEmail()))
                        .header("X-Institution-Id", institution.getId().toString()))
                .andExpect(status().isOk());
    }

    /**
     * LIVE_CLASS is a reserved origin: only the recording hand-off writes it, because
     * {@code canViewAsset} derives replay entitlement from it and V150 makes
     * {@code (source_type, source_id)} unique. A client that could claim it would attach an
     * arbitrary file to someone else's session and squat the one slot the genuine recording
     * needs - permanently blocking that session's recording from ever reaching the library.
     */
    @Test
    void createMedia_cannotClaimLiveClassOrigin() throws Exception {
        User teacher = saveTeacher("origin");

        mockMvc.perform(post("/v1/media")
                        .header("Authorization", "Bearer " + TestTokens.userToken(teacher.getEmail()))
                        .header("X-Institution-Id", institution.getId().toString())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"forged recording\",\"mediaType\":\"RECORDING\","
                                + "\"fileUrl\":\"https://cdn.test/forged.mp4\","
                                + "\"sourceType\":\"LIVE_CLASS\",\"sourceId\":\"" + liveClassId + "\"}"))
                .andExpect(status().isBadRequest());

        assertEquals(0, mediaAssetRepository
                        .findBySourceTypeAndSourceIdAndIsDeletedFalse("LIVE_CLASS", liveClassId).size(),
                "the reserved origin must not be written by a client request");
    }

    /** The entitlement check is case-insensitive, so the guard must be too. */
    @Test
    void createMedia_cannotClaimLiveClassOriginInAnotherCasing() throws Exception {
        User teacher = saveTeacher("origin-casing");

        for (String casing : new String[] {"live_class", "Live_Class", " LIVE_CLASS "}) {
            mockMvc.perform(post("/v1/media")
                            .header("Authorization", "Bearer " + TestTokens.userToken(teacher.getEmail()))
                            .header("X-Institution-Id", institution.getId().toString())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"forged recording\",\"mediaType\":\"RECORDING\","
                                    + "\"fileUrl\":\"https://cdn.test/forged.mp4\","
                                    + "\"sourceType\":\"" + casing + "\",\"sourceId\":\"" + liveClassId + "\"}"))
                    .andExpect(status().isBadRequest());
        }

        assertEquals(0, mediaAssetRepository
                        .findBySourceTypeAndSourceIdAndIsDeletedFalse("LIVE_CLASS", liveClassId).size());
    }

    /** The guard must not stop ordinary uploads, which never carry that origin. */
    @Test
    void createMedia_stillAcceptsAnOrdinaryUpload() throws Exception {
        User teacher = saveTeacher("ordinary");

        mockMvc.perform(post("/v1/media")
                        .header("Authorization", "Bearer " + TestTokens.userToken(teacher.getEmail()))
                        .header("X-Institution-Id", institution.getId().toString())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"lesson video\",\"mediaType\":\"VIDEO\","
                                + "\"fileUrl\":\"https://cdn.test/lesson.mp4\","
                                + "\"sourceType\":\"COURSE\",\"sourceId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated());
    }

    private User saveTeacher(String tag) {
        User teacher = userRepository.save(User.builder()
                .email("media-" + tag + "-" + UUID.randomUUID() + "@test.com")
                .passwordHash("test-hash")
                .firstName("Media")
                .lastName("Teacher")
                .role(User.Role.TEACHER)
                .institutionId(institution.getId())
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        membershipRepository.save(InstitutionMembership.builder()
                .userId(teacher.getId())
                .institutionId(institution.getId())
                .role(InstitutionMembership.Role.TEACHER)
                .isActive(true)
                .build());
        return teacher;
    }
}