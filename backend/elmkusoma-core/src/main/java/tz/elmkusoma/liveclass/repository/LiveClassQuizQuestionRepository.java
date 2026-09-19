package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.LiveClassQuizQuestion;

import java.util.List;
import java.util.UUID;

@Repository
public interface LiveClassQuizQuestionRepository extends JpaRepository<LiveClassQuizQuestion, UUID> {
    List<LiveClassQuizQuestion> findByQuizIdAndIsDeletedFalseOrderByDisplayOrderAsc(UUID quizId);
}
