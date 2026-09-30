package tz.elmkusoma.offering.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.offering.dto.LearningOfferingResponse;
import tz.elmkusoma.offering.service.LearningOfferingService;

import java.util.UUID;

@RestController
@RequestMapping("/v1/learner/offerings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OTHER_LEARNER', 'TEACHER', 'STUDENT', 'INSTITUTION_ADMIN', 'ADMIN')")
@Tag(name = "Learner Learning Offerings", description = "Discover published learning offerings from teachers")
public class LearnerLearningOfferingController {

    private final LearningOfferingService offeringService;

    @GetMapping
    @Operation(summary = "Discover published learning offerings visible to the caller")
    public ResponseEntity<ApiResponse<Page<LearningOfferingResponse>>> discover(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EducationLevel educationLevel,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) UUID ownerId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        return ResponseEntity.ok(ApiResponse.success(
                offeringService.discover(q, educationLevel, subjectId, ownerId, userId, institutionId, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a learning offering visible to the caller")
    public ResponseEntity<ApiResponse<LearningOfferingResponse>> detail(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        return ResponseEntity.ok(ApiResponse.success(offeringService.getVisible(id, userId, institutionId)));
    }
}
