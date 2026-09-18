package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.highereducation.domain.Competency;
import tz.elmkusoma.highereducation.domain.CompetencyType;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompetencyRepository extends JpaRepository<Competency, UUID> {

    List<Competency> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<Competency> findBySubjectIdAndIsDeletedFalse(UUID subjectId);

    List<Competency> findByProgrammeIdAndIsDeletedFalse(UUID programmeId);

    List<Competency> findByInstitutionIdAndSubjectIdAndIsDeletedFalse(UUID institutionId, UUID subjectId);

    List<Competency> findByInstitutionIdAndCompetencyTypeAndIsDeletedFalse(UUID institutionId, CompetencyType competencyType);

    List<Competency> findByIsActiveTrueAndIsDeletedFalse();
}
