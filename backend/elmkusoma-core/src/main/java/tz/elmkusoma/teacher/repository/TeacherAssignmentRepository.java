package tz.elmkusoma.teacher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.teacher.domain.TeacherAssignment;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, UUID> {

    @Query("SELECT ta FROM TeacherAssignment ta WHERE ta.teacherId = :teacherId AND ta.isDeleted = false")
    List<TeacherAssignment> findAllByTeacherId(@Param("teacherId") UUID teacherId);

    @Query("SELECT ta FROM TeacherAssignment ta WHERE ta.institutionId = :institutionId AND ta.isDeleted = false")
    List<TeacherAssignment> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT ta FROM TeacherAssignment ta WHERE ta.classGroupId = :classGroupId AND ta.isDeleted = false")
    List<TeacherAssignment> findAllByClassGroupId(@Param("classGroupId") UUID classGroupId);

    @Query("SELECT DISTINCT ta.classGroupId FROM TeacherAssignment ta WHERE ta.teacherId = :teacherId AND ta.isDeleted = false")
    List<UUID> findClassGroupIdsByTeacherId(@Param("teacherId") UUID teacherId);

    @Query("SELECT ta.classGroupId FROM TeacherAssignment ta WHERE ta.teacherId = :teacherId AND ta.subjectId = :subjectId AND ta.isDeleted = false")
    List<UUID> findClassGroupIdsByTeacherIdAndSubjectId(@Param("teacherId") UUID teacherId, @Param("subjectId") UUID subjectId);
}
