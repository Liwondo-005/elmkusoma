package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.RealWorldMission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RealWorldMissionRepository extends JpaRepository<RealWorldMission, UUID> {

    List<RealWorldMission> findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID studentId, UUID institutionId);

    Optional<RealWorldMission> findByIdAndStudentIdAndIsDeletedFalse(UUID id, UUID studentId);
}
