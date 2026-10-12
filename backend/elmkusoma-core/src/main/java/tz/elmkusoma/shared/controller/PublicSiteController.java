package tz.elmkusoma.shared.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.config.security.RateLimitService;
import tz.elmkusoma.shared.domain.ContactMessage;
import tz.elmkusoma.shared.domain.LegalDocumentVersion;
import tz.elmkusoma.shared.dto.ContactReceipt;
import tz.elmkusoma.shared.dto.ContactRequest;
import tz.elmkusoma.shared.dto.LegalDocumentView;
import tz.elmkusoma.shared.service.ContactMessageService;
import tz.elmkusoma.shared.service.LegalContentService;
import tz.elmkusoma.shared.service.PublicSiteSettingsService;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Anonymous public surface for the website: configured contact details, the contact form, and
 * published legal documents.
 *
 * <p>Sits alongside the existing {@code PublicPlatformController} on {@code /v1/public}. This
 * one is kept separate because the two have different rules: statistics and course discovery are
 * safe reads, while a contact submission is an unauthenticated write and therefore carries its
 * own rate limit and spam controls.</p>
 */
@RestController
@RequestMapping("/v1/public")
@RequiredArgsConstructor
@Tag(name = "Public Site", description = "Public contact details, contact submissions and published legal documents")
public class PublicSiteController {

    /**
     * Below the floor a person could plausibly type, and low enough that a scripted flood is
     * cut off quickly. Keyed on the caller's address, so one noisy network cannot lock out
     * everyone behind it, and enforced in the service rather than by a filter so the limit and
     * the code that stores the message cannot drift apart.
     */
    private static final int CONTACT_LIMIT = 10;
    private static final long CONTACT_WINDOW_MILLIS = TimeUnit.HOURS.toMillis(1);

    /**
     * A submission that lands faster than this was not typed by a person. Deliberately short:
     * the threshold only has to exclude instant scripted posts, and being generous costs
     * nothing but catching more bots.
     */
    private static final long MIN_FILL_MILLIS = 3_000;

    private final PublicSiteSettingsService siteSettingsService;
    private final ContactMessageService contactMessageService;
    private final LegalContentService legalContentService;
    private final RateLimitService rateLimitService;

    @GetMapping("/site-settings")
    @Operation(summary = "Approved public site settings (contact details and social links)")
    public ResponseEntity<ApiResponse<Map<String, String>>> siteSettings() {
        return ResponseEntity.ok(ApiResponse.success(siteSettingsService.publicSettings()));
    }

    @PostMapping("/contact")
    @Operation(summary = "Submit a public contact enquiry")
    public ResponseEntity<ApiResponse<ContactReceipt>> submitContact(
            @Valid @RequestBody ContactRequest request,
            HttpServletRequest httpRequest) {

        String ip = clientAddress(httpRequest);

        // Spam traps first and cheap: a filled honeypot is discarded without touching the
        // database, and is answered exactly like a real submission so a bot learns nothing.
        if (looksAutomated(request, ip)) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                    "Message received", discardedReceipt()));
        }

        ContactMessage stored = contactMessageService.create(
                request.getName(), request.getEmail(), request.getCategory(),
                request.getSubject(), request.getMessage(), ip, httpRequest.getHeader("User-Agent"));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Message received",
                ContactReceipt.builder()
                        .reference(stored.getReference())
                        .status(stored.getStatus())
                        .receivedAt(stored.getCreatedAt())
                        // Reported as it actually is. The form must not claim a message was
                        // emailed to staff when no recipient is configured or delivery failed.
                        .notificationStatus(stored.getNotificationStatus())
                        .build()));
    }

    @GetMapping("/legal/{type}")
    @Operation(summary = "The published version of a legal document")
    public ResponseEntity<ApiResponse<LegalDocumentView>> legalDocument(@PathVariable String type) {
        LegalDocumentVersion published = legalContentService.publishedVersion(type);
        if (published == null) {
            // 404 rather than an empty body: the caller can then fall back honestly instead of
            // rendering a blank legal page that looks like "there are no terms".
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No published " + type + " document"));
        }
        String normalised = type.trim().toUpperCase(java.util.Locale.ROOT);
        return ResponseEntity.ok(ApiResponse.success(
                LegalDocumentView.builder()
                        .type(normalised)
                        .title(published.getTitle())
                        .version(published.getVersion())
                        .content(published.getContent())
                        .effectiveDate(published.getEffectiveDate())
                        .publishedAt(published.getPublishedAt())
                        .build()));
    }

    private boolean looksAutomated(ContactRequest request, String ip) {
        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            return true;
        }
        if (request.getFormStartedAt() != null) {
            long elapsed = System.currentTimeMillis() - request.getFormStartedAt();
            if (elapsed >= 0 && elapsed < MIN_FILL_MILLIS) {
                return true;
            }
        }
        if (!rateLimitService.allow("public-contact:" + ip, CONTACT_LIMIT, CONTACT_WINDOW_MILLIS)) {
            throw new tz.elmkusoma.exception.RateLimitExceededException();
        }
        return false;
    }

    private static ContactReceipt discardedReceipt() {
        return ContactReceipt.builder()
                .reference("received")
                .status("NEW")
                .receivedAt(java.time.LocalDateTime.now())
                .notificationStatus("NOT_CONFIGURED")
                .build();
    }

    private static String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // First hop is the original client when a trusted proxy appends to the chain.
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}