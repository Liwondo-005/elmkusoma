package tz.elmkusoma.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.course.domain.LiveClass;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LiveClassRepository extends JpaRepository<LiveClass, UUID> {

    List<LiveClass> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<LiveClass> findByInstitutionIdAndStatusAndIsDeletedFalse(UUID institutionId, String status);

    long countByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    long countByInstitutionIdAndStatusAndIsDeletedFalse(UUID institutionId, String status);

    @Query("SELECT lc FROM LiveClass lc WHERE lc.institutionId = :institutionId AND lc.isDeleted = false AND lc.scheduledAt >= :from AND lc.scheduledAt <= :to ORDER BY lc.scheduledAt")
    List<LiveClass> findByInstitutionIdAndScheduledAtBetween(@Param("institutionId") UUID institutionId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT lc FROM LiveClass lc WHERE lc.teacherId = :teacherId AND lc.isDeleted = false ORDER BY lc.scheduledAt DESC")
    List<LiveClass> findByTeacherIdAndIsDeletedFalse(@Param("teacherId") UUID teacherId);

    @Query("SELECT lc FROM LiveClass lc WHERE lc.teacherId = :teacherId AND lc.status = :status AND lc.isDeleted = false ORDER BY lc.scheduledAt DESC")
    List<LiveClass> findByTeacherIdAndStatusAndIsDeletedFalse(@Param("teacherId") UUID teacherId, @Param("status") String status);
}
