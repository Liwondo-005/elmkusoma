package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.QuestChallenge;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestChallengeRepository extends JpaRepository<QuestChallenge, UUID> {

    List<QuestChallenge> findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID studentId, UUID institutionId);

    Optional<QuestChallenge> findByIdAndStudentIdAndIsDeletedFalse(UUID id, UUID studentId);
}
