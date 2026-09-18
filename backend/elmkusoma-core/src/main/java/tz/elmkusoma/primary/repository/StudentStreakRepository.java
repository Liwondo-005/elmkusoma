package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.StudentStreak;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentStreakRepository extends JpaRepository<StudentStreak, UUID> {

    Optional<StudentStreak> findByStudentIdAndInstitutionIdAndIsDeletedFalse(UUID studentId, UUID institutionId);
}
