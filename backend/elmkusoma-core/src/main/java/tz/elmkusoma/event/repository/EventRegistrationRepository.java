package tz.elmkusoma.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.event.domain.EventRegistration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {

    Optional<EventRegistration> findByEventIdAndUserIdAndIsDeletedFalse(UUID eventId, UUID userId);

    boolean existsByEventIdAndUserIdAndIsDeletedFalse(UUID eventId, UUID userId);

    List<EventRegistration> findByUserIdAndIsDeletedFalseOrderByRegisteredAtDesc(UUID userId);

    List<EventRegistration> findByEventIdAndIsDeletedFalse(UUID eventId);

    List<EventRegistration> findByEventIdAndStatusAndIsDeletedFalse(UUID eventId, String status);

    long countByEventIdAndIsDeletedFalse(UUID eventId);

    long countByEventIdAndStatusAndIsDeletedFalse(UUID eventId, String status);

    @Query("SELECT er FROM EventRegistration er WHERE er.userId = :userId AND er.isDeleted = false " +
           "AND er.status IN ('REGISTERED', 'ATTENDED') " +
           "ORDER BY er.registeredAt DESC")
    List<EventRegistration> findActiveRegistrationsByUser(@Param("userId") UUID userId);

    @Query("SELECT er FROM EventRegistration er JOIN Event e ON er.eventId = e.id " +
           "WHERE er.userId = :userId AND er.isDeleted = false " +
           "AND er.status = 'REGISTERED' AND e.startsAt > CURRENT_TIMESTAMP")
    List<EventRegistration> findUpcomingByUser(@Param("userId") UUID userId);

    @Query("SELECT er FROM EventRegistration er WHERE er.userId = :userId AND er.isDeleted = false " +
           "AND er.status = 'CANCELLED' AND er.eventId = :eventId")
    Optional<EventRegistration> findCancelledByEventAndUser(@Param("eventId") UUID eventId, @Param("userId") UUID userId);
}
