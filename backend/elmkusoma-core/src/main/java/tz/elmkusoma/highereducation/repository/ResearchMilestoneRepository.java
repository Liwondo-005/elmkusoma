package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.highereducation.domain.ResearchMilestone;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResearchMilestoneRepository extends JpaRepository<ResearchMilestone, UUID> {

    List<ResearchMilestone> findByResearchProjectIdAndIsDeletedFalse(UUID researchProjectId);
}
