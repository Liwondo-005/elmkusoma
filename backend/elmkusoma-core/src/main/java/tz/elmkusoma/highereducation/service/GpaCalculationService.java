package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.highereducation.domain.*;
import tz.elmkusoma.highereducation.repository.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GpaCalculationService {

    private final StudentCourseEnrollmentRepository enrollmentRepository;
    private final AcademicRecordRepository academicRecordRepository;

    public Double computeSemesterGpa(UUID studentId, String semester, String academicYear) {
        List<StudentCourseEnrollment> enrollments = enrollmentRepository
                .findByStudentIdAndSemesterAndAcademicYearAndIsDeletedFalse(studentId, semester, academicYear);

        return computeWeightedGpa(enrollments);
    }

    public Double computeCumulativeGpa(UUID studentId) {
        List<StudentCourseEnrollment> allCompleted = enrollmentRepository
                .findByStudentIdAndIsDeletedFalse(studentId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .filter(e -> e.getGradePoints() != null && e.getCreditHours() != null)
                .collect(Collectors.toList());

        return computeWeightedGpa(allCompleted);
    }

    @Transactional
    public AcademicRecord computeAndUpdateAcademicRecord(UUID studentId, String semester, String academicYear) {
        List<StudentCourseEnrollment> semesterEnrollments = enrollmentRepository
                .findByStudentIdAndSemesterAndAcademicYearAndIsDeletedFalse(studentId, semester, academicYear);

        List<StudentCourseEnrollment> allCompleted = enrollmentRepository
                .findByStudentIdAndIsDeletedFalse(studentId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .collect(Collectors.toList());

        Double semesterGpa = computeWeightedGpa(semesterEnrollments);
        Double cumulativeGpa = computeWeightedGpa(allCompleted);

        int totalCredits = semesterEnrollments.stream()
                .filter(e -> e.getCreditHours() != null)
                .mapToInt(StudentCourseEnrollment::getCreditHours).sum();
        int earnedCredits = allCompleted.stream()
                .filter(e -> e.getCreditHours() != null)
                .mapToInt(StudentCourseEnrollment::getCreditHours).sum();
        int totalCourses = semesterEnrollments.size();
        int completedCourses = (int) semesterEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED).count();
        int failedCourses = (int) semesterEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED && e.getGradePoints() != null && e.getGradePoints() < 1.0).count();

        String academicStanding = computeAcademicStanding(cumulativeGpa);

        AcademicRecord record = academicRecordRepository
                .findByStudentIdAndSemesterAndAcademicYearAndIsDeletedFalse(studentId, semester, academicYear)
                .orElse(AcademicRecord.builder()
                        .studentId(studentId)
                        .semester(semester)
                        .academicYear(academicYear)
                        .build());

        record.setSemesterGpa(semesterGpa);
        record.setCumulativeGpa(cumulativeGpa);
        record.setTotalCreditHours(totalCredits);
        record.setEarnedCreditHours(earnedCredits);
        record.setTotalCourses(totalCourses);
        record.setCompletedCourses(completedCourses);
        record.setFailedCourses(failedCourses);
        record.setAcademicStanding(academicStanding);

        return academicRecordRepository.save(record);
    }

    private Double computeWeightedGpa(List<StudentCourseEnrollment> enrollments) {
        List<StudentCourseEnrollment> graded = enrollments.stream()
                .filter(e -> e.getGradePoints() != null && e.getCreditHours() != null && e.getCreditHours() > 0)
                .collect(Collectors.toList());

        if (graded.isEmpty()) return null;

        double totalWeightedPoints = 0;
        int totalCredits = 0;
        for (StudentCourseEnrollment e : graded) {
            totalWeightedPoints += e.getGradePoints() * e.getCreditHours();
            totalCredits += e.getCreditHours();
        }

        if (totalCredits == 0) return null;
        return Math.round((totalWeightedPoints / totalCredits) * 100.0) / 100.0;
    }

    private String computeAcademicStanding(Double gpa) {
        if (gpa == null) return null;
        if (gpa >= 3.7) return "FIRST_CLASS";
        if (gpa >= 3.0) return "SECOND_CLASS_UPPER";
        if (gpa >= 2.0) return "SECOND_CLASS_LOWER";
        if (gpa >= 1.0) return "PASS";
        return "PROBATION";
    }
}
