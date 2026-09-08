package tz.elmkusoma.nursery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.nursery.domain.NurseryMilestone;

import java.util.List;
import java.util.UUID;

@Repository
public interface NurseryMilestoneRepository extends JpaRepository<NurseryMilestone, UUID> {

    List<NurseryMilestone> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<NurseryMilestone> findByCategoryAndIsDeletedFalse(NurseryMilestone.MilestoneCategory category);

    List<NurseryMilestone> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    @Query("SELECT nm FROM NurseryMilestone nm WHERE nm.studentId = :studentId AND nm.category = :category AND nm.isDeleted = false")
    List<NurseryMilestone> findByStudentAndCategory(
            @Param("studentId") UUID studentId,
            @Param("category") NurseryMilestone.MilestoneCategory category);

    @Query("SELECT nm FROM NurseryMilestone nm WHERE nm.studentId = :studentId AND nm.status = :status AND nm.isDeleted = false")
    List<NurseryMilestone> findByStudentAndStatus(
            @Param("studentId") UUID studentId,
            @Param("status") NurseryMilestone.MilestoneStatus status);
}