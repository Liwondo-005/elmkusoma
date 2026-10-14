package tz.elmkusoma.attendance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.attendance.repository.AttendanceRecordRepository;
import tz.elmkusoma.attendance.repository.AttendanceSummaryRepository;
import tz.elmkusoma.attendance.service.impl.AttendanceServiceImpl;
import tz.elmkusoma.common.OwnershipGuard;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Institution scoping of the attendance read paths in
 * {@link AttendanceServiceImpl}: class-keyed reads verify the ClassGroup,
 * student-keyed reads verify the Student, and both fail closed across tenants
 * (403) before any record or summary row is fetched.
 */
@ExtendWith(MockitoExtension.class)
class AttendanceReadScopeSecurityTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private AttendanceSummaryRepository attendanceSummaryRepository;
    @Mock
    private ClassGroupRepository classGroupRepository;
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
    private AttendanceServiceImpl service;

    private UUID institutionId;
    private UUID otherInstitutionId;
    private UUID classGroupId;
    private UUID studentId;
    private UUID termId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        otherInstitutionId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        termId = UUID.randomUUID();
    }

    private ClassGroup classGroupOwnedBy(UUID ownerInstitutionId) {
        return ClassGroup.builder()
                .id(classGroupId)
                .institutionId(ownerInstitutionId)
                .name("Kidu A")
                .build();
    }

    private Student studentOwnedBy(UUID ownerInstitutionId) {
        return Student.builder()
                .id(studentId)
                .institutionId(ownerInstitutionId)
                .admissionNumber("ADM-001")
                .build();
    }

    // ── getByClassAndDate ──

    @Test
    void getByClassAndDate_crossInstitutionClassGroupIsRejected() {
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getByClassAndDate(
                institutionId, classGroupId, LocalDate.of(2026, 1, 5)));

        verify(attendanceRecordRepository, never())
                .findByClassGroupIdAndAttendanceDateAndIsDeletedFalse(any(), any());
    }

    @Test
    void getByClassAndDate_unknownClassGroupIsNotFound() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getByClassAndDate(
                institutionId, classGroupId, LocalDate.of(2026, 1, 5)));

        verify(attendanceRecordRepository, never())
                .findByClassGroupIdAndAttendanceDateAndIsDeletedFalse(any(), any());
    }

    @Test
    void getByClassAndDate_sameInstitutionIsAllowed() {
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(institutionId)));
        when(attendanceRecordRepository.findByClassGroupIdAndAttendanceDateAndIsDeletedFalse(
                classGroupId, LocalDate.of(2026, 1, 5))).thenReturn(List.of());

        assertDoesNotThrow(() -> service.getByClassAndDate(
                institutionId, classGroupId, LocalDate.of(2026, 1, 5)));

        verify(attendanceRecordRepository)
                .findByClassGroupIdAndAttendanceDateAndIsDeletedFalse(classGroupId, LocalDate.of(2026, 1, 5));
    }

    @Test
    void getByClassAndDate_nullInstitutionFailsClosed() {
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(institutionId)));

        assertThrows(ForbiddenException.class, () -> service.getByClassAndDate(
                null, classGroupId, LocalDate.of(2026, 1, 5)));

        verify(attendanceRecordRepository, never())
                .findByClassGroupIdAndAttendanceDateAndIsDeletedFalse(any(), any());
    }

    // ── getByStudentAndDateRange ──

    @Test
    void getByStudentAndDateRange_crossInstitutionStudentIsRejected() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getByStudentAndDateRange(
                institutionId, studentId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));

        verify(attendanceRecordRepository, never()).findByStudentAndDateRange(any(), any(), any());
    }

    @Test
    void getByStudentAndDateRange_unknownStudentIsNotFound() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getByStudentAndDateRange(
                institutionId, studentId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));

        verify(attendanceRecordRepository, never()).findByStudentAndDateRange(any(), any(), any());
    }

    @Test
    void getByStudentAndDateRange_sameInstitutionIsAllowed() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(institutionId)));
        when(attendanceRecordRepository.findByStudentAndDateRange(
                studentId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))).thenReturn(List.of());

        assertDoesNotThrow(() -> service.getByStudentAndDateRange(
                institutionId, studentId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));

        verify(attendanceRecordRepository)
                .findByStudentAndDateRange(studentId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    }

    // ── getSummary ──

    @Test
    void getSummary_crossInstitutionStudentIsRejected() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class,
                () -> service.getSummary(institutionId, studentId, termId));

        verify(attendanceSummaryRepository, never())
                .findByStudentIdAndTermIdAndIsDeletedFalse(any(), any());
    }

    @Test
    void getSummary_unknownStudentIsNotFound() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getSummary(institutionId, studentId, termId));

        verify(attendanceSummaryRepository, never())
                .findByStudentIdAndTermIdAndIsDeletedFalse(any(), any());
    }

    @Test
    void getSummary_ownStudentWithoutSummaryStillReturnsNull() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(institutionId)));
        when(attendanceSummaryRepository.findByStudentIdAndTermIdAndIsDeletedFalse(studentId, termId))
                .thenReturn(Optional.empty());

        assertNull(service.getSummary(institutionId, studentId, termId));
    }

    // ── getByClassAndTerm ──

    @Test
    void getByClassAndTerm_crossInstitutionClassGroupIsRejected() {
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class,
                () -> service.getByClassAndTerm(institutionId, classGroupId, termId));

        verify(attendanceSummaryRepository, never())
                .findByClassGroupIdAndTermIdAndIsDeletedFalse(any(), any());
    }

    @Test
    void getByClassAndTerm_unknownClassGroupIsNotFound() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getByClassAndTerm(institutionId, classGroupId, termId));

        verify(attendanceSummaryRepository, never())
                .findByClassGroupIdAndTermIdAndIsDeletedFalse(any(), any());
    }

    @Test
    void getByClassAndTerm_sameInstitutionIsAllowed() {
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(institutionId)));
        when(attendanceSummaryRepository.findByClassGroupIdAndTermIdAndIsDeletedFalse(classGroupId, termId))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> service.getByClassAndTerm(institutionId, classGroupId, termId));

        verify(attendanceSummaryRepository)
                .findByClassGroupIdAndTermIdAndIsDeletedFalse(classGroupId, termId);
    }
}
