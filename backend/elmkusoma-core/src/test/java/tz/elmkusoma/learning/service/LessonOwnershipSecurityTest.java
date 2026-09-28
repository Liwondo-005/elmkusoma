package tz.elmkusoma.learning.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.domain.Lesson;
import tz.elmkusoma.learning.dto.request.LessonRequest;
import tz.elmkusoma.learning.repository.AssignmentRepository;
import tz.elmkusoma.learning.repository.AssignmentSubmissionRepository;
import tz.elmkusoma.learning.repository.LessonProgressRepository;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.learning.service.impl.LearningServiceImpl;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Ownership / institution / status hardening of {@link LearningServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class LessonOwnershipSecurityTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private AssignmentSubmissionRepository submissionRepository;
    @Mock
    private StudentClassAssignmentRepository studentClassAssignmentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LearningServiceImpl service;

    private UUID lessonId;
    private UUID institutionId;
    private UUID otherInstitutionId;
    private UUID subjectId;
    private UUID classGroupId;

    @BeforeEach
    void setUp() {
        lessonId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        otherInstitutionId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
    }

    private Lesson lesson(String createdBy) {
        return Lesson.builder()
                .id(lessonId)
                .institutionId(institutionId)
                .subjectId(subjectId)
                .classGroupId(classGroupId)
                .title("Existing lesson")
                .sortOrder(0)
                .isPublished(false)
                .status("DRAFT")
                .isDeleted(false)
                .createdBy(createdBy)
                .build();
    }

    private LessonRequest requestWithTitle(String title) {
        LessonRequest request = new LessonRequest();
        request.setSubjectId(subjectId);
        request.setClassGroupId(classGroupId);
        request.setTitle(title);
        request.setSortOrder(0);
        return request;
    }

    private void stubLesson(Lesson lesson) {
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
    }

    private void stubSave() {
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ── updateLesson ──

    @Test
    void updateLesson_ownerMayUpdateOwnLesson() {
        Lesson lesson = lesson("owner@example.com");
        stubLesson(lesson);
        stubSave();

        service.updateLesson(lessonId, requestWithTitle("Updated"), institutionId, "owner@example.com", "TEACHER");

        ArgumentCaptor<Lesson> captor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository).save(captor.capture());
        assertEquals("Updated", captor.getValue().getTitle());
    }

    @Test
    void updateLesson_ownerMatchIsCaseInsensitive() {
        stubLesson(lesson("Owner@Example.com"));
        stubSave();

        assertDoesNotThrow(() -> service.updateLesson(
                lessonId, requestWithTitle("Updated"), institutionId, "owner@example.com", "TEACHER"));
    }

    @Test
    void updateLesson_nonOwnerWithNonAdminRoleIsRejected() {
        stubLesson(lesson("owner@example.com"));

        SecurityException ex = assertThrows(SecurityException.class, () -> service.updateLesson(
                lessonId, requestWithTitle("Hijacked"), institutionId, "intruder@example.com", "TEACHER"));

        assertTrue(ex.getMessage().contains("do not own"));
        verify(lessonRepository, never()).save(any());
    }

    @Test
    void updateLesson_allNonAdminRolesAreRejectedForNonOwner() {
        for (String role : List.of("STUDENT", "PARENT", "TEACHER", "LECTURER", "INSTRUCTOR", "OTHER_LEARNER")) {
            stubLesson(lesson("owner@example.com"));

            assertThrows(SecurityException.class, () -> service.updateLesson(
                    lessonId, requestWithTitle("Hijacked"), institutionId, "intruder@example.com", role),
                    "role " + role + " must not be able to edit someone else's lesson");

            reset(lessonRepository);
        }
        verify(lessonRepository, never()).save(any());
    }

    @Test
    void updateLesson_nullCallerEmailIsRejected() {
        stubLesson(lesson("owner@example.com"));

        assertThrows(SecurityException.class, () -> service.updateLesson(
                lessonId, requestWithTitle("Updated"), institutionId, null, "TEACHER"));
    }

    @Test
    void updateLesson_adminRolesBypassOwnership() {
        for (String role : List.of("ADMIN", "INSTITUTION_ADMIN", "NATIONAL_ADMIN")) {
            stubLesson(lesson("owner@example.com"));
            stubSave();

            assertDoesNotThrow(() -> service.updateLesson(
                            lessonId, requestWithTitle("Admin edit"), institutionId, "other@example.com", role),
                    "role " + role + " must bypass lesson ownership");

            reset(lessonRepository);
        }
    }

    @Test
    void updateLesson_institutionMismatchIsRejectedForOwner() {
        stubLesson(lesson("owner@example.com"));

        assertThrows(ResourceNotFoundException.class, () -> service.updateLesson(
                lessonId, requestWithTitle("Updated"), otherInstitutionId, "owner@example.com", "TEACHER"));
        verify(lessonRepository, never()).save(any());
    }

    @Test
    void updateLesson_adminFromAnotherInstitutionIsRejected() {
        stubLesson(lesson("owner@example.com"));

        assertThrows(ResourceNotFoundException.class, () -> service.updateLesson(
                lessonId, requestWithTitle("Updated"), otherInstitutionId, "admin@example.com", "ADMIN"));
        verify(lessonRepository, never()).save(any());
    }

    @Test
    void updateLesson_missingLessonIsRejected() {
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateLesson(
                lessonId, requestWithTitle("Updated"), institutionId, "admin@example.com", "ADMIN"));
    }

    @Test
    void updateLesson_deletedLessonIsRejected() {
        Lesson deleted = lesson("owner@example.com");
        deleted.setIsDeleted(true);
        stubLesson(deleted);

        assertThrows(ResourceNotFoundException.class, () -> service.updateLesson(
                lessonId, requestWithTitle("Updated"), institutionId, "owner@example.com", "TEACHER"));
        verify(lessonRepository, never()).save(any());
    }

    // ── deleteLesson ──

    @Test
    void deleteLesson_nonOwnerIsRejected() {
        stubLesson(lesson("owner@example.com"));

        assertThrows(SecurityException.class, () -> service.deleteLesson(
                lessonId, institutionId, "intruder@example.com", "TEACHER"));
        verify(lessonRepository, never()).save(any());
    }

    @Test
    void deleteLesson_ownerMaySoftDeleteOwnLesson() {
        Lesson lesson = lesson("owner@example.com");
        stubLesson(lesson);
        stubSave();

        service.deleteLesson(lessonId, institutionId, "owner@example.com", "TEACHER");

        assertTrue(lesson.getIsDeleted());
        verify(lessonRepository).save(lesson);
    }

    @Test
    void deleteLesson_adminMayDeleteAnotherUsersLesson() {
        Lesson lesson = lesson("owner@example.com");
        stubLesson(lesson);
        stubSave();

        service.deleteLesson(lessonId, institutionId, "admin@example.com", "INSTITUTION_ADMIN");

        assertTrue(lesson.getIsDeleted());
    }

    @Test
    void deleteLesson_institutionMismatchIsRejected() {
        stubLesson(lesson("owner@example.com"));

        assertThrows(ResourceNotFoundException.class, () -> service.deleteLesson(
                lessonId, otherInstitutionId, "owner@example.com", "TEACHER"));
    }

    // ── setLessonStatus ──

    @Test
    void setLessonStatus_invalidStatusIsRejectedBeforeAnyLookup() {
        for (String status : List.of("BOGUS", "PUBLISH", "", "null")) {
            assertThrows(IllegalArgumentException.class, () -> service.setLessonStatus(
                    lessonId, status, institutionId, "owner@example.com", "TEACHER"),
                    "status '" + status + "' must be rejected");
        }
        assertThrows(IllegalArgumentException.class, () -> service.setLessonStatus(
                lessonId, null, institutionId, "owner@example.com", "TEACHER"));
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void setLessonStatus_normalizesWhitespaceAndCase() {
        Lesson lesson = lesson("owner@example.com");
        stubLesson(lesson);
        stubSave();

        service.setLessonStatus(lessonId, "  published  ", institutionId, "owner@example.com", "TEACHER");

        assertEquals("PUBLISHED", lesson.getStatus());
        assertTrue(lesson.getIsPublished());
    }

    @Test
    void setLessonStatus_publishingSyncsIsPublishedTrue() {
        Lesson lesson = lesson("owner@example.com");
        stubLesson(lesson);
        stubSave();

        service.setLessonStatus(lessonId, "PUBLISHED", institutionId, "owner@example.com", "TEACHER");

        assertEquals("PUBLISHED", lesson.getStatus());
        assertTrue(lesson.getIsPublished());
    }

    @Test
    void setLessonStatus_leavingPublishedStateClearsIsPublished() {
        for (String status : List.of("DRAFT", "READY", "ARCHIVED")) {
            Lesson lesson = lesson("owner@example.com");
            lesson.setStatus("PUBLISHED");
            lesson.setIsPublished(true);
            stubLesson(lesson);
            stubSave();

            service.setLessonStatus(lessonId, status, institutionId, "owner@example.com", "TEACHER");

            assertEquals(status, lesson.getStatus());
            assertFalse(lesson.getIsPublished(), "isPublished must be cleared for status " + status);

            reset(lessonRepository);
        }
    }

    @Test
    void setLessonStatus_nonOwnerIsRejected() {
        stubLesson(lesson("owner@example.com"));

        assertThrows(SecurityException.class, () -> service.setLessonStatus(
                lessonId, "PUBLISHED", institutionId, "intruder@example.com", "TEACHER"));
        verify(lessonRepository, never()).save(any());
    }

    @Test
    void setLessonStatus_institutionMismatchIsRejected() {
        stubLesson(lesson("owner@example.com"));

        assertThrows(ResourceNotFoundException.class, () -> service.setLessonStatus(
                lessonId, "PUBLISHED", otherInstitutionId, "admin@example.com", "ADMIN"));
    }

    // ── getLessonsByClass visibility filtering ──

    @Test
    void getLessonsByClass_hidesUnpublishedAndArchivedByDefault() {
        Lesson published = lesson("owner@example.com");
        published.setIsPublished(true);
        published.setStatus("PUBLISHED");

        Lesson draft = lesson("owner@example.com");
        draft.setStatus("DRAFT");
        draft.setIsPublished(false);

        Lesson archived = lesson("owner@example.com");
        archived.setStatus("ARCHIVED");
        archived.setIsPublished(true);

        Lesson readyUnpublished = lesson("owner@example.com");
        readyUnpublished.setStatus("READY");
        readyUnpublished.setIsPublished(false);

        when(lessonRepository.findByClassGroupIdAndIsDeletedFalseOrderBySortOrder(classGroupId))
                .thenReturn(List.of(published, draft, archived, readyUnpublished));

        List<?> visible = service.getLessonsByClass(classGroupId, false);

        assertEquals(1, visible.size(), "only the published lesson must be visible by default");
    }

    @Test
    void getLessonsByClass_includeUnpublishedReturnsEverything() {
        Lesson published = lesson("owner@example.com");
        published.setIsPublished(true);
        published.setStatus("PUBLISHED");

        Lesson draft = lesson("owner@example.com");
        draft.setStatus("DRAFT");
        draft.setIsPublished(false);

        Lesson archived = lesson("owner@example.com");
        archived.setStatus("ARCHIVED");
        archived.setIsPublished(false);

        when(lessonRepository.findByClassGroupIdAndIsDeletedFalseOrderBySortOrder(classGroupId))
                .thenReturn(List.of(published, draft, archived));

        assertEquals(3, service.getLessonsByClass(classGroupId, true).size());
    }

    @Test
    void getLessonsBySubjectAndClass_hidesUnpublishedAndArchivedByDefault() {
        Lesson published = lesson("owner@example.com");
        published.setIsPublished(true);
        published.setStatus("PUBLISHED");

        Lesson archived = lesson("owner@example.com");
        archived.setStatus("ARCHIVED");
        archived.setIsPublished(true);

        when(lessonRepository.findBySubjectIdAndClassGroupIdAndIsDeletedFalseOrderBySortOrder(
                subjectId, classGroupId)).thenReturn(List.of(published, archived));

        assertEquals(1, service.getLessonsBySubjectAndClass(subjectId, classGroupId, false).size());
        assertEquals(2, service.getLessonsBySubjectAndClass(subjectId, classGroupId, true).size());
    }
}
