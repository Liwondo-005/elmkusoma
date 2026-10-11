package tz.elmkusoma.administration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.administration.dto.LegalDocumentUpsertRequest;
import tz.elmkusoma.administration.dto.LegalDocumentAdminView;
import tz.elmkusoma.administration.dto.LegalRevertRequest;
import tz.elmkusoma.administration.dto.LegalVersionView;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.shared.domain.LegalDocument;
import tz.elmkusoma.shared.domain.LegalDocumentVersion;
import tz.elmkusoma.shared.service.LegalContentService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Platform Admin management of legal documents.
 *
 * <p>Editors work on a draft and publication is a separate, explicit act, so an in-progress
 * rewrite is never what the public sees. Published versions are archived, never edited - see
 * {@link LegalContentService} for why reverting means "new draft", not "roll back".</p>
 *
 * <p>Platform Admin authors the text; this API makes no claim that the content is legally
 * reviewed. Publication is a business/legal sign-off recorded in the version's {@code
 * publishedBy}.</p>
 */
@RestController
@RequestMapping("/v1/platform-admin/legal-documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Platform Legal Content", description = "Draft, publish and version Terms, Privacy and support policies")
public class LegalDocumentAdminController {

    private final LegalContentService legalContentService;

    @GetMapping
    @Operation(summary = "List legal documents with their publication state")
    public ResponseEntity<ApiResponse<List<LegalDocumentAdminView>>> list(
            @RequestParam(required = false) String type) {
        List<LegalDocument> documents = legalContentService.listAll().stream()
                .filter(d -> type == null || type.isBlank()
                        || d.getDocType().equalsIgnoreCase(type.trim()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(documents.stream()
                .map(LegalDocumentAdminController::toAdminView)
                .toList()));
    }

    @GetMapping("/types")
    @Operation(summary = "Document types this platform supports")
    public ResponseEntity<ApiResponse<Map<String, Object>>> types() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "types", LegalContentService.validTypes(),
                "contentFormat", "plain-text",
                "contentFormatNote",
                "Legal content is stored and served as plain text and is never interpreted as HTML.")));
    }

    @PostMapping
    @Operation(summary = "Create a new legal document draft")
    public ResponseEntity<ApiResponse<LegalDocumentAdminView>> create(
            @Valid @RequestBody LegalDocumentUpsertRequest request,
            HttpServletRequest httpRequest) {
        LegalDocument created = legalContentService.createDraft(
                request.getType(), request.getTitle(), request.getContent(),
                request.getEffectiveDate(), actor(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(toAdminView(created)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit the working copy. Does not change what the public sees.")
    public ResponseEntity<ApiResponse<LegalDocumentAdminView>> update(
            @PathVariable UUID id,
            @Valid @RequestBody LegalDocumentUpsertRequest request,
            HttpServletRequest httpRequest) {
        LegalDocument updated = legalContentService.updateDraft(
                id, request.getTitle(), request.getContent(),
                request.getEffectiveDate(), actor(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(toAdminView(updated)));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish the working copy as the next version")
    public ResponseEntity<ApiResponse<LegalDocumentAdminView>> publish(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        LegalDocument published = legalContentService.publish(id, actor(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(toAdminView(published)));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Published history, newest first")
    public ResponseEntity<ApiResponse<List<LegalVersionView>>> versions(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                legalContentService.history(id).stream()
                        .map(LegalDocumentAdminController::toVersionView)
                        .toList()));
    }

    @PostMapping("/{id}/revert")
    @Operation(summary = "Restore an earlier version into the draft (history is preserved)")
    public ResponseEntity<ApiResponse<LegalDocumentAdminView>> revert(
            @PathVariable UUID id,
            @Valid @RequestBody LegalRevertRequest request,
            HttpServletRequest httpRequest) {
        LegalDocument draft = legalContentService.revert(id, request.getVersion(), actor(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(toAdminView(draft)));
    }

    private static String actor(HttpServletRequest request) {
        Object email = request.getAttribute("userEmail");
        return email != null ? email.toString() : "platform-admin";
    }

    private static LegalDocumentAdminView toAdminView(LegalDocument d) {
        return LegalDocumentAdminView.builder()
                .id(d.getId())
                .type(d.getDocType())
                .title(d.getTitle())
                .content(d.getContent())
                .effectiveDate(d.getEffectiveDate())
                .draftVersion(d.getVersion())
                .publishedVersion(d.getPublishedVersion())
                .lastModifiedBy(d.getLastModifiedBy())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private static LegalVersionView toVersionView(LegalDocumentVersion v) {
        return LegalVersionView.builder()
                .id(v.getId())
                .version(v.getVersion())
                .title(v.getTitle())
                .content(v.getContent())
                .effectiveDate(v.getEffectiveDate())
                .publishedAt(v.getPublishedAt())
                .publishedBy(v.getPublishedBy())
                .build();
    }
}