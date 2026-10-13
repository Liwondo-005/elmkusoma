package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.learner.domain.LearnerNotification;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.parent.domain.SupportTicket;
import tz.elmkusoma.parent.domain.SupportTicketMessage;
import tz.elmkusoma.parent.repository.SupportTicketMessageRepository;
import tz.elmkusoma.parent.repository.SupportTicketRepository;
import tz.elmkusoma.shared.domain.ContactMessage;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.ContactMessageRepository;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Duplicate prevention and notification privacy on the support and contact threads.
 *
 * <p>Two failure modes are pinned here. The first is ordinary rather than exotic: a parent taps
 * "send" twice, or a mobile connection drops after the server commits but before the response
 * lands, and the retry used to file a second case or append a second identical message. The
 * second is the leak that must never return: a staff-only note reaching the person it is about,
 * whether through the thread or through a notification.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommunicationIdempotencyAndPrivacyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstitutionMembershipRepository membershipRepository;

    @Autowired
    private SupportTicketRepository ticketRepository;

    @Autowired
    private SupportTicketMessageRepository ticketMessageRepository;

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Autowired
    private tz.elmkusoma.shared.repository.ContactMessageReplyRepository replyRepository;

    @Autowired
    private LearnerNotificationRepository notificationRepository;

    private String adminToken;
    private String parentToken;
    private UUID parentId;
    private UUID tenantId;

    @BeforeAll
    void fixtures() {
        tenantId = TestDataSeeder.INSTITUTION_ID;

        String adminEmail = "idem-admin-" + shortId() + "@test.com";
        User admin = userRepository.save(User.builder()
                .email(adminEmail)
                .passwordHash("test-hash")
                .firstName("Idem")
                .lastName("Admin")
                .role(User.Role.ADMIN)
                .institutionId(tenantId)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        membershipRepository.save(InstitutionMembership.builder()
                .userId(admin.getId())
                .institutionId(tenantId)
                .role(InstitutionMembership.Role.ADMIN)
                .isActive(true)
                .build());
        adminToken = TestTokens.userToken(adminEmail);

        String parentEmail = "idem-parent-" + shortId() + "@test.com";
        User parent = userRepository.save(User.builder()
                .email(parentEmail)
                .passwordHash("test-hash")
                .firstName("Idem")
                .lastName("Parent")
                .role(User.Role.PARENT)
                .institutionId(tenantId)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        membershipRepository.save(InstitutionMembership.builder()
                .userId(parent.getId())
                .institutionId(tenantId)
                .role(InstitutionMembership.Role.PARENT)
                .isActive(true)
                .build());
        parentId = parent.getId();
        parentToken = TestTokens.userToken(parentEmail);
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // ── conversation creation ──────────────────────────────────────────────────────────────

    @Test
    void retryingTicketCreationWithTheSameKeyFilesOneCaseNotTwo() throws Exception {
        String key = "create-ticket-" + UUID.randomUUID();

        MvcResult first = mockMvc.perform(post("/v1/my/support/tickets")
                        .header("Authorization", "Bearer " + parentToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Printer offline\",\"description\":"
                                + "\"The office printer will not respond.\",\"category\":\"TECHNICAL\"}"))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/v1/my/support/tickets")
                        .header("Authorization", "Bearer " + parentToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Printer offline\",\"description\":"
                                + "\"The office printer will not respond.\",\"category\":\"TECHNICAL\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String firstId = com.jayway.jsonpath.JsonPath.read(first.getResponse().getContentAsString(), "$.data.id");
        String secondId = com.jayway.jsonpath.JsonPath.read(second.getResponse().getContentAsString(), "$.data.id");

        assertEquals(firstId, secondId,
                "a retry with the same key must return the original ticket, not file a second case");
        assertEquals(1, ticketRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(parentId).stream()
                        .filter(t -> "Printer offline".equals(t.getSubject())).count(),
                "exactly one row may exist for a retried creation");
    }

    @Test
    void reusingAKeyForDifferentContentIsRefusedNotSilentlyReplayed() throws Exception {
        String key = "conflict-create-" + UUID.randomUUID();

        mockMvc.perform(post("/v1/my/support/tickets")
                        .header("Authorization", "Bearer " + parentToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"First subject\",\"description\":\"First body\","
                                + "\"category\":\"GENERAL\"}"))
                .andExpect(status().isOk());

        // Same key, different subject: answering with the first ticket would reply to a
        // question the parent did not ask.
        mockMvc.perform(post("/v1/my/support/tickets")
                        .header("Authorization", "Bearer " + parentToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"A DIFFERENT subject\",\"description\":\"Different body\","
                                + "\"category\":\"GENERAL\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void theSameKeyFromTwoParentsDoesNotCollide() throws Exception {
        String key = "shared-key-" + UUID.randomUUID();
        String otherParentEmail = "idem-parent2-" + shortId() + "@test.com";
        User other = userRepository.save(User.builder()
                .email(otherParentEmail)
                .passwordHash("test-hash")
                .firstName("Other")
                .lastName("Parent")
                .role(User.Role.PARENT)
                .institutionId(tenantId)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        membershipRepository.save(InstitutionMembership.builder()
                .userId(other.getId())
                .institutionId(tenantId)
                .role(InstitutionMembership.Role.PARENT)
                .isActive(true)
                .build());

        mockMvc.perform(post("/v1/my/support/tickets")
                        .header("Authorization", "Bearer " + parentToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"From parent one\",\"description\":\"A\","
                                + "\"category\":\"GENERAL\"}"))
                .andExpect(status().isOk());

        // A key is scoped to its actor. Two people are two requests even with a matching key,
        // so one parent's retry token must never swallow another parent's case.
        mockMvc.perform(post("/v1/my/support/tickets")
                        .header("Authorization", "Bearer " + TestTokens.userToken(otherParentEmail))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"From parent two\",\"description\":\"B\","
                                + "\"category\":\"GENERAL\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void aKeyReusedAcrossOperationsIsRefused() throws Exception {
        String key = "cross-op-" + UUID.randomUUID();

        mockMvc.perform(post("/v1/my/support/tickets")
                        .header("Authorization", "Bearer " + parentToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Cross operation\",\"description\":\"Body\","
                                + "\"category\":\"GENERAL\"}"))
                .andExpect(status().isOk());

        SupportTicket ticket = ticketRepository.save(SupportTicket.builder()
                .userId(parentId)
                .institutionId(tenantId)
                .subject("Unrelated ticket")
                .description("Body")
                .category("GENERAL")
                .priority("NORMAL")
                .status("OPEN")
                .build());

        // Creating a ticket and posting a message are different operations. Reusing a key
        // across them is a client bug, and must not be read as a replay of either.
        mockMvc.perform(post("/v1/my/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + parentToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Same key, different operation\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void aMalformedKeyIsRefusedBeforeItReachesTheDatabase() throws Exception {
        mockMvc.perform(post("/v1/my/support/tickets")
                        .header("Authorization", "Bearer " + parentToken)
                        .header("Idempotency-Key", "has spaces and <tags>")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Malformed key\",\"description\":\"Body\","
                                + "\"category\":\"GENERAL\"}"))
                .andExpect(status().isConflict());
    }

    // ── message submission ─────────────────────────────────────────────────────────────────

    @Test
    void retryingAMessageWithTheSameKeyAppendsItOnce() throws Exception {
        SupportTicket ticket = ticketRepository.save(SupportTicket.builder()
                .userId(parentId)
                .institutionId(tenantId)
                .subject("Thread idempotency")
                .description("Original")
                .category("GENERAL")
                .priority("NORMAL")
                .status("OPEN")
                .build());
        String key = "msg-" + UUID.randomUUID();
        String body = "{\"message\":\"Adding the same detail twice would confuse the thread.\"}";

        mockMvc.perform(post("/v1/my/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + parentToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/my/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + parentToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        long matching = ticketMessageRepository
                .findByTicketIdAndIsDeletedFalseOrderByCreatedAtAsc(ticket.getId()).stream()
                .filter(m -> m.getMessage().startsWith("Adding the same detail twice"))
                .count();
        assertEquals(1, matching, "a retried message must be appended once, not twice");
    }

    @Test
    void retryingAnAdminReplyWithTheSameKeyRecordsItOnce() throws Exception {
        SupportTicket ticket = ticketRepository.save(SupportTicket.builder()
                .userId(parentId)
                .institutionId(tenantId)
                .subject("Admin reply idempotency")
                .description("Original")
                .category("GENERAL")
                .priority("NORMAL")
                .status("OPEN")
                .build());
        String key = "admin-reply-" + UUID.randomUUID();
        String body = "{\"message\":\"We have ordered the part.\",\"internal\":false}";

        mockMvc.perform(post("/v1/platform-admin/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/platform-admin/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        long matching = ticketMessageRepository
                .findByTicketIdAndIsDeletedFalseOrderByCreatedAtAsc(ticket.getId()).stream()
                .filter(m -> m.getMessage().contains("ordered the part"))
                .count();
        assertEquals(1, matching, "a retried admin reply must be recorded once");
    }

    @Test
    void aKeyCannotBeReplayedFromAVisibleReplyIntoAnInternalNote() throws Exception {
        SupportTicket ticket = ticketRepository.save(SupportTicket.builder()
                .userId(parentId)
                .institutionId(tenantId)
                .subject("Reply to note confusion")
                .description("Original")
                .category("GENERAL")
                .priority("NORMAL")
                .status("OPEN")
                .build());
        String key = "reply-then-note-" + UUID.randomUUID();

        mockMvc.perform(post("/v1/platform-admin/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Visible to the parent.\",\"internal\":false}"))
                .andExpect(status().isOk());

        // Same key, same text, different visibility. Treating this as a replay would hand back
        // the public reply; treating it as new would write a note the parent cannot see under a
        // key that already produced something they can. Refusing is the only honest answer.
        mockMvc.perform(post("/v1/platform-admin/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Visible to the parent.\",\"internal\":true}"))
                .andExpect(status().isConflict());
    }

    // ── notification lifecycle ─────────────────────────────────────────────────────────────

    @Test
    void aStaffReplyNotifiesTheRequesterButAnInternalNoteDoesNot() throws Exception {
        SupportTicket ticket = ticketRepository.save(SupportTicket.builder()
                .userId(parentId)
                .institutionId(tenantId)
                .subject("Notification privacy")
                .description("Original")
                .category("GENERAL")
                .priority("NORMAL")
                .status("OPEN")
                .build());

        long before = countNotificationsForTicket(ticket.getId());

        mockMvc.perform(post("/v1/platform-admin/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"A reply the parent should hear about.\",\"internal\":false}"))
                .andExpect(status().isOk());

        long afterReply = countNotificationsForTicket(ticket.getId());
        assertTrue(afterReply > before,
                "a staff reply must reach the ticket's own user through the notification feed");

        mockMvc.perform(post("/v1/platform-admin/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Internal assessment of this parent.\",\"internal\":true}"))
                .andExpect(status().isOk());

        assertEquals(afterReply, countNotificationsForTicket(ticket.getId()),
                "an internal note must never raise a notification naming it");

        // The requester's own read of the thread must not disclose the note either.
        String requesterView = mockMvc.perform(
                        get("/v1/my/support/tickets/" + ticket.getId() + "/messages")
                                .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(requesterView.contains("Internal assessment"),
                "an internal note must never reach the ticket's requester");
        assertTrue(requesterView.contains("A reply the parent should hear about."),
                "the visible reply must still reach the requester");
    }

    @Test
    void theNotificationCarriesASummaryNotTheFullReply() throws Exception {
        SupportTicket ticket = ticketRepository.save(SupportTicket.builder()
                .userId(parentId)
                .institutionId(tenantId)
                .subject("Summary not body")
                .description("Original")
                .category("GENERAL")
                .priority("NORMAL")
                .status("OPEN")
                .build());

        // A long reply may quote account details. The notification list is a coarser surface
        // than the thread, so it carries a bounded preview rather than the whole body.
        String longReply = "x".repeat(400);
        mockMvc.perform(post("/v1/platform-admin/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + longReply + "\",\"internal\":false}"))
                .andExpect(status().isOk());

        List<LearnerNotification> notes = notificationRepository.findAll().stream()
                .filter(n -> ticket.getId().equals(n.getTargetId()))
                .toList();
        assertFalse(notes.isEmpty(), "the reply must produce a notification");
        for (LearnerNotification note : notes) {
            assertTrue(note.getMessage().length() < 200,
                    "a notification must carry a bounded preview, not the whole reply");
        }
    }

    // ── contact enquiries ──────────────────────────────────────────────────────────────────

    @Test
    void retryingAContactReplyWithTheSameKeySendsItOnce() throws Exception {
        ContactMessage enquiry = contactMessageRepository.save(ContactMessage.builder()
                .reference("REF" + shortId().toUpperCase())
                .name("Contact Idem")
                .email("contact.idem." + shortId() + "@example.com")
                .category("GENERAL")
                .subject("Retry safety")
                .message("Original enquiry body.")
                .status("NEW")
                .priority("NORMAL")
                .notificationStatus("NOT_CONFIGURED")
                .notificationAttempts(0)
                .build());
        String key = "enquiry-reply-" + UUID.randomUUID();

        mockMvc.perform(post("/v1/platform-admin/contact-messages/" + enquiry.getId() + "/replies")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Thanks for getting in touch.\",\"internal\":false}"))
                .andExpect(status().isOk());

        MvcResult replay = mockMvc.perform(post(
                        "/v1/platform-admin/contact-messages/" + enquiry.getId() + "/replies")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Thanks for getting in touch.\",\"internal\":false}"))
                .andExpect(status().isOk())
                .andReturn();

        // The replay must return the original reply's identity, not a fresh one.
        assertEquals(1, countEnquiryReplies(enquiry.getId()),
                "a retried enquiry reply must be recorded once");
    }

    @Test
    void anInternalEnquiryNoteIsNeverExposedToTheEnquirer() throws Exception {
        ContactMessage enquiry = contactMessageRepository.save(ContactMessage.builder()
                .reference("REF" + shortId().toUpperCase())
                .name("Contact Privacy")
                .email("contact.privacy." + shortId() + "@example.com")
                .category("COMPLAINT")
                .subject("Privacy check")
                .message("Original enquiry body.")
                .status("NEW")
                .priority("NORMAL")
                .notificationStatus("NOT_CONFIGURED")
                .notificationAttempts(0)
                .build());

        mockMvc.perform(post("/v1/platform-admin/contact-messages/" + enquiry.getId() + "/replies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Internal triage note about this person.\",\"internal\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.internal").value(true))
                .andExpect(jsonPath("$.data.delivered").value(false));

        // The admin's own view includes it.
        String adminView = mockMvc.perform(get(
                        "/v1/platform-admin/contact-messages/" + enquiry.getId() + "/replies")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(adminView.contains("Internal triage note"),
                "an admin must be able to read their own internal note");
    }

    @Test
    void theContactInboxStaysClosedToNonAdmins() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/contact-messages"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/platform-admin/contact-messages")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/platform-admin/contact-messages")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ── helpers ────────────────────────────────────────────────────────────────────────────

    private long countNotificationsForTicket(UUID ticketId) {
        return notificationRepository.findAll().stream()
                .filter(n -> ticketId.equals(n.getTargetId()))
                .count();
    }

    /** Replies recorded against one enquiry, counted from the repository the admin reads. */
    private long countEnquiryReplies(UUID enquiryId) {
        return replyRepository.findAll().stream()
                .filter(r -> enquiryId.equals(r.getContactMessageId()))
                .count();
    }
}
