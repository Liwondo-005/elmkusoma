package tz.elmkusoma.student.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.student.domain.StudentClassAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentClassAssignmentRepository extends JpaRepository<StudentClassAssignment, UUID> {

    List<StudentClassAssignment> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<StudentClassAssignment> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);

    List<StudentClassAssignment> findByStudentIdAndTermIdAndIsActiveTrueAndIsDeletedFalse(UUID studentId, UUID termId);

    Optional<StudentClassAssignment> findByStudentIdAndClassGroupIdAndIsActiveTrueAndIsDeletedFalse(
            UUID studentId, UUID classGroupId);

    @Query("SELECT sca FROM StudentClassAssignment sca WHERE sca.classGroupId = :classGroupId " +
           "AND sca.academicYearId = :academicYearId AND sca.isActive = true AND sca.isDeleted = false")
    List<StudentClassAssignment> findActiveByClassAndYear(
            @Param("classGroupId") UUID classGroupId, @Param("academicYearId") UUID academicYearId);

    @Query("SELECT COUNT(sca) FROM StudentClassAssignment sca WHERE sca.classGroupId = :classGroupId " +
           "AND sca.isActive = true AND sca.isDeleted = false")
    long countActiveByClassGroupId(@Param("classGroupId") UUID classGroupId);
}
