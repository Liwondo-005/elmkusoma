package tz.elmkusoma.course.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.course.domain.LiveClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LiveClassRepository extends JpaRepository<LiveClass, UUID> {

    List<LiveClass> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<LiveClass> findByInstitutionIdAndStatusAndIsDeletedFalse(UUID institutionId, String status);

    long countByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    long countByInstitutionIdAndStatusAndIsDeletedFalse(UUID institutionId, String status);

    @Query("SELECT COUNT(lc) FROM LiveClass lc WHERE lc.institutionId IN :institutionIds AND lc.status = :status AND lc.isDeleted = false")
    long countByInstitutionIdsAndStatusAndIsDeletedFalse(@Param("institutionIds") List<UUID> institutionIds, @Param("status") String status);

    @Query("SELECT COUNT(lc) FROM LiveClass lc WHERE lc.institutionId IN :institutionIds AND lc.status = :status AND DATE(lc.scheduledAt) = :today AND lc.isDeleted = false")
    long countByInstitutionIdsAndStatusAndScheduledToday(@Param("institutionIds") List<UUID> institutionIds, @Param("status") String status, @Param("today") LocalDate today);

    @Query("SELECT COUNT(lc) FROM LiveClass lc WHERE lc.institutionId IN :institutionIds AND lc.status = :status AND DATE(lc.scheduledAt) = :today AND lc.isDeleted = false")
    long countByInstitutionIdsAndStatusAndCompletedToday(@Param("institutionIds") List<UUID> institutionIds, @Param("status") String status, @Param("today") LocalDate today);

    @Query("SELECT COUNT(lc) FROM LiveClass lc WHERE lc.institutionId IN :institutionIds AND lc.scheduledAt >= :weekStart AND lc.scheduledAt <= :weekEnd AND lc.isDeleted = false")
    long countByInstitutionIdsAndScheduledThisWeek(@Param("institutionIds") List<UUID> institutionIds, @Param("weekStart") LocalDateTime weekStart, @Param("weekEnd") LocalDateTime weekEnd);

    @Query("SELECT lc FROM LiveClass lc WHERE lc.institutionId IN :institutionIds AND lc.isDeleted = false ORDER BY lc.scheduledAt")
    List<LiveClass> findByInstitutionIdsAndIsDeletedFalseOrderByScheduledAt(@Param("institutionIds") List<UUID> institutionIds);

    @Query("SELECT lc FROM LiveClass lc WHERE lc.institutionId = :institutionId AND lc.isDeleted = false AND lc.scheduledAt >= :from AND lc.scheduledAt <= :to ORDER BY lc.scheduledAt")
    List<LiveClass> findByInstitutionIdAndScheduledAtBetween(@Param("institutionId") UUID institutionId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT lc FROM LiveClass lc WHERE lc.teacherId = :teacherId AND lc.isDeleted = false ORDER BY lc.scheduledAt DESC")
    List<LiveClass> findByTeacherIdAndIsDeletedFalse(@Param("teacherId") UUID teacherId);

    @Query("SELECT lc FROM LiveClass lc WHERE lc.teacherId = :teacherId AND lc.status = :status AND lc.isDeleted = false ORDER BY lc.scheduledAt DESC")
    List<LiveClass> findByTeacherIdAndStatusAndIsDeletedFalse(@Param("teacherId") UUID teacherId, @Param("status") String status);

    @Query("SELECT lc FROM LiveClass lc WHERE lc.isDeleted = false AND lc.status IN ('SCHEDULED', 'IN_PROGRESS') ORDER BY lc.scheduledAt DESC")
    List<LiveClass> findAllActiveAndIsDeletedFalse();

    @Query("SELECT lc FROM LiveClass lc WHERE lc.isDeleted = false AND LOWER(lc.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY lc.scheduledAt DESC")
    List<LiveClass> searchByTitleAndIsDeletedFalse(@Param("query") String query);

    @Query("SELECT lc FROM LiveClass lc WHERE lc.isDeleted = false AND lc.id <> :excludeId AND ((lc.subjectId IS NOT NULL AND lc.subjectId = :subjectId) OR LOWER(lc.title) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY lc.scheduledAt DESC")
    List<LiveClass> findRelatedLiveClasses(@Param("excludeId") UUID excludeId, @Param("subjectId") UUID subjectId, @Param("query") String query);

    @Query("SELECT lc FROM LiveClass lc WHERE lc.teacherId = :teacherId AND lc.isDeleted = false AND lc.status NOT IN ('CANCELLED', 'COMPLETED', 'ENDED') AND lc.scheduledAt < :endTime AND lc.scheduledAt > :fromTime ORDER BY lc.scheduledAt")
    List<LiveClass> findOverlappingForTeacher(@Param("teacherId") UUID teacherId, @Param("fromTime") LocalDateTime fromTime, @Param("endTime") LocalDateTime endTime);

    long countByIsDeletedFalse();

    long countByStatusAndIsDeletedFalse(String status);

    Page<LiveClass> findByStatusAndIsDeletedFalse(String status, Pageable pageable);

    Page<LiveClass> findAllByIsDeletedFalse(Pageable pageable);
}