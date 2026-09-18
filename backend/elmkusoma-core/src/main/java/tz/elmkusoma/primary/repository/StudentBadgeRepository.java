package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.StudentBadge;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentBadgeRepository extends JpaRepository<StudentBadge, UUID> {

    List<StudentBadge> findByStudentIdAndInstitutionIdAndIsDeletedFalse(UUID studentId, UUID institutionId);
}
