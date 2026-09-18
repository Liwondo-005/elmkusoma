package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.highereducation.domain.DemonstrationStatus;
import tz.elmkusoma.highereducation.domain.PracticalDemonstration;

import java.util.List;
import java.util.UUID;

@Repository
public interface DemonstrationRepository extends JpaRepository<PracticalDemonstration, UUID> {

    List<PracticalDemonstration> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<PracticalDemonstration> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<PracticalDemonstration> findByStatusAndIsDeletedFalse(DemonstrationStatus status);

    List<PracticalDemonstration> findByCompetencyIdAndIsDeletedFalse(UUID competencyId);

    List<PracticalDemonstration> findByStudentIdAndInstitutionIdAndIsDeletedFalse(UUID studentId, UUID institutionId);
}
