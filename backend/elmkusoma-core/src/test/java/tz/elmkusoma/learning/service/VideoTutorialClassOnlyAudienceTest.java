package tz.elmkusoma.learning.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tz.elmkusoma.learning.domain.Lesson;
import tz.elmkusoma.learning.domain.VideoTutorial;
import tz.elmkusoma.learning.dto.VideoTutorialResponse;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.learning.repository.VideoTutorialProgressRepository;
import tz.elmkusoma.learning.repository.VideoTutorialRepository;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.liveclass.service.LiveClassRecordingAccessService;
import tz.elmkusoma.liveclass.service.MediaProxyService;
import tz.elmkusoma.liveclass.service.RecordingPlaybackUrls;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@code CLASS_ONLY} must mean class only, and a client-supplied scoping id must never name
 * another tenant's content.
 *
 * <p>Two defects are pinned here. The visibility gate returned {@code true} for {@code CLASS_ONLY}
 * unconditionally, so the label was an institution-wide switch wearing a class-scoped name. And the
 * lesson/module/course listing branches carried no tenant predicate, so anyone who learned a
 * foreign {@code lessonId} could read that school's tutorials - the exact hole
 * {@code ResourceService} already guards against on its own equivalent branches.</p>
 *
 * <p>Class scope is derived from the tutorial's existing {@code lessonId} through
 * {@code Lesson.classGroupId}; no second targeting model and no migration. When no class can be
 * derived the item stays institution-wide, which is what every pre-existing record relied on and
 * what {@link VideoTutorialAccessSecurityTest} continues to pin untouched.</p>
 */
class VideoTutorialClassOnlyAudienceTest {

    private VideoTutorialRepository videoTutorialRepository;
    private LessonRepository lessonRepository;
    private UserRepository userRepository;
    private ClassAccessGuard classAccessGuard;
    private InstitutionMembershipRepository membershipRepository;
    private VideoTutorialService service;

    private UUID institutionMine;
    private UUID institutionForeign;
    private UUID classMine;
    private UUID classOther;
    private UUID lessonId;
    private UUID videoId;
    private UUID learnerId;
    private String learnerEmail;

    private static final List<String> STUDENT_VISIBILITIES =
            List.of("PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION");

    @BeforeEach
    void setUp() {
        institutionMine = UUID.randomUUID();
        institutionForeign = UUID.randomUUID();
        classMine = UUID.randomUUID();
        classOther = UUID.randomUUID();
        lessonId = UUID.randomUUID();
        videoId = UUID.randomUUID();
        learnerId = UUID.randomUUID();
        learnerEmail = "learner@darms.edu.tz";

        videoTutorialRepository = mock(VideoTutorialRepository.class);
        lessonRepository = mock(LessonRepository.class);
        userRepository = mock(UserRepository.class);
        classAccessGuard = mock(ClassAccessGuard.class);
        membershipRepository = mock(InstitutionMembershipRepository.class);

        User learner = new User();
        learner.setEmail(learnerEmail);
        when(userRepository.findById(learnerId)).thenReturn(Optional.of(learner));
        when(membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(learnerId, institutionMine))
                .thenReturn(true);
        when(classAccessGuard.isLearnerInClass(learnerEmail, classMine)).thenReturn(true);
        when(classAccessGuard.isLearnerInClass(learnerEmail, classOther)).thenReturn(false);

        // The real audience service, so the chain a video walks is the same one a recording walks.
        LiveClassRecordingAccessService recordingAccess = new LiveClassRecordingAccessService(
                mock(LiveClassRepository.class), mock(TeacherRepository.class),
                membershipRepository, userRepository, classAccessGuard);

        service = new VideoTutorialService(
                videoTutorialRepository,
                mock(VideoTutorialProgressRepository.class),
                userRepository,
                mock(MediaProxyService.class),
                mock(MediaAssetRepository.class),
                mock(RecordingPlaybackUrls.class),
                lessonRepository,
                recordingAccess);
    }

    private void lessonTeaching(UUID classGroupId) {
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setClassGroupId(classGroupId);
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
    }

    private VideoTutorial classOnlyVideo() {
        VideoTutorial video = new VideoTutorial();
        video.setId(videoId);
        video.setInstitutionId(institutionMine);
        video.setLessonId(lessonId);
        video.setTitle("Class-only tutorial");
        video.setVisibility(VideoTutorial.ResourceVisibility.CLASS_ONLY);
        video.setIsDeleted(false);
        return video;
    }

    private void stubVideo(VideoTutorial video) {
        when(videoTutorialRepository.findById(videoId)).thenReturn(Optional.of(video));
    }

    // ---- detail --------------------------------------------------------------------------

    @Test
    void classOnlyTutorialIsVisibleToALearnerOfTheNamedClass() {
        lessonTeaching(classMine);
        stubVideo(classOnlyVideo());

        VideoTutorialResponse response =
                service.getVideoTutorial(videoId, institutionMine, learnerId.toString(), "STUDENT");

        assertNotNull(response, "a learner placed in the class the tutorial names must reach it");
    }

    @Test
    void classOnlyTutorialIsDeniedToALearnerOfAnotherClass() {
        // The tutorial is written for classOther; this learner sits in classMine only.
        lessonTeaching(classOther);
        stubVideo(classOnlyVideo());

        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.getVideoTutorial(videoId, institutionMine, learnerId.toString(), "STUDENT"));

        assertTrue(ex.getMessage().contains("not allowed"),
                "the denial must come from the real authorization rule, not a silent default");
    }

    @Test
    void classOnlyTutorialIsDeniedToACallerWithNoClassIdentity() {
        lessonTeaching(classMine);
        stubVideo(classOnlyVideo());

        // No resolvable caller identity: a class-scoped check must fail closed, not open.
        assertThrows(SecurityException.class,
                () -> service.getVideoTutorial(videoId, institutionMine, null, "STUDENT"));
    }

    @Test
    void classOnlyTutorialStaysInstitutionWideWhenItDeclaresNoLesson() {
        VideoTutorial video = classOnlyVideo();
        video.setLessonId(null);
        stubVideo(video);

        assertNotNull(service.getVideoTutorial(videoId, institutionMine, learnerId.toString(), "STUDENT"),
                "with nothing to scope to the tutorial keeps the institution-wide behaviour every "
                        + "existing record depends on");
        verify(lessonRepository, never()).findById(any());
    }

    @Test
    void classOnlyTutorialStaysInstitutionWideWhenItsLessonNamesNoClass() {
        lessonTeaching(null);
        stubVideo(classOnlyVideo());

        assertNotNull(service.getVideoTutorial(videoId, institutionMine, learnerId.toString(), "STUDENT"),
                "a lesson that names no class declares no audience, so the session-wide policy holds");
    }

    @Test
    void staffKeepTheirExistingReachOverClassOnlyTutorials() {
        lessonTeaching(classMine);
        stubVideo(classOnlyVideo());

        // The requirement binds learners; staff reach within their own institution is unchanged.
        for (String staffRole : List.of("TEACHER", "INSTITUTION_ADMIN")) {
            assertNotNull(service.getVideoTutorial(videoId, institutionMine, learnerId.toString(), staffRole),
                    staffRole + " must retain access to class-scoped material in its own institution");
            reset(videoTutorialRepository);
            stubVideo(classOnlyVideo());
        }
    }

    @Test
    void crossInstitutionDetailRequestIsStillRejected() {
        VideoTutorial video = classOnlyVideo();
        video.setInstitutionId(institutionForeign);
        stubVideo(video);

        assertThrows(RuntimeException.class,
                () -> service.getVideoTutorial(videoId, institutionMine, learnerId.toString(), "STUDENT"));
    }

    // ---- listing -------------------------------------------------------------------------

    @Test
    void listingDoesNotRevealClassOnlyMetadataToAnOutsider() {
        lessonTeaching(classMine);
        when(videoTutorialRepository.findVisibleByLessonId(lessonId, STUDENT_VISIBILITIES))
                .thenReturn(List.of(classOnlyVideo()));
        // A learner placed somewhere else entirely.
        when(classAccessGuard.isLearnerInClass(learnerEmail, classMine)).thenReturn(false);

        List<VideoTutorialResponse> result = service.listVideoTutorials(
                institutionMine, learnerId.toString(), "STUDENT",
                lessonId, null, null, null, null, 0, 20);

        assertTrue(result.isEmpty(),
                "the listing must not advertise a CLASS_ONLY tutorial the detail endpoint refuses");
    }

    @Test
    void listingRevealsClassOnlyMetadataToAMemberOfTheNamedClass() {
        lessonTeaching(classMine);
        when(videoTutorialRepository.findVisibleByLessonId(lessonId, STUDENT_VISIBILITIES))
                .thenReturn(List.of(classOnlyVideo()));

        List<VideoTutorialResponse> result = service.listVideoTutorials(
                institutionMine, learnerId.toString(), "STUDENT",
                lessonId, null, null, null, null, 0, 20);

        assertEquals(1, result.size(),
                "a learner of the named class must see the tutorial in the listing");
    }

    @Test
    void aForeignLessonIdCannotListAnotherTenantsTutorials() {
        UUID foreignLessonId = UUID.randomUUID();
        VideoTutorial foreign = new VideoTutorial();
        foreign.setId(UUID.randomUUID());
        foreign.setInstitutionId(institutionForeign);
        foreign.setLessonId(foreignLessonId);
        foreign.setTitle("Foreign school tutorial");
        foreign.setVisibility(VideoTutorial.ResourceVisibility.PUBLIC);
        foreign.setIsDeleted(false);

        when(videoTutorialRepository.findVisibleByLessonId(foreignLessonId, STUDENT_VISIBILITIES))
                .thenReturn(List.of(foreign));

        List<VideoTutorialResponse> result = service.listVideoTutorials(
                institutionMine, learnerId.toString(), "STUDENT",
                foreignLessonId, null, null, null, null, 0, 20);

        assertTrue(result.isEmpty(),
                "the lesson id is client-supplied and proves nothing about the caller's tenant");
    }

    @Test
    void aForeignModuleIdAndCourseIdAreRejectedTheSameWay() {
        UUID foreignModuleId = UUID.randomUUID();
        UUID foreignCourseId = UUID.randomUUID();
        VideoTutorial foreign = new VideoTutorial();
        foreign.setId(UUID.randomUUID());
        foreign.setInstitutionId(institutionForeign);
        foreign.setVisibility(VideoTutorial.ResourceVisibility.PUBLIC);
        foreign.setIsDeleted(false);

        when(videoTutorialRepository.findVisibleByModuleId(foreignModuleId, STUDENT_VISIBILITIES))
                .thenReturn(List.of(foreign));
        when(videoTutorialRepository.findVisibleByCourseId(foreignCourseId, STUDENT_VISIBILITIES))
                .thenReturn(List.of(foreign));

        assertTrue(service.listVideoTutorials(institutionMine, learnerId.toString(), "STUDENT",
                        null, foreignModuleId, null, null, null, 0, 20).isEmpty(),
                "a foreign module id must not list another tenant's tutorials");
        assertTrue(service.listVideoTutorials(institutionMine, learnerId.toString(), "STUDENT",
                        null, null, foreignCourseId, null, null, 0, 20).isEmpty(),
                "a foreign course id must not list another tenant's tutorials");
    }

    @Test
    void aTeacherListingByLessonStillSeesTheirOwnTenant() {
        when(videoTutorialRepository.findVisibleByLessonId(lessonId, List.of(
                        "PUBLIC", "COURSE_ONLY", "CLASS_ONLY", "SCHOOL", "INSTITUTION", "PRIVATE", "DRAFT")))
                .thenReturn(List.of(classOnlyVideo()));

        List<VideoTutorialResponse> result = service.listVideoTutorials(
                institutionMine, learnerId.toString(), "TEACHER",
                lessonId, null, null, null, null, 0, 20);

        assertEquals(1, result.size(),
                "staff listing reach within their own tenant must be preserved");
    }
}
