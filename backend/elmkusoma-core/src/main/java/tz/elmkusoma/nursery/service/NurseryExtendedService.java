package tz.elmkusoma.nursery.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NurseryExtendedService {
    Map<String, Object> getStory(UUID institutionId, UUID id);
    List<Map<String, Object>> getStoriesByClass(UUID institutionId, UUID classId);
    Map<String, Object> createStory(UUID institutionId, UUID userId, Map<String, Object> request);
    Map<String, Object> updateStory(UUID institutionId, UUID id, Map<String, Object> request);
    void deleteStory(UUID institutionId, UUID id);

    Map<String, Object> getDailyQuest(UUID institutionId, UUID id);
    List<Map<String, Object>> getDailyQuestsByClass(UUID institutionId, UUID classId);
    List<Map<String, Object>> getDailyQuestsByStudent(UUID institutionId, UUID studentId);
    Map<String, Object> createDailyQuest(UUID institutionId, UUID userId, Map<String, Object> request);
    Map<String, Object> updateDailyQuest(UUID institutionId, UUID id, Map<String, Object> request);
    void deleteDailyQuest(UUID institutionId, UUID id);

    Map<String, Object> getFeelingsCheckin(UUID institutionId, UUID id);
    List<Map<String, Object>> getFeelingsCheckinsByStudent(UUID institutionId, UUID studentId);
    Map<String, Object> createFeelingsCheckin(UUID institutionId, UUID userId, Map<String, Object> request);
    void deleteFeelingsCheckin(UUID institutionId, UUID id);

    Map<String, Object> getMission(UUID institutionId, UUID id);
    List<Map<String, Object>> getMissionsByClass(UUID institutionId, UUID classId);
    List<Map<String, Object>> getMissionsByStudent(UUID institutionId, UUID studentId);
    Map<String, Object> createMission(UUID institutionId, UUID userId, Map<String, Object> request);
    Map<String, Object> updateMission(UUID institutionId, UUID id, Map<String, Object> request);
    void deleteMission(UUID institutionId, UUID id);

    Map<String, Object> getTanzaniaTopic(UUID institutionId, UUID id);
    List<Map<String, Object>> getTanzaniaTopics(UUID institutionId, UUID classId);
    List<Map<String, Object>> getTanzaniaByCategory(UUID institutionId, String category);
    Map<String, Object> createTanzaniaTopic(UUID institutionId, UUID userId, Map<String, Object> request);
    void deleteTanzaniaTopic(UUID institutionId, UUID id);

    Map<String, Object> getParentLearning(UUID institutionId, UUID id);
    List<Map<String, Object>> getParentLearningByStudent(UUID institutionId, UUID studentId);
    Map<String, Object> createParentLearning(UUID institutionId, UUID userId, Map<String, Object> request);
    Map<String, Object> updateParentLearning(UUID institutionId, UUID id, Map<String, Object> request);
    void deleteParentLearning(UUID institutionId, UUID id);
}
