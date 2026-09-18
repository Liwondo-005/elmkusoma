package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.highereducation.domain.ProjectSubmission;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectSubmissionRepository extends JpaRepository<ProjectSubmission, UUID> {

    List<ProjectSubmission> findByProjectIdAndIsDeletedFalse(UUID projectId);

    List<ProjectSubmission> findByMilestoneIdAndIsDeletedFalse(UUID milestoneId);
}
