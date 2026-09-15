package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassChatMessage;

import java.util.List;
import java.util.UUID;

@Repository
public interface LiveClassChatMessageRepository extends JpaRepository<LiveClassChatMessage, UUID> {

    List<LiveClassChatMessage> findByLiveClassIdAndIsDeletedFalseOrderBySentAtAsc(UUID liveClassId);

    List<LiveClassChatMessage> findByLiveClassIdAndIsDeletedFalseOrderBySentAtDesc(UUID liveClassId);

    long countByLiveClassIdAndIsDeletedFalse(UUID liveClassId);

    @Query("SELECT COUNT(m) FROM LiveClassChatMessage m WHERE m.liveClassId = :classId AND m.isDeleted = false")
    long countMessages(@Param("classId") UUID classId);
}
