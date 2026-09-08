package tz.elmkusoma.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.assessment.domain.Option;

import java.util.List;
import java.util.UUID;

@Repository
public interface OptionRepository extends JpaRepository<Option, UUID> {

    List<Option> findByQuestionIdAndIsDeletedFalseOrderBySortOrder(UUID questionId);
}
