package tz.elmkusoma.learner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learner.domain.LearnerEnrollment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearnerEnrollmentRepository extends JpaRepository<LearnerEnrollment, UUID> {

    List<LearnerEnrollment> findByUserIdAndIsDeletedFalseOrderByEnrolledAtDesc(UUID userId);

    Optional<LearnerEnrollment> findByUserIdAndCourseIdAndIsDeletedFalse(UUID userId, UUID courseId);

    long countByUserIdAndIsDeletedFalse(UUID userId);

    long countByUserIdAndCompletedAtIsNotNullAndIsDeletedFalse(UUID userId);
}
