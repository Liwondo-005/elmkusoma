package tz.elmkusoma.liveclass.controller;

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
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.liveclass.dto.MediaAssetRequest;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherAssignmentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Audience targeting when a media asset is created.
 *
 * <p>createMedia already refused a client-supplied LIVE_CLASS origin, but stored any classGroupId
 * and subjectId the body carried - and those decide who can see the asset in their library. This
 * proves the endpoint now routes through {@link tz.elmkusoma.common.AudienceTargetGuard}: another
 * institution's class is 403, an unknown one is 404 rather than a confirmation it exists, and a
 * teacher cannot file material against a class they do not teach.</p>
 *
 * <p>The asset's own update path never accepts classGroupId or subjectId, so it needs no
 * equivalent test: there is no retarget to validate.</p>
 */
@ExtendWith(MockitoExtension.class)
class MediaAssetCreateTargetSecurityTest {

    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private ClassGroupRepository classGroupRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private TeacherAssignmentRepository teacherAssignmentRepository;

    @Spy private OwnershipGuard ownershipGuard = new OwnershipGuard();

    @InjectMocks private MediaLibraryController controller;

    private UUID institutionId;
    private UUID foreignInstitutionId;
    private UUID userId;
    private UUID classGroupId;
    private UUID subjectId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        foreignInstitutionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
        subjectId = UUID.randomUUID();

        // A real guard over these mocks so the assertions exercise the actual rule rather than a
        // stub that would let a regression through.
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "audienceTargetGuard",
                new tz.elmkusoma.common.AudienceTargetGuard(ownershipGuard, classGroupRepository,
                        subjectRepository, teacherRepository, teacherAssignmentRepository));

        lenient().when(mediaAssetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private MediaAssetRequest request() {
        MediaAssetRequest request = new MediaAssetRequest();
        request.setTitle("Optics demo");
        request.setMediaType("RECORDING");
        request.setSourceType("UPLOAD");
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

    private void teacherHasNoAssignment() {
        lenient().when(teacherRepository.findByUserIdAndInstitutionId(userId, institutionId))
                .thenReturn(Optional.of(teacher()));
        lenient().when(teacherAssignmentRepository.existsByTeacherIdAndClassGroupIdAndIsDeletedFalse(
                any(), any())).thenReturn(false);
    }

    private Teacher teacher() {
        return Teacher.builder().userId(userId).build();
    }

    private void teacherExists() {
        lenient().when(teacherRepository.findByUserIdAndInstitutionId(userId, institutionId))
                .thenReturn(Optional.of(teacher()));
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_classGroupInAnotherInstitution_isRejected() {
        classGroupOwnedBy(foreignInstitutionId);
        subjectOwnedBy(institutionId);

        assertThrows(ForbiddenException.class, () ->
                controller.createMedia(institutionId, userId, "TEACHER", request()));
    }

    @Test
    void create_subjectInAnotherInstitution_isRejected() {
        classGroupOwnedBy(institutionId);
        subjectOwnedBy(foreignInstitutionId);

        assertThrows(ForbiddenException.class, () ->
                controller.createMedia(institutionId, userId, "TEACHER", request()));
    }

    @Test
    void create_unknownClassGroup_isRejectedWithoutConfirmingItExists() {
        lenient().when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                controller.createMedia(institutionId, userId, "TEACHER", request()));
    }

    @Test
    void create_teacherWithoutAssignment_isRejected() {
        classGroupOwnedBy(institutionId);
        subjectOwnedBy(institutionId);
        teacherHasNoAssignment();

        assertThrows(SecurityException.class, () ->
                controller.createMedia(institutionId, userId, "TEACHER", request()));
    }

    @Test
    void create_institutionAdminMayTargetOwnClassWithoutAssignment_isAccepted() {
        classGroupOwnedBy(institutionId);
        subjectOwnedBy(institutionId);

        assertDoesNotThrow(() ->
                controller.createMedia(institutionId, userId, "INSTITUTION_ADMIN", request()));
    }

    @Test
    void create_institutionAdminCannotTargetForeignClass_isRejected() {
        // Exempting admins from the assignment half must not become a cross-tenant bypass.
        classGroupOwnedBy(foreignInstitutionId);
        subjectOwnedBy(institutionId);

        assertThrows(ForbiddenException.class, () ->
                controller.createMedia(institutionId, userId, "INSTITUTION_ADMIN", request()));
    }

    @Test
    void create_withoutAnyTarget_staysInstitutionWideAndIsAccepted() {
        MediaAssetRequest request = new MediaAssetRequest();
        request.setTitle("Whole school clip");
        request.setMediaType("VIDEO");
        request.setSourceType("UPLOAD");

        assertDoesNotThrow(() ->
                controller.createMedia(institutionId, userId, "TEACHER", request));
    }

    @Test
    void create_reservedLiveClassOriginIsStillRefused() {
        // The audience guard must not displace the existing check that stops a client claiming the
        // LIVE_CLASS origin and squatting a recording's slot.
        MediaAssetRequest request = new MediaAssetRequest();
        request.setTitle("Imposter");
        request.setSourceType("LIVE_CLASS");

        org.springframework.http.ResponseEntity<?> response =
                controller.createMedia(institutionId, userId, "TEACHER", request);
        assertEquals(400, response.getStatusCode().value());
    }
}
