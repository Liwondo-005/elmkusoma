package tz.elmkusoma.administration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.administration.dto.ContactMessageReplyRequest;
import tz.elmkusoma.administration.dto.ContactMessageViewDto;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.shared.domain.ContactMessage;
import tz.elmkusoma.shared.domain.ContactMessageReply;
import tz.elmkusoma.shared.service.ContactMessageService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Platform Admin support inbox for public contact enquiries.
 *
 * <p>Lives under {@code /v1/platform-admin}, which {@code SecurityConfig} already fences to
 * {@code hasRole('ADMIN')} at the URL level and which this class mirrors with method security,
 * so the two cannot drift. Support staff who need only the inbox are a separate product
 * decision: no role is widened here, and no platform-wide authority is granted to anyone.</p>
 */
@RestController
@RequestMapping("/v1/platform-admin/contact-messages")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Platform Contact Inbox", description = "Triage public enquiries from the website contact form")
public class ContactMessageAdminController {

    private final ContactMessageService contactMessageService;

    @GetMapping
    @Operation(summary = "List contact enquiries, newest first")
    public ResponseEntity<ApiResponse<PageResponse<ContactMessageViewDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q) {
        // Bounded page size: an unbounded size would let one request pull the whole inbox.
        int bounded = Math.max(1, Math.min(size, 100));
        var result = contactMessageService.list(status, category, q,
                PageRequest.of(Math.max(0, page), bounded));
        List<ContactMessageViewDto> content = result.getContent().stream()
                .map(ContactMessageAdminController::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(new PageResponse<>(
                content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one enquiry")
    public ResponseEntity<ApiResponse<ContactMessageViewDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toDto(contactMessageService.get(id))));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Advance an enquiry's status")
    public ResponseEntity<ApiResponse<ContactMessageViewDto>> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success(toDto(contactMessageService.updateStatus(id, status))));
    }

    @PutMapping("/{id}/assign")
    @Operation(summary = "Assign an enquiry to a support agent")
    public ResponseEntity<ApiResponse<ContactMessageViewDto>> assign(
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> body) {
        return ResponseEntity.ok(ApiResponse.success(
                toDto(contactMessageService.assign(id, body.get("assigneeId")))));
    }

    @GetMapping("/{id}/replies")
    @Operation(summary = "Reply thread, internal notes included for admins")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> replies(@PathVariable UUID id) {
        List<ContactMessageReply> replies = contactMessageService.replies(id, true);
        return ResponseEntity.ok(ApiResponse.success(replies.stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "authorId", String.valueOf(r.getAuthorId()),
                        "message", r.getMessage(),
                        "internal", Boolean.TRUE.equals(r.getIsInternal()),
                        "delivered", Boolean.TRUE.equals(r.getDelivered()),
                        "createdAt", String.valueOf(r.getCreatedAt())))
                .collect(Collectors.toList())));
    }

    @PostMapping("/{id}/replies")
    @Operation(summary = "Reply to the enquirer, or record an internal note")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reply(
            @PathVariable UUID id,
            @Valid @RequestBody ContactMessageReplyRequest request,
            HttpServletRequest httpRequest) {
        UUID authorId = actorId(httpRequest);
        ContactMessageReply saved = contactMessageService.reply(
                id, authorId, request.getMessage(),
                Boolean.TRUE.equals(request.getInternal()));
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", saved.getId(),
                "internal", Boolean.TRUE.equals(saved.getIsInternal()),
                // Honest: an internal note is never mailed, and a reply says whether the
                // delivery attempt actually happened.
                "delivered", Boolean.TRUE.equals(saved.getDelivered()))));
    }

    @PostMapping("/{id}/notify")
    @Operation(summary = "Re-attempt notification for an existing enquiry")
    public ResponseEntity<ApiResponse<ContactMessageViewDto>> renotify(@PathVariable UUID id) {
        // Retry the same row. There is deliberately no "create another ticket" path here:
        // re-notifying must never duplicate the enquiry.
        contactMessageService.notifySeparately(id);
        return ResponseEntity.ok(ApiResponse.success(toDto(contactMessageService.get(id))));
    }

    private static UUID actorId(HttpServletRequest request) {
        Object id = request.getAttribute("userId");
        return id instanceof UUID uuid ? uuid : null;
    }

    private static ContactMessageViewDto toDto(ContactMessage m) {
        return ContactMessageViewDto.builder()
                .id(m.getId())
                .reference(m.getReference())
                .name(m.getName())
                .email(m.getEmail())
                .category(m.getCategory())
                .subject(m.getSubject())
                .message(m.getMessage())
                .status(m.getStatus())
                .priority(m.getPriority())
                .assignedTo(m.getAssignedTo())
                .createdAt(m.getCreatedAt())
                .resolvedAt(m.getResolvedAt())
                .notificationStatus(m.getNotificationStatus())
                .notificationError(m.getNotificationError())
                .notificationAttempts(m.getNotificationAttempts())
                .notifiedAt(m.getNotifiedAt())
                .userId(m.getUserId())
                .ipAddress(m.getIpAddress())
                .userAgent(m.getUserAgent())
                .build();
    }
}