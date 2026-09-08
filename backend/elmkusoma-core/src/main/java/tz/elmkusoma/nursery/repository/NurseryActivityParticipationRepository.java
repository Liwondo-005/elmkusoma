package tz.elmkusoma.nursery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.nursery.domain.NurseryActivityParticipation;

import java.util.List;
import java.util.UUID;

@Repository
public interface NurseryActivityParticipationRepository extends JpaRepository<NurseryActivityParticipation, UUID> {

    List<NurseryActivityParticipation> findByActivityIdAndIsDeletedFalse(UUID activityId);

    List<NurseryActivityParticipation> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<NurseryActivityParticipation> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);
}