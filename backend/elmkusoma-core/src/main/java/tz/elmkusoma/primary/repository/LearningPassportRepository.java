package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.LearningPassport;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningPassportRepository extends JpaRepository<LearningPassport, UUID> {

    Optional<LearningPassport> findByStudentIdAndInstitutionIdAndIsDeletedFalse(UUID studentId, UUID institutionId);
}
