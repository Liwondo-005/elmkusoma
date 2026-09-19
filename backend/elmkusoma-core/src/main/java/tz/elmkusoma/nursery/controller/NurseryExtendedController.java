package tz.elmkusoma.nursery.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.nursery.service.NurseryExtendedService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/nursery/extended")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN','STUDENT','PARENT')")
@Tag(name = "Nursery Extended", description = "Stories, Daily Quests, Feelings, Missions, Tanzania, Parent Learning")
public class NurseryExtendedController {

    private final NurseryExtendedService service;

    // ==================== STORIES ====================

    @PostMapping("/stories")
    @Operation(summary = "Create a story")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createStory(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Story created", service.createStory(institutionId, userId, request)));
    }

    @GetMapping("/stories")
    @Operation(summary = "Get stories by class")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStoriesByClass(@RequestParam UUID classId) {
        return ResponseEntity.ok(ApiResponse.success(service.getStoriesByClass(classId)));
    }

    @GetMapping("/stories/{id}")
    @Operation(summary = "Get story by ID")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getStory(id)));
    }

    @PutMapping("/stories/{id}")
    @Operation(summary = "Update story")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStory(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.success("Story updated", service.updateStory(id, request)));
    }

    @DeleteMapping("/stories/{id}")
    @Operation(summary = "Delete story")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteStory(@PathVariable UUID id) {
        service.deleteStory(id);
        return ResponseEntity.ok(ApiResponse.success("Story deleted", null));
    }

    // ==================== DAILY QUESTS ====================

    @PostMapping("/daily-quests")
    @Operation(summary = "Create a daily quest")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createDailyQuest(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Daily quest created", service.createDailyQuest(institutionId, userId, request)));
    }

    @GetMapping("/daily-quests")
    @Operation(summary = "Get daily quests by class")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDailyQuestsByClass(@RequestParam UUID classId) {
        return ResponseEntity.ok(ApiResponse.success(service.getDailyQuestsByClass(classId)));
    }

    @GetMapping("/daily-quests/student/{studentId}")
    @Operation(summary = "Get daily quests by student")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDailyQuestsByStudent(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(service.getDailyQuestsByStudent(studentId)));
    }

    @GetMapping("/daily-quests/{id}")
    @Operation(summary = "Get daily quest by ID")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDailyQuest(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getDailyQuest(id)));
    }

    @PutMapping("/daily-quests/{id}")
    @Operation(summary = "Update daily quest")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateDailyQuest(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.success("Daily quest updated", service.updateDailyQuest(id, request)));
    }

    @DeleteMapping("/daily-quests/{id}")
    @Operation(summary = "Delete daily quest")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteDailyQuest(@PathVariable UUID id) {
        service.deleteDailyQuest(id);
        return ResponseEntity.ok(ApiResponse.success("Daily quest deleted", null));
    }

    // ==================== FEELINGS CHECK-IN ====================

    @PostMapping("/feelings")
    @Operation(summary = "Create a feelings check-in")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createFeelingsCheckin(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Feelings check-in recorded", service.createFeelingsCheckin(institutionId, userId, request)));
    }

    @GetMapping("/feelings/student/{studentId}")
    @Operation(summary = "Get feelings by student")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFeelingsByStudent(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(service.getFeelingsCheckinsByStudent(studentId)));
    }

    @DeleteMapping("/feelings/{id}")
    @Operation(summary = "Delete feelings check-in")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteFeelingsCheckin(@PathVariable UUID id) {
        service.deleteFeelingsCheckin(id);
        return ResponseEntity.ok(ApiResponse.success("Feelings check-in deleted", null));
    }

    // ==================== MISSIONS ====================

    @PostMapping("/missions")
    @Operation(summary = "Create a mission")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createMission(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Mission created", service.createMission(institutionId, userId, request)));
    }

    @GetMapping("/missions")
    @Operation(summary = "Get missions by class")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMissionsByClass(@RequestParam UUID classId) {
        return ResponseEntity.ok(ApiResponse.success(service.getMissionsByClass(classId)));
    }

    @GetMapping("/missions/student/{studentId}")
    @Operation(summary = "Get missions by student")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMissionsByStudent(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(service.getMissionsByStudent(studentId)));
    }

    @PutMapping("/missions/{id}")
    @Operation(summary = "Update mission")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMission(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.success("Mission updated", service.updateMission(id, request)));
    }

    @DeleteMapping("/missions/{id}")
    @Operation(summary = "Delete mission")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteMission(@PathVariable UUID id) {
        service.deleteMission(id);
        return ResponseEntity.ok(ApiResponse.success("Mission deleted", null));
    }

    // ==================== TANZANIA DISCOVERY ====================

    @PostMapping("/tanzania")
    @Operation(summary = "Create a Tanzania discovery topic")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTanzania(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tanzania topic created", service.createTanzaniaTopic(institutionId, userId, request)));
    }

    @GetMapping("/tanzania")
    @Operation(summary = "Get Tanzania topics by class")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTanzaniaByClass(@RequestParam UUID classId) {
        return ResponseEntity.ok(ApiResponse.success(service.getTanzaniaTopics(classId)));
    }

    @GetMapping("/tanzania/category/{category}")
    @Operation(summary = "Get Tanzania topics by category")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTanzaniaByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.success(service.getTanzaniaByCategory(category)));
    }

    @DeleteMapping("/tanzania/{id}")
    @Operation(summary = "Delete Tanzania topic")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteTanzania(@PathVariable UUID id) {
        service.deleteTanzaniaTopic(id);
        return ResponseEntity.ok(ApiResponse.success("Tanzania topic deleted", null));
    }

    // ==================== PARENT LEARNING ====================

    @PostMapping("/parent-learning")
    @Operation(summary = "Create parent learning activity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createParentLearning(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Parent learning created", service.createParentLearning(institutionId, userId, request)));
    }

    @GetMapping("/parent-learning/student/{studentId}")
    @Operation(summary = "Get parent learning by student")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getParentLearningByStudent(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(service.getParentLearningByStudent(studentId)));
    }

    @PutMapping("/parent-learning/{id}")
    @Operation(summary = "Update parent learning")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateParentLearning(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.success("Parent learning updated", service.updateParentLearning(id, request)));
    }

    @DeleteMapping("/parent-learning/{id}")
    @Operation(summary = "Delete parent learning")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteParentLearning(@PathVariable UUID id) {
        service.deleteParentLearning(id);
        return ResponseEntity.ok(ApiResponse.success("Parent learning deleted", null));
    }
}
