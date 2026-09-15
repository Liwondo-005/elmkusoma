package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveClassParticipantRepository extends JpaRepository<LiveClassParticipant, UUID> {

    Optional<LiveClassParticipant> findByLiveClassIdAndUserIdAndIsDeletedFalse(UUID liveClassId, UUID userId);

    List<LiveClassParticipant> findByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(UUID liveClassId);

    List<LiveClassParticipant> findByLiveClassIdAndIsDeletedFalse(UUID liveClassId);

    long countByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(UUID liveClassId);

    long countByLiveClassIdAndIsDeletedFalse(UUID liveClassId);

    @Query("SELECT lcp FROM LiveClassParticipant lcp WHERE lcp.liveClassId = :liveClassId AND lcp.isDeleted = false AND lcp.leftAt IS NULL AND lcp.connectionId = :connectionId")
    Optional<LiveClassParticipant> findByLiveClassIdAndConnectionIdAndIsDeletedFalse(@Param("liveClassId") UUID liveClassId, @Param("connectionId") String connectionId);
}
