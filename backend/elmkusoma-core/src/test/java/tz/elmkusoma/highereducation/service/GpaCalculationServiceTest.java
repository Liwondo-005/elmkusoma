package tz.elmkusoma.highereducation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.highereducation.domain.*;
import tz.elmkusoma.highereducation.repository.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GpaCalculationServiceTest {

    @Mock
    private StudentCourseEnrollmentRepository enrollmentRepository;

    @Mock
    private AcademicRecordRepository academicRecordRepository;

    @InjectMocks
    private GpaCalculationService gpaCalculationService;

    private UUID studentId;
    private UUID courseId1;
    private UUID courseId2;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        courseId1 = UUID.randomUUID();
        courseId2 = UUID.randomUUID();
    }

    @Test
    void computeSemesterGpa_returnsWeightedAverage() {
        List<StudentCourseEnrollment> enrollments = List.of(
                buildEnrollment(courseId1, 3, 3.5, EnrollmentStatus.COMPLETED),
                buildEnrollment(courseId2, 4, 4.0, EnrollmentStatus.COMPLETED)
        );
        when(enrollmentRepository.findByStudentIdAndSemesterAndAcademicYearAndIsDeletedFalse(
                eq(studentId), eq("SEMESTER1"), eq("2025"))).thenReturn(enrollments);

        Double result = gpaCalculationService.computeSemesterGpa(studentId, "SEMESTER1", "2025");

        assertNotNull(result);
        // (3*3.5 + 4*4.0) / (3+4) = (10.5 + 16) / 7 = 26.5/7 ≈ 3.79
        assertEquals(3.79, result, 0.01);
    }

    @Test
    void computeSemesterGpa_noGrades_returnsNull() {
        when(enrollmentRepository.findByStudentIdAndSemesterAndAcademicYearAndIsDeletedFalse(
                any(), any(), any())).thenReturn(Collections.emptyList());

        Double result = gpaCalculationService.computeSemesterGpa(studentId, "SEMESTER1", "2025");

        assertNull(result);
    }

    @Test
    void computeCumulativeGpa_returnsWeightedAverage() {
        List<StudentCourseEnrollment> allEnrollments = List.of(
                buildEnrollment(courseId1, 3, 3.0, EnrollmentStatus.COMPLETED),
                buildEnrollment(courseId2, 4, 4.0, EnrollmentStatus.COMPLETED),
                buildEnrollment(UUID.randomUUID(), 2, null, EnrollmentStatus.ENROLLED)
        );
        when(enrollmentRepository.findByStudentIdAndIsDeletedFalse(studentId)).thenReturn(allEnrollments);

        Double result = gpaCalculationService.computeCumulativeGpa(studentId);

        assertNotNull(result);
        // (3*3.0 + 4*4.0) / (3+4) = (9+16)/7 = 25/7 ≈ 3.57
        assertEquals(3.57, result, 0.01);
    }

    @Test
    void computeAcademicStanding_firstClass() {
        List<StudentCourseEnrollment> enrollments = List.of(
                buildEnrollment(courseId1, 3, 3.8, EnrollmentStatus.COMPLETED),
                buildEnrollment(courseId2, 3, 3.9, EnrollmentStatus.COMPLETED)
        );
        when(enrollmentRepository.findByStudentIdAndSemesterAndAcademicYearAndIsDeletedFalse(
                any(), any(), any())).thenReturn(enrollments);
        when(enrollmentRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(enrollments);
        when(academicRecordRepository.findByStudentIdAndSemesterAndAcademicYearAndIsDeletedFalse(
                any(), any(), any())).thenReturn(Optional.empty());
        when(academicRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AcademicRecord result = gpaCalculationService.computeAndUpdateAcademicRecord(
                studentId, "SEMESTER1", "2025");

        assertEquals("FIRST_CLASS", result.getAcademicStanding());
    }

    @Test
    void computeAcademicStanding_probation() {
        List<StudentCourseEnrollment> enrollments = List.of(
                buildEnrollment(courseId1, 3, 0.5, EnrollmentStatus.COMPLETED)
        );
        when(enrollmentRepository.findByStudentIdAndSemesterAndAcademicYearAndIsDeletedFalse(
                any(), any(), any())).thenReturn(enrollments);
        when(enrollmentRepository.findByStudentIdAndIsDeletedFalse(any())).thenReturn(enrollments);
        when(academicRecordRepository.findByStudentIdAndSemesterAndAcademicYearAndIsDeletedFalse(
                any(), any(), any())).thenReturn(Optional.empty());
        when(academicRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AcademicRecord result = gpaCalculationService.computeAndUpdateAcademicRecord(
                studentId, "SEMESTER1", "2025");

        assertEquals("PROBATION", result.getAcademicStanding());
    }

    @Test
    void computeWeightedGpa_zeroCredits_returnsNull() {
        List<StudentCourseEnrollment> enrollments = List.of(
                buildEnrollment(courseId1, 0, 3.5, EnrollmentStatus.COMPLETED)
        );
        when(enrollmentRepository.findByStudentIdAndSemesterAndAcademicYearAndIsDeletedFalse(
                any(), any(), any())).thenReturn(enrollments);

        Double result = gpaCalculationService.computeSemesterGpa(studentId, "SEMESTER1", "2025");

        assertNull(result);
    }

    private StudentCourseEnrollment buildEnrollment(UUID courseId, int creditHours, Double gradePoints, EnrollmentStatus status) {
        return StudentCourseEnrollment.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .courseId(courseId)
                .creditHours(creditHours)
                .gradePoints(gradePoints)
                .grade(gradePoints != null ? "A" : null)
                .status(status)
                .semester("SEMESTER1")
                .academicYear("2025")
                .build();
    }
}
