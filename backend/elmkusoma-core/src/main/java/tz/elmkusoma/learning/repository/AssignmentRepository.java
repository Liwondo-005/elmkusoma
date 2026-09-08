package tz.elmkusoma.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.Assignment;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findBySubjectIdAndClassGroupIdAndIsDeletedFalse(UUID subjectId, UUID classGroupId);

    List<Assignment> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);

    List<Assignment> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);
}
