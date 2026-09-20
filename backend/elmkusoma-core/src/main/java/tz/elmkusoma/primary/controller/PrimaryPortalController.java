package tz.elmkusoma.primary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.primary.domain.CurriculumTopic;
import tz.elmkusoma.primary.domain.PrimaryLearningCollaboration;
import tz.elmkusoma.primary.dto.*;
import tz.elmkusoma.primary.service.PrimaryPortalService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/primary")
@CrossOrigin(origins = "*")
@Tag(name = "Primary Portal", description = "Primary learner experience - portfolio, badges, streak, curriculum")
public class PrimaryPortalController {

    private final PrimaryPortalService primaryPortalService;

    public PrimaryPortalController(PrimaryPortalService primaryPortalService) {
        this.primaryPortalService = primaryPortalService;
    }

    @GetMapping("/me/teachers")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get teachers teaching the student's classes")
    public ResponseEntity<ApiResponse<List<TeacherInfoResponse>>> getStudentTeachers(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        List<TeacherInfoResponse> teachers = primaryPortalService.getStudentTeachers(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(teachers));
    }

    @GetMapping("/me/portfolio")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get student portfolio items")
    public ResponseEntity<ApiResponse<List<PortfolioItemResponse>>> getStudentPortfolio(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        List<PortfolioItemResponse> items = primaryPortalService.getStudentPortfolio(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @PostMapping("/me/portfolio")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Add a portfolio item")
    public ResponseEntity<ApiResponse<PortfolioItemResponse>> addPortfolioItem(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @Valid @RequestBody PortfolioItemRequest request) {
        PortfolioItemResponse item = primaryPortalService.addPortfolioItem(userId, institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Portfolio item added", item));
    }

    @DeleteMapping("/me/portfolio/{itemId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Delete a portfolio item")
    public ResponseEntity<ApiResponse<Void>> deletePortfolioItem(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID itemId) {
        primaryPortalService.deletePortfolioItem(userId, itemId);
        return ResponseEntity.ok(ApiResponse.success("Portfolio item deleted", null));
    }

    @GetMapping("/me/badges")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get student badges")
    public ResponseEntity<ApiResponse<List<BadgeResponse>>> getStudentBadges(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        List<BadgeResponse> badges = primaryPortalService.getStudentBadges(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(badges));
    }

    @GetMapping("/me/streak")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get student streak info")
    public ResponseEntity<ApiResponse<StreakResponse>> getStudentStreak(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        StreakResponse streak = primaryPortalService.getStudentStreak(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(streak));
    }

    @GetMapping("/curriculum/subject/{subjectId}/topics")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'INSTITUTION_ADMIN')")
    @Operation(summary = "Get curriculum topics for a subject")
    public ResponseEntity<ApiResponse<List<CurriculumTopic>>> getCurriculumTopics(
            @PathVariable UUID subjectId) {
        List<CurriculumTopic> topics = primaryPortalService.getCurriculumTopics(subjectId);
        return ResponseEntity.ok(ApiResponse.success(topics));
    }

    @GetMapping("/me/notifications")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get student notifications")
    public ResponseEntity<ApiResponse<List<?>>> getMyNotifications(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        List<?> notifications = primaryPortalService.getMyNotifications(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/me/learning-profile")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get learning profile")
    public ResponseEntity<ApiResponse<LearningProfileResponse>> getLearningProfile(
            @RequestAttribute("userId") UUID userId) {
        LearningProfileResponse profile = primaryPortalService.getLearningProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/me/learning-profile")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Update learning profile")
    public ResponseEntity<ApiResponse<LearningProfileResponse>> updateLearningProfile(
            @RequestAttribute("userId") UUID userId,
            @Valid @RequestBody LearningProfileRequest request) {
        LearningProfileResponse profile = primaryPortalService.updateLearningProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Learning profile updated", profile));
    }

    @GetMapping("/me/discovery")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get discovery entries")
    public ResponseEntity<ApiResponse<List<DiscoveryEntryResponse>>> getDiscoveryEntries(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        List<DiscoveryEntryResponse> entries = primaryPortalService.getDiscoveryEntries(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    @PostMapping("/me/discovery")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Add a discovery entry")
    public ResponseEntity<ApiResponse<DiscoveryEntryResponse>> addDiscoveryEntry(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @Valid @RequestBody DiscoveryEntryRequest request) {
        DiscoveryEntryResponse entry = primaryPortalService.addDiscoveryEntry(userId, institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Discovery entry added", entry));
    }

    @GetMapping("/me/reading-adventures")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get reading adventures")
    public ResponseEntity<ApiResponse<List<ReadingAdventureResponse>>> getReadingAdventures(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        List<ReadingAdventureResponse> adventures = primaryPortalService.getReadingAdventures(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(adventures));
    }

    @PostMapping("/me/reading-adventures/{id}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Mark reading adventure as complete")
    public ResponseEntity<ApiResponse<ReadingAdventureResponse>> markReadingComplete(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id) {
        ReadingAdventureResponse adventure = primaryPortalService.markReadingComplete(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Reading marked complete", adventure));
    }

    @PostMapping("/me/reading-adventures/{id}/favorite")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Toggle favorite on reading adventure")
    public ResponseEntity<ApiResponse<ReadingAdventureResponse>> toggleFavoriteReading(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id) {
        ReadingAdventureResponse adventure = primaryPortalService.toggleFavoriteReading(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Favorite toggled", adventure));
    }

    @GetMapping("/live-class/{liveClassId}/activities")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    @Operation(summary = "Get live class activities")
    public ResponseEntity<ApiResponse<List<LiveClassActivityResponse>>> getLiveClassActivities(
            @PathVariable UUID liveClassId) {
        List<LiveClassActivityResponse> activities = primaryPortalService.getLiveClassActivities(liveClassId);
        return ResponseEntity.ok(ApiResponse.success(activities));
    }

    @PostMapping("/live-class/{liveClassId}/activities")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Create a live class activity")
    public ResponseEntity<ApiResponse<LiveClassActivityResponse>> createLiveClassActivity(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID liveClassId,
            @Valid @RequestBody LiveClassActivityRequest request) {
        LiveClassActivityResponse activity = primaryPortalService.createLiveClassActivity(liveClassId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Activity created", activity));
    }

    @PostMapping("/live-class/activities/{activityId}/respond")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Respond to a live class activity")
    public ResponseEntity<ApiResponse<LiveClassResponseDTO>> respondToActivity(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID activityId,
            @RequestParam String selectedAnswer) {
        LiveClassResponseDTO response = primaryPortalService.respondToActivity(activityId, userId, selectedAnswer);
        return ResponseEntity.ok(ApiResponse.success("Response recorded", response));
    }

    @GetMapping("/me/evidence")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get learning evidence")
    public ResponseEntity<ApiResponse<List<LearningEvidenceResponse>>> getLearningEvidence(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        List<LearningEvidenceResponse> evidence = primaryPortalService.getLearningEvidence(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(evidence));
    }

    @PostMapping("/me/evidence")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Add learning evidence")
    public ResponseEntity<ApiResponse<LearningEvidenceResponse>> addLearningEvidence(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @Valid @RequestBody LearningEvidenceRequest request) {
        LearningEvidenceResponse evidence = primaryPortalService.addLearningEvidence(userId, institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Evidence added", evidence));
    }

    @GetMapping("/me/passport")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get learning passport")
    public ResponseEntity<ApiResponse<LearningPassportResponse>> getLearningPassport(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        LearningPassportResponse passport = primaryPortalService.getLearningPassport(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(passport));
    }

    @GetMapping("/me/quests")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get quest challenges")
    public ResponseEntity<ApiResponse<List<QuestChallengeResponse>>> getQuestChallenges(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        List<QuestChallengeResponse> quests = primaryPortalService.getQuestChallenges(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(quests));
    }

    @PostMapping("/me/quests/{questId}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Complete a quest challenge")
    public ResponseEntity<ApiResponse<QuestChallengeResponse>> completeQuest(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID questId,
            @RequestParam Integer score) {
        QuestChallengeResponse quest = primaryPortalService.completeQuest(userId, questId, score);
        return ResponseEntity.ok(ApiResponse.success("Quest completed", quest));
    }

    @GetMapping("/me/mistake-lab")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get mistake lab entries")
    public ResponseEntity<ApiResponse<List<MistakeLabEntryResponse>>> getMistakeLabEntries(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        List<MistakeLabEntryResponse> entries = primaryPortalService.getMistakeLabEntries(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    @PostMapping("/me/mistake-lab")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Add mistake lab entry")
    public ResponseEntity<ApiResponse<MistakeLabEntryResponse>> addMistakeLabEntry(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @Valid @RequestBody MistakeLabEntryRequest request) {
        MistakeLabEntryResponse entry = primaryPortalService.addMistakeLabEntry(userId, institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Mistake lab entry added", entry));
    }

    @GetMapping("/me/collaborations")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get learning collaborations")
    public ResponseEntity<ApiResponse<List<PrimaryLearningCollaboration>>> getCollaborations(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        List<PrimaryLearningCollaboration> collaborations = primaryPortalService.getCollaborations(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(collaborations));
    }

    @GetMapping("/me/labs")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get ELMKUSOMA labs for the student")
    public ResponseEntity<ApiResponse<List<ELmkusomaLabResponse>>> getLabs(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        List<ELmkusomaLabResponse> labs = primaryPortalService.getLabs(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(labs));
    }

    @PostMapping("/me/labs/{labId}/attempt")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Attempt an ELMKUSOMA lab")
    public ResponseEntity<ApiResponse<ELmkusomaLabResponse>> attemptLab(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID labId,
            @Valid @RequestBody ELmkusomaLabAttemptRequest request) {
        ELmkusomaLabResponse lab = primaryPortalService.attemptLab(userId, labId, request);
        return ResponseEntity.ok(ApiResponse.success("Lab attempted", lab));
    }

    @GetMapping("/me/speaking")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get speaking activities")
    public ResponseEntity<ApiResponse<List<SpeakingActivityResponse>>> getSpeakingActivities(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        List<SpeakingActivityResponse> activities = primaryPortalService.getSpeakingActivities(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(activities));
    }

    @PostMapping("/me/speaking")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Add a speaking activity")
    public ResponseEntity<ApiResponse<SpeakingActivityResponse>> addSpeakingActivity(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @Valid @RequestBody SpeakingActivityRequest request) {
        SpeakingActivityResponse activity = primaryPortalService.addSpeakingActivity(userId, institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Speaking activity added", activity));
    }

    @PostMapping("/me/speaking/{activityId}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Complete a speaking activity")
    public ResponseEntity<ApiResponse<SpeakingActivityResponse>> completeSpeakingActivity(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID activityId) {
        SpeakingActivityResponse activity = primaryPortalService.completeSpeakingActivity(userId, activityId);
        return ResponseEntity.ok(ApiResponse.success("Speaking activity completed", activity));
    }

    @GetMapping("/me/missions")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get real world missions")
    public ResponseEntity<ApiResponse<List<RealWorldMissionResponse>>> getRealWorldMissions(
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        List<RealWorldMissionResponse> missions = primaryPortalService.getRealWorldMissions(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(missions));
    }

    @PostMapping("/me/missions/{missionId}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Complete a real world mission")
    public ResponseEntity<ApiResponse<RealWorldMissionResponse>> completeMission(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID missionId,
            @RequestParam String evidence) {
        RealWorldMissionResponse mission = primaryPortalService.completeMission(userId, missionId, evidence);
        return ResponseEntity.ok(ApiResponse.success("Mission completed", mission));
    }

    @PostMapping("/me/ai-guide/ask")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Ask AI learning guide a question")
    public ResponseEntity<ApiResponse<Map<String, String>>> askAI(
            @RequestAttribute("userId") UUID userId,
            @RequestBody Map<String, String> request) {
        String question = request.getOrDefault("question", "");
        String answer = primaryPortalService.getAIResponse(userId, question);
        return ResponseEntity.ok(ApiResponse.success(Map.of("answer", answer)));
    }
}
