package tz.elmkusoma.course.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.common.OwnershipGuard;
import tz.elmkusoma.course.domain.Announcement;
import tz.elmkusoma.course.dto.AnnouncementResponse;
import tz.elmkusoma.course.dto.CreateAnnouncementRequest;
import tz.elmkusoma.course.repository.AnnouncementRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherAssignmentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Audience targeting for teacher announcements.
 *
 * <p>Announcements used to store whatever classGroupId and subjectId the request carried, so a
 * teacher could address another school's class, or a class in their own school they do not teach.
 * The rule is inherited from {@link tz.elmkusoma.common.AudienceTargetGuard}, and this proves the
 * announcement paths actually route through it.</p>
 *
 * <p>Expectations: an unknown target is 404 ({@link ResourceNotFoundException}) so the API never
 * confirms that a foreign id exists; a target owned by another institution is 403
 * ({@link ForbiddenException}); a teacher naming a class they hold no assignment for is refused
 * with {@link SecurityException}; an institution admin is not held to the assignment half; and an
 * institution-wide announcement (no target) is unaffected.</p>
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementAudienceTargetSecurityTest {

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private ClassGroupRepository classGroupRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private TeacherAssignmentRepository teacherAssignmentRepository;

    @Spy private OwnershipGuard ownershipGuard = new OwnershipGuard();

    @InjectMocks private AnnouncementServiceImpl service;

    private UUID institutionId;
    private UUID foreignInstitutionId;
    private UUID classGroupId;
    private UUID subjectId;
    private UUID teacherUserId;
    private UUID teacherId;
    private UUID classGroupInstitutionId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        foreignInstitutionId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
        teacherUserId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        classGroupInstitutionId = institutionId;

        // The audience rule lives in the guard, so hand the service a real one over these same
        // mocks. Mocking it would let every assertion below pass without testing anything.
        org.springframework.test.util.ReflectionTestUtils.setField(service, "audienceTargetGuard",
                new tz.elmkusoma.common.AudienceTargetGuard(ownershipGuard, classGroupRepository,
                        subjectRepository, teacherRepository, teacherAssignmentRepository));

        // Helper stubs are lenient: validation short-circuits on the first bad target, so several
        // tests legitimately never reach the repositories stubbed here.
        lenient().when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateAnnouncementRequest request() {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest();
        request.setTitle("Term begins");
        request.setContent("Classes resume Monday");
        request.setClassGroupId(classGroupId);
        request.setSubjectId(subjectId);
        return request;
    }

    private void classGroupOwnedBy(UUID owner) {
        lenient().when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(
                ClassGroup.builder().id(classGroupId).institutionId(owner).isDeleted(false).build()));
    }

    private void subjectOwnedBy(UUID owner) {
        lenient().when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(
                Subject.builder().id(subjectId).institutionId(owner).isDeleted(false).build()));
    }

    private void teacherExistsWithoutAssignment() {
        lenient().when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(teacher()));
        lenient().when(teacherAssignmentRepository.existsByTeacherIdAndClassGroupIdAndIsDeletedFalse(
                teacherId, classGroupId)).thenReturn(false);
    }

    private Teacher teacher() {
        Teacher teacher = Teacher.builder().userId(teacherUserId).build();
        teacher.setId(teacherId);
        return teacher;
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_classGroupOwnedByAnotherInstitution_isRejected() {
        classGroupOwnedBy(foreignInstitutionId);
        subjectOwnedBy(institutionId);

        assertThrows(ForbiddenException.class, () -> service.createAnnouncement(
                teacherUserId, institutionId, "TEACHER", request()));
    }

    @Test
    void create_unknownClassGroup_isRejectedWithoutConfirmingItExists() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.empty());
        lenient().when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(
                Subject.builder().id(subjectId).institutionId(institutionId).isDeleted(false).build()));

        assertThrows(ResourceNotFoundException.class, () -> service.createAnnouncement(
                teacherUserId, institutionId, "TEACHER", request()));
    }

    @Test
    void create_teacherWithoutAssignmentForOwnClass_isRejected() {
        classGroupOwnedBy(institutionId);
        subjectOwnedBy(institutionId);
        teacherExistsWithoutAssignment();

        assertThrows(SecurityException.class, () -> service.createAnnouncement(
                teacherUserId, institutionId, "TEACHER", request()));
    }

    @Test
    void create_teacherWithAssignment_isAccepted() {
        classGroupOwnedBy(institutionId);
        subjectOwnedBy(institutionId);
        when(teacherRepository.findByUserIdAndInstitutionId(teacherUserId, institutionId))
                .thenReturn(Optional.of(teacher()));
        when(teacherAssignmentRepository.existsByTeacherIdAndClassGroupIdAndIsDeletedFalse(
                teacherId, classGroupId)).thenReturn(true);

        AnnouncementResponse saved = assertDoesNotThrow(() -> service.createAnnouncement(
                teacherUserId, institutionId, "TEACHER", request()));
        assertEquals(classGroupId, saved.getClassGroupId());
        assertEquals(teacherUserId, saved.getAuthorId());
    }

    @Test
    void create_institutionAdminTargetsOwnClassWithoutAssignment_isAccepted() {
        // The assignment half binds teachers only; an admin may address any class in their own
        // school. Containment still applies, so this must not become a cross-tenant bypass.
        classGroupOwnedBy(institutionId);
        subjectOwnedBy(institutionId);

        AnnouncementResponse saved = assertDoesNotThrow(() -> service.createAnnouncement(
                teacherUserId, institutionId, "INSTITUTION_ADMIN", request()));
        assertEquals(classGroupId, saved.getClassGroupId());
    }

    @Test
    void create_institutionAdminCannotTargetForeignClass_isRejected() {
        classGroupOwnedBy(foreignInstitutionId);
        subjectOwnedBy(institutionId);

        assertThrows(ForbiddenException.class, () -> service.createAnnouncement(
                teacherUserId, institutionId, "INSTITUTION_ADMIN", request()));
    }

    @Test
    void create_withoutAnyTarget_staysInstitutionWideAndIsAccepted() {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest();
        request.setTitle("Whole school notice");
        request.setContent("Sports day Friday");

        AnnouncementResponse saved = assertDoesNotThrow(() -> service.createAnnouncement(
                teacherUserId, institutionId, "TEACHER", request));
        assertNull(saved.getClassGroupId());
        assertNull(saved.getSubjectId());
    }

    // ── retarget ─────────────────────────────────────────────────────────────

    private Announcement existingAnnouncement(UUID announcementId) {
        Announcement announcement = Announcement.builder()
                .authorId(teacherUserId)
                .title("Term begins")
                .content("Classes resume Monday")
                .build();
        announcement.setId(announcementId);
        announcement.setInstitutionId(institutionId);
        announcement.setIsDeleted(false);
        when(announcementRepository.findById(announcementId)).thenReturn(Optional.of(announcement));
        return announcement;
    }

    @Test
    void retarget_cannotAdoptAnotherInstitutionsClass() {
        UUID announcementId = UUID.randomUUID();
        existingAnnouncement(announcementId);

        CreateAnnouncementRequest request = new CreateAnnouncementRequest();
        request.setClassGroupId(classGroupId);
        classGroupOwnedBy(foreignInstitutionId);

        assertThrows(ForbiddenException.class, () -> service.updateAnnouncement(
                teacherUserId, announcementId, "TEACHER", request));
    }

    @Test
    void retarget_cannotAimAtAClassTheTeacherDoesNotTeach() {
        UUID announcementId = UUID.randomUUID();
        existingAnnouncement(announcementId);

        CreateAnnouncementRequest request = new CreateAnnouncementRequest();
        request.setClassGroupId(classGroupId);
        classGroupOwnedBy(institutionId);
        teacherExistsWithoutAssignment();

        assertThrows(SecurityException.class, () -> service.updateAnnouncement(
                teacherUserId, announcementId, "TEACHER", request));
    }

    @Test
    void retarget_fieldThatIsNotBeingChangedIsNotValidated() {
        // A legacy announcement recorded before this rule existed must still be editable for
        // title and content: only the audience fields actually supplied are checked.
        UUID announcementId = UUID.randomUUID();
        existingAnnouncement(announcementId);

        CreateAnnouncementRequest request = new CreateAnnouncementRequest();
        request.setTitle("Updated title");

        assertDoesNotThrow(() -> service.updateAnnouncement(
                teacherUserId, announcementId, "TEACHER", request));
    }
}
