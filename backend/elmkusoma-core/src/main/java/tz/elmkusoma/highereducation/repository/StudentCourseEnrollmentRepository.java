package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.elmkusoma.highereducation.domain.EnrollmentStatus;
import tz.elmkusoma.highereducation.domain.StudentCourseEnrollment;

import java.util.List;
import java.util.UUID;

public interface StudentCourseEnrollmentRepository extends JpaRepository<StudentCourseEnrollment, UUID> {
    List<StudentCourseEnrollment> findByStudentIdAndIsDeletedFalse(UUID studentId);
    List<StudentCourseEnrollment> findByStudentIdAndStatusAndIsDeletedFalse(UUID studentId, EnrollmentStatus status);
    List<StudentCourseEnrollment> findByStudentIdAndSemesterAndAcademicYearAndIsDeletedFalse(UUID studentId, String semester, String academicYear);
    long countByStudentIdAndStatusAndIsDeletedFalse(UUID studentId, EnrollmentStatus status);
    List<StudentCourseEnrollment> findByStudentIdAndProgrammeIdAndIsDeletedFalse(UUID studentId, UUID programmeId);
}
