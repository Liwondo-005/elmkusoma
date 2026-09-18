package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.highereducation.domain.ProjectMilestone;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, UUID> {

    List<ProjectMilestone> findByProjectIdAndIsDeletedFalse(UUID projectId);

    List<ProjectMilestone> findByProjectIdAndIsCompletedFalse(UUID projectId);
}
