package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.elmkusoma.highereducation.domain.CareerProfile;

import java.util.Optional;
import java.util.UUID;

public interface CareerProfileRepository extends JpaRepository<CareerProfile, UUID> {
    Optional<CareerProfile> findByStudentIdAndIsDeletedFalse(UUID studentId);
}
