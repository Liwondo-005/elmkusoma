package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassBreakoutAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveClassBreakoutAssignmentRepository extends JpaRepository<LiveClassBreakoutAssignment, UUID> {
    List<LiveClassBreakoutAssignment> findByBreakoutRoomIdAndIsDeletedFalse(UUID breakoutRoomId);
    Optional<LiveClassBreakoutAssignment> findByUserIdAndIsDeletedFalse(UUID userId);
}
