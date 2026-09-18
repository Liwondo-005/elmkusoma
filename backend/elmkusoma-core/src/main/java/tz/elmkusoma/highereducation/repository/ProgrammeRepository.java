package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.highereducation.domain.Programme;
import tz.elmkusoma.highereducation.domain.ProgrammeType;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProgrammeRepository extends JpaRepository<Programme, UUID> {

    List<Programme> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<Programme> findByInstitutionIdAndEducationLevelAndIsDeletedFalse(UUID institutionId, EducationLevel educationLevel);

    List<Programme> findByInstitutionIdAndProgrammeTypeAndIsDeletedFalse(UUID institutionId, ProgrammeType programmeType);

    long countByInstitutionIdAndIsDeletedFalse(UUID institutionId);
}
