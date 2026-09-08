package tz.elmkusoma.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.AssignmentSubmission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    Optional<AssignmentSubmission> findByAssignmentIdAndStudentIdAndIsDeletedFalse(
            UUID assignmentId, UUID studentId);

    List<AssignmentSubmission> findByAssignmentIdAndIsDeletedFalse(UUID assignmentId);

    List<AssignmentSubmission> findByStudentIdAndIsDeletedFalse(UUID studentId);
}
