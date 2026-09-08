package tz.elmkusoma.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.assessment.domain.Assessment;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    List<Assessment> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);

    List<Assessment> findBySubjectIdAndIsDeletedFalse(UUID subjectId);

    List<Assessment> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);
}
