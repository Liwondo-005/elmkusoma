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
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.dto.CreateLiveClassRequest;
import tz.elmkusoma.course.dto.LiveClassResponse;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherAssignmentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Audience targeting for live sessions.
 *
 * <p>classGroupId and subjectId decide who may join a session and who later receives the
 * recording, yet createLiveClass and updateLiveClass stored whatever the request carried. This
 * proves both paths route through {@link tz.elmkusoma.common.AudienceTargetGuard}: a target in
 * another institution is 403, an unknown one is 404 rather than a confirmation that it exists,
 * and a teacher cannot schedule a session for a class they hold no assignment for.</p>
 *
 * <p>The teacher-assignment half binds teachers only - an institution admin may target any class
 * in their own school - so the admin cross-tenant case is asserted separately to show containment
 * does not weaken when the assignment check is skipped.</p>
 */
@ExtendWith(MockitoExtension.class)
class LiveClassAudienceTargetSecurityTest {

    @Mock private LiveClassRepository liveClassRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private ClassGroupRepository classGroupRepository;
    @Mock private TeacherAssignmentRepository teacherAssignmentRepository;
    // Needed by mapToResponse on the two happy-path tests; unrelated to audience validation.
    @Mock private tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository participantRepository;

    @Spy private OwnershipGuard ownershipGuard = new OwnershipGuard();

    @InjectMocks private LiveClassServiceImpl liveClassService;

    private UUID teacherId;
    private UUID userId;
    private UUID institutionId;
    private UUID foreignInstitutionId;
    private UUID classGroupId;
    private UUID subjectId;
    private UUID liveClassId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        userId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        foreignInstitutionId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
        liveClassId = UUID.randomUUID();

        // A real guard over these same mocks, so the assertions below exercise the actual rule
        // instead of a stubbed no-op that would let a regression through unnoticed.
        org.springframework.test.util.ReflectionTestUtils.setField(liveClassService,
                "audienceTargetGuard",
                new tz.elmkusoma.common.AudienceTargetGuard(ownershipGuard, classGroupRepository,
                        subjectRepository, teacherRepository, teacherAssignmentRepository));

        lenient().when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> {
            LiveClass saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
    }

    private CreateLiveClassRequest request() {
        CreateLiveClassRequest request = new CreateLiveClassRequest();
        request.setTitle("Physics 101");
        request.setScheduledAt(LocalDateTime.now().plusDays(1).toString());
        request.setDurationMinutes(60);
        request.setClassGroupId(classGroupId);
        request.setSubjectId(subjectId);
        return request;
    }

    private void teacherExists() {
        lenient().when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher()));
    }

    private Teacher teacher() {
        Teacher teacher = Teacher.builder().userId(userId).build();
        teacher.setId(teacherId);
        return teacher;
    }

    private void classGroupOwnedBy(UUID owner) {
        lenient().when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.of(
                ClassGroup.builder().id(classGroupId).institutionId(owner).isDeleted(false).build()));
    }

    private void subjectOwnedBy(UUID owner) {
        lenient().when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(
                Subject.builder().id(subjectId).institutionId(owner).isDeleted(false).build()));
    }

    private void teacherHasNoAssignment() {
        lenient().when(teacherRepository.findByUserIdAndInstitutionId(userId, institutionId))
                .thenReturn(Optional.of(teacher()));
        lenient().when(teacherAssignmentRepository.existsByTeacherIdAndClassGroupIdAndIsDeletedFalse(
                teacherId, classGroupId)).thenReturn(false);
    }

    private LiveClass existingLiveClass(UUID owner) {
        LiveClass liveClass = LiveClass.builder()
                .teacherId(teacherId)
                .title("Physics 101")
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .durationMinutes(60)
                .status("SCHEDULED")
                .build();
        liveClass.setId(liveClassId);
        liveClass.setInstitutionId(owner);
        liveClass.setIsDeleted(false);
        when(liveClassRepository.findById(liveClassId)).thenReturn(Optional.of(liveClass));
        return liveClass;
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_classGroupInAnotherInstitution_isRejected() {
        teacherExists();
        classGroupOwnedBy(foreignInstitutionId);
        subjectOwnedBy(institutionId);

        assertThrows(ForbiddenException.class, () -> liveClassService.createLiveClass(
                teacherId, institutionId, userId, "TEACHER", request()));
    }

    @Test
    void create_subjectInAnotherInstitution_isRejected() {
        teacherExists();
        classGroupOwnedBy(institutionId);
        subjectOwnedBy(foreignInstitutionId);

        assertThrows(ForbiddenException.class, () -> liveClassService.createLiveClass(
                teacherId, institutionId, userId, "TEACHER", request()));
    }

    @Test
    void create_unknownClassGroup_isRejectedWithoutConfirmingItExists() {
        teacherExists();
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> liveClassService.createLiveClass(
                teacherId, institutionId, userId, "TEACHER", request()));
    }

    @Test
    void create_teacherWithoutAssignment_isRejected() {
        teacherExists();
        classGroupOwnedBy(institutionId);
        subjectOwnedBy(institutionId);
        teacherHasNoAssignment();

        assertThrows(SecurityException.class, () -> liveClassService.createLiveClass(
                teacherId, institutionId, userId, "TEACHER", request()));
    }

    @Test
    void create_institutionAdminMayTargetOwnClassWithoutAssignment_isAccepted() {
        teacherExists();
        classGroupOwnedBy(institutionId);
        subjectOwnedBy(institutionId);

        LiveClassResponse saved = assertDoesNotThrow(() -> liveClassService.createLiveClass(
                teacherId, institutionId, userId, "INSTITUTION_ADMIN", request()));
        assertEquals(classGroupId, saved.getClassGroupId());
    }

    @Test
    void create_institutionAdminCannotTargetForeignClass_isRejected() {
        // Containment binds every role, so exempting admins from the assignment half must not
        // open a cross-tenant path.
        teacherExists();
        classGroupOwnedBy(foreignInstitutionId);
        subjectOwnedBy(institutionId);

        assertThrows(ForbiddenException.class, () -> liveClassService.createLiveClass(
                teacherId, institutionId, userId, "INSTITUTION_ADMIN", request()));
    }

    // ── retarget ─────────────────────────────────────────────────────────────

    @Test
    void retarget_cannotAdoptAnotherInstitutionsClass() {
        existingLiveClass(institutionId);

        CreateLiveClassRequest request = new CreateLiveClassRequest();
        request.setClassGroupId(classGroupId);
        classGroupOwnedBy(foreignInstitutionId);

        assertThrows(ForbiddenException.class, () -> liveClassService.updateLiveClass(
                teacherId, liveClassId, userId, "TEACHER", request));
    }

    @Test
    void retarget_cannotAimAtAClassTheTeacherDoesNotTeach() {
        existingLiveClass(institutionId);

        CreateLiveClassRequest request = new CreateLiveClassRequest();
        request.setClassGroupId(classGroupId);
        classGroupOwnedBy(institutionId);
        teacherHasNoAssignment();

        assertThrows(SecurityException.class, () -> liveClassService.updateLiveClass(
                teacherId, liveClassId, userId, "TEACHER", request));
    }

    @Test
    void retarget_checkedAgainstTheLiveClassesOwnInstitutionNotTheCallers() {
        // The session belongs to another institution while the requested class is ours. The guard
        // compares the target against the persisted owner rather than the caller's context, so
        // this is refused - proving the owner it reads is the stored one.
        existingLiveClass(foreignInstitutionId);

        CreateLiveClassRequest request = new CreateLiveClassRequest();
        request.setClassGroupId(classGroupId);
        classGroupOwnedBy(institutionId);

        assertThrows(ForbiddenException.class, () -> liveClassService.updateLiveClass(
                teacherId, liveClassId, userId, "TEACHER", request));
    }

    @Test
    void retarget_fieldThatIsNotBeingChangedIsNotValidated() {
        // Editing the title must not trip the audience guard, so a session recorded before this
        // rule existed stays maintainable.
        existingLiveClass(institutionId);

        CreateLiveClassRequest request = new CreateLiveClassRequest();
        request.setTitle("Physics 101 - renamed");

        assertDoesNotThrow(() -> liveClassService.updateLiveClass(
                teacherId, liveClassId, userId, "TEACHER", request));
    }
}
