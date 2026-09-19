package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassHandRaiseQueue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveClassHandRaiseQueueRepository extends JpaRepository<LiveClassHandRaiseQueue, UUID> {
    List<LiveClassHandRaiseQueue> findByLiveClassIdAndIsActiveTrueAndIsDeletedFalseOrderByPositionAsc(UUID liveClassId);
    Optional<LiveClassHandRaiseQueue> findByLiveClassIdAndUserIdAndIsActiveTrueAndIsDeletedFalse(UUID liveClassId, UUID userId);

    @Query("SELECT COALESCE(MAX(h.position), 0) FROM LiveClassHandRaiseQueue h WHERE h.liveClassId = :liveClassId AND h.isDeleted = false")
    Integer findMaxPositionByLiveClassId(@Param("liveClassId") UUID liveClassId);
}
