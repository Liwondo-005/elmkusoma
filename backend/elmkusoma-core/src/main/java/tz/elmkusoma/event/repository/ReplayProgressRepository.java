package tz.elmkusoma.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.event.domain.ReplayProgress;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReplayProgressRepository extends JpaRepository<ReplayProgress, UUID> {

    Optional<ReplayProgress> findByReplayIdAndUserId(UUID replayId, UUID userId);

    List<ReplayProgress> findByUserIdAndReplayIdIn(UUID userId, Collection<UUID> replayIds);
}
