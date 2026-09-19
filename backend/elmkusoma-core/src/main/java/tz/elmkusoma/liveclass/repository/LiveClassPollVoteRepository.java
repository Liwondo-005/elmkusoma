package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassPollVote;

import java.util.List;
import java.util.UUID;

@Repository
public interface LiveClassPollVoteRepository extends JpaRepository<LiveClassPollVote, UUID> {
    List<LiveClassPollVote> findByPollIdAndIsDeletedFalse(UUID pollId);
}
