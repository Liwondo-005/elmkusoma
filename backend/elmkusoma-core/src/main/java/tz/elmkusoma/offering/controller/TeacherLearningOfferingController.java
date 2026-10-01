package tz.elmkusoma.offering.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.offering.dto.LearningOfferingRequest;
import tz.elmkusoma.offering.dto.LearningOfferingResponse;
import tz.elmkusoma.offering.service.LearningOfferingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/teachers/me/offerings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Teacher Learning Offerings", description = "Teachers create and manage their own learning offerings")
public class TeacherLearningOfferingController {

    private final LearningOfferingService offeringService;

    @PostMapping
    @Operation(summary = "Create a learning offering owned by the authenticated teacher")
    public ResponseEntity<ApiResponse<LearningOfferingResponse>> create(
            @Valid @RequestBody LearningOfferingRequest request,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        LearningOfferingResponse response = offeringService.create(request, userId, institutionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Learning offering created", response));
    }

    @GetMapping
    @Operation(summary = "List learning offerings owned by the authenticated teacher")
    public ResponseEntity<ApiResponse<List<LearningOfferingResponse>>> listMine(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(offeringService.listMine(userId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an owned learning offering")
    public ResponseEntity<ApiResponse<LearningOfferingResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody LearningOfferingRequest request,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        return ResponseEntity.ok(ApiResponse.success("Learning offering updated",
                offeringService.update(id, request, userId, institutionId)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an owned learning offering")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        offeringService.delete(id, userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Learning offering deleted", null));
    }
}
