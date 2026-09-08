package tz.elmkusoma.teacher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.teacher.domain.TeacherQualification;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherQualificationRepository extends JpaRepository<TeacherQualification, UUID> {

    @Query("SELECT tq FROM TeacherQualification tq WHERE tq.teacherId = :teacherId AND tq.isDeleted = false")
    List<TeacherQualification> findAllByTeacherId(@Param("teacherId") UUID teacherId);
}
