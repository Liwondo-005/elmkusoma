package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.CurriculumTopic;

import java.util.List;
import java.util.UUID;

@Repository
public interface CurriculumTopicRepository extends JpaRepository<CurriculumTopic, UUID> {

    List<CurriculumTopic> findBySubjectIdAndIsDeletedFalseOrderBySortOrderAsc(UUID subjectId);
}
