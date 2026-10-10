package tz.elmkusoma.shared.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.shared.domain.ContactMessage;
import tz.elmkusoma.shared.dto.ContactMessageSummary;
import tz.elmkusoma.shared.repository.ContactMessageReplyRepository;
import tz.elmkusoma.shared.service.ContactMessageService;

import java.util.List;

/**
 * The caller's own contact enquiries.
 *
 * <p>Ownership is decided server-side by matching the account's own email address. There is no
 * path here that accepts a reference or an id from the client, so a signed-in user cannot read
 * someone else's enquiry by guessing either. The response is deliberately narrow - reference,
 * subject, status and counts, never the body, the IP, the notification detail or any internal
 * note.</p>
 */
@RestController
@RequestMapping("/v1/my/contact-messages")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "My Enquiries", description = "A signed-in user's own website contact submissions")
public class MyContactMessagesController {

    private final ContactMessageService contactMessageService;
    private final ContactMessageReplyRepository replyRepository;

    @GetMapping
    @Operation(summary = "List enquiries submitted from this account's email address")
    public ResponseEntity<ApiResponse<List<ContactMessageSummary>>> list(
            @RequestAttribute(value = "userEmail", required = false) String userEmail) {
        List<ContactMessageSummary> summaries = contactMessageService.forUser(userEmail).stream()
                .map(m -> ContactMessageSummary.builder()
                        .reference(m.getReference())
                        .subject(m.getSubject())
                        .category(m.getCategory())
                        .status(m.getStatus())
                        .receivedAt(m.getCreatedAt())
                        .lastUpdateAt(m.getUpdatedAt() != null ? m.getUpdatedAt() : m.getCreatedAt())
                        // Counts only what the enquirer can actually see.
                        .replyCount(replyRepository.findVisibleToRequester(m.getId()).size())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(summaries));
    }
}