package tz.elmkusoma.learning.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tz.elmkusoma.learning.domain.VideoTutorial;
import tz.elmkusoma.learning.dto.VideoTutorialRequest;
import tz.elmkusoma.learning.repository.VideoTutorialProgressRepository;
import tz.elmkusoma.learning.repository.VideoTutorialRepository;
import tz.elmkusoma.liveclass.service.MediaProxyService;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Visibility / ownership hardening of {@link VideoTutorialService}.
 */
@ExtendWith(MockitoExtension.class)
class VideoTutorialAccessSecurityTest {

    private static final List<String> OPEN_VISIBILITIES =
            List.of("PUBLIC", "INSTITUTION", "SCHOOL", "CLASS_ONLY", "COURSE_ONLY");

    @Mock
    private VideoTutorialRepository videoTutorialRepository;
    @Mock
    private VideoTutorialProgressRepository progressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MediaProxyService mediaProxyService;

    @InjectMocks
    private VideoTutorialService service;

    private UUID videoId;
    private UUID institutionId;
    private UUID otherInstitutionId;
    private UUID lessonId;
    private String creatorId;
    private String strangerId;

    @BeforeEach
    void setUp() {
        videoId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        otherInstitutionId = UUID.randomUUID();
        lessonId = UUID.randomUUID();
        creatorId = UUID.randomUUID().toString();
        strangerId = UUID.randomUUID().toString();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private VideoTutorial video(String createdBy, VideoTutorial.ResourceVisibility visibility) {
        return VideoTutorial.builder()
                .id(videoId)
                .institutionId(institutionId)
                .createdBy(createdBy)
                .title("Existing video")
                .status(VideoTutorial.VideoTutorialStatus.DRAFT)
                .visibility(visibility)
                .sortOrder(0)
                .isDeleted(false)
                .build();
    }

    private void stubVideo(VideoTutorial video) {
        when(videoTutorialRepository.findById(videoId)).thenReturn(Optional.of(video));
    }

    private void stubSave() {
        when(videoTutorialRepository.save(any(VideoTutorial.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private VideoTutorialRequest requestWithTitle(String title) {
        VideoTutorialRequest request = new VideoTutorialRequest();
        request.setTitle(title);
        return request;
    }

    // ── getVideoTutorial ──

    @Test
    void getVideoTutorial_adminRolesMayViewPrivateVideoOfAnotherCreator() {
        for (String role : List.of("ADMIN", "INSTITUTION_ADMIN", "NATIONAL_ADMIN")) {
            stubVideo(video(creatorId, VideoTutorial.ResourceVisibility.PRIVATE));

            assertNotNull(service.getVideoTutorial(videoId, institutionId, strangerId, role),
                    "role " + role + " must be able to view any video");

            reset(videoTutorialRepository);
        }
    }

    @Test
    void getVideoTutorial_openVisibilitiesAreViewableByNonCreator() {
        for (VideoTutorial.ResourceVisibility visibility : VideoTutorial.ResourceVisibility.values()) {
            if (!OPEN_VISIBILITIES.contains(visibility.name())) {
                continue;
            }
            stubVideo(video(creatorId, visibility));

            assertNotNull(service.getVideoTutorial(videoId, institutionId, strangerId, "STUDENT"),
                    visibility + " must be viewable without ownership");

            reset(videoTutorialRepository);
        }
    }

    @Test
    void getVideoTutorial_privateVideoDeniedToNonCreator() {
        stubVideo(video(creatorId, VideoTutorial.ResourceVisibility.PRIVATE));

        SecurityException ex = assertThrows(SecurityException.class, () ->
                service.getVideoTutorial(videoId, institutionId, strangerId, "STUDENT"));

        assertTrue(ex.getMessage().contains("not allowed"));
    }

    @Test
    void getVideoTutorial_draftVideoDeniedToNonCreator() {
        stubVideo(video(creatorId, VideoTutorial.ResourceVisibility.DRAFT));

        assertThrows(SecurityException.class, () ->
                service.getVideoTutorial(videoId, institutionId, strangerId, "TEACHER"));
    }

    @Test
    void getVideoTutorial_creatorMayViewOwnPrivateVideo() {
        stubVideo(video(creatorId, VideoTutorial.ResourceVisibility.PRIVATE));

        assertNotNull(service.getVideoTutorial(videoId, institutionId, creatorId, "TEACHER"));
    }

    @Test
    void getVideoTutorial_authenticatedEmailCountsAsCreator() {
        stubVideo(video("owner@example.com", VideoTutorial.ResourceVisibility.PRIVATE));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@example.com", null, List.of()));

        assertNotNull(service.getVideoTutorial(videoId, institutionId, strangerId, "TEACHER"));
    }

    @Test
    void getVideoTutorial_privateVideoDeniedWithoutMatchingAuthentication() {
        stubVideo(video("owner@example.com", VideoTutorial.ResourceVisibility.PRIVATE));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone.else@example.com", null, List.of()));

        assertThrows(SecurityException.class, () ->
                service.getVideoTutorial(videoId, institutionId, strangerId, "TEACHER"));
    }

    @Test
    void getVideoTutorial_crossInstitutionIsRejected() {
        assertThrows(RuntimeException.class, () ->
                service.getVideoTutorial(videoId, otherInstitutionId, creatorId, "TEACHER"));
    }

    @Test
    void getVideoTutorial_deletedVideoIsRejected() {
        VideoTutorial deleted = video(creatorId, VideoTutorial.ResourceVisibility.PUBLIC);
        deleted.setIsDeleted(true);
        stubVideo(deleted);

        assertThrows(RuntimeException.class, () ->
                service.getVideoTutorial(videoId, institutionId, creatorId, "ADMIN"));
    }

    // ── updateVideoTutorial / deleteVideoTutorial ──

    @Test
    void updateVideoTutorial_nonOwnerIsRejected() {
        stubVideo(video(creatorId, VideoTutorial.ResourceVisibility.PRIVATE));

        assertThrows(SecurityException.class, () -> service.updateVideoTutorial(
                videoId, requestWithTitle("Hijacked"), institutionId, strangerId, "TEACHER"));
        verify(videoTutorialRepository, never()).save(any());
    }

    @Test
    void updateVideoTutorial_ownerMayUpdate() {
        VideoTutorial video = video(creatorId, VideoTutorial.ResourceVisibility.PRIVATE);
        stubVideo(video);
        stubSave();

        service.updateVideoTutorial(videoId, requestWithTitle("Owned edit"), institutionId, creatorId, "TEACHER");

        ArgumentCaptor<VideoTutorial> captor = ArgumentCaptor.forClass(VideoTutorial.class);
        verify(videoTutorialRepository).save(captor.capture());
        assertEquals("Owned edit", captor.getValue().getTitle());
    }

    @Test
    void updateVideoTutorial_adminRolesMayUpdateAnotherUsersVideo() {
        for (String role : List.of("ADMIN", "INSTITUTION_ADMIN", "NATIONAL_ADMIN")) {
            stubVideo(video(creatorId, VideoTutorial.ResourceVisibility.PRIVATE));
            stubSave();

            assertDoesNotThrow(() -> service.updateVideoTutorial(
                            videoId, requestWithTitle("Admin edit"), institutionId, strangerId, role),
                    "role " + role + " must bypass video ownership");

            reset(videoTutorialRepository);
        }
    }

    @Test
    void updateVideoTutorial_crossInstitutionIsRejected() {
        stubVideo(video(creatorId, VideoTutorial.ResourceVisibility.PUBLIC));

        assertThrows(RuntimeException.class, () -> service.updateVideoTutorial(
                videoId, requestWithTitle("Cross org"), otherInstitutionId, creatorId, "ADMIN"));
        verify(videoTutorialRepository, never()).save(any());
    }

    @Test
    void deleteVideoTutorial_nonOwnerIsRejected() {
        stubVideo(video(creatorId, VideoTutorial.ResourceVisibility.PUBLIC));

        assertThrows(SecurityException.class, () -> service.deleteVideoTutorial(
                videoId, institutionId, strangerId, "TEACHER"));
        verify(videoTutorialRepository, never()).save(any());
    }

    @Test
    void deleteVideoTutorial_ownerMaySoftDelete() {
        VideoTutorial video = video(creatorId, VideoTutorial.ResourceVisibility.PUBLIC);
        stubVideo(video);
        stubSave();

        service.deleteVideoTutorial(videoId, institutionId, creatorId, "TEACHER");

        assertTrue(video.getIsDeleted());
        verify(videoTutorialRepository).save(video);
    }

    @Test
    void deleteVideoTutorial_adminMayDeleteAnotherUsersVideo() {
        VideoTutorial video = video(creatorId, VideoTutorial.ResourceVisibility.PUBLIC);
        stubVideo(video);
        stubSave();

        service.deleteVideoTutorial(videoId, institutionId, strangerId, "ADMIN");

        assertTrue(video.getIsDeleted());
    }

    // ── listVideoTutorials uses the caller's visibility set ──

    @Test
    void listVideoTutorials_studentOnlySeesOpenVisibilities() {
        List<String> studentVisibilities = List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION");
        when(videoTutorialRepository.findByInstitutionIdAndVisibilities(institutionId, studentVisibilities))
                .thenReturn(List.of());

        service.listVideoTutorials(institutionId, strangerId, "STUDENT", null, null, null, null, null, 0, 20);

        verify(videoTutorialRepository).findByInstitutionIdAndVisibilities(institutionId, studentVisibilities);
    }

    @Test
    void listVideoTutorials_teacherSeesPrivateAndDraft() {
        List<String> teacherVisibilities =
                List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");
        when(videoTutorialRepository.findByInstitutionIdAndVisibilities(institutionId, teacherVisibilities))
                .thenReturn(List.of());

        service.listVideoTutorials(institutionId, strangerId, "TEACHER", null, null, null, null, null, 0, 20);

        verify(videoTutorialRepository).findByInstitutionIdAndVisibilities(institutionId, teacherVisibilities);
    }

    @Test
    void listVideoTutorials_institutionAdminSeesPrivateAndDraft() {
        List<String> adminVisibilities =
                List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");
        when(videoTutorialRepository.findByInstitutionIdAndVisibilities(institutionId, adminVisibilities))
                .thenReturn(List.of());

        service.listVideoTutorials(institutionId, strangerId, "INSTITUTION_ADMIN", null, null, null, null, null, 0, 20);

        verify(videoTutorialRepository).findByInstitutionIdAndVisibilities(institutionId, adminVisibilities);
    }

    @Test
    void listVideoTutorials_lessonScopedQueryUsesCallerVisibilitySet() {
        List<String> teacherVisibilities =
                List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT");
        when(videoTutorialRepository.findVisibleByLessonId(lessonId, teacherVisibilities))
                .thenReturn(List.of());

        service.listVideoTutorials(institutionId, strangerId, "TEACHER",
                lessonId, null, null, null, null, 0, 20);

        verify(videoTutorialRepository).findVisibleByLessonId(lessonId, teacherVisibilities);
    }

    @Test
    void listVideoTutorials_studentLessonScopedQueryExcludesPrivateAndDraft() {
        List<String> studentVisibilities = List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION");
        when(videoTutorialRepository.findVisibleByLessonId(lessonId, studentVisibilities))
                .thenReturn(List.of());

        service.listVideoTutorials(institutionId, strangerId, "STUDENT",
                lessonId, null, null, null, null, 0, 20);

        verify(videoTutorialRepository).findVisibleByLessonId(lessonId, studentVisibilities);
    }
}
