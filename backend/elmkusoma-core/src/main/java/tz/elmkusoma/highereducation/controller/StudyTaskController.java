package tz.elmkusoma.highereducation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.dto.StudyTaskDTO;
import tz.elmkusoma.highereducation.service.StudyTaskService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/study-tasks")
@RequiredArgsConstructor
public class StudyTaskController {

    private final StudyTaskService studyTaskService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudyTaskDTO>>> listStudyTasks(
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<StudyTaskDTO> tasks = studyTaskService.getStudyTasks(institutionId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudyTaskDTO>> getStudyTask(@PathVariable UUID id) {
        StudyTaskDTO task = studyTaskService.getStudyTask(id);
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StudyTaskDTO>> createStudyTask(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody StudyTaskDTO dto) {
        dto.setInstitutionId(institutionId);
        StudyTaskDTO created = studyTaskService.createStudyTask(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Study task created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudyTaskDTO>> updateStudyTask(
            @PathVariable UUID id,
            @Valid @RequestBody StudyTaskDTO dto) {
        StudyTaskDTO updated = studyTaskService.updateStudyTask(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Study task updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudyTask(@PathVariable UUID id) {
        studyTaskService.deleteStudyTask(id);
        return ResponseEntity.ok(ApiResponse.success("Study task deleted", null));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<StudyTaskDTO>>> getStudentTasks(
            @PathVariable UUID studentId,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<StudyTaskDTO> tasks = studyTaskService.getStudentTasks(studentId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/student/{studentId}/today")
    public ResponseEntity<ApiResponse<List<StudyTaskDTO>>> getTodayTasks(@PathVariable UUID studentId) {
        List<StudyTaskDTO> tasks = studyTaskService.getTodayTasks(studentId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/student/{studentId}/week")
    public ResponseEntity<ApiResponse<List<StudyTaskDTO>>> getWeekTasks(@PathVariable UUID studentId) {
        List<StudyTaskDTO> tasks = studyTaskService.getWeekTasks(studentId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<StudyTaskDTO>> completeStudyTask(@PathVariable UUID id) {
        StudyTaskDTO completed = studyTaskService.completeStudyTask(id);
        return ResponseEntity.ok(ApiResponse.success("Study task completed", completed));
    }
}
