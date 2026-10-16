package tz.elmkusoma.event.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.event.domain.Event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    /**
     * Row-locked read used by registration.
     *
     * <p>Seat capacity is a count-then-insert: the available seats are read and the
     * registration is written a moment later. Without a lock, two people registering for the
     * last seat both read "1 of 1 free" and both are inserted, so the event is oversold - which
     * is a lost sale at best and a broken promise to every attendee at worst. Taking the row
     * lock first makes the second registration observe the first one's write and refuse,
     * which is what "prevent event overselling" actually requires.</p>
     *
     * <p>Same convention as {@code LiveClassRepository.findByIdForUpdate} and
     * {@code PaymentRepository.findByIdForUpdate}; the event row is the only lock taken, so
     * registration and cancellation cannot deadlock against each other.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") UUID id);

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
           "AND e.status = 'PUBLISHED' AND e.startsAt > :now")
    Page<Event> findUpcomingPublished(@Param("instId") UUID institutionId, @Param("now") LocalDateTime now,
                                      Pageable pageable);

    /** Paged, filterable list used by controllers that pass optional page/size (§81/§82). */
    @Query("SELECT e FROM Event e WHERE e.isDeleted = false " +
           "AND (:institutionId IS NULL OR e.institutionId = :institutionId) " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:eventType IS NULL OR e.eventType = :eventType) " +
           "AND (:category IS NULL OR e.category = :category) " +
           "AND (:providerId IS NULL OR e.providerId = :providerId)")
    Page<Event> findFiltered(@Param("institutionId") UUID institutionId,
                             @Param("status") String status,
                             @Param("eventType") String eventType,
                             @Param("category") String category,
                             @Param("providerId") String providerId,
                             Pageable pageable);

    /** §97 — provider-scoped lookup (findByProviderId usage in getEvents filter). */
    List<Event> findByInstitutionIdAndProviderIdAndIsDeletedFalse(UUID institutionId, String providerId);

    /** Events about to start — used by the reminder scheduler (§18/19). */
    @Query("SELECT e FROM Event e WHERE e.isDeleted = false " +
           "AND e.startsAt > :from AND e.startsAt <= :to " +
           "AND e.status IN ('PUBLISHED', 'REGISTRATION_OPEN', 'REGISTRATION_CLOSED', 'PREPARING', 'STARTING')")
    List<Event> findEventsStartingWithin(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

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

    Page<Event> findByIsDeletedFalse(Pageable pageable);
    long countByIsDeletedFalse();
}
