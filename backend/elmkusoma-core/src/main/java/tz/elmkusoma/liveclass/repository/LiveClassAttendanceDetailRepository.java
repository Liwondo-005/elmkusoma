package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassAttendanceDetail;

import java.util.List;
import java.util.UUID;

@Repository
public interface LiveClassAttendanceDetailRepository extends JpaRepository<LiveClassAttendanceDetail, UUID> {
    List<LiveClassAttendanceDetail> findByLiveClassIdAndIsDeletedFalse(UUID liveClassId);
}
