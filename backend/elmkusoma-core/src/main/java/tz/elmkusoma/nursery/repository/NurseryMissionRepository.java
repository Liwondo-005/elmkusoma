package tz.elmkusoma.nursery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.nursery.domain.NurseryMission;

import java.util.List;
import java.util.UUID;

@Repository
public interface NurseryMissionRepository extends JpaRepository<NurseryMission, UUID> {
    List<NurseryMission> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);
    List<NurseryMission> findByStudentIdAndIsDeletedFalse(UUID studentId);
    List<NurseryMission> findByStudentIdAndStatusAndIsDeletedFalse(UUID studentId, NurseryMission.MissionStatus status);
}
