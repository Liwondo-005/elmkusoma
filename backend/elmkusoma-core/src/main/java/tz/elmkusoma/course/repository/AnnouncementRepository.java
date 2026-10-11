package tz.elmkusoma.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.course.domain.Announcement;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    @Query("SELECT a FROM Announcement a WHERE a.authorId = :authorId AND a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Announcement> findByAuthorIdAndIsDeletedFalse(@Param("authorId") UUID authorId);

    @Query("SELECT a FROM Announcement a WHERE a.institutionId = :institutionId AND a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Announcement> findByInstitutionIdAndIsDeletedFalse(@Param("institutionId") UUID institutionId);

    /**
     * Institution announcements a learner is allowed to see: published, or due to publish.
     *
     * <p>Added because the learner feed loaded institution announcements with no status filter
     * at all, while the jurisdictional loop next to it did check. A SCHEDULED announcement
     * therefore reached learners the moment it was created, well before {@code scheduledAt}.
     * Treating a due SCHEDULED row as visible keeps the scheduler's publish step a tidy-up
     * rather than the only thing that can make an announcement appear.</p>
     */
    @Query("""
            SELECT a FROM Announcement a
            WHERE a.institutionId = :institutionId
              AND a.isDeleted = false
              AND (a.status IS NULL OR a.status = 'PUBLISHED'
                   OR (a.status = 'SCHEDULED' AND a.scheduledAt IS NOT NULL AND a.scheduledAt <= :now))
            ORDER BY a.createdAt DESC
            """)
    List<Announcement> findVisibleToLearnersByInstitutionId(
            @Param("institutionId") UUID institutionId,
            @Param("now") java.time.LocalDateTime now);

    @Query("SELECT a FROM Announcement a WHERE a.institutionId = :institutionId AND a.classGroupId = :classGroupId AND a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Announcement> findByInstitutionIdAndClassGroupIdAndIsDeletedFalse(
            @Param("institutionId") UUID institutionId, @Param("classGroupId") UUID classGroupId);

    @Query("SELECT a FROM Announcement a WHERE a.institutionId = :institutionId AND a.classGroupId IS NULL AND a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Announcement> findInstitutionWideByInstitutionIdAndIsDeletedFalse(@Param("institutionId") UUID institutionId);

    @Query("SELECT a FROM Announcement a WHERE a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Announcement> findAllAndIsDeletedFalse();

    @Query("SELECT a FROM Announcement a WHERE a.isDeleted = false AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.content) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY a.createdAt DESC")
    List<Announcement> searchByTitleOrContentAndIsDeletedFalse(@Param("query") String query);

    @Query("SELECT a FROM Announcement a WHERE a.isDeleted = false AND a.institutionId = :institutionId AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.content) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY a.createdAt DESC")
    List<Announcement> searchByInstitutionIdAndQuery(@Param("institutionId") UUID institutionId, @Param("query") String query);

    // ── Jurisdictional audiences (Nationaladmin.md §23) ─────────────────────

    @Query("SELECT a FROM Announcement a WHERE a.isDeleted = false AND a.audienceType IS NOT NULL ORDER BY a.createdAt DESC")
    List<Announcement> findJurisdictionalAndIsDeletedFalse();

    @Query("SELECT a FROM Announcement a WHERE a.isDeleted = false AND a.status = 'SCHEDULED' AND a.scheduledAt IS NOT NULL AND a.scheduledAt <= :now")
    List<Announcement> findDueScheduled(@Param("now") java.time.LocalDateTime now);
}
