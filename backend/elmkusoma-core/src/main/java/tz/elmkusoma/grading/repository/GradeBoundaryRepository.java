package tz.elmkusoma.grading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.grading.domain.GradeBoundary;

import java.util.List;
import java.util.UUID;

@Repository
public interface GradeBoundaryRepository extends JpaRepository<GradeBoundary, UUID> {

    List<GradeBoundary> findByGradingScaleIdAndIsDeletedFalse(UUID gradingScaleId);

    List<GradeBoundary> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    @Query("SELECT gb FROM GradeBoundary gb WHERE gb.gradingScaleId = :scaleId AND gb.isDeleted = false ORDER BY gb.minPercentage DESC")
    List<GradeBoundary> findBoundariesByScaleId(@Param("scaleId") UUID scaleId);
}