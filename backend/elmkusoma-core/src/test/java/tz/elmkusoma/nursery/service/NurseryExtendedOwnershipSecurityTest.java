package tz.elmkusoma.nursery.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.common.OwnershipGuard;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nursery.domain.*;
import tz.elmkusoma.nursery.repository.*;
import tz.elmkusoma.nursery.service.impl.NurseryExtendedServiceImpl;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Institution scoping of {@link NurseryExtendedServiceImpl}: class-keyed and
 * student-keyed reads must resolve the owning ClassGroup / Student, and
 * id-based reads and writes must verify the row's own server-set
 * {@code institution_id}, failing closed across tenants (403) before any
 * data is returned or mutated.
 */
@ExtendWith(MockitoExtension.class)
class NurseryExtendedOwnershipSecurityTest {

    @Mock
    private NurseryStoryRepository storyRepo;
    @Mock
    private NurseryDailyQuestRepository dailyQuestRepo;
    @Mock
    private NurseryFeelingsCheckinRepository feelingsRepo;
    @Mock
    private NurseryMissionRepository missionRepo;
    @Mock
    private NurseryTanzaniaDiscoveryRepository tanzaniaRepo;
    @Mock
    private NurseryParentLearningRepository parentLearningRepo;
    @Mock
    private ClassGroupRepository classGroupRepository;
    @Mock
    private StudentRepository studentRepository;

    /**
     * A real instance, spied. OwnershipGuard is a concrete collaborator, not an
     * interface, so {@code @InjectMocks} would otherwise pass null and every
     * ownership check would NPE instead of failing closed.
     */
    @Spy
    private OwnershipGuard ownershipGuard = new OwnershipGuard();

    @InjectMocks
    private NurseryExtendedServiceImpl service;

    private UUID institutionId;
    private UUID otherInstitutionId;
    private UUID classGroupId;
    private UUID studentId;
    private UUID storyId;
    private UUID dailyQuestId;
    private UUID feelingsId;
    private UUID missionId;
    private UUID tanzaniaId;
    private UUID parentLearningId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        otherInstitutionId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        storyId = UUID.randomUUID();
        dailyQuestId = UUID.randomUUID();
        feelingsId = UUID.randomUUID();
        missionId = UUID.randomUUID();
        tanzaniaId = UUID.randomUUID();
        parentLearningId = UUID.randomUUID();
    }

    private ClassGroup classGroupOwnedBy(UUID ownerInstitutionId) {
        return ClassGroup.builder()
                .id(classGroupId)
                .institutionId(ownerInstitutionId)
                .name("Kidu A")
                .build();
    }

    private Student studentOwnedBy(UUID ownerInstitutionId) {
        return Student.builder()
                .id(studentId)
                .institutionId(ownerInstitutionId)
                .admissionNumber("ADM-001")
                .build();
    }

    private NurseryStory storyOwnedBy(UUID ownerInstitutionId) {
        return NurseryStory.builder()
                .id(storyId)
                .institutionId(ownerInstitutionId)
                .classGroupId(classGroupId)
                .title("Jambo Rasimu")
                .content("Once upon a time...")
                .storyType(NurseryStory.StoryType.ANIMAL)
                .isDeleted(false)
                .build();
    }

    private NurseryDailyQuest dailyQuestOwnedBy(UUID ownerInstitutionId) {
        return NurseryDailyQuest.builder()
                .id(dailyQuestId)
                .institutionId(ownerInstitutionId)
                .classGroupId(classGroupId)
                .studentId(studentId)
                .questTitle("Read one page")
                .questType(NurseryDailyQuest.QuestType.READING)
                .isDeleted(false)
                .build();
    }

    private NurseryFeelingsCheckin feelingsCheckinOwnedBy(UUID ownerInstitutionId) {
        return NurseryFeelingsCheckin.builder()
                .id(feelingsId)
                .institutionId(ownerInstitutionId)
                .studentId(studentId)
                .classGroupId(classGroupId)
                .feeling(NurseryFeelingsCheckin.Feeling.HAPPY)
                .checkinDate(LocalDate.of(2026, 1, 5))
                .isDeleted(false)
                .build();
    }

    private NurseryMission missionOwnedBy(UUID ownerInstitutionId) {
        return NurseryMission.builder()
                .id(missionId)
                .institutionId(ownerInstitutionId)
                .classGroupId(classGroupId)
                .studentId(studentId)
                .missionTitle("Tidy your desk")
                .missionType(NurseryMission.MissionType.HOME)
                .isDeleted(false)
                .build();
    }

    private NurseryTanzaniaDiscovery tanzaniaTopicOwnedBy(UUID ownerInstitutionId) {
        return NurseryTanzaniaDiscovery.builder()
                .id(tanzaniaId)
                .institutionId(ownerInstitutionId)
                .classGroupId(classGroupId)
                .topicTitle("Kilimanjaro")
                .category(NurseryTanzaniaDiscovery.DiscoveryCategory.LANDMARKS)
                .isDeleted(false)
                .build();
    }

    private NurseryParentLearning parentLearningOwnedBy(UUID ownerInstitutionId) {
        return NurseryParentLearning.builder()
                .id(parentLearningId)
                .institutionId(ownerInstitutionId)
                .studentId(studentId)
                .classGroupId(classGroupId)
                .activityTitle("Counting fruits")
                .activityType(NurseryParentLearning.ActivityType.COUNTING)
                .isDeleted(false)
                .build();
    }

    // ── getStory ──

    @Test
    void getStory_crossInstitutionStoryIsRejected() {
        when(storyRepo.findById(storyId)).thenReturn(Optional.of(storyOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getStory(institutionId, storyId));
    }

    @Test
    void getStory_unknownStoryIsNotFound() {
        when(storyRepo.findById(storyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getStory(institutionId, storyId));
    }

    @Test
    void getStory_legacyRowWithoutInstitutionFailsClosed() {
        when(storyRepo.findById(storyId)).thenReturn(Optional.of(storyOwnedBy(null)));

        assertThrows(ForbiddenException.class, () -> service.getStory(institutionId, storyId));
    }

    @Test
    void getStory_sameInstitutionStoryIsReadable() {
        when(storyRepo.findById(storyId)).thenReturn(Optional.of(storyOwnedBy(institutionId)));

        Map<String, Object> response = service.getStory(institutionId, storyId);

        assertEquals("Jambo Rasimu", response.get("title"));
    }

    // ── getStoriesByClass ──

    @Test
    void getStoriesByClass_crossInstitutionClassGroupIsRejected() {
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getStoriesByClass(institutionId, classGroupId));

        verify(storyRepo, never()).findByClassGroupIdAndIsDeletedFalse(any());
    }

    @Test
    void getStoriesByClass_unknownClassGroupIsNotFound() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getStoriesByClass(institutionId, classGroupId));

        verify(storyRepo, never()).findByClassGroupIdAndIsDeletedFalse(any());
    }

    @Test
    void getStoriesByClass_sameInstitutionIsAllowed() {
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(institutionId)));
        when(storyRepo.findByClassGroupIdAndIsDeletedFalse(classGroupId)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.getStoriesByClass(institutionId, classGroupId));

        verify(storyRepo).findByClassGroupIdAndIsDeletedFalse(classGroupId);
    }

    // ── updateStory / deleteStory ──

    @Test
    void updateStory_crossInstitutionStoryIsRejectedWithoutSave() {
        when(storyRepo.findById(storyId)).thenReturn(Optional.of(storyOwnedBy(otherInstitutionId)));

        Map<String, Object> request = Map.of("title", "Hijacked");

        assertThrows(ForbiddenException.class, () -> service.updateStory(institutionId, storyId, request));

        verify(storyRepo, never()).save(any());
    }

    @Test
    void deleteStory_crossInstitutionStoryIsRejectedWithoutSave() {
        when(storyRepo.findById(storyId)).thenReturn(Optional.of(storyOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.deleteStory(institutionId, storyId));

        verify(storyRepo, never()).save(any());
    }

    @Test
    void deleteStory_sameInstitutionSoftDeletes() {
        NurseryStory story = storyOwnedBy(institutionId);
        when(storyRepo.findById(storyId)).thenReturn(Optional.of(story));
        when(storyRepo.save(any(NurseryStory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteStory(institutionId, storyId);

        assertTrue(story.getIsDeleted());
        verify(storyRepo).save(story);
    }

    // ── daily quests ──

    @Test
    void getDailyQuestsByClass_crossInstitutionClassGroupIsRejected() {
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getDailyQuestsByClass(institutionId, classGroupId));

        verify(dailyQuestRepo, never()).findByClassGroupIdAndIsDeletedFalse(any());
    }

    @Test
    void getDailyQuestsByStudent_crossInstitutionStudentIsRejected() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getDailyQuestsByStudent(institutionId, studentId));

        verify(dailyQuestRepo, never()).findByStudentIdAndIsDeletedFalse(any());
    }

    @Test
    void getDailyQuestsByStudent_unknownStudentIsNotFound() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getDailyQuestsByStudent(institutionId, studentId));

        verify(dailyQuestRepo, never()).findByStudentIdAndIsDeletedFalse(any());
    }

    @Test
    void getDailyQuest_sameInstitutionDailyQuestIsReadable() {
        when(dailyQuestRepo.findById(dailyQuestId)).thenReturn(Optional.of(dailyQuestOwnedBy(institutionId)));

        Map<String, Object> response = service.getDailyQuest(institutionId, dailyQuestId);

        assertEquals("Read one page", response.get("questTitle"));
    }

    @Test
    void updateDailyQuest_crossInstitutionDailyQuestIsRejectedWithoutSave() {
        when(dailyQuestRepo.findById(dailyQuestId)).thenReturn(Optional.of(dailyQuestOwnedBy(otherInstitutionId)));

        Map<String, Object> request = Map.of("questTitle", "Hijacked");

        assertThrows(ForbiddenException.class, () -> service.updateDailyQuest(institutionId, dailyQuestId, request));

        verify(dailyQuestRepo, never()).save(any());
    }

    @Test
    void deleteDailyQuest_crossInstitutionDailyQuestIsRejectedWithoutSave() {
        when(dailyQuestRepo.findById(dailyQuestId)).thenReturn(Optional.of(dailyQuestOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.deleteDailyQuest(institutionId, dailyQuestId));

        verify(dailyQuestRepo, never()).save(any());
    }

    // ── feelings check-ins ──

    @Test
    void getFeelingsCheckinsByStudent_crossInstitutionStudentIsRejected() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getFeelingsCheckinsByStudent(institutionId, studentId));

        verify(feelingsRepo, never()).findByStudentIdAndIsDeletedFalse(any());
    }

    @Test
    void getFeelingsCheckinsByStudent_unknownStudentIsNotFound() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getFeelingsCheckinsByStudent(institutionId, studentId));

        verify(feelingsRepo, never()).findByStudentIdAndIsDeletedFalse(any());
    }

    @Test
    void deleteFeelingsCheckin_crossInstitutionFeelingsCheckinIsRejectedWithoutSave() {
        when(feelingsRepo.findById(feelingsId)).thenReturn(Optional.of(feelingsCheckinOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.deleteFeelingsCheckin(institutionId, feelingsId));

        verify(feelingsRepo, never()).save(any());
    }

    // ── missions ──

    @Test
    void getMissionsByClass_crossInstitutionClassGroupIsRejected() {
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getMissionsByClass(institutionId, classGroupId));

        verify(missionRepo, never()).findByClassGroupIdAndIsDeletedFalse(any());
    }

    @Test
    void getMissionsByStudent_crossInstitutionStudentIsRejected() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getMissionsByStudent(institutionId, studentId));

        verify(missionRepo, never()).findByStudentIdAndIsDeletedFalse(any());
    }

    @Test
    void updateMission_crossInstitutionMissionIsRejectedWithoutSave() {
        when(missionRepo.findById(missionId)).thenReturn(Optional.of(missionOwnedBy(otherInstitutionId)));

        Map<String, Object> request = Map.of("missionTitle", "Hijacked");

        assertThrows(ForbiddenException.class, () -> service.updateMission(institutionId, missionId, request));

        verify(missionRepo, never()).save(any());
    }

    // ── Tanzania discovery ──

    @Test
    void getTanzaniaTopics_crossInstitutionClassGroupIsRejected() {
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getTanzaniaTopics(institutionId, classGroupId));

        verify(tanzaniaRepo, never()).findByClassGroupIdAndIsDeletedFalse(any());
    }

    @Test
    void getTanzaniaByCategory_scopesQueryToCallerInstitutionNotUnscopedCategoryFinder() {
        when(tanzaniaRepo.findByCategoryAndInstitutionIdAndIsDeletedFalse(
                NurseryTanzaniaDiscovery.DiscoveryCategory.LANDMARKS, institutionId))
                .thenReturn(List.of(tanzaniaTopicOwnedBy(institutionId)));

        List<?> result = service.getTanzaniaByCategory(institutionId, "LANDMARKS");

        assertEquals(1, result.size());
        verify(tanzaniaRepo).findByCategoryAndInstitutionIdAndIsDeletedFalse(
                NurseryTanzaniaDiscovery.DiscoveryCategory.LANDMARKS, institutionId);
        verify(tanzaniaRepo, never()).findByCategoryAndIsDeletedFalse(any());
    }

    @Test
    void deleteTanzaniaTopic_crossInstitutionTanzaniaTopicIsRejectedWithoutSave() {
        when(tanzaniaRepo.findById(tanzaniaId)).thenReturn(Optional.of(tanzaniaTopicOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.deleteTanzaniaTopic(institutionId, tanzaniaId));

        verify(tanzaniaRepo, never()).save(any());
    }

    @Test
    void deleteTanzaniaTopic_unknownTanzaniaTopicIsNotFound() {
        when(tanzaniaRepo.findById(tanzaniaId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteTanzaniaTopic(institutionId, tanzaniaId));

        verify(tanzaniaRepo, never()).save(any());
    }

    // ── parent learning ──

    @Test
    void getParentLearningByStudent_crossInstitutionStudentIsRejected() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(studentOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getParentLearningByStudent(institutionId, studentId));

        verify(parentLearningRepo, never()).findByStudentIdAndIsDeletedFalse(any());
    }

    @Test
    void updateParentLearning_crossInstitutionParentLearningIsRejectedWithoutSave() {
        when(parentLearningRepo.findById(parentLearningId))
                .thenReturn(Optional.of(parentLearningOwnedBy(otherInstitutionId)));

        Map<String, Object> request = Map.of("notes", "Hijacked");

        assertThrows(ForbiddenException.class,
                () -> service.updateParentLearning(institutionId, parentLearningId, request));

        verify(parentLearningRepo, never()).save(any());
    }

    @Test
    void deleteParentLearning_sameInstitutionSoftDeletes() {
        NurseryParentLearning activity = parentLearningOwnedBy(institutionId);
        when(parentLearningRepo.findById(parentLearningId)).thenReturn(Optional.of(activity));
        when(parentLearningRepo.save(any(NurseryParentLearning.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteParentLearning(institutionId, parentLearningId);

        assertTrue(activity.getIsDeleted());
        verify(parentLearningRepo).save(activity);
    }
}
