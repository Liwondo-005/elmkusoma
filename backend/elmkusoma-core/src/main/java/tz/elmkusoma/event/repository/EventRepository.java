package tz.elmkusoma.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.event.domain.Event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByInstitutionIdAndIsDeletedFalseOrderByStartsAtAsc(UUID institutionId);

    List<Event> findByInstitutionIdAndStatusAndIsDeletedFalseOrderByStartsAtAsc(UUID institutionId, String status);

    List<Event> findByInstitutionIdAndEventTypeAndIsDeletedFalseOrderByStartsAtAsc(UUID institutionId, String eventType);

    List<Event> findByInstitutionIdAndIsDeletedFalseAndStartsAtAfterOrderByStartsAtAsc(UUID institutionId, LocalDateTime now);

    List<Event> findByInstitutionIdAndIsDeletedFalseAndStartsAtBeforeOrderByStartsAtDesc(UUID institutionId, LocalDateTime now);

    List<Event> findByInstitutionIdAndStatusAndIsDeletedFalseAndStartsAtAfterOrderByStartsAtAsc(UUID institutionId, String status, LocalDateTime now);

    @Query("SELECT e FROM Event e WHERE e.institutionId = :instId AND e.isDeleted = false " +
           "AND e.status = 'PUBLISHED' AND e.startsAt > :now ORDER BY e.startsAt ASC")
    List<Event> findUpcomingPublished(@Param("instId") UUID institutionId, @Param("now") LocalDateTime now);

    @Query("SELECT e FROM Event e WHERE e.institutionId = :instId AND e.isDeleted = false " +
           "AND e.status = 'PUBLISHED' AND e.startsAt <= :now " +
           "AND (e.endsAt IS NULL OR e.endsAt >= :windowStart) ORDER BY e.startsAt DESC")
    List<Event> findPastOrOngoing(@Param("instId") UUID institutionId, @Param("now") LocalDateTime now, @Param("windowStart") LocalDateTime windowStart);

    @Query("SELECT e FROM Event e WHERE e.institutionId = :instId AND e.isDeleted = false " +
           "AND e.status = 'PUBLISHED' AND " +
           "(LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(e.category) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(e.tags) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Event> searchByQuery(@Param("instId") UUID institutionId, @Param("query") String query);

    @Query("SELECT e FROM Event e WHERE e.institutionId = :instId AND e.isDeleted = false " +
           "AND e.status = 'PUBLISHED' AND e.category = :category ORDER BY e.startsAt ASC")
    List<Event> findByCategory(@Param("instId") UUID institutionId, @Param("category") String category);

    long countByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    long countByInstitutionIdAndStatusAndIsDeletedFalse(UUID institutionId, String status);
}
