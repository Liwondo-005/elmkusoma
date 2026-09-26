package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassBreakoutAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveClassBreakoutAssignmentRepository extends JpaRepository<LiveClassBreakoutAssignment, UUID> {
    List<LiveClassBreakoutAssignment> findByBreakoutRoomIdAndIsDeletedFalse(UUID breakoutRoomId);
    Optional<LiveClassBreakoutAssignment> findByUserIdAndIsDeletedFalse(UUID userId);

    /** All live assignments of every breakout room of one live class (assignment rows store the room id, not an association, so the join is explicit). */
    @Query("SELECT a FROM LiveClassBreakoutAssignment a JOIN LiveClassBreakoutRoom r ON r.id = a.breakoutRoomId WHERE r.liveClassId = :liveClassId AND a.isDeleted = false AND r.isDeleted = false")
    List<LiveClassBreakoutAssignment> findByLiveClassIdAndIsDeletedFalse(@Param("liveClassId") UUID liveClassId);
}
