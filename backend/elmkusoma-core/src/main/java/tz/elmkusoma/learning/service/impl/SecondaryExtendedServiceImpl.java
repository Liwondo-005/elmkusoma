package tz.elmkusoma.learning.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.domain.*;
import tz.elmkusoma.learning.repository.*;
import tz.elmkusoma.learning.service.SecondaryExtendedService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SecondaryExtendedServiceImpl implements SecondaryExtendedService {

    private final SecondaryConceptBankRepository conceptRepo;
    private final SecondaryProblemBankRepository problemRepo;
    private final SecondaryErrorBankRepository errorRepo;
    private final SecondaryStudyPlannerRepository plannerRepo;

    // ==================== CONCEPTS ====================

    @Override
    public Map<String, Object> createConcept(UUID institutionId, UUID userId, Map<String, Object> req) {
        SecondaryConceptBank c = SecondaryConceptBank.builder()
                .institutionId(institutionId)
                .subjectId(UUID.fromString((String) req.get("subjectId")))
                .classGroupId(UUID.fromString((String) req.get("classGroupId")))
                .conceptName((String) req.get("conceptName"))
                .conceptDescription((String) req.get("conceptDescription"))
                .examples((String) req.get("examples"))
                .relatedConcepts((String) req.get("relatedConcepts"))
                .difficultyLevel(SecondaryConceptBank.DifficultyLevel.valueOf((String) req.getOrDefault("difficultyLevel", "BASIC")))
                .category((String) req.get("category"))
                .build();
        return toMap(conceptRepo.save(c));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getConcept(UUID id) { return toMap(findConcept(id)); }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getConceptsBySubject(UUID subjectId) {
        return conceptRepo.findBySubjectIdAndIsDeletedFalse(subjectId).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getConceptsByClass(UUID classGroupId) {
        return conceptRepo.findByClassGroupIdAndIsDeletedFalse(classGroupId).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> updateConcept(UUID id, Map<String, Object> req) {
        SecondaryConceptBank c = findConcept(id);
        if (req.containsKey("conceptName")) c.setConceptName((String) req.get("conceptName"));
        if (req.containsKey("conceptDescription")) c.setConceptDescription((String) req.get("conceptDescription"));
        if (req.containsKey("examples")) c.setExamples((String) req.get("examples"));
        if (req.containsKey("relatedConcepts")) c.setRelatedConcepts((String) req.get("relatedConcepts"));
        if (req.containsKey("difficultyLevel")) c.setDifficultyLevel(SecondaryConceptBank.DifficultyLevel.valueOf((String) req.get("difficultyLevel")));
        if (req.containsKey("category")) c.setCategory((String) req.get("category"));
        return toMap(conceptRepo.save(c));
    }

    @Override
    public void deleteConcept(UUID id) { SecondaryConceptBank c = findConcept(id); c.setIsDeleted(true); conceptRepo.save(c); }

    private SecondaryConceptBank findConcept(UUID id) {
        return conceptRepo.findById(id).filter(e -> !e.getIsDeleted()).orElseThrow(() -> new ResourceNotFoundException("Concept not found"));
    }

    // ==================== PROBLEMS ====================

    @Override
    public Map<String, Object> createProblem(UUID institutionId, UUID userId, Map<String, Object> req) {
        SecondaryProblemBank p = SecondaryProblemBank.builder()
                .institutionId(institutionId)
                .subjectId(UUID.fromString((String) req.get("subjectId")))
                .classGroupId(UUID.fromString((String) req.get("classGroupId")))
                .problemTitle((String) req.get("problemTitle"))
                .problemDescription((String) req.get("problemDescription"))
                .problemType(SecondaryProblemBank.ProblemType.valueOf((String) req.get("problemType")))
                .options((String) req.get("options"))
                .correctAnswer((String) req.get("correctAnswer"))
                .solution((String) req.get("solution"))
                .difficultyLevel(SecondaryProblemBank.DifficultyLevel.valueOf((String) req.getOrDefault("difficultyLevel", "BASIC")))
                .marks(req.get("marks") != null ? Integer.parseInt(req.get("marks").toString()) : 1)
                .build();
        return toMap(problemRepo.save(p));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getProblem(UUID id) { return toMap(findProblem(id)); }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProblemsBySubject(UUID subjectId) {
        return problemRepo.findBySubjectIdAndIsDeletedFalse(subjectId).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProblemsByClass(UUID classGroupId) {
        return problemRepo.findByClassGroupIdAndIsDeletedFalse(classGroupId).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> updateProblem(UUID id, Map<String, Object> req) {
        SecondaryProblemBank p = findProblem(id);
        if (req.containsKey("problemTitle")) p.setProblemTitle((String) req.get("problemTitle"));
        if (req.containsKey("problemDescription")) p.setProblemDescription((String) req.get("problemDescription"));
        if (req.containsKey("options")) p.setOptions((String) req.get("options"));
        if (req.containsKey("correctAnswer")) p.setCorrectAnswer((String) req.get("correctAnswer"));
        if (req.containsKey("solution")) p.setSolution((String) req.get("solution"));
        if (req.containsKey("difficultyLevel")) p.setDifficultyLevel(SecondaryProblemBank.DifficultyLevel.valueOf((String) req.get("difficultyLevel")));
        return toMap(problemRepo.save(p));
    }

    @Override
    public void deleteProblem(UUID id) { SecondaryProblemBank p = findProblem(id); p.setIsDeleted(true); problemRepo.save(p); }

    private SecondaryProblemBank findProblem(UUID id) {
        return problemRepo.findById(id).filter(e -> !e.getIsDeleted()).orElseThrow(() -> new ResourceNotFoundException("Problem not found"));
    }

    // ==================== ERRORS ====================

    @Override
    public Map<String, Object> createError(UUID institutionId, UUID userId, Map<String, Object> req) {
        SecondaryErrorBank e = SecondaryErrorBank.builder()
                .institutionId(institutionId)
                .subjectId(UUID.fromString((String) req.get("subjectId")))
                .classGroupId(UUID.fromString((String) req.get("classGroupId")))
                .errorTitle((String) req.get("errorTitle"))
                .errorDescription((String) req.get("errorDescription"))
                .incorrectExample((String) req.get("incorrectExample"))
                .correctExample((String) req.get("correctExample"))
                .explanation((String) req.get("explanation"))
                .category((String) req.get("category"))
                .frequency(SecondaryErrorBank.Frequency.valueOf((String) req.getOrDefault("frequency", "COMMON")))
                .build();
        return toMap(errorRepo.save(e));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getError(UUID id) { return toMap(findError(id)); }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getErrorsBySubject(UUID subjectId) {
        return errorRepo.findBySubjectIdAndIsDeletedFalse(subjectId).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getErrorsByClass(UUID classGroupId) {
        return errorRepo.findByClassGroupIdAndIsDeletedFalse(classGroupId).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> updateError(UUID id, Map<String, Object> req) {
        SecondaryErrorBank e = findError(id);
        if (req.containsKey("errorTitle")) e.setErrorTitle((String) req.get("errorTitle"));
        if (req.containsKey("errorDescription")) e.setErrorDescription((String) req.get("errorDescription"));
        if (req.containsKey("incorrectExample")) e.setIncorrectExample((String) req.get("incorrectExample"));
        if (req.containsKey("correctExample")) e.setCorrectExample((String) req.get("correctExample"));
        if (req.containsKey("explanation")) e.setExplanation((String) req.get("explanation"));
        return toMap(errorRepo.save(e));
    }

    @Override
    public void deleteError(UUID id) { SecondaryErrorBank e = findError(id); e.setIsDeleted(true); errorRepo.save(e); }

    private SecondaryErrorBank findError(UUID id) {
        return errorRepo.findById(id).filter(e -> !e.getIsDeleted()).orElseThrow(() -> new ResourceNotFoundException("Error entry not found"));
    }

    // ==================== STUDY PLANNER ====================

    @Override
    public Map<String, Object> createStudyPlan(UUID institutionId, UUID userId, Map<String, Object> req) {
        SecondaryStudyPlanner sp = SecondaryStudyPlanner.builder()
                .institutionId(institutionId)
                .studentId(UUID.fromString((String) req.get("studentId")))
                .classGroupId(UUID.fromString((String) req.get("classGroupId")))
                .subjectId(req.get("subjectId") != null ? UUID.fromString((String) req.get("subjectId")) : null)
                .topicName((String) req.get("topicName"))
                .plannedDate(LocalDate.parse((String) req.get("plannedDate")))
                .durationMinutes(req.get("durationMinutes") != null ? Integer.parseInt(req.get("durationMinutes").toString()) : 30)
                .priority(SecondaryStudyPlanner.Priority.valueOf((String) req.getOrDefault("priority", "MEDIUM")))
                .notes((String) req.get("notes"))
                .build();
        return toMap(plannerRepo.save(sp));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStudyPlan(UUID id) { return toMap(findPlanner(id)); }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStudyPlansByStudent(UUID studentId) {
        return plannerRepo.findByStudentIdAndIsDeletedFalse(studentId).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStudyPlansByStudentAndDateRange(UUID studentId, String startDate, String endDate) {
        return plannerRepo.findByStudentIdAndPlannedDateBetweenAndIsDeletedFalse(studentId, LocalDate.parse(startDate), LocalDate.parse(endDate)).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> updateStudyPlan(UUID id, Map<String, Object> req) {
        SecondaryStudyPlanner sp = findPlanner(id);
        if (req.containsKey("topicName")) sp.setTopicName((String) req.get("topicName"));
        if (req.containsKey("plannedDate")) sp.setPlannedDate(LocalDate.parse((String) req.get("plannedDate")));
        if (req.containsKey("durationMinutes")) sp.setDurationMinutes(Integer.parseInt(req.get("durationMinutes").toString()));
        if (req.containsKey("priority")) sp.setPriority(SecondaryStudyPlanner.Priority.valueOf((String) req.get("priority")));
        if (req.containsKey("notes")) sp.setNotes((String) req.get("notes"));
        if (req.containsKey("status")) {
            sp.setStatus(SecondaryStudyPlanner.PlannerStatus.valueOf((String) req.get("status")));
            if (sp.getStatus() == SecondaryStudyPlanner.PlannerStatus.COMPLETED) sp.setCompletedDate(LocalDate.now());
        }
        return toMap(plannerRepo.save(sp));
    }

    @Override
    public void deleteStudyPlan(UUID id) { SecondaryStudyPlanner sp = findPlanner(id); sp.setIsDeleted(true); plannerRepo.save(sp); }

    private SecondaryStudyPlanner findPlanner(UUID id) {
        return plannerRepo.findById(id).filter(e -> !e.getIsDeleted()).orElseThrow(() -> new ResourceNotFoundException("Study plan not found"));
    }

    // ==================== MAP HELPERS ====================

    private Map<String, Object> toMap(SecondaryConceptBank e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("subjectId", e.getSubjectId()); m.put("classGroupId", e.getClassGroupId());
        m.put("conceptName", e.getConceptName()); m.put("conceptDescription", e.getConceptDescription());
        m.put("examples", e.getExamples()); m.put("relatedConcepts", e.getRelatedConcepts());
        m.put("difficultyLevel", e.getDifficultyLevel().name()); m.put("category", e.getCategory());
        m.put("createdAt", e.getCreatedAt());
        return m;
    }

    private Map<String, Object> toMap(SecondaryProblemBank e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("subjectId", e.getSubjectId()); m.put("classGroupId", e.getClassGroupId());
        m.put("problemTitle", e.getProblemTitle()); m.put("problemDescription", e.getProblemDescription());
        m.put("problemType", e.getProblemType().name()); m.put("options", e.getOptions());
        m.put("correctAnswer", e.getCorrectAnswer()); m.put("solution", e.getSolution());
        m.put("difficultyLevel", e.getDifficultyLevel().name()); m.put("marks", e.getMarks());
        m.put("createdAt", e.getCreatedAt());
        return m;
    }

    private Map<String, Object> toMap(SecondaryErrorBank e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("subjectId", e.getSubjectId()); m.put("classGroupId", e.getClassGroupId());
        m.put("errorTitle", e.getErrorTitle()); m.put("errorDescription", e.getErrorDescription());
        m.put("incorrectExample", e.getIncorrectExample()); m.put("correctExample", e.getCorrectExample());
        m.put("explanation", e.getExplanation()); m.put("category", e.getCategory());
        m.put("frequency", e.getFrequency().name()); m.put("createdAt", e.getCreatedAt());
        return m;
    }

    private Map<String, Object> toMap(SecondaryStudyPlanner e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("studentId", e.getStudentId()); m.put("classGroupId", e.getClassGroupId());
        m.put("subjectId", e.getSubjectId()); m.put("topicName", e.getTopicName());
        m.put("plannedDate", e.getPlannedDate()); m.put("durationMinutes", e.getDurationMinutes());
        m.put("status", e.getStatus().name()); m.put("priority", e.getPriority().name());
        m.put("notes", e.getNotes()); m.put("completedDate", e.getCompletedDate());
        m.put("createdAt", e.getCreatedAt());
        return m;
    }
}
