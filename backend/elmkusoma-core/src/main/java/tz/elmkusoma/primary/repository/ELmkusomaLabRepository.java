package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.ELmkusomaLab;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ELmkusomaLabRepository extends JpaRepository<ELmkusomaLab, UUID> {

    List<ELmkusomaLab> findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID studentId, UUID institutionId);

    Optional<ELmkusomaLab> findByIdAndStudentIdAndIsDeletedFalse(UUID id, UUID studentId);
}
