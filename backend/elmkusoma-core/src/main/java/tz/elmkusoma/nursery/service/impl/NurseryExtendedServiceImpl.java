package tz.elmkusoma.nursery.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.common.OwnershipGuard;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nursery.domain.*;
import tz.elmkusoma.nursery.repository.*;
import tz.elmkusoma.nursery.service.NurseryExtendedService;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NurseryExtendedServiceImpl implements NurseryExtendedService {

    private final NurseryStoryRepository storyRepo;
    private final NurseryDailyQuestRepository dailyQuestRepo;
    private final NurseryFeelingsCheckinRepository feelingsRepo;
    private final NurseryMissionRepository missionRepo;
    private final NurseryTanzaniaDiscoveryRepository tanzaniaRepo;
    private final NurseryParentLearningRepository parentLearningRepo;
    private final OwnershipGuard ownershipGuard;
    private final ClassGroupRepository classGroupRepository;
    private final StudentRepository studentRepository;

    // ==================== STORIES ====================

    @Override
    public Map<String, Object> createStory(UUID institutionId, UUID userId, Map<String, Object> req) {
        NurseryStory story = NurseryStory.builder()
                .institutionId(institutionId)
                .classGroupId(UUID.fromString((String) req.get("classGroupId")))
                .title((String) req.get("title"))
                .content((String) req.get("content"))
                .storyType(NurseryStory.StoryType.valueOf((String) req.get("storyType")))
                .illustrationUrl((String) req.get("illustrationUrl"))
                .audioUrl((String) req.get("audioUrl"))
                .durationMinutes(req.get("durationMinutes") != null ? Integer.parseInt(req.get("durationMinutes").toString()) : null)
                .readingLevel((String) req.get("readingLevel"))
                .isPublished(Boolean.TRUE.equals(req.get("isPublished")))
                .build();
        return toMap(storyRepo.save(story));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStory(UUID institutionId, UUID id) {
        NurseryStory story = findStory(id);
        verifyRowInScope(institutionId, story.getInstitutionId());
        return toMap(story);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStoriesByClass(UUID institutionId, UUID classId) {
        verifyClassGroupInScope(institutionId, classId);
        return storyRepo.findByClassGroupIdAndIsDeletedFalse(classId).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> updateStory(UUID institutionId, UUID id, Map<String, Object> req) {
        NurseryStory s = findStory(id);
        verifyRowInScope(institutionId, s.getInstitutionId());
        if (req.containsKey("title")) s.setTitle((String) req.get("title"));
        if (req.containsKey("content")) s.setContent((String) req.get("content"));
        if (req.containsKey("storyType")) s.setStoryType(NurseryStory.StoryType.valueOf((String) req.get("storyType")));
        if (req.containsKey("illustrationUrl")) s.setIllustrationUrl((String) req.get("illustrationUrl"));
        if (req.containsKey("audioUrl")) s.setAudioUrl((String) req.get("audioUrl"));
        if (req.containsKey("durationMinutes")) s.setDurationMinutes(req.get("durationMinutes") != null ? Integer.parseInt(req.get("durationMinutes").toString()) : null);
        if (req.containsKey("isPublished")) s.setIsPublished(Boolean.TRUE.equals(req.get("isPublished")));
        return toMap(storyRepo.save(s));
    }

    @Override
    public void deleteStory(UUID institutionId, UUID id) {
        NurseryStory s = findStory(id);
        verifyRowInScope(institutionId, s.getInstitutionId());
        s.setIsDeleted(true);
        storyRepo.save(s);
    }

    private NurseryStory findStory(UUID id) {
        return storyRepo.findById(id).filter(e -> !e.getIsDeleted()).orElseThrow(() -> new ResourceNotFoundException("Story not found"));
    }

    // ==================== DAILY QUESTS ====================

    @Override
    public Map<String, Object> createDailyQuest(UUID institutionId, UUID userId, Map<String, Object> req) {
        NurseryDailyQuest q = NurseryDailyQuest.builder()
                .institutionId(institutionId)
                .classGroupId(UUID.fromString((String) req.get("classGroupId")))
                .studentId(req.get("studentId") != null ? UUID.fromString((String) req.get("studentId")) : null)
                .questTitle((String) req.get("questTitle"))
                .questDescription((String) req.get("questDescription"))
                .questType(NurseryDailyQuest.QuestType.valueOf((String) req.get("questType")))
                .rewardPoints(req.get("rewardPoints") != null ? Integer.parseInt(req.get("rewardPoints").toString()) : 0)
                .status(req.get("status") != null ? NurseryDailyQuest.QuestStatus.valueOf((String) req.get("status")) : NurseryDailyQuest.QuestStatus.PENDING)
                .dueDate(req.get("dueDate") != null ? LocalDate.parse((String) req.get("dueDate")) : null)
                .build();
        return toMap(dailyQuestRepo.save(q));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDailyQuest(UUID institutionId, UUID id) {
        NurseryDailyQuest quest = findDailyQuest(id);
        verifyRowInScope(institutionId, quest.getInstitutionId());
        return toMap(quest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDailyQuestsByClass(UUID institutionId, UUID classId) {
        verifyClassGroupInScope(institutionId, classId);
        return dailyQuestRepo.findByClassGroupIdAndIsDeletedFalse(classId).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDailyQuestsByStudent(UUID institutionId, UUID studentId) {
        verifyStudentInScope(institutionId, studentId);
        return dailyQuestRepo.findByStudentIdAndIsDeletedFalse(studentId).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> updateDailyQuest(UUID institutionId, UUID id, Map<String, Object> req) {
        NurseryDailyQuest q = findDailyQuest(id);
        verifyRowInScope(institutionId, q.getInstitutionId());
        if (req.containsKey("questTitle")) q.setQuestTitle((String) req.get("questTitle"));
        if (req.containsKey("questDescription")) q.setQuestDescription((String) req.get("questDescription"));
        if (req.containsKey("questType")) q.setQuestType(NurseryDailyQuest.QuestType.valueOf((String) req.get("questType")));
        if (req.containsKey("rewardPoints")) q.setRewardPoints(Integer.parseInt(req.get("rewardPoints").toString()));
        if (req.containsKey("status")) {
            q.setStatus(NurseryDailyQuest.QuestStatus.valueOf((String) req.get("status")));
            if (q.getStatus() == NurseryDailyQuest.QuestStatus.COMPLETED) q.setCompletedDate(LocalDate.now());
        }
        if (req.containsKey("dueDate")) q.setDueDate(req.get("dueDate") != null ? LocalDate.parse((String) req.get("dueDate")) : null);
        return toMap(dailyQuestRepo.save(q));
    }

    @Override
    public void deleteDailyQuest(UUID institutionId, UUID id) {
        NurseryDailyQuest q = findDailyQuest(id);
        verifyRowInScope(institutionId, q.getInstitutionId());
        q.setIsDeleted(true);
        dailyQuestRepo.save(q);
    }

    private NurseryDailyQuest findDailyQuest(UUID id) {
        return dailyQuestRepo.findById(id).filter(e -> !e.getIsDeleted()).orElseThrow(() -> new ResourceNotFoundException("Daily quest not found"));
    }

    // ==================== FEELINGS CHECK-IN ====================

    @Override
    public Map<String, Object> createFeelingsCheckin(UUID institutionId, UUID userId, Map<String, Object> req) {
        NurseryFeelingsCheckin f = NurseryFeelingsCheckin.builder()
                .institutionId(institutionId)
                .studentId(UUID.fromString((String) req.get("studentId")))
                .classGroupId(UUID.fromString((String) req.get("classGroupId")))
                .feeling(NurseryFeelingsCheckin.Feeling.valueOf((String) req.get("feeling")))
                .emoji((String) req.get("emoji"))
                .note((String) req.get("note"))
                .checkinDate(req.get("checkinDate") != null ? LocalDate.parse((String) req.get("checkinDate")) : LocalDate.now())
                .build();
        return toMap(feelingsRepo.save(f));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getFeelingsCheckin(UUID institutionId, UUID id) {
        NurseryFeelingsCheckin checkin = findFeelings(id);
        verifyRowInScope(institutionId, checkin.getInstitutionId());
        return toMap(checkin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFeelingsCheckinsByStudent(UUID institutionId, UUID studentId) {
        verifyStudentInScope(institutionId, studentId);
        return feelingsRepo.findByStudentIdAndIsDeletedFalse(studentId).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    public void deleteFeelingsCheckin(UUID institutionId, UUID id) {
        NurseryFeelingsCheckin f = findFeelings(id);
        verifyRowInScope(institutionId, f.getInstitutionId());
        f.setIsDeleted(true);
        feelingsRepo.save(f);
    }

    private NurseryFeelingsCheckin findFeelings(UUID id) {
        return feelingsRepo.findById(id).filter(e -> !e.getIsDeleted()).orElseThrow(() -> new ResourceNotFoundException("Feelings checkin not found"));
    }

    // ==================== MISSIONS ====================

    @Override
    public Map<String, Object> createMission(UUID institutionId, UUID userId, Map<String, Object> req) {
        NurseryMission m = NurseryMission.builder()
                .institutionId(institutionId)
                .classGroupId(UUID.fromString((String) req.get("classGroupId")))
                .studentId(req.get("studentId") != null ? UUID.fromString((String) req.get("studentId")) : null)
                .missionTitle((String) req.get("missionTitle"))
                .missionDescription((String) req.get("missionDescription"))
                .missionType(NurseryMission.MissionType.valueOf((String) req.get("missionType")))
                .rewardPoints(req.get("rewardPoints") != null ? Integer.parseInt(req.get("rewardPoints").toString()) : 0)
                .status(req.get("status") != null ? NurseryMission.MissionStatus.valueOf((String) req.get("status")) : NurseryMission.MissionStatus.PENDING)
                .dueDate(req.get("dueDate") != null ? LocalDate.parse((String) req.get("dueDate")) : null)
                .build();
        return toMap(missionRepo.save(m));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMission(UUID institutionId, UUID id) {
        NurseryMission mission = findMission(id);
        verifyRowInScope(institutionId, mission.getInstitutionId());
        return toMap(mission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMissionsByClass(UUID institutionId, UUID classId) {
        verifyClassGroupInScope(institutionId, classId);
        return missionRepo.findByClassGroupIdAndIsDeletedFalse(classId).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMissionsByStudent(UUID institutionId, UUID studentId) {
        verifyStudentInScope(institutionId, studentId);
        return missionRepo.findByStudentIdAndIsDeletedFalse(studentId).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> updateMission(UUID institutionId, UUID id, Map<String, Object> req) {
        NurseryMission m = findMission(id);
        verifyRowInScope(institutionId, m.getInstitutionId());
        if (req.containsKey("missionTitle")) m.setMissionTitle((String) req.get("missionTitle"));
        if (req.containsKey("missionDescription")) m.setMissionDescription((String) req.get("missionDescription"));
        if (req.containsKey("missionType")) m.setMissionType(NurseryMission.MissionType.valueOf((String) req.get("missionType")));
        if (req.containsKey("rewardPoints")) m.setRewardPoints(Integer.parseInt(req.get("rewardPoints").toString()));
        if (req.containsKey("status")) {
            m.setStatus(NurseryMission.MissionStatus.valueOf((String) req.get("status")));
            if (m.getStatus() == NurseryMission.MissionStatus.COMPLETED) m.setCompletedDate(LocalDate.now());
        }
        if (req.containsKey("evidenceNotes")) m.setEvidenceNotes((String) req.get("evidenceNotes"));
        if (req.containsKey("evidenceImageUrl")) m.setEvidenceImageUrl((String) req.get("evidenceImageUrl"));
        return toMap(missionRepo.save(m));
    }

    @Override
    public void deleteMission(UUID institutionId, UUID id) {
        NurseryMission m = findMission(id);
        verifyRowInScope(institutionId, m.getInstitutionId());
        m.setIsDeleted(true);
        missionRepo.save(m);
    }

    private NurseryMission findMission(UUID id) {
        return missionRepo.findById(id).filter(e -> !e.getIsDeleted()).orElseThrow(() -> new ResourceNotFoundException("Mission not found"));
    }

    // ==================== TANZANIA DISCOVERY ====================

    @Override
    public Map<String, Object> createTanzaniaTopic(UUID institutionId, UUID userId, Map<String, Object> req) {
        NurseryTanzaniaDiscovery t = NurseryTanzaniaDiscovery.builder()
                .institutionId(institutionId)
                .classGroupId(req.get("classGroupId") != null ? UUID.fromString((String) req.get("classGroupId")) : null)
                .topicTitle((String) req.get("topicTitle"))
                .topicDescription((String) req.get("topicDescription"))
                .category(NurseryTanzaniaDiscovery.DiscoveryCategory.valueOf((String) req.get("category")))
                .region((String) req.get("region"))
                .funFacts((String) req.get("funFacts"))
                .imageUrl((String) req.get("imageUrl"))
                .isPublished(Boolean.TRUE.equals(req.get("isPublished")))
                .build();
        return toMap(tanzaniaRepo.save(t));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTanzaniaTopic(UUID institutionId, UUID id) {
        NurseryTanzaniaDiscovery topic = findTanzania(id);
        verifyRowInScope(institutionId, topic.getInstitutionId());
        return toMap(topic);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTanzaniaTopics(UUID institutionId, UUID classId) {
        verifyClassGroupInScope(institutionId, classId);
        return tanzaniaRepo.findByClassGroupIdAndIsDeletedFalse(classId).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTanzaniaByCategory(UUID institutionId, String category) {
        // Scoped by institution at the query level; the unscoped category finder
        // would leak every tenant's topics to any caller.
        return tanzaniaRepo.findByCategoryAndInstitutionIdAndIsDeletedFalse(
                        NurseryTanzaniaDiscovery.DiscoveryCategory.valueOf(category), institutionId)
                .stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    public void deleteTanzaniaTopic(UUID institutionId, UUID id) {
        NurseryTanzaniaDiscovery t = findTanzania(id);
        verifyRowInScope(institutionId, t.getInstitutionId());
        t.setIsDeleted(true);
        tanzaniaRepo.save(t);
    }

    private NurseryTanzaniaDiscovery findTanzania(UUID id) {
        return tanzaniaRepo.findById(id).filter(e -> !e.getIsDeleted()).orElseThrow(() -> new ResourceNotFoundException("Tanzania topic not found"));
    }

    // ==================== PARENT LEARNING ====================

    @Override
    public Map<String, Object> createParentLearning(UUID institutionId, UUID userId, Map<String, Object> req) {
        NurseryParentLearning p = NurseryParentLearning.builder()
                .institutionId(institutionId)
                .studentId(UUID.fromString((String) req.get("studentId")))
                .classGroupId(UUID.fromString((String) req.get("classGroupId")))
                .activityTitle((String) req.get("activityTitle"))
                .activityDescription((String) req.get("activityDescription"))
                .activityType(NurseryParentLearning.ActivityType.valueOf((String) req.get("activityType")))
                .parentName((String) req.get("parentName"))
                .completionStatus(req.get("completionStatus") != null ? NurseryParentLearning.CompletionStatus.valueOf((String) req.get("completionStatus")) : NurseryParentLearning.CompletionStatus.PENDING)
                .notes((String) req.get("notes"))
                .build();
        return toMap(parentLearningRepo.save(p));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getParentLearning(UUID institutionId, UUID id) {
        NurseryParentLearning activity = findParentLearning(id);
        verifyRowInScope(institutionId, activity.getInstitutionId());
        return toMap(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getParentLearningByStudent(UUID institutionId, UUID studentId) {
        verifyStudentInScope(institutionId, studentId);
        return parentLearningRepo.findByStudentIdAndIsDeletedFalse(studentId).stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> updateParentLearning(UUID institutionId, UUID id, Map<String, Object> req) {
        NurseryParentLearning p = findParentLearning(id);
        verifyRowInScope(institutionId, p.getInstitutionId());
        if (req.containsKey("completionStatus")) {
            p.setCompletionStatus(NurseryParentLearning.CompletionStatus.valueOf((String) req.get("completionStatus")));
            if (p.getCompletionStatus() == NurseryParentLearning.CompletionStatus.COMPLETED) p.setCompletedDate(LocalDate.now());
        }
        if (req.containsKey("notes")) p.setNotes((String) req.get("notes"));
        return toMap(parentLearningRepo.save(p));
    }

    @Override
    public void deleteParentLearning(UUID institutionId, UUID id) {
        NurseryParentLearning p = findParentLearning(id);
        verifyRowInScope(institutionId, p.getInstitutionId());
        p.setIsDeleted(true);
        parentLearningRepo.save(p);
    }

    private NurseryParentLearning findParentLearning(UUID id) {
        return parentLearningRepo.findById(id).filter(e -> !e.getIsDeleted()).orElseThrow(() -> new ResourceNotFoundException("Parent learning not found"));
    }

    /**
     * Tenant check for id-based access: the row's own {@code institution_id}
     * column (server-set at creation via BaseEntity) defines the tenant
     * boundary and fails closed when absent. Another tenant's row -&gt; 403.
     */
    private void verifyRowInScope(UUID institutionId, UUID rowInstitutionId) {
        ownershipGuard.verifyInstitution(rowInstitutionId, institutionId);
    }

    /**
     * Tenant check for class-keyed access: the class group owns the tenant
     * boundary (its {@code institution_id} column is NOT NULL and server-set).
     * Unknown class group -&gt; 404; another tenant's class group -&gt; 403.
     */
    private void verifyClassGroupInScope(UUID institutionId, UUID classGroupId) {
        ClassGroup classGroup = classGroupRepository.findById(classGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Class group not found"));
        ownershipGuard.verifyInstitution(classGroup.getInstitutionId(), institutionId);
    }

    /**
     * Tenant check for student-keyed access: the student's institution defines the
     * tenant boundary ({@code students.institution_id} is NOT NULL).
     * Unknown student -&gt; 404; another tenant's student -&gt; 403.
     */
    private void verifyStudentInScope(UUID institutionId, UUID studentId) {
        Student student = studentRepository.findByIdAndIsDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        ownershipGuard.verifyInstitution(student.getInstitutionId(), institutionId);
    }

    // ==================== MAP HELPERS ====================

    private Map<String, Object> toMap(NurseryStory e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("classGroupId", e.getClassGroupId()); m.put("title", e.getTitle());
        m.put("content", e.getContent()); m.put("storyType", e.getStoryType().name());
        m.put("illustrationUrl", e.getIllustrationUrl()); m.put("audioUrl", e.getAudioUrl());
        m.put("durationMinutes", e.getDurationMinutes()); m.put("readingLevel", e.getReadingLevel());
        m.put("isPublished", e.getIsPublished()); m.put("createdAt", e.getCreatedAt());
        return m;
    }

    private Map<String, Object> toMap(NurseryDailyQuest e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("classGroupId", e.getClassGroupId()); m.put("studentId", e.getStudentId());
        m.put("questTitle", e.getQuestTitle()); m.put("questDescription", e.getQuestDescription());
        m.put("questType", e.getQuestType().name()); m.put("rewardPoints", e.getRewardPoints());
        m.put("status", e.getStatus().name()); m.put("dueDate", e.getDueDate());
        m.put("completedDate", e.getCompletedDate()); m.put("createdAt", e.getCreatedAt());
        return m;
    }

    private Map<String, Object> toMap(NurseryFeelingsCheckin e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("studentId", e.getStudentId()); m.put("classGroupId", e.getClassGroupId());
        m.put("feeling", e.getFeeling().name()); m.put("emoji", e.getEmoji());
        m.put("note", e.getNote()); m.put("checkinDate", e.getCheckinDate());
        m.put("createdAt", e.getCreatedAt());
        return m;
    }

    private Map<String, Object> toMap(NurseryMission e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("classGroupId", e.getClassGroupId()); m.put("studentId", e.getStudentId());
        m.put("missionTitle", e.getMissionTitle()); m.put("missionDescription", e.getMissionDescription());
        m.put("missionType", e.getMissionType().name()); m.put("rewardPoints", e.getRewardPoints());
        m.put("status", e.getStatus().name()); m.put("dueDate", e.getDueDate());
        m.put("completedDate", e.getCompletedDate()); m.put("evidenceNotes", e.getEvidenceNotes());
        m.put("evidenceImageUrl", e.getEvidenceImageUrl()); m.put("createdAt", e.getCreatedAt());
        return m;
    }

    private Map<String, Object> toMap(NurseryTanzaniaDiscovery e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("classGroupId", e.getClassGroupId());
        m.put("topicTitle", e.getTopicTitle()); m.put("topicDescription", e.getTopicDescription());
        m.put("category", e.getCategory().name()); m.put("region", e.getRegion());
        m.put("funFacts", e.getFunFacts()); m.put("imageUrl", e.getImageUrl());
        m.put("isPublished", e.getIsPublished()); m.put("createdAt", e.getCreatedAt());
        return m;
    }

    private Map<String, Object> toMap(NurseryParentLearning e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("studentId", e.getStudentId()); m.put("classGroupId", e.getClassGroupId());
        m.put("activityTitle", e.getActivityTitle()); m.put("activityDescription", e.getActivityDescription());
        m.put("activityType", e.getActivityType().name()); m.put("parentName", e.getParentName());
        m.put("completionStatus", e.getCompletionStatus().name()); m.put("completedDate", e.getCompletedDate());
        m.put("notes", e.getNotes()); m.put("createdAt", e.getCreatedAt());
        return m;
    }
}
