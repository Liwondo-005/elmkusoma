package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassQuizResponse;

import java.util.List;
import java.util.UUID;

@Repository
public interface LiveClassQuizResponseRepository extends JpaRepository<LiveClassQuizResponse, UUID> {
    List<LiveClassQuizResponse> findByQuizIdAndIsDeletedFalse(UUID quizId);
    List<LiveClassQuizResponse> findByUserIdAndQuizIdAndIsDeletedFalse(UUID userId, UUID quizId);
}
