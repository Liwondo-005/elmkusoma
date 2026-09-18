package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.highereducation.domain.ResearchResource;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResearchResourceRepository extends JpaRepository<ResearchResource, UUID> {

    List<ResearchResource> findByResearchProjectIdAndIsDeletedFalse(UUID researchProjectId);
}
