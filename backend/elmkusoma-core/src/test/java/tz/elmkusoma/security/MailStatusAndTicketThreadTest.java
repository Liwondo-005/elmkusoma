package tz.elmkusoma.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.util.ReflectionTestUtils;
import tz.elmkusoma.administration.domain.IntegrationStatus;
import tz.elmkusoma.administration.repository.IntegrationStatusRepository;
import tz.elmkusoma.administration.repository.PlatformConfigRepository;
import tz.elmkusoma.administration.service.PlatformIntegrationService;
import tz.elmkusoma.parent.domain.SupportTicket;
import tz.elmkusoma.parent.repository.SupportTicketRepository;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SMTP status honesty, and the support-ticket conversation endpoints.
 *
 * <p>The SMTP assertions exist because a {@code localhost} default in application.yml used to
 * report email as CONFIGURED. That is the failure mode this pins: an operator looking at the
 * integration screen would conclude production email works because a development sink name was
 * present in a property file.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MailStatusAndTicketThreadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlatformIntegrationService integrationService;

    @Autowired
    private PlatformConfigRepository configRepository;

    @Autowired
    private IntegrationStatusRepository integrationStatusRepository;

    @Autowired
    private SupportTicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstitutionMembershipRepository membershipRepository;

    private String adminToken;
    private String requesterEmail;
    private UUID tenantId;

    @BeforeAll
    void fixtures() {
        tenantId = TestDataSeeder.INSTITUTION_ID;
        String email = "mail-status-admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        User admin = userRepository.save(User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("Mail")
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
        adminToken = TestTokens.userToken(email);

        // probe() resolves the integration row before probing it, so the 'email' key must
        // exist for any status assertion to run at all.
        if (integrationStatusRepository.findByIntegrationKeyAndIsDeletedFalse("email").isEmpty()) {
            integrationStatusRepository.save(IntegrationStatus.builder()
                    .integrationKey("email")
                    .displayName("Email (SMTP)")
                    .category("COMMUNICATION")
                    .connectionStatus("UNKNOWN")
                    .configStatus("UNKNOWN")
                    .failureCount(0)
                    .build());
        }

        // A requester inside the test schema. The dev-database parent account does not exist
        // here, so ticket ownership had nothing real to attach to.
        String requester = "thread-requester-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        User requesterUser = userRepository.save(User.builder()
                .email(requester)
                .passwordHash("test-hash")
                .firstName("Ticket")
                .lastName("Requester")
                .role(User.Role.PARENT)
                .institutionId(tenantId)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        membershipRepository.save(InstitutionMembership.builder()
                .userId(requesterUser.getId())
                .institutionId(tenantId)
                .role(InstitutionMembership.Role.PARENT)
                .isActive(true)
                .build());
        requesterEmail = requester;
    }

    @AfterEach
    void restoreMailProperties() {
        // Each test mutates injected fields; leaving one behind would colour the next.
        ReflectionTestUtils.setField(integrationService, "mailHost", "localhost");
        ReflectionTestUtils.setField(integrationService, "mailPort", 25);
        ReflectionTestUtils.setField(integrationService, "mailUsername", "");
        ReflectionTestUtils.setField(integrationService, "mailPassword", "");
    }

    // ── SMTP status ─────────────────────────────────────────────────────────────────────

    @Test
    void aLocalhostDefaultIsNotReportedAsProductionReadyEmail() {
        ReflectionTestUtils.setField(integrationService, "mailHost", "localhost");
        ReflectionTestUtils.setField(integrationService, "mailPort", 1025);
        ReflectionTestUtils.setField(integrationService, "mailUsername", "");
        ReflectionTestUtils.setField(integrationService, "mailPassword", "");

        var response = integrationService.probe("email");

        assertEquals("NOT_CONFIGURED", response.getConfigStatus(),
                "the development mail sink must not read as production-ready");
        assertNotNull(response.getProbeDetail());
        assertTrue(response.getProbeDetail().contains("development default"),
                "the reason should be stated: " + response.getProbeDetail());
    }

    @Test
    void otherDevelopmentSinksAreAlsoTreatedAsNotConfigured() {
        for (String sink : new String[]{"127.0.0.1", "mailhog", "mailpit", "::1"}) {
            ReflectionTestUtils.setField(integrationService, "mailHost", sink);
            assertEquals("NOT_CONFIGURED", integrationService.probe("email").getConfigStatus(),
                    sink + " is a development sink, not a mail service");
        }
    }

    @Test
    void aBlankHostIsNotConfigured() {
        ReflectionTestUtils.setField(integrationService, "mailHost", "");
        var response = integrationService.probe("email");
        assertEquals("NOT_CONFIGURED", response.getConfigStatus());
        assertTrue(response.getProbeDetail().contains("No SMTP host configured"));
    }

    /**
     * A real host with no credentials is deliberately not called CONFIGURED: reachability was
     * never checked, and an unauthenticated relay is usually a misconfiguration.
     */
    @Test
    void aRealHostWithoutCredentialsIsConfiguredButUnverified() {
        ReflectionTestUtils.setField(integrationService, "mailHost", "smtp.example.com");
        ReflectionTestUtils.setField(integrationService, "mailUsername", "");
        ReflectionTestUtils.setField(integrationService, "mailPassword", "");

        var response = integrationService.probe("email");
        assertEquals("CONFIGURED_UNVERIFIED", response.getConfigStatus());
    }

    /**
     * Even when the port answers, the probe may not claim more than it checked. It opens a
     * socket and reads the 220 banner; it never authenticates and never sends a message, so
     * HEALTHY would be a claim it cannot support.
     */
    @Test
    void aReachableHostIsNeverReportedAsFullyHealthy() {
        ReflectionTestUtils.setField(integrationService, "mailHost", "smtp.example.com");
        ReflectionTestUtils.setField(integrationService, "mailPort", 25);
        ReflectionTestUtils.setField(integrationService, "mailUsername", "user@example.com");
        ReflectionTestUtils.setField(integrationService, "mailPassword", "secret-value");

        var response = integrationService.probe("email");

        assertFalse("HEALTHY".equals(response.getConfigStatus()),
                "a TCP banner check cannot support a HEALTHY claim");
        assertTrue("CONFIGURED_UNVERIFIED".equals(response.getConfigStatus())
                        || "FAILED".equals(response.getConfigStatus()),
                "expected CONFIGURED_UNVERIFIED or FAILED, got " + response.getConfigStatus());
        assertFalse(response.getProbeDetail().contains("secret-value"),
                "the SMTP password must never appear in a probe detail");
    }

    @Test
    void probeDetailsNeverLeakTheMailPassword() {
        ReflectionTestUtils.setField(integrationService, "mailHost", "smtp.example.com");
        ReflectionTestUtils.setField(integrationService, "mailUsername", "user@example.com");
        ReflectionTestUtils.setField(integrationService, "mailPassword", "hunter2-do-not-leak");

        var response = integrationService.probe("email");
        assertFalse(response.getProbeDetail().contains("hunter2-do-not-leak"));
        assertTrue(response.getProbeDetail().contains("credentials present")
                        || response.getProbeDetail().contains("no username/password"),
                "credential presence may be reported; the value may not");
    }

    // ── support ticket conversation ─────────────────────────────────────────────────────

    @Test
    void supportTicketThreadIsNotReachableAnonymously() throws Exception {
        SupportTicket ticket = seedTicket("anon thread");
        mockMvc.perform(get("/v1/platform-admin/support/tickets/" + ticket.getId() + "/messages"))
                .andExpect(status().isForbidden());
    }

    @Test
    void supportTicketThreadIsNotReachableByANonAdmin() throws Exception {
        SupportTicket ticket = seedTicket("teacher thread");
        mockMvc.perform(get("/v1/platform-admin/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanPostAndReadATicketThread() throws Exception {
        SupportTicket ticket = seedTicket("round trip");

        mockMvc.perform(post("/v1/platform-admin/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"We are looking into this.\",\"internal\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.internal").value(false));

        MvcResult read = mockMvc.perform(get("/v1/platform-admin/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(read.getResponse().getContentAsString().contains("We are looking into this."));
    }

    @Test
    void adminInternalNoteIsRecordedButNotShownToTheRequester() throws Exception {
        SupportTicket ticket = seedTicket("internal note");
        mockMvc.perform(post("/v1/platform-admin/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Suspected abuse, do not reply yet.\",\"internal\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.internal").value(true));

        // The admin view includes it.
        String adminView = mockMvc.perform(get("/v1/platform-admin/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getContentAsString();
        assertTrue(adminView.contains("Suspected abuse"),
                "an admin must be able to see their own internal note");

        // The ticket's requester must not. This is the assertion that pins the leak that
        // previously existed: ParentSupportService returned every message on the table.
        String requesterView = mockMvc.perform(
                        get("/v1/my/support/tickets/" + ticket.getId() + "/messages")
                                .header("Authorization", "Bearer " + TestTokens.userToken(requesterEmail)))
                .andReturn().getResponse().getContentAsString();
        assertFalse(requesterView.contains("Suspected abuse"),
                "an internal staff note must never reach the ticket's requester");
    }

    @Test
    void readingAnUnknownTicketThreadIs404() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/support/tickets/"
                        + UUID.fromString("00000000-0000-0000-0000-00000000dead") + "/messages")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void anEmptyReplyIsRejected() throws Exception {
        SupportTicket ticket = seedTicket("empty reply");
        mockMvc.perform(post("/v1/platform-admin/support/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    // ── contact honesty ────────────────────────────────────────────────────────────────

    /**
     * With no recipient configured the receipt must not imply a notification happened, and the
     * stored row must survive regardless - a failed mail path may never lose an enquiry.
     */
    @Test
    void contactEnquirySurvivesAndReportsHonestNotificationState() throws Exception {
        configRepository.findByConfigKeyAndIsDeletedFalse("support.notify.email")
                .ifPresent(entry -> {
                    entry.setConfigValue("");
                    configRepository.save(entry);
                });

        MvcResult posted = mockMvc.perform(post("/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mail Status\",\"email\":\"mail.status@example.com\","
                                + "\"category\":\"GENERAL\",\"subject\":\"Delivery honesty check\","
                                + "\"message\":\"Confirming that a stored enquiry reports its true state.\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String body = posted.getResponse().getContentAsString();
        assertEquals("NOT_CONFIGURED", JsonPath.read(body, "$.data.notificationStatus"));

        String reference = JsonPath.read(body, "$.data.reference");
        var inbox = mockMvc.perform(get("/v1/platform-admin/contact-messages")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(inbox.contains(reference),
                "an enquiry must be retrievable by staff even though nobody was notified");
    }

    @Test
    void contactInboxNeverExposesEnquiriesToAnonymousCallers() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/contact-messages"))
                .andExpect(status().isForbidden());
    }

    // ── helpers ───────────────────────────────────────────────────────────────────────

    private SupportTicket seedTicket(String subject) {
        return ticketRepository.save(SupportTicket.builder()
                .userId(userRepository.findByEmailAndIsDeletedFalse(requesterEmail).orElseThrow().getId())
                .subject("E2E " + subject + " " + UUID.randomUUID())
                .description("Thread access test fixture")
                .category("GENERAL")
                .priority("NORMAL")
                .status("OPEN")
                .isDeleted(false)
                .build());
    }
}