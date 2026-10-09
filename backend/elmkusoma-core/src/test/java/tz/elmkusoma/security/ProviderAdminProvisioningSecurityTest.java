package tz.elmkusoma.security;

import com.jayway.jsonpath.JsonPath;
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
import tz.elmkusoma.administration.domain.InstitutionInvitation;
import tz.elmkusoma.administration.repository.InstitutionInvitationRepository;
import tz.elmkusoma.nfe.provider.domain.EducationProvider;
import tz.elmkusoma.nfe.provider.repository.EducationProviderRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Provider Admin provisioning: who may create a PROVIDER_ADMIN account, and what an
 * invitation is allowed to change when it is redeemed.
 *
 * <p>Two distinct defects motivated this suite, and they are the reason the invitation path
 * needs this much attention at all:</p>
 *
 * <ol>
 *   <li>The invite role was an unvalidated string on an endpoint that INSTITUTION_ADMIN and
 *       PROVIDER_ADMIN could both call, so a tenant administrator could mint a PROVIDER_ADMIN
 *       (or ADMIN, or NATIONAL_ADMIN) invitation.</li>
 *   <li>Redeeming wrote a <em>global</em> {@code users.role} and overwrote the redeemer's
 *       existing global {@code institution_id}. The second made an institution administrator
 *       able to move an existing user from another tenant into their own, using a token they
 *       generated and redeemed themselves.</li>
 * </ol>
 *
 * <p>Both were unreachable in production only because invitation creation itself was broken -
 * Lombok's {@code @Builder} discarded the {@code status}/{@code isDeleted} field initialisers,
 * so every insert failed the NOT NULL constraint. Fixing the insert without fixing the
 * authorization would have turned a 409 into an escalation, so the insert regression is pinned
 * here too.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProviderAdminProvisioningSecurityTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private InstitutionMembershipRepository membershipRepository;

    @Autowired
    private InstitutionInvitationRepository invitationRepository;

    @Autowired
    private EducationProviderRepository providerRepository;

    private UUID tenantA;
    private UUID tenantB;
    private EducationProvider providerA;

    private User instAdminA;
    private User providerAdminA;
    private User teacherA;
    private User outsiderB;

    private String instAdminAToken;
    private String providerAdminAToken;
    private String teacherAToken;
    private String outsiderBToken;
    private String platformToken;

    private String run;

    @BeforeAll
    void createFixtures() {
        run = UUID.randomUUID().toString().substring(0, 8);
        tenantA = TestDataSeeder.INSTITUTION_ID;

        instAdminA = saveUser("provadmin-instadmin-" + run + "@test.com", User.Role.INSTITUTION_ADMIN, tenantA);
        providerAdminA = saveUser("provadmin-provider-" + run + "@test.com", User.Role.PROVIDER_ADMIN, tenantA);
        teacherA = saveUser("provadmin-teacher-" + run + "@test.com", User.Role.TEACHER, tenantA);

        // Tenant B exists only so "cross-tenant" is a real second tenant rather than a null id.
        Institution instB = institutionRepository.save(Institution.builder()
                .name("ProvAdmin B " + run)
                .code("PB-" + run)
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());
        tenantB = instB.getId();
        outsiderB = saveUser("provadmin-outsider-" + run + "@test.com", User.Role.INSTRUCTOR, tenantB);

        // institutionId lives on BaseEntity, which this entity's plain @Builder does not cover.
        EducationProvider provider = EducationProvider.builder()
                .name("ProvAdmin Provider " + run)
                .providerType(EducationProvider.ProviderType.ORGANIZATION)
                .isActive(true)
                .isVerified(true)
                .build();
        provider.setInstitutionId(tenantA);
        providerA = providerRepository.save(provider);

        instAdminAToken = TestTokens.userToken(instAdminA.getEmail());
        providerAdminAToken = TestTokens.userToken(providerAdminA.getEmail());
        teacherAToken = TestTokens.userToken(teacherA.getEmail());
        outsiderBToken = TestTokens.userToken(outsiderB.getEmail());
        platformToken = TestTokens.userToken(saveUser(
                "provadmin-platform-" + run + "@test.com", User.Role.ADMIN, tenantA).getEmail());
    }

    // ── invitation creation actually persists ────────────────────────────────────────────

    /**
     * Regression for the insert that always failed. Without {@code @Builder.Default} the
     * builder produced NULL status/is_deleted and PostgreSQL rejected the row with a NOT NULL
     * violation, so the whole invitation feature returned 409 for every role.
     */
    @Test
    void institutionAdmin_canInviteWithinItsTenant_andTheRowReallyPersists() throws Exception {
        String email = "provadmin-ok-" + run + "@test.com";
        mockMvc.perform(post("/v1/admin/people/invite")
                        .header("Authorization", "Bearer " + instAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"role\":\"TEACHER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("TEACHER"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        InstitutionInvitation saved = invitationRepository
                .findByInstitutionId(tenantA).stream()
                .filter(i -> email.equals(i.getEmail()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("invitation row was not persisted"));

        assertEquals("PENDING", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getIsDeleted(), "is_deleted must be written, not NULL");
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()), "invitation must expire in the future");
        assertTrue(saved.getExpiresAt().isBefore(LocalDateTime.now().plusDays(8)));
    }

    // ── who may hand out which role ───────────────────────────────────────────────────────

    @Test
    void institutionAdmin_cannotInviteAnyPlatformOrProviderRole() throws Exception {
        for (String role : new String[]{"PROVIDER_ADMIN", "PROVIDER_STAFF", "ADMIN", "NATIONAL_ADMIN",
                "REGIONAL_ADMIN", "DISTRICT_ADMIN"}) {
            mockMvc.perform(post("/v1/admin/people/invite")
                            .header("Authorization", "Bearer " + instAdminAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"provadmin-esc-" + role + "-" + run
                                    + "@test.com\",\"role\":\"" + role + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        // Scoped to these attempts: another test in this class legitimately holds a
        // platform-issued PROVIDER_ADMIN invitation in the same tenant.
        assertTrue(invitationRepository.findByInstitutionId(tenantA).stream()
                        .noneMatch(i -> i.getEmail() != null && i.getEmail().startsWith("provadmin-esc-")),
                "no escalated invitation may have been stored");
    }

    /** The escalation was available to Provider Admins too, not only institution admins. */
    @Test
    void providerAdmin_cannotInviteProviderOrPlatformRoles() throws Exception {
        for (String role : new String[]{"PROVIDER_ADMIN", "ADMIN", "NATIONAL_ADMIN"}) {
            mockMvc.perform(post("/v1/admin/people/invite")
                            .header("Authorization", "Bearer " + providerAdminAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"provadmin-pesc-" + role + "-" + run
                                    + "@test.com\",\"role\":\"" + role + "\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void unknownRole_isRejectedRatherThanStoredVerbatim() throws Exception {
        mockMvc.perform(post("/v1/admin/people/invite")
                        .header("Authorization", "Bearer " + instAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"provadmin-bogus-" + run + "@test.com\",\"role\":\"SUPER_ADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void teacher_cannotInviteAtAll() throws Exception {
        mockMvc.perform(post("/v1/admin/people/invite")
                        .header("Authorization", "Bearer " + teacherAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"provadmin-t-" + run + "@test.com\",\"role\":\"TEACHER\"}"))
                .andExpect(status().isForbidden());
    }

    /** Cross-tenant: tenant B's admin must not be able to invite into tenant A. */
    @Test
    void outsiderCannotInviteIntoAnotherTenant() throws Exception {
        mockMvc.perform(post("/v1/admin/people/invite")
                        .header("Authorization", "Bearer " + outsiderBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"provadmin-x-" + run + "@test.com\",\"role\":\"TEACHER\"}"))
                .andExpect(status().isForbidden());
    }

    // ── the platform is the only source of PROVIDER_ADMIN ─────────────────────────────────

    @Test
    void platformAdmin_canIssueAProviderAdminInvitation_withoutAPassword() throws Exception {
        String email = "provadmin-platform-invite-" + run + "@test.com";
        MvcResult result = mockMvc.perform(post("/v1/platform-admin/invitations")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"institutionId\":\"" + tenantA
                                + "\",\"role\":\"PROVIDER_ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("PROVIDER_ADMIN"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(!body.contains("password"), "an invitation must never carry a credential");

        InstitutionInvitation saved = invitationRepository.findByTokenAndIsDeletedFalse(
                        JsonPath.read(body, "$.data.token"))
                .orElseThrow(() -> new AssertionError("platform invitation not persisted"));
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now().plusDays(6)));
        assertTrue(saved.getExpiresAt().isBefore(LocalDateTime.now().plusDays(8)));
    }

    @Test
    void institutionAdmin_cannotReachThePlatformInvitationEndpoint() throws Exception {
        mockMvc.perform(post("/v1/platform-admin/invitations")
                        .header("Authorization", "Bearer " + instAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"provadmin-pe-" + run + "@test.com\",\"institutionId\":\""
                                + tenantA + "\",\"role\":\"PROVIDER_ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void platformInvitationForAnUnknownInstitution_isRejected() throws Exception {
        mockMvc.perform(post("/v1/platform-admin/invitations")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"provadmin-ni-" + run + "@test.com\",\"institutionId\":\""
                                + UUID.randomUUID() + "\",\"role\":\"PROVIDER_ADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── redemption ───────────────────────────────────────────────────────────────────────

    @Test
    void redeemingAProviderAdminInvitation_createsAProviderAdminBoundToItsProvider() throws Exception {
        String email = "provadmin-redeem-" + run + "@test.com";
        String token = issuePlatformInvitation(email, "PROVIDER_ADMIN");

        mockMvc.perform(post("/v1/admin/people/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"Chosen-Secret-42\"}"))
                .andExpect(status().isOk());

        User created = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AssertionError("redeemer was not created"));

        // The bug this pins: membership collapses PROVIDER_ADMIN and INSTITUTION_ADMIN into
        // ADMIN, so deriving the global role from the membership produced a platform-grade
        // INSTITUTION_ADMIN out of a provider invitation.
        assertEquals(User.Role.PROVIDER_ADMIN, created.getRole(),
                "a provider invitation must not mint a global INSTITUTION_ADMIN");
        assertEquals(tenantA, created.getInstitutionId());
        assertEquals(Boolean.TRUE, created.getIsActive());

        InstitutionMembership membership = membershipRepository
                .findByUserIdAndIsActiveTrue(created.getId()).stream()
                .filter(m -> tenantA.equals(m.getInstitutionId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("membership was not created"));

        assertEquals(providerA.getId(), membership.getProviderId(),
                "a provider account with no provider_id has no provider scope");
    }

    @Test
    void redeemingDoesNotMoveAnExistingUserOutOfTheirOwnTenant() throws Exception {
        UUID originalInstitution = outsiderB.getInstitutionId();
        User.Role originalRole = outsiderB.getRole();

        // Tenant A's admin invites an existing tenant-B user, then redeems the token themselves.
        // Before the fix this rewrote the victim's global institution_id into tenant A.
        mockMvc.perform(post("/v1/admin/people/invite")
                        .header("Authorization", "Bearer " + instAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + outsiderB.getEmail() + "\",\"role\":\"TEACHER\"}"))
                .andExpect(status().isCreated());
        String token = latestToken(outsiderB.getEmail());

        mockMvc.perform(post("/v1/admin/people/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"Chosen-Secret-42\"}"))
                .andExpect(status().isOk());

        User after = userRepository.findByIdAndIsDeletedFalse(outsiderB.getId()).orElseThrow();
        assertEquals(originalInstitution, after.getInstitutionId(),
                "an invitation must never re-point an existing account's global institution");
        assertEquals(originalRole, after.getRole(), "an invitation must not rewrite a global role");
    }

    @Test
    void invitationToken_isSingleUse() throws Exception {
        String email = "provadmin-replay-" + run + "@test.com";
        String token = issuePlatformInvitation(email, "PROVIDER_ADMIN");

        mockMvc.perform(post("/v1/admin/people/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"Chosen-Secret-42\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/admin/people/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"Chosen-Secret-43\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void expiredInvitation_cannotBeRedeemed() throws Exception {
        String email = "provadmin-expired-" + run + "@test.com";
        String token = issuePlatformInvitation(email, "PROVIDER_ADMIN");

        // Backdate the expiry rather than waiting a week for it.
        InstitutionInvitation invitation = invitationRepository
                .findByTokenAndIsDeletedFalse(token).orElseThrow();
        invitation.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        invitationRepository.save(invitation);

        mockMvc.perform(post("/v1/admin/people/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"Chosen-Secret-42\"}"))
                .andExpect(status().is4xxClientError());

        assertTrue(userRepository.findByEmailAndIsDeletedFalse(email).isEmpty(),
                "an expired invitation must not create an account");
    }

    @Test
    void garbageToken_isRejected() throws Exception {
        mockMvc.perform(post("/v1/admin/people/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + "0".repeat(32) + "\",\"password\":\"Chosen-Secret-42\"}"))
                .andExpect(status().is4xxClientError());
    }

    /**
     * A tenant administrator must not be able to mint a platform-only role and redeem it,
     * even if such a row were inserted directly - the check is repeated at redemption time.
     */
    @Test
    void aPlatformOnlyInvitationIssuedByATenantAdminIsRefusedAtRedemption() throws Exception {
        String email = "provadmin-forged-" + run + "@test.com";
        String token = UUID.randomUUID().toString().replace("-", "");
        invitationRepository.save(InstitutionInvitation.builder()
                .institutionId(tenantA)
                .email(email)
                .role("PROVIDER_ADMIN")
                .invitedBy(instAdminA.getId())
                .token(token)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());

        mockMvc.perform(post("/v1/admin/people/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"Chosen-Secret-42\"}"))
                .andExpect(status().is4xxClientError());

        assertTrue(userRepository.findByEmailAndIsDeletedFalse(email).isEmpty(),
                "an institution-issued PROVIDER_ADMIN invitation must not be redeemable");
    }

    // ── public registration ──────────────────────────────────────────────────────────────

    @Test
    void publicRegistration_cannotSelfSelectAProviderOrPlatformRole() throws Exception {
        for (String role : new String[]{"PROVIDER_ADMIN", "PROVIDER_STAFF", "ADMIN", "NATIONAL_ADMIN"}) {
            mockMvc.perform(post("/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"firstName\":\"Mallory\",\"lastName\":\"Public\",\"email\":\"provadmin-reg-"
                                    + role + "-" + run + "@test.com\",\"password\":\"Str0ng-Passw0rd!\",\"role\":\""
                                    + role + "\"}"))
                    .andExpect(status().is4xxClientError());
        }
        assertTrue(userRepository.findByEmailAndIsDeletedFalse("provadmin-reg-PROVIDER_ADMIN-" + run + "@test.com")
                .isEmpty());
    }

    @Test
    void publicRegistration_stillAcceptsTheOrdinaryRoles() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Grace\",\"lastName\":\"Learner\",\"email\":\"provadmin-legit-"
                                + run + "@test.com\",\"password\":\"Str0ng-Passw0rd!\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isCreated());
        assertNotNull(userRepository.findByEmailAndIsDeletedFalse("provadmin-legit-" + run + "@test.com").orElse(null));
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────

    private String issuePlatformInvitation(String email, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/platform-admin/invitations")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"institutionId\":\"" + tenantA
                                + "\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private String latestToken(String email) {
        return invitationRepository.findByInstitutionId(tenantA).stream()
                .filter(i -> email.equals(i.getEmail()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("no invitation for " + email))
                .getToken();
    }

    private User saveUser(String email, User.Role role, UUID institutionId) {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("ProvAdmin")
                .lastName("Tester")
                .role(role)
                .institutionId(institutionId)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        InstitutionMembership.Role membershipRole = switch (role) {
            case STUDENT -> InstitutionMembership.Role.STUDENT;
            case TEACHER -> InstitutionMembership.Role.TEACHER;
            case PARENT -> InstitutionMembership.Role.PARENT;
            case INSTRUCTOR -> InstitutionMembership.Role.INSTRUCTOR;
            default -> InstitutionMembership.Role.ADMIN;
        };
        membershipRepository.save(InstitutionMembership.builder()
                .userId(user.getId())
                .institutionId(institutionId)
                .role(membershipRole)
                .isActive(true)
                .build());
        return user;
    }
}
