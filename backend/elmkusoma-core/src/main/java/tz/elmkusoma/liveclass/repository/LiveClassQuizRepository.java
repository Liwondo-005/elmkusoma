package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassQuiz;

import java.util.List;
import java.util.UUID;

@Repository
public interface LiveClassQuizRepository extends JpaRepository<LiveClassQuiz, UUID> {
    List<LiveClassQuiz> findByLiveClassIdAndIsDeletedFalse(UUID liveClassId);
}
