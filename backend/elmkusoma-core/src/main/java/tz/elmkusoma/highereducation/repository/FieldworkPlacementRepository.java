package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.highereducation.domain.FieldworkPlacement;
import tz.elmkusoma.highereducation.domain.PlacementStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface FieldworkPlacementRepository extends JpaRepository<FieldworkPlacement, UUID> {

    List<FieldworkPlacement> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<FieldworkPlacement> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<FieldworkPlacement> findByStatusAndIsDeletedFalse(PlacementStatus status);

    List<FieldworkPlacement> findByStudentIdAndInstitutionIdAndIsDeletedFalse(UUID studentId, UUID institutionId);

    List<FieldworkPlacement> findByInstitutionSupervisorIdAndIsDeletedFalse(UUID institutionSupervisorId);
}
