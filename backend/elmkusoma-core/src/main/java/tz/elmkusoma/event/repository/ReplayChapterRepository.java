package tz.elmkusoma.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.event.domain.ReplayChapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReplayChapterRepository extends JpaRepository<ReplayChapter, UUID> {

    /** Ordered by position, which is the order the player renders and navigates. */
    List<ReplayChapter> findByReplayIdAndIsDeletedFalseOrderByPositionSecondsAsc(UUID replayId);

    Optional<ReplayChapter> findByReplayIdAndPositionSecondsAndIsDeletedFalse(
            UUID replayId, Integer positionSeconds);

    long countByReplayIdAndIsDeletedFalse(UUID replayId);
}
