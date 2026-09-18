package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.LearningProfile;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningProfileRepository extends JpaRepository<LearningProfile, UUID> {

    Optional<LearningProfile> findByStudentIdAndIsDeletedFalse(UUID studentId);
}
