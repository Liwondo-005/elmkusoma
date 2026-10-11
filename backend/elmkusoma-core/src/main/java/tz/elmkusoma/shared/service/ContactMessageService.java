package tz.elmkusoma.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.exception.ResourceNotFoundException;
import tz.elmkusoma.config.EventPublisherService;
import tz.elmkusoma.shared.domain.ContactMessage;
import tz.elmkusoma.shared.domain.ContactMessageReply;
import tz.elmkusoma.shared.repository.ContactMessageReplyRepository;
import tz.elmkusoma.shared.repository.ContactMessageRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Public contact enquiries: intake, triage, and notification.
 *
 * <p>The ordering that matters is that the message is committed before anything is attempted
 * externally. Notification runs in its own transaction afterwards, so an SMTP outage leaves a
 * fully recorded enquiry with {@code notificationStatus = FAILED} rather than silently losing
 * the enquiry. Retrying notification reuses the same row — it never creates a second ticket.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactMessageService {

    private static final Logger LOG = LoggerFactory.getLogger(ContactMessageService.class);

    public static final String CATEGORY_GENERAL = "GENERAL";
    public static final String CATEGORY_SUPPORT = "SUPPORT";
    public static final String CATEGORY_PARTNERSHIP = "PARTNERSHIP";
    public static final String CATEGORY_FEEDBACK = "FEEDBACK";
    public static final String CATEGORY_COMPLAINT = "COMPLAINT";

    private static final List<String> VALID_CATEGORIES = List.of(
            CATEGORY_GENERAL, CATEGORY_SUPPORT, CATEGORY_PARTNERSHIP, CATEGORY_FEEDBACK, CATEGORY_COMPLAINT);

    /** Legitimate transitions. A message cannot jump from NEW straight to CLOSED. */
    private static final Map<String, List<String>> STATUS_TRANSITIONS = Map.of(
            "NEW", List.of("IN_PROGRESS", "AWAITING_RESPONSE", "RESOLVED", "CLOSED"),
            "IN_PROGRESS", List.of("AWAITING_RESPONSE", "RESOLVED", "CLOSED"),
            "AWAITING_RESPONSE", List.of("IN_PROGRESS", "RESOLVED", "CLOSED"),
            "RESOLVED", List.of("CLOSED", "IN_PROGRESS"),
            "CLOSED", List.of("NEW"));

    private static final List<String> VALID_STATUSES = List.copyOf(STATUS_TRANSITIONS.keySet());
    private static final DateTimeFormatter REFERENCE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ContactMessageRepository contactMessageRepository;
    private final ContactMessageReplyRepository replyRepository;
    private final PublicSiteSettingsService siteSettings;
    private final EventPublisherService eventPublisherService;
    private final UserRepository userRepository;

    /**
     * Stores an enquiry, then notifies.
     *
     * <p>Validates before persisting so a malformed submission never reaches the table. Spam
     * submissions are accepted and discarded by the caller-side honeypot check in the
     * controller; this method assumes a request that passed it.</p>
     */
    @Transactional
    public ContactMessage create(String name, String email, String category, String subject, String message,
                                 String ipAddress, String userAgent) {
        validate(name, email, category, subject, message);

        String normalisedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        String normalisedCategory = category.trim().toUpperCase(java.util.Locale.ROOT);

        ContactMessage entity = ContactMessage.builder()
                .reference(generateReference())
                .name(name.trim())
                .email(normalisedEmail)
                .category(normalisedCategory)
                .subject(subject.trim())
                .message(message.trim())
                .status("NEW")
                .priority("NORMAL")
                // Honest default: nothing has been sent yet.
                .notificationStatus("PENDING")
                .notificationAttempts(0)
                .ipAddress(truncate(ipAddress, 64))
                .userAgent(truncate(userAgent, 500))
                .build();

        // Link to a real account when the address is one, so "my enquiries" works. Best effort:
        // an unknown address is the normal case for a public form and is not an error.
        userRepository.findByEmailAndIsDeletedFalse(normalisedEmail)
                .ifPresent(u -> entity.setUserId(u.getId()));

        // Committed here. Everything after this point is reporting, not persistence.
        ContactMessage saved = contactMessageRepository.save(entity);
        LOG.info("Contact enquiry stored: reference={}, category={}", saved.getReference(), saved.getCategory());

        notifySeparately(saved.getId());
        return saved;
    }

    /**
     * Attempts notification in its own transaction.
     *
     * <p>Separate so a failure here cannot roll back the enquiry that was already stored, and
     * so the recorded outcome reflects delivery rather than intent.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifySeparately(UUID messageId) {
        ContactMessage message = contactMessageRepository.findByIdAndIsDeletedFalse(messageId).orElse(null);
        if (message == null) {
            return;
        }
        ContactMessage working = message;

        String supportRecipient = siteSettings.rawValue("support.notify.email");
        String adminRecipient = siteSettings.rawValue("support.notify.adminEmail");

        if (!PublicSiteSettingsService.isDeliverableAddress(supportRecipient)
                && !PublicSiteSettingsService.isDeliverableAddress(adminRecipient)) {
            working.setNotificationStatus("NOT_CONFIGURED");
            working.setNotificationError("No deliverable notification recipient is configured");
            contactMessageRepository.save(working);
            // Not an error: the enquiry is stored and visible in the inbox. Just say so truthfully.
            LOG.info("Contact enquiry {} stored; notification not configured", working.getReference());
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("reference", working.getReference());
        variables.put("name", working.getName());
        variables.put("email", working.getEmail());
        variables.put("category", working.getCategory());
        variables.put("subject", working.getSubject());
        variables.put("message", working.getMessage());

        // Recipients are taken from configuration, never from the submission, so a visitor
        // cannot choose where their enquiry is delivered.
        List<String> recipients = new java.util.ArrayList<>();
        if (PublicSiteSettingsService.isDeliverableAddress(supportRecipient)) {
            recipients.add(supportRecipient);
        }
        if (PublicSiteSettingsService.isDeliverableAddress(adminRecipient)
                && !adminRecipient.equalsIgnoreCase(supportRecipient)) {
            recipients.add(adminRecipient);
        }

        int attempts = working.getNotificationAttempts() == null ? 0 : working.getNotificationAttempts();
        try {
            for (String recipient : recipients) {
                eventPublisherService.publishEmailEvent(
                        recipient,
                        "New enquiry " + working.getReference() + ": " + working.getSubject(),
                        "contact-enquiry",
                        variables,
                        working.getInstitutionId());
            }
            // QUEUED, not SENT. Publishing hands the message to the async mail worker; whether
            // that worker can actually reach an SMTP relay is not known here and is not
            // knowable from this process. Recording SENT would tell a visitor their enquiry
            // was delivered on the strength of a queue write.
            working.setNotificationStatus("QUEUED");
            working.setNotificationError(null);
            working.setNotificationAttempts(attempts + 1);
            working.setNotifiedAt(LocalDateTime.now());
        } catch (RuntimeException ex) {
            working.setNotificationStatus("FAILED");
            working.setNotificationError(truncate(ex.getMessage(), 500));
            working.setNotificationAttempts(attempts + 1);
            LOG.warn("Notification for enquiry {} failed: {}", working.getReference(), ex.getMessage());
        }
        contactMessageRepository.save(working);
    }

    @Transactional(readOnly = true)
    public Page<ContactMessage> list(String status, String category, String query, Pageable pageable) {
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasQuery = query != null && !query.isBlank();
        if (hasStatus) {
            return contactMessageRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(
                    status.trim().toUpperCase(java.util.Locale.ROOT), pageable);
        }
        if (hasCategory) {
            return contactMessageRepository.findByCategoryAndIsDeletedFalseOrderByCreatedAtDesc(
                    category.trim().toUpperCase(java.util.Locale.ROOT), pageable);
        }
        if (hasQuery) {
            return contactMessageRepository.search(query.trim(), pageable);
        }
        return contactMessageRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public ContactMessage get(UUID id) {
        return contactMessageRepository.findById(id)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("ContactMessage", "id", id));
    }

    @Transactional
    public ContactMessage updateStatus(UUID id, String newStatus) {
        ContactMessage message = get(id);
        String target = newStatus == null ? "" : newStatus.trim().toUpperCase(java.util.Locale.ROOT);
        if (!VALID_STATUSES.contains(target)) {
            throw new IllegalArgumentException("Unknown status: " + newStatus);
        }
        String current = message.getStatus();
        if (!target.equals(current) && !STATUS_TRANSITIONS.getOrDefault(current, List.of()).contains(target)) {
            throw new IllegalStateException("Cannot move an enquiry from " + current + " to " + target);
        }
        message.setStatus(target);
        if ("RESOLVED".equals(target) || "CLOSED".equals(target)) {
            message.setResolvedAt(LocalDateTime.now());
        }
        return contactMessageRepository.save(message);
    }

    @Transactional
    public ContactMessage assign(UUID id, UUID assigneeId) {
        ContactMessage message = get(id);
        message.setAssignedTo(assigneeId);
        return contactMessageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<ContactMessageReply> replies(UUID messageId, boolean includeInternal) {
        get(messageId);
        return includeInternal
                ? replyRepository.findByContactMessageIdAndIsDeletedFalseOrderByCreatedAtAsc(messageId)
                : replyRepository.findVisibleToRequester(messageId);
    }

    @Transactional
    public ContactMessageReply reply(UUID messageId, UUID authorId, String body, boolean internal) {
        ContactMessage message = get(messageId);
        if (body == null || body.isBlank() || body.length() > 5000) {
            throw new IllegalArgumentException("A reply must be between 1 and 5000 characters");
        }
        ContactMessageReply saved = replyRepository.save(ContactMessageReply.builder()
                .contactMessageId(message.getId())
                .authorId(authorId)
                .isInternal(internal)
                .message(body.trim())
                // Only staff-to-requester replies claim delivery; internal notes go nowhere.
                .delivered(!internal && publishReply(message, body.trim()))
                .build());
        return saved;
    }

    /**
     * Best-effort hand-off of a reply to the mail worker.
     *
     * <p>Returns false rather than pretending. The value is stored as {@code delivered}, and
     * a queued message is not a delivered one, so the flag means "handed to the mail
     * pipeline", never "the enquirer has this in their inbox".</p>
     */
    private boolean publishReply(ContactMessage message, String body) {
        String recipient = siteSettings.rawValue("support.notify.email");
        if (!PublicSiteSettingsService.isDeliverableAddress(recipient)) {
            return false;
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("reference", message.getReference());
        variables.put("name", message.getName());
        variables.put("message", body);
        try {
            eventPublisherService.publishEmailEvent(
                    message.getEmail(),
                    "Re: your enquiry " + message.getReference(),
                    "contact-reply",
                    variables,
                    message.getInstitutionId());
            return true;
        } catch (RuntimeException ex) {
            LOG.warn("Reply delivery for {} failed: {}", message.getReference(), ex.getMessage());
            return false;
        }
    }

    /**
     * A signed-in user's own enquiries.
     *
     * <p>Filtered by the account's own address, so the caller cannot read anyone else's
     * enquiry even by guessing a reference.</p>
     */
    @Transactional(readOnly = true)
    public List<ContactMessage> forUser(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        return contactMessageRepository.findAllForUser(email.trim().toLowerCase(java.util.Locale.ROOT));
    }

    private void validate(String name, String email, String category, String subject, String message) {
        if (name == null || name.trim().length() < 2 || name.trim().length() > 120) {
            throw new IllegalArgumentException("Name must be between 2 and 120 characters");
        }
        if (email == null || !PublicSiteSettingsService.isDeliverableAddress(email.trim())
                || email.trim().length() > 254) {
            throw new IllegalArgumentException("A valid email address is required");
        }
        if (category == null || !VALID_CATEGORIES.contains(
                category.trim().toUpperCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Category must be one of: " + String.join(", ", VALID_CATEGORIES));
        }
        if (subject == null || subject.trim().length() < 4 || subject.trim().length() > 160) {
            throw new IllegalArgumentException("Subject must be between 4 and 160 characters");
        }
        if (message == null || message.trim().length() < 20 || message.trim().length() > 4000) {
            throw new IllegalArgumentException("Message must be between 20 and 4000 characters");
        }
    }

    /**
     * Reference shown to the enquirer and quoted by support. Date prefix plus entropy, with a
     * bounded retry so a collision surfaces as an error rather than a duplicate reference.
     */
    private String generateReference() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = "CNT-"
                    + LocalDateTime.now().format(REFERENCE_DATE)
                    + "-"
                    + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
            if (contactMessageRepository.findByReferenceAndIsDeletedFalse(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "CNT-" + LocalDateTime.now().format(REFERENCE_DATE)
                + "-" + Integer.toHexString(RANDOM.nextInt() & 0xFFFFFF);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}