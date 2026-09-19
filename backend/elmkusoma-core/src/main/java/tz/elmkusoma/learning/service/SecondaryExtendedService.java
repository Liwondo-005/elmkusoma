package tz.elmkusoma.learning.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SecondaryExtendedService {
    Map<String, Object> createConcept(UUID institutionId, UUID userId, Map<String, Object> request);
    Map<String, Object> getConcept(UUID id);
    List<Map<String, Object>> getConceptsBySubject(UUID subjectId);
    List<Map<String, Object>> getConceptsByClass(UUID classGroupId);
    Map<String, Object> updateConcept(UUID id, Map<String, Object> request);
    void deleteConcept(UUID id);

    Map<String, Object> createProblem(UUID institutionId, UUID userId, Map<String, Object> request);
    Map<String, Object> getProblem(UUID id);
    List<Map<String, Object>> getProblemsBySubject(UUID subjectId);
    List<Map<String, Object>> getProblemsByClass(UUID classGroupId);
    Map<String, Object> updateProblem(UUID id, Map<String, Object> request);
    void deleteProblem(UUID id);

    Map<String, Object> createError(UUID institutionId, UUID userId, Map<String, Object> request);
    Map<String, Object> getError(UUID id);
    List<Map<String, Object>> getErrorsBySubject(UUID subjectId);
    List<Map<String, Object>> getErrorsByClass(UUID classGroupId);
    Map<String, Object> updateError(UUID id, Map<String, Object> request);
    void deleteError(UUID id);

    Map<String, Object> createStudyPlan(UUID institutionId, UUID userId, Map<String, Object> request);
    Map<String, Object> getStudyPlan(UUID id);
    List<Map<String, Object>> getStudyPlansByStudent(UUID studentId);
    List<Map<String, Object>> getStudyPlansByStudentAndDateRange(UUID studentId, String startDate, String endDate);
    Map<String, Object> updateStudyPlan(UUID id, Map<String, Object> request);
    void deleteStudyPlan(UUID id);
}
