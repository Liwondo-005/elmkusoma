package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.course.domain.Announcement;
import tz.elmkusoma.course.repository.AnnouncementRepository;
import tz.elmkusoma.exception.GlobalExceptionHandler;
import tz.elmkusoma.learner.controller.LearnerController;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An announcement aimed at a class must reach that class - and only that class.
 *
 * <p>Creating or retargeting an announcement now validates {@code classGroupId} against the
 * authoritative targeting rule, but the learner read path fetched every published row in the
 * institution with no class predicate at all. Targeting enforced on write and ignored on read is
 * not targeting: it let any learner read another class's announcements.</p>
 *
 * <p>Institution-wide rows ({@code classGroupId == null}) deliberately keep their existing
 * visibility, so this narrows the class-scoped case only.</p>
 */
@ExtendWith(MockitoExtension.class)
class LearnerAnnouncementAudienceTest {

    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClassAccessGuard classAccessGuard;

    @InjectMocks
    private LearnerController controller;

    private MockMvc mockMvc;

    private UUID institutionId;
    private UUID userId;
    private UUID classMine;
    private UUID classOther;
    private String learnerEmail;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        classMine = UUID.randomUUID();
        classOther = UUID.randomUUID();
        learnerEmail = "learner@darms.edu.tz";

        User learner = new User();
        learner.setId(userId);
        learner.setEmail(learnerEmail);
        when(userRepository.findById(userId)).thenReturn(Optional.of(learner));
        when(announcementRepository.findJurisdictionalAndIsDeletedFalse()).thenReturn(List.of());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Announcement announcement(String title, UUID classGroupId) {
        Announcement announcement = new Announcement();
        announcement.setId(UUID.randomUUID());
        announcement.setInstitutionId(institutionId);
        announcement.setTitle(title);
        announcement.setClassGroupId(classGroupId);
        announcement.setStatus("PUBLISHED");
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setIsDeleted(false);
        return announcement;
    }

    private void stubLearnerFeed(List<Announcement> announcements) {
        when(announcementRepository.findVisibleToLearnersByInstitutionId(any(), any()))
                .thenReturn(announcements);
    }

    private String fetchFeed() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/learner/announcements")
                        .requestAttr("institutionId", institutionId)
                        .requestAttr("userId", userId))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    @Test
    void classScopedAnnouncementIsHiddenFromALearnerOfAnotherClass() throws Exception {
        stubLearnerFeed(List.of(
                announcement("Class A notice", classMine),
                announcement("Class B notice", classOther)));
        when(classAccessGuard.isLearnerInClass(learnerEmail, classMine)).thenReturn(true);
        when(classAccessGuard.isLearnerInClass(learnerEmail, classOther)).thenReturn(false);

        String body = fetchFeed();

        assertTrue(body.contains("Class A notice"),
                "the learner belongs to this class and must see its notice");
        assertFalse(body.contains("Class B notice"),
                "class targeting is meaningless if the read path serves every class's notice to "
                        + "the whole school");
    }

    @Test
    void learnerWithNoPlacementSeesOnlyInstitutionWideAnnouncements() throws Exception {
        stubLearnerFeed(List.of(
                announcement("Class A notice", classMine),
                announcement("Whole school notice", null)));
        when(classAccessGuard.isLearnerInClass(any(), any())).thenReturn(false);

        String body = fetchFeed();

        assertFalse(body.contains("Class A notice"),
                "a learner with no placement must not receive class-scoped announcements");
        assertTrue(body.contains("Whole school notice"),
                "institution-wide announcements must keep the visibility they always had");
    }

    @Test
    void institutionWideFeedIsUntouchedWhenNothingIsClassScoped() throws Exception {
        stubLearnerFeed(List.of(
                announcement("Whole school notice", null),
                announcement("Sports day", null)));

        String body = fetchFeed();

        assertTrue(body.contains("Whole school notice"));
        assertTrue(body.contains("Sports day"));
    }

    @Test
    void aLearnerOfTheTargetClassStillReceivesIt() throws Exception {
        stubLearnerFeed(List.of(announcement("Class A notice", classMine)));
        when(classAccessGuard.isLearnerInClass(learnerEmail, classMine)).thenReturn(true);

        String body = fetchFeed();

        assertTrue(body.contains("Class A notice"),
                "the filter must narrow to the audience, not empty the feed");
    }
}
