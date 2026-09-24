package tz.elmkusoma.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.event.domain.Replay;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReplayRepository extends JpaRepository<Replay, UUID> {

    List<Replay> findByEventIdAndIsDeletedFalse(UUID eventId);

    List<Replay> findByStatusAndIsDeletedFalse(String status);

    org.springframework.data.domain.Page<Replay> findByStatusAndIsDeletedFalse(String status,
                                                                              org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Replay> findByStatusAndIsDeletedFalseAndInstitutionId(
            String status, UUID institutionId, org.springframework.data.domain.Pageable pageable);

    List<Replay> findByLiveSessionIdAndIsDeletedFalse(UUID liveSessionId);
}
