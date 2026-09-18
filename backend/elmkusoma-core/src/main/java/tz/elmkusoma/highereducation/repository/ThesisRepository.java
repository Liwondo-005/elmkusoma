package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.highereducation.domain.Thesis;
import tz.elmkusoma.highereducation.domain.ThesisStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface ThesisRepository extends JpaRepository<Thesis, UUID> {

    List<Thesis> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<Thesis> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<Thesis> findByStatusAndIsDeletedFalse(ThesisStatus status);

    List<Thesis> findBySupervisorIdAndIsDeletedFalse(UUID supervisorId);

    List<Thesis> findByStudentIdAndInstitutionIdAndIsDeletedFalse(UUID studentId, UUID institutionId);
}
