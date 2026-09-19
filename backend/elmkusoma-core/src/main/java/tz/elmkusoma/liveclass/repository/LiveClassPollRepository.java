package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassPoll;

import java.util.List;
import java.util.UUID;

@Repository
public interface LiveClassPollRepository extends JpaRepository<LiveClassPoll, UUID> {
    List<LiveClassPoll> findByLiveClassIdAndIsDeletedFalse(UUID liveClassId);
}
