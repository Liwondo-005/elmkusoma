package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassIssue;

import java.util.List;
import java.util.UUID;

@Repository
public interface LiveClassIssueRepository extends JpaRepository<LiveClassIssue, UUID> {

    List<LiveClassIssue> findByLiveClassIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID liveClassId);

    long countByStatusAndIsDeletedFalse(String status);
}
