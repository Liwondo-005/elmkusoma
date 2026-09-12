package tz.elmkusoma.teacher.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.dto.AnnouncementResponse;
import tz.elmkusoma.course.dto.CreateAnnouncementRequest;
import tz.elmkusoma.course.service.AnnouncementService;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/teachers/me/announcements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Teacher Announcements", description = "Manage announcements for teachers")
public class TeacherAnnouncementController {

    private final AnnouncementService announcementService;
    private final TeacherRepository teacherRepository;

    @GetMapping
    @Operation(summary = "List my announcements")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getMyAnnouncements(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        List<AnnouncementResponse> announcements = announcementService.getTeacherAnnouncements(userId);
        return ResponseEntity.ok(ApiResponse.success(announcements));
    }

    @PostMapping
    @Operation(summary = "Create an announcement")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestBody CreateAnnouncementRequest request) {
        AnnouncementResponse created = announcementService.createAnnouncement(userId, institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Announcement created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an announcement")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> updateAnnouncement(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id,
            @RequestBody CreateAnnouncementRequest request) {
        AnnouncementResponse updated = announcementService.updateAnnouncement(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Announcement updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an announcement")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id) {
        announcementService.deleteAnnouncement(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Announcement deleted successfully", null));
    }
}
