package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.SpeakingActivity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpeakingActivityRepository extends JpaRepository<SpeakingActivity, UUID> {

    List<SpeakingActivity> findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID studentId, UUID institutionId);

    Optional<SpeakingActivity> findByIdAndStudentIdAndIsDeletedFalse(UUID id, UUID studentId);
}
