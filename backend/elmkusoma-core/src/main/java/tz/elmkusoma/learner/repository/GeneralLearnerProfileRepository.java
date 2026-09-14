package tz.elmkusoma.learner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learner.domain.GeneralLearnerProfile;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeneralLearnerProfileRepository extends JpaRepository<GeneralLearnerProfile, UUID> {

    Optional<GeneralLearnerProfile> findByUserIdAndIsDeletedFalse(UUID userId);

    boolean existsByUserIdAndIsDeletedFalse(UUID userId);
}
