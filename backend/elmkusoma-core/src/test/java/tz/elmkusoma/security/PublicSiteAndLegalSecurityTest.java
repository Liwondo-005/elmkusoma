package tz.elmkusoma.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import tz.elmkusoma.administration.domain.PlatformConfigEntry;
import tz.elmkusoma.administration.repository.PlatformConfigRepository;
import tz.elmkusoma.shared.domain.ContactMessage;
import tz.elmkusoma.shared.repository.ContactMessageRepository;
import tz.elmkusoma.shared.repository.LegalDocumentRepository;
import tz.elmkusoma.shared.repository.LegalDocumentVersionRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Public contact, site settings and legal content.
 *
 * <p>Three boundaries drive most of this suite.</p>
 *
 * <ul>
 *   <li><b>Nothing private reaches an anonymous caller.</b> The public settings endpoint reads
 *       a table that also holds internal notification recipients, so the test asserts those
 *       keys are absent from the response body rather than trusting the allow-list.</li>
 *   <li><b>A stored enquiry is not a delivered notification.</b> With no recipient configured,
 *       the receipt must say so. This is the difference between a contact form that works and
 *       one that lies.</li>
 *   <li><b>Only the published legal version is public.</b> A draft edit must not be readable
 *       anonymously, and publishing must not destroy the previous version.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicSiteAndLegalSecurityTest {

    private static final String TERMS_BODY = """
            Terms and Conditions

            1. Acceptance
            By using ELMKUSOMA you agree to these terms.

            2. Changes
            We may update these terms and will publish the date of the change.
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlatformConfigRepository configRepository;

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Autowired
    private LegalDocumentRepository legalDocumentRepository;

    @Autowired
    private LegalDocumentVersionRepository legalVersionRepository;

    private String platformToken;
    private UUID tenantId;

    @Autowired
    private tz.elmkusoma.shared.repository.UserRepository userRepository;

    @Autowired
    private tz.elmkusoma.shared.repository.InstitutionMembershipRepository membershipRepository;

    @BeforeAll
    void fixtures() {
        tenantId = TestDataSeeder.INSTITUTION_ID;
        // TestDataSeeder's admin@elmkusoma.tz is an INSTITUTION_ADMIN, which every
        // hasRole('ADMIN') fence here would reject. A real platform ADMIN is required.
        String email = "public-site-admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        var admin = userRepository.save(tz.elmkusoma.shared.domain.User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("Public")
                .lastName("Admin")
                .role(tz.elmkusoma.shared.domain.User.Role.ADMIN)
                .institutionId(tenantId)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        membershipRepository.save(tz.elmkusoma.shared.domain.InstitutionMembership.builder()
                .userId(admin.getId())
                .institutionId(tenantId)
                .role(tz.elmkusoma.shared.domain.InstitutionMembership.Role.ADMIN)
                .isActive(true)
                .build());
        platformToken = TestTokens.userToken(email);
    }

    @BeforeEach
    void seedSettings() {
        setConfig("public.contact.email", "info@elmkusoma.co.tz");
        setConfig("public.contact.phone", "");
        setConfig("public.contact.whatsapp", "");
        setConfig("public.social.facebook", "");
        setConfig("support.notify.email", "");
        setConfig("support.notify.adminEmail", "");
    }

    // ── public settings ────────────────────────────────────────────────────────────────

    @Test
    void publicSettings_exposeOnlyApprovedNonSensitiveKeys() throws Exception {
        // An internal recipient exists and must never appear on the anonymous endpoint.
        setConfig("support.notify.email", "support-internal@elmkusoma.co.tz");
        setConfig("ops.backup.dir", "/var/backups/secret-path");
        setConfig("policy.charge.enabled", "true");

        mockMvc.perform(get("/v1/public/site-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data['public.contact.email']").value("info@elmkusoma.co.tz"));

        String body = mockMvc.perform(get("/v1/public/site-settings"))
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains("support-internal"),
                "an internal support recipient must never be exposed publicly");
        assertFalse(body.contains("support.notify.email"),
                "notification routing is not public configuration");
        assertFalse(body.contains("secret-path"),
                "an ops setting must never leak through the public settings endpoint");
        assertFalse(body.contains("policy.charge"),
                "policy flags are not public site settings");
    }

    /** A value that is not configured is omitted, so the client can hide it rather than render a blank. */
    @Test
    void publicSettings_omitUnconfiguredValuesEntirely() throws Exception {
        setConfig("public.contact.phone", "");
        setConfig("public.social.facebook", "");

        String body = mockMvc.perform(get("/v1/public/site-settings"))
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains("public.contact.phone"),
                "an unset phone number must be absent, not present-and-empty");
        assertFalse(body.contains("public.social.facebook"),
                "an unset social link must be absent so no placeholder icon is rendered");
    }

    @Test
    void publicSettings_rejectUnsafeUrls() throws Exception {
        setConfig("public.social.facebook", "javascript:alert(document.cookie)");

        String body = mockMvc.perform(get("/v1/public/site-settings"))
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains("javascript:"),
                "a javascript: URL must never reach the browser");
        assertFalse(body.contains("public.social.facebook"),
                "an unsafe social URL is omitted entirely rather than sanitised into something else");
    }

    @Test
    void publicSettings_acceptAConfiguredHttpUrl() throws Exception {
        setConfig("public.social.facebook", "https://facebook.com/elmkusoma");
        mockMvc.perform(get("/v1/public/site-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data['public.social.facebook']").value("https://facebook.com/elmkusoma"));
    }

    // ── contact form ──────────────────────────────────────────────────────────────────

    @Test
    void contactForm_storesTheEnquiryAndReturnsAReference() throws Exception {
        long before = contactMessageRepository.count();
        MvcResult result = mockMvc.perform(post("/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validContact("Ada", "ada@example.com")))
                .andExpect(status().isCreated())
                .andReturn();

        String reference = JsonPath.read(result.getResponse().getContentAsString(), "$.data.reference");
        assertNotNull(reference);
        assertTrue(reference.startsWith("CNT-"), "reference should be quotable by support: " + reference);

        assertEquals(before + 1, contactMessageRepository.count(), "the enquiry must be persisted");
        ContactMessage stored = contactMessageRepository
                .findByReferenceAndIsDeletedFalse(reference).orElseThrow();
        assertEquals("ada@example.com", stored.getEmail());
        assertEquals("NEW", stored.getStatus());
    }

    /**
     * The critical honesty test. With no notification recipient configured, the receipt must say
     * the message was stored but nobody was notified. Reporting "sent" here would be the exact
     * lie that loses an enquiry a school is relying on.
     */
    @Test
    void contactForm_reportsNotConfiguredRatherThanClaimingDelivery() throws Exception {
        setConfig("support.notify.email", "");
        setConfig("support.notify.adminEmail", "");

        mockMvc.perform(post("/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validContact("Grace", "grace@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.notificationStatus").value("NOT_CONFIGURED"))
                .andExpect(jsonPath("$.data.reference").exists());
    }

    /**
     * The counterpart to the test above: once a recipient is configured, the enquiry must
     * actually attempt notification.
     *
     * <p>Asserted as "not NOT_CONFIGURED" rather than a fixed QUEUED, because the outcome of
     * handing the message to the async mail worker depends on whether RabbitMQ happens to be
     * reachable from the test run, which is not what this test is about. What it is about is
     * that the recipient was resolved and the delivery path was entered. QUEUED and FAILED both
     * prove that; NOT_CONFIGURED proves the opposite.</p>
     *
     * <p>SENT is excluded deliberately: the process can only prove it queued the message.</p>
     */
    @Test
    void contactForm_attemptsDeliveryWhenRecipientIsConfigured() throws Exception {
        setConfig("support.notify.email", "info@elmkusoma.co.tz");
        setConfig("support.notify.adminEmail", "");

        MvcResult created = mockMvc.perform(post("/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validContact("Habari", "habari@example.com")))
                .andExpect(status().isCreated())
                .andReturn();

        String reference = JsonPath.read(created.getResponse().getContentAsString(), "$.data.reference");
        String status = contactMessageRepository.findByReferenceAndIsDeletedFalse(reference)
                .orElseThrow().getNotificationStatus();

        assertNotEquals("NOT_CONFIGURED", status,
                "a configured recipient must be resolved and delivery attempted");
        assertNotEquals("SENT", status,
                "queueing a message cannot prove it was delivered");
        assertTrue("QUEUED".equals(status) || "FAILED".equals(status),
                "expected the notification path to have been entered, got: " + status);
    }

    @Test
    void contactForm_doesNotEchoTheMessageBodyBack() throws Exception {
        String body = mockMvc.perform(post("/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validContact("Alan", "alan@example.com")))
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains("I would like to ask about the science curriculum revision process."),
                "the stored message must not be reflected to the submitter");
    }

    @Test
    void contactForm_rejectsInvalidInput() throws Exception {
        // Too short
        mockMvc.perform(post("/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"email\":\"a@example.com\",\"category\":\"GENERAL\","
                                + "\"subject\":\"hi\",\"message\":\"short\"}"))
                .andExpect(status().isBadRequest());

        // Not an email
        mockMvc.perform(post("/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validContact("Alan", "not-an-email")))
                .andExpect(status().isBadRequest());

        // Unknown category
        mockMvc.perform(post("/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alan\",\"email\":\"alan@example.com\",\"category\":\"BOGUS\","
                                + "\"subject\":\"A valid subject\",\"message\":\""
                                + "I would like to ask about the science curriculum revision process.\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contactForm_silentlyDiscardsHoneypotSubmissions() throws Exception {
        long before = contactMessageRepository.count();
        String body = "{"
                + "\"name\":\"Spam Bot\",\"email\":\"bot@example.com\",\"category\":\"GENERAL\","
                + "\"subject\":\"Cheap links here\",\"message\":\"" + "x".repeat(50) + "\","
                + "\"website\":\"http://spam.example\"}";

        // Answered exactly like a success so the bot learns nothing from the status code.
        mockMvc.perform(post("/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        assertEquals(before, contactMessageRepository.count(),
                "a submission that filled the honeypot must not be stored");
    }

    @Test
    void contactForm_discardsSubmissionsThatArrivedTooFastToBeTyped() throws Exception {
        long before = contactMessageRepository.count();
        String body = "{\"name\":\"Fast Bot\",\"email\":\"fast@example.com\",\"category\":\"GENERAL\","
                + "\"subject\":\"Instant submission\",\"message\":\"" + "y".repeat(60) + "\","
                + "\"formStartedAt\":" + System.currentTimeMillis() + "}";

        mockMvc.perform(post("/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        assertEquals(before, contactMessageRepository.count(),
                "a submission faster than a human could type must be discarded");
    }

    // ── contact inbox authorisation ───────────────────────────────────────────────────

    @Test
    void contactInbox_isNotReachableAnonymously() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/contact-messages"))
                .andExpect(status().isForbidden());
    }

    @Test
    void contactInbox_isNotReachableByANonAdmin() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/contact-messages")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void contactInbox_isReachableByPlatformAdmin() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/contact-messages")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk());
    }

    /** Retrying notification must act on the same row, never create a second ticket. */
    @Test
    void renotify_doesNotCreateASecondEnquiry() throws Exception {
        MvcResult created = mockMvc.perform(post("/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validContact("Retry", "retry@example.com")))
                .andExpect(status().isCreated())
                .andReturn();
        String reference = JsonPath.read(created.getResponse().getContentAsString(), "$.data.reference");
        UUID id = contactMessageRepository.findByReferenceAndIsDeletedFalse(reference).orElseThrow().getId();

        long before = contactMessageRepository.count();
        mockMvc.perform(post("/v1/platform-admin/contact-messages/" + id + "/notify")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk());

        assertEquals(before, contactMessageRepository.count(),
                "re-notifying must update the existing enquiry, not create another one");
        assertNotNull(contactMessageRepository.findById(id).orElseThrow().getNotificationStatus());
    }

    @Test
    void internalNotes_areNeverVisibleToTheEnquirer() throws Exception {
        MvcResult created = mockMvc.perform(post("/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validContact("Note", "note@example.com")))
                .andReturn();
        String reference = JsonPath.read(created.getResponse().getContentAsString(), "$.data.reference");
        UUID id = contactMessageRepository.findByReferenceAndIsDeletedFalse(reference).orElseThrow().getId();

        mockMvc.perform(post("/v1/platform-admin/contact-messages/" + id + "/replies")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Staff-only triage note\",\"internal\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.delivered").value(false));

        String adminView = mockMvc.perform(get("/v1/platform-admin/contact-messages/" + id + "/replies")
                        .header("Authorization", "Bearer " + platformToken))
                .andReturn().getResponse().getContentAsString();
        assertTrue(adminView.contains("Staff-only triage note"),
                "admins must still be able to see internal notes");

        String learnerView = mockMvc.perform(get("/v1/my/contact-messages")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken()))
                .andReturn().getResponse().getContentAsString();
        assertFalse(learnerView.contains("Staff-only triage note"),
                "an internal note must not leak into another user's enquiry list");
    }

    // ── legal content ─────────────────────────────────────────────────────────────────

    @Test
    void legalDocument_is404UntilPublished() throws Exception {
        createTermsDraft(TERMS_BODY);
        mockMvc.perform(get("/v1/public/legal/TERMS"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publishingMakesTheDocumentPublicAndArchivesThePreviousVersion() throws Exception {
        UUID id = createTermsDraft(TERMS_BODY);
        mockMvc.perform(post("/v1/platform-admin/legal-documents/" + id + "/publish")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publishedVersion").value(1));

        mockMvc.perform(get("/v1/public/legal/TERMS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.containsString("Acceptance")));

        // Edit the draft. The public copy must not move.
        String updated = TERMS_BODY + "\n3. Fees\nFees are as published on the course page.";
        mockMvc.perform(put("/v1/platform-admin/legal-documents/" + id)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("TERMS", "Terms and Conditions", updated)))
                .andExpect(status().isOk());

        String publicBody = mockMvc.perform(get("/v1/public/legal/TERMS"))
                .andReturn().getResponse().getContentAsString();
        assertFalse(publicBody.contains("Fees are as published"),
                "an unpublished draft edit must not be visible to the public");

        // Publish again, then confirm v1 was archived rather than overwritten.
        mockMvc.perform(post("/v1/platform-admin/legal-documents/" + id + "/publish")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publishedVersion").value(2));

        assertEquals(2, legalVersionRepository.findByLegalDocumentIdAndIsDeletedFalseOrderByVersionDesc(id).size());
        assertTrue(legalVersionRepository
                .findByLegalDocumentIdAndVersionAndIsDeletedFalse(id, 1).isPresent(),
                "v1 must still exist after v2 is published");
    }

    @Test
    void republishingUnchangedContentIsRefused() throws Exception {
        UUID id = createTermsDraft(TERMS_BODY);
        mockMvc.perform(post("/v1/platform-admin/legal-documents/" + id + "/publish")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/platform-admin/legal-documents/" + id + "/publish")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().is4xxClientError());

        assertEquals(1, legalVersionRepository.findByLegalDocumentIdAndIsDeletedFalseOrderByVersionDesc(id).size(),
                "a double publish must not burn a version number");
    }

    @Test
    void revertRestoresOldTextIntoTheDraftWithoutRewritingHistory() throws Exception {
        UUID id = createTermsDraft(TERMS_BODY);
        mockMvc.perform(post("/v1/platform-admin/legal-documents/" + id + "/publish")
                        .header("Authorization", "Bearer " + platformToken)).andExpect(status().isOk());

        String updated = TERMS_BODY + "\n3. Fees\nA clause that later turns out to be wrong.";
        mockMvc.perform(put("/v1/platform-admin/legal-documents/" + id)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("TERMS", "Terms and Conditions", updated))).andExpect(status().isOk());
        mockMvc.perform(post("/v1/platform-admin/legal-documents/" + id + "/publish")
                        .header("Authorization", "Bearer " + platformToken)).andExpect(status().isOk());

        mockMvc.perform(post("/v1/platform-admin/legal-documents/" + id + "/revert")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("turns out to be wrong"))));

        // v2 is still the live version: reverting prepares a draft, it does not roll back.
        mockMvc.perform(get("/v1/public/legal/TERMS"))
                .andExpect(jsonPath("$.data.version").value(2));
        assertEquals(2, legalVersionRepository.findByLegalDocumentIdAndIsDeletedFalseOrderByVersionDesc(id).size(),
                "history must still hold both versions after a revert");
    }

    @Test
    void legalContent_isNotWritableOrReadableByNonAdmins() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/legal-documents"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/platform-admin/legal-documents")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("TERMS", "Terms", TERMS_BODY)))
                .andExpect(status().isForbidden());
    }

    @Test
    void legalContent_rejectsAnUnknownDocumentType() throws Exception {
        mockMvc.perform(get("/v1/public/legal/NOT_A_DOCUMENT"))
                .andExpect(status().is4xxClientError());
    }

    // ── helpers ───────────────────────────────────────────────────────────────────────

    private UUID createTermsDraft(String content) throws Exception {
        // The class shares one database, so clear any TERMS document a previous test created.
        // Only one document per type is allowed by design, so this is the test adapting to the
        // rule rather than the rule being relaxed.
        legalDocumentRepository.findByDocTypeAndIsDeletedFalse("TERMS").ifPresent(existing -> {
            legalVersionRepository.deleteAll(
                    legalVersionRepository.findByLegalDocumentIdAndIsDeletedFalseOrderByVersionDesc(existing.getId()));
            legalVersionRepository.flush();
            legalDocumentRepository.delete(existing);
            legalDocumentRepository.flush();
        });

        MvcResult created = mockMvc.perform(post("/v1/platform-admin/legal-documents")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("TERMS", "Terms and Conditions", content)))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(JsonPath.read(created.getResponse().getContentAsString(), "$.data.id"));
    }

    private static String validContact(String name, String email) {
        return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"category\":\"GENERAL\","
                + "\"subject\":\"Question about the science curriculum\","
                + "\"message\":\"I would like to ask about the science curriculum revision process.\"}";
    }

    private static String json(String type, String title, String content) {
        return "{\"type\":\"" + type + "\",\"title\":\"" + title + "\",\"content\":\""
                + content.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"")
                + "\"}";
    }

    private void setConfig(String key, String value) {
        PlatformConfigEntry entry = configRepository.findByConfigKeyAndIsDeletedFalse(key)
                .orElseGet(() -> PlatformConfigEntry.builder().configKey(key).build());
        entry.setConfigValue(value);
        configRepository.save(entry);
    }
}