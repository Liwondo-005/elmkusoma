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
}
