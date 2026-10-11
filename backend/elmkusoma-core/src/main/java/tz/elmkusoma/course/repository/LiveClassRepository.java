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

    /**
     * Live classes currently in an active (started, not finished) state. Used by the
     * authoritative expiry sweep, which ends a session once its configured duration
     * has elapsed so a session can never stay LIVE indefinitely when the teacher
     * forgets to end it (or the client never sends the end call).
     */
    @Query("SELECT lc FROM LiveClass lc WHERE lc.status IN :statuses AND lc.isDeleted = false")
    List<LiveClass> findByStatusInAndIsDeletedFalse(@Param("statuses") List<String> statuses);

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

    long countByStatusInAndIsDeletedFalse(java.util.Collection<String> statuses);

    Page<LiveClass> findByStatusAndIsDeletedFalse(String status, Pageable pageable);

    Page<LiveClass> findByStatusInAndIsDeletedFalse(java.util.Collection<String> statuses, Pageable pageable);

    Page<LiveClass> findAllByIsDeletedFalse(Pageable pageable);

    @Query("SELECT lc FROM LiveClass lc WHERE lc.isDeleted = false AND lc.institutionId = :institutionId AND LOWER(lc.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY lc.scheduledAt DESC")
    List<LiveClass> searchByInstitutionIdAndQuery(@Param("institutionId") UUID institutionId, @Param("query") String query);

/** Lesson ↔ Live Class: every non-deleted live class linked to a lesson. */
    @Query("SELECT lc FROM LiveClass lc WHERE lc.isDeleted = false AND lc.lessonId = :lessonId ORDER BY lc.scheduledAt DESC")
    List<LiveClass> findByLessonIdAndIsDeletedFalse(@Param("lessonId") UUID lessonId);

    // ── public discovery (V157) ─────────────────────────────────────────────────────────────
    //
    // Public eligibility is decided here rather than in the service, so there is no input a
    // caller could supply to widen it. Two conditions, both required: the class is PUBLISHED for
    // public discovery, and its access level is PUBLIC. Approval alone does not expose a class -
    // a teacher must also have declared it publicly joinable - which is what stops one
    // administrative click from putting every teacher's session on the marketing site.
    //
    // CANCELLED is excluded from Past deliberately. A cancelled session is not a completed
    // learning event, and listing it beside real past classes with a "watch replay" affordance
    // would misrepresent what happened.

    /** Live Now: genuinely in progress, by authoritative session status and elapsed duration. */
    @Query("""
            SELECT lc FROM LiveClass lc
            WHERE lc.isDeleted = false
              AND lc.publicationStatus = 'PUBLISHED'
              AND lc.accessLevel = 'PUBLIC'
              AND lc.status IN :activeStatuses
              AND COALESCE(lc.startedAt, lc.scheduledAt) <= :now
            ORDER BY COALESCE(lc.startedAt, lc.scheduledAt) DESC, lc.id ASC
            """)
    List<LiveClass> findPublicLiveNow(@Param("now") LocalDateTime now,
                                      @Param("activeStatuses") List<String> activeStatuses,
                                      Pageable pageable);

    /** Upcoming: scheduled to start in the future. */
    @Query("""
            SELECT lc FROM LiveClass lc
            WHERE lc.isDeleted = false
              AND lc.publicationStatus = 'PUBLISHED'
              AND lc.accessLevel = 'PUBLIC'
              AND lc.status = 'SCHEDULED'
              AND lc.scheduledAt > :now
            ORDER BY lc.scheduledAt ASC, lc.id ASC
            """)
    List<LiveClass> findPublicUpcoming(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Past: the session actually finished.
     *
     * <p>Ordered by {@code endedAt} rather than {@code scheduledAt}, so a long class that started
     * at 09:00 and ran until 11:00 sorts by when it ended. Classes completed before V157 have a
     * null {@code endedAt} and fall back to {@code scheduledAt}, which is the best available
     * estimate for them.</p>
     */
    @Query("""
            SELECT lc FROM LiveClass lc
            WHERE lc.isDeleted = false
              AND lc.publicationStatus = 'PUBLISHED'
              AND lc.accessLevel = 'PUBLIC'
              AND lc.status IN :endedStatuses
            ORDER BY COALESCE(lc.endedAt, lc.scheduledAt) DESC, lc.id ASC
            """)
    List<LiveClass> findPublicPast(@Param("endedStatuses") List<String> endedStatuses,
                                  Pageable pageable);

    /** Platform Admin publication queue: every class with a publication state, newest first. */
    @Query("""
            SELECT lc FROM LiveClass lc
            WHERE lc.isDeleted = false
              AND (:publicationStatus IS NULL OR lc.publicationStatus = :publicationStatus)
              AND (:accessLevel IS NULL OR lc.accessLevel = :accessLevel)
            ORDER BY lc.updatedAt DESC, lc.id ASC
            """)
    Page<LiveClass> findForPublicationReview(@Param("publicationStatus") String publicationStatus,
                                            @Param("accessLevel") String accessLevel,
                                            Pageable pageable);

    /** True when any publicly discoverable class exists. Lets the landing section hide cleanly. */
    @Query("""
            SELECT COUNT(lc) > 0 FROM LiveClass lc
            WHERE lc.isDeleted = false
              AND lc.publicationStatus = 'PUBLISHED'
              AND lc.accessLevel = 'PUBLIC'
              AND lc.status IN ('SCHEDULED', 'STARTING', 'IN_PROGRESS', 'LIVE', 'ENDING', 'COMPLETED', 'ENDED')
            """)
    boolean hasAnyPubliclyDiscoverable();
}