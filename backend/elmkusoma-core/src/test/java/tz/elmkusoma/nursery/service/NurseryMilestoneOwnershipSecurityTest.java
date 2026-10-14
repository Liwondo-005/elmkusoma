package tz.elmkusoma.nursery.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.common.OwnershipGuard;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nursery.domain.NurseryMilestone;
import tz.elmkusoma.nursery.dto.request.CreateNurseryMilestoneRequest;
import tz.elmkusoma.nursery.dto.response.NurseryMilestoneResponse;
import tz.elmkusoma.nursery.repository.NurseryMilestoneRepository;
import tz.elmkusoma.nursery.service.impl.NurseryMilestoneServiceImpl;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Institution scoping of {@link NurseryMilestoneServiceImpl}: student-keyed
 * reads and id-based reads must resolve the owning Student and fail closed
 * across tenants (403) before touching the repository.
 */
@ExtendWith(MockitoExtension.class)
class NurseryMilestoneOwnershipSecurityTest {

    @Mock
    private NurseryMilestoneRepository nurseryMilestoneRepository;
    @Mock
    private StudentRepository studentRepository;

    /**
     * A real instance, spied. OwnershipGuard is a concrete collaborator, not an
     * interface, so {@code @InjectMocks} would otherwise pass null and every
     * ownership check would NPE instead of failing closed.
     */
    @Spy
    private OwnershipGuard ownershipGuard = new OwnershipGuard();

    @InjectMocks
    private NurseryMilestoneServiceImpl service;

    private UUID institutionId;
    private UUID otherInstitutionId;
    private UUID studentId;
    private UUID milestoneId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        otherInstitutionId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        milestoneId = UUID.randomUUID();
    }

    private Student studentOwnedBy(UUID ownerInstitutionId) {
        return Student.builder()
                .id(studentId)
                .institutionId(ownerInstitutionId)
                .admissionNumber("ADM-001")
                .build();
    }

    private NurseryMilestone milestone() {
        return NurseryMilestone.builder()
                .id(milestoneId)
                .studentId(studentId)
                .category(NurseryMilestone.MilestoneCategory.LANGUAGE)
                .milestoneName("First words")
                .status(NurseryMilestone.MilestoneStatus.PENDING)
                .isDeleted(false)
                .build();
    }

    // ── getByStudentId ──

    @Test
    void getByStudentId_crossInstitutionStudentIsRejected() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getByStudentId(institutionId, studentId));

        verify(nurseryMilestoneRepository, never()).findByStudentIdAndIsDeletedFalse(any());
    }

    @Test
    void getByStudentId_unknownStudentIsNotFound() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getByStudentId(institutionId, studentId));

        verify(nurseryMilestoneRepository, never()).findByStudentIdAndIsDeletedFalse(any());
    }

    @Test
    void getByStudentId_sameInstitutionIsAllowed() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(institutionId)));
        when(nurseryMilestoneRepository.findByStudentIdAndIsDeletedFalse(studentId))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> service.getByStudentId(institutionId, studentId));

        verify(nurseryMilestoneRepository).findByStudentIdAndIsDeletedFalse(studentId);
    }

    // ── getByStudentAndCategory ──

    @Test
    void getByStudentAndCategory_crossInstitutionStudentIsRejected() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getByStudentAndCategory(
                institutionId, studentId, "LANGUAGE"));

        verify(nurseryMilestoneRepository, never()).findByStudentAndCategory(any(), any());
    }

    // ── getById ──

    @Test
    void getById_crossInstitutionMilestoneIsRejected() {
        when(nurseryMilestoneRepository.findById(milestoneId)).thenReturn(Optional.of(milestone()));
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getById(institutionId, milestoneId));
    }

    @Test
    void getById_unknownMilestoneIsNotFound() {
        when(nurseryMilestoneRepository.findById(milestoneId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(institutionId, milestoneId));
    }

    @Test
    void getById_sameInstitutionMilestoneIsReadable() {
        when(nurseryMilestoneRepository.findById(milestoneId)).thenReturn(Optional.of(milestone()));
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(institutionId)));

        NurseryMilestoneResponse response = service.getById(institutionId, milestoneId);

        assertEquals("First words", response.getMilestoneName());
    }

    // ── update ──

    @Test
    void update_crossInstitutionMilestoneIsRejectedWithoutSave() {
        when(nurseryMilestoneRepository.findById(milestoneId)).thenReturn(Optional.of(milestone()));
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(otherInstitutionId)));

        CreateNurseryMilestoneRequest request = new CreateNurseryMilestoneRequest();
        request.setMilestoneName("Hijacked");

        assertThrows(ForbiddenException.class,
                () -> service.update(institutionId, milestoneId, request));

        verify(nurseryMilestoneRepository, never()).save(any());
    }

    @Test
    void update_unknownMilestoneIsNotFound() {
        when(nurseryMilestoneRepository.findById(milestoneId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(
                institutionId, milestoneId, new CreateNurseryMilestoneRequest()));

        verify(nurseryMilestoneRepository, never()).save(any());
    }

    // ── delete ──

    @Test
    void delete_crossInstitutionMilestoneIsRejectedWithoutSave() {
        when(nurseryMilestoneRepository.findById(milestoneId)).thenReturn(Optional.of(milestone()));
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.delete(institutionId, milestoneId));

        verify(nurseryMilestoneRepository, never()).save(any());
    }

    @Test
    void delete_sameInstitutionSoftDeletes() {
        NurseryMilestone milestone = milestone();
        when(nurseryMilestoneRepository.findById(milestoneId)).thenReturn(Optional.of(milestone));
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(institutionId)));
        when(nurseryMilestoneRepository.save(any(NurseryMilestone.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(institutionId, milestoneId);

        assertTrue(milestone.getIsDeleted());
        verify(nurseryMilestoneRepository).save(milestone);
    }
}
