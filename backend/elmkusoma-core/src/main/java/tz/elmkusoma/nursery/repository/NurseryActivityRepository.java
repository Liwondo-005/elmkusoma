package tz.elmkusoma.nursery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.nursery.domain.NurseryActivity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface NurseryActivityRepository extends JpaRepository<NurseryActivity, UUID> {

    List<NurseryActivity> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);

    List<NurseryActivity> findByClassGroupIdAndActivityDateAndIsDeletedFalse(UUID classGroupId, LocalDate activityDate);

    List<NurseryActivity> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<NurseryActivity> findByActivityTypeAndIsDeletedFalse(NurseryActivity.ActivityType activityType);

    @Query("SELECT na FROM NurseryActivity na WHERE na.classGroupId = :classGroupId AND na.activityDate BETWEEN :startDate AND :endDate AND na.isDeleted = false")
    List<NurseryActivity> findByClassGroupAndDateRange(
            @Param("classGroupId") UUID classGroupId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT na FROM NurseryActivity na WHERE na.activityType = :type AND na.activityDate BETWEEN :startDate AND :endDate AND na.isDeleted = false")
    List<NurseryActivity> findByTypeAndDateRange(
            @Param("type") NurseryActivity.ActivityType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}