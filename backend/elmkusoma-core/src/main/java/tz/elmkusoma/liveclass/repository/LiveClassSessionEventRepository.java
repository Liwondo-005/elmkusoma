package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassSessionEvent;

import java.util.List;
import java.util.UUID;

@Repository
public interface LiveClassSessionEventRepository extends JpaRepository<LiveClassSessionEvent, UUID> {

    List<LiveClassSessionEvent> findByLiveClassIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID liveClassId);

    List<LiveClassSessionEvent> findByLiveClassIdAndEventTypeAndIsDeletedFalse(UUID liveClassId, String eventType);

    long countByLiveClassIdAndIsDeletedFalse(UUID liveClassId);

    @Query("SELECT e.eventType, COUNT(e) FROM LiveClassSessionEvent e WHERE e.liveClassId = :classId AND e.isDeleted = false GROUP BY e.eventType")
    List<Object[]> countByEventType(@Param("classId") UUID classId);
}
