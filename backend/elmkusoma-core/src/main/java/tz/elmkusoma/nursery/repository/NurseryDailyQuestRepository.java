package tz.elmkusoma.nursery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.nursery.domain.NurseryDailyQuest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface NurseryDailyQuestRepository extends JpaRepository<NurseryDailyQuest, UUID> {
    List<NurseryDailyQuest> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);
    List<NurseryDailyQuest> findByStudentIdAndIsDeletedFalse(UUID studentId);
    List<NurseryDailyQuest> findByStudentIdAndStatusAndIsDeletedFalse(UUID studentId, NurseryDailyQuest.QuestStatus status);
    List<NurseryDailyQuest> findByClassGroupIdAndDueDateAndIsDeletedFalse(UUID classGroupId, LocalDate dueDate);
}
