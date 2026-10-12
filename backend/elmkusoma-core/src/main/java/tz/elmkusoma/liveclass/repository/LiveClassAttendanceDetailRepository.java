package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassAttendanceDetail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveClassAttendanceDetailRepository extends JpaRepository<LiveClassAttendanceDetail, UUID> {

    List<LiveClassAttendanceDetail> findByLiveClassIdAndIsDeletedFalse(UUID liveClassId);

    /**
     * One attendance row per participant per class. The table previously had no such
     * lookup, which is why repeated POSTs created duplicate rows for the same participant.
     */
    Optional<LiveClassAttendanceDetail> findByLiveClassIdAndUserIdAndIsDeletedFalse(UUID liveClassId, UUID userId);
}