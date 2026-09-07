package tz.elmkusoma.grading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.grading.domain.GradingScale;

import java.util.List;
import java.util.UUID;

@Repository
public interface GradingScaleRepository extends JpaRepository<GradingScale, UUID> {

    List<GradingScale> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);
}