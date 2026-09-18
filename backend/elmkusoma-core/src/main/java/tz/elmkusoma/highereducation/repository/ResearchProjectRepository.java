package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.highereducation.domain.ResearchProject;
import tz.elmkusoma.highereducation.domain.ResearchStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResearchProjectRepository extends JpaRepository<ResearchProject, UUID> {

    List<ResearchProject> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<ResearchProject> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<ResearchProject> findByStatusAndIsDeletedFalse(ResearchStatus status);

    List<ResearchProject> findBySupervisorIdAndIsDeletedFalse(UUID supervisorId);

    List<ResearchProject> findByStudentIdAndInstitutionIdAndIsDeletedFalse(UUID studentId, UUID institutionId);
}
