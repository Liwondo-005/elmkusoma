package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassBreakoutRoom;

import java.util.List;
import java.util.UUID;

@Repository
public interface LiveClassBreakoutRoomRepository extends JpaRepository<LiveClassBreakoutRoom, UUID> {
    List<LiveClassBreakoutRoom> findByLiveClassIdAndIsDeletedFalse(UUID liveClassId);
}
