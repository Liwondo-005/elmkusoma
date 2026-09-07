package tz.elmkusoma.enrollment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.enrollment.domain.Enrollment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    Page<Enrollment> findByInstitutionIdAndIsDeletedFalse(UUID institutionId, Pageable pageable);

    List<Enrollment> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<Enrollment> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);

    Optional<Enrollment> findByStudentIdAndClassGroupIdAndAcademicYearIdAndIsDeletedFalse(
            UUID studentId, UUID classGroupId, UUID academicYearId);

    long countByClassGroupIdAndAcademicYearIdAndStatusAndIsDeletedFalse(
            UUID classGroupId, UUID academicYearId, Enrollment.EnrollmentStatus status);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.classGroupId = :classGroupId " +
           "AND e.academicYearId = :academicYearId " +
           "AND e.status = 'ENROLLED' AND e.isDeleted = false")
    long countActiveEnrollments(@Param("classGroupId") UUID classGroupId,
                                @Param("academicYearId") UUID academicYearId);

    boolean existsByStudentIdAndClassGroupIdAndAcademicYearIdAndStatusAndIsDeletedFalse(
            UUID studentId, UUID classGroupId, UUID academicYearId,
            Enrollment.EnrollmentStatus status);
}
