package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.teacher.domain.TeacherAssignment;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrimaryPortalRepository extends JpaRepository<TeacherAssignment, UUID> {

    @Query("SELECT ta FROM TeacherAssignment ta " +
           "WHERE ta.institutionId = :institutionId AND ta.isDeleted = false " +
           "AND ta.classGroupId IN (SELECT sca.classGroupId FROM tz.elmkusoma.student.domain.StudentClassAssignment sca " +
           "WHERE sca.studentId = :studentId AND sca.isDeleted = false)")
    List<TeacherAssignment> findTeacherAssignmentsForStudent(
            @Param("studentId") UUID studentId, @Param("institutionId") UUID institutionId);

    @Query("SELECT DISTINCT sca2.studentId FROM tz.elmkusoma.student.domain.StudentClassAssignment sca2 " +
           "WHERE sca2.isDeleted = false AND sca2.isActive = true " +
           "AND sca2.studentId <> :studentId " +
           "AND sca2.classGroupId IN (SELECT sca.classGroupId FROM tz.elmkusoma.student.domain.StudentClassAssignment sca " +
           "WHERE sca.studentId = :studentId AND sca.isDeleted = false)")
    List<UUID> findClassmateStudentIds(@Param("studentId") UUID studentId);
}
