package tz.elmkusoma.nursery.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NurseryExtendedService {
    Map<String, Object> getStory(UUID id);
    List<Map<String, Object>> getStoriesByClass(UUID classId);
    Map<String, Object> createStory(UUID institutionId, UUID userId, Map<String, Object> request);
    Map<String, Object> updateStory(UUID id, Map<String, Object> request);
    void deleteStory(UUID id);

    Map<String, Object> getDailyQuest(UUID id);
    List<Map<String, Object>> getDailyQuestsByClass(UUID classId);
    List<Map<String, Object>> getDailyQuestsByStudent(UUID studentId);
    Map<String, Object> createDailyQuest(UUID institutionId, UUID userId, Map<String, Object> request);
    Map<String, Object> updateDailyQuest(UUID id, Map<String, Object> request);
    void deleteDailyQuest(UUID id);

    Map<String, Object> getFeelingsCheckin(UUID id);
    List<Map<String, Object>> getFeelingsCheckinsByStudent(UUID studentId);
    Map<String, Object> createFeelingsCheckin(UUID institutionId, UUID userId, Map<String, Object> request);
    void deleteFeelingsCheckin(UUID id);

    Map<String, Object> getMission(UUID id);
    List<Map<String, Object>> getMissionsByClass(UUID classId);
    List<Map<String, Object>> getMissionsByStudent(UUID studentId);
    Map<String, Object> createMission(UUID institutionId, UUID userId, Map<String, Object> request);
    Map<String, Object> updateMission(UUID id, Map<String, Object> request);
    void deleteMission(UUID id);

    Map<String, Object> getTanzaniaTopic(UUID id);
    List<Map<String, Object>> getTanzaniaTopics(UUID classId);
    List<Map<String, Object>> getTanzaniaByCategory(String category);
    Map<String, Object> createTanzaniaTopic(UUID institutionId, UUID userId, Map<String, Object> request);
    void deleteTanzaniaTopic(UUID id);

    Map<String, Object> getParentLearning(UUID id);
    List<Map<String, Object>> getParentLearningByStudent(UUID studentId);
    Map<String, Object> createParentLearning(UUID institutionId, UUID userId, Map<String, Object> request);
    Map<String, Object> updateParentLearning(UUID id, Map<String, Object> request);
    void deleteParentLearning(UUID id);
}
