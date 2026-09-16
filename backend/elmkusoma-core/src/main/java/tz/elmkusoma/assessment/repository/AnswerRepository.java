package tz.elmkusoma.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.assessment.domain.Answer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    List<Answer> findByAttemptIdAndIsDeletedFalse(UUID attemptId);

    Optional<Answer> findByAttemptIdAndQuestionIdAndIsDeletedFalse(UUID attemptId, UUID questionId);

    List<Answer> findByQuestionIdAndIsDeletedFalse(UUID questionId);
}
