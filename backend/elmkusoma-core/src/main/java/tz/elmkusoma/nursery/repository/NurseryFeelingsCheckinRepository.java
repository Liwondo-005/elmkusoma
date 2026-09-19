package tz.elmkusoma.nursery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.nursery.domain.NurseryFeelingsCheckin;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface NurseryFeelingsCheckinRepository extends JpaRepository<NurseryFeelingsCheckin, UUID> {
    List<NurseryFeelingsCheckin> findByStudentIdAndIsDeletedFalse(UUID studentId);
    List<NurseryFeelingsCheckin> findByStudentIdAndCheckinDateAndIsDeletedFalse(UUID studentId, LocalDate checkinDate);
    List<NurseryFeelingsCheckin> findByClassGroupIdAndCheckinDateAndIsDeletedFalse(UUID classGroupId, LocalDate checkinDate);
}
