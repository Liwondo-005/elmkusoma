package tz.elmkusoma.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.administration.domain.AdminDelegation;
import tz.elmkusoma.administration.domain.InstitutionInvitation;
import tz.elmkusoma.administration.repository.AdminDelegationRepository;
import tz.elmkusoma.administration.repository.InstitutionInvitationRepository;
import tz.elmkusoma.identity.service.OtpMailService;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Provider staff provisioning and lifecycle - authority levels C to D.
 *
 * <p>This is the workflow the product describes but did not implement. {@code PROVIDER_STAFF} sat
 * in {@code InvitationRolePolicy.PLATFORM_ONLY}, so the only way to create a staff account was for
 * Platform Admin to invite one, and there was no endpoint at all for listing a provider's staff,
 * assigning a responsibility, or removing a member. The live database held one PROVIDER_ADMIN and
 * no PROVIDER_STAFF, so nothing about this path had ever been exercised.</p>
 *
 * <p>Opening it introduces the only path in the product where a non-platform account can cause an
 * account to be created, so the suite spends most of its weight on boundaries: who may staff whom,
 * what happens across provider and tenant borders, what an owner cannot escalate to, and whether
 * revocation is real.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProviderStaffProvisioningSecurityTest {

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
    private AdminDelegationRepository delegationRepository;

    @Autowired
    private EducationProviderRepository providerRepository;

    /** Lets the test read the activation code that would otherwise only reach the mailbox. */
    @MockitoBean
    private OtpMailService otpMailService;

    private UUID tenantA;
    private UUID tenantB;

    private EducationProvider providerA;
    private EducationProvider providerB;

    private User ownerA;
    private User unscopedProviderAdmin;
    private User staffA;
    private User teacherA;
    private User learnerA;
    private User ownerB;
    private User platformAdmin;

    private String ownerAToken;
    private String unscopedAdminToken;
    private String staffAToken;
    private String teacherAToken;
    private String learnerAToken;
    private String ownerBToken;
    private String platformToken;

    private String run;

    @BeforeAll
    void createFixtures() {
        run = UUID.randomUUID().toString().substring(0, 8);
        tenantA = TestDataSeeder.INSTITUTION_ID;

        // Two providers inside one institution: the whole point is to prove staff land on the
        // provider the owner actually chose, not on whichever one the lookup found first.
        providerA = saveProvider("Staff Provider A " + run, tenantA);
        providerB = saveProvider("Staff Provider B " + run, tenantA);

        Institution instB = institutionRepository.save(Institution.builder()
                .name("StaffTenant B " + run)
                .code("SB-" + run)
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());
        tenantB = instB.getId();
        EducationProvider providerBInOtherTenant = saveProvider("Staff Provider B2 " + run, tenantB);

        ownerA = saveUser("staff-owner-" + run + "@test.com", User.Role.PROVIDER_ADMIN, tenantA);
        bindOwner(ownerA, providerA);

        // Same global role, no provider_id: a governance-shaped account, not an owner.
        unscopedProviderAdmin = saveUser("staff-unscoped-" + run + "@test.com", User.Role.PROVIDER_ADMIN, tenantA);

        staffA = saveUser("staff-member-" + run + "@test.com", User.Role.PROVIDER_STAFF, tenantA);
        bindStaff(staffA, providerA);

        teacherA = saveUser("staff-teacher-" + run + "@test.com", User.Role.TEACHER, tenantA);
        learnerA = saveUser("staff-learner-" + run + "@test.com", User.Role.STUDENT, tenantA);
        ownerB = saveUser("staff-ownerb-" + run + "@test.com", User.Role.PROVIDER_ADMIN, tenantA);
        bindOwner(ownerB, providerBInOtherTenant);
        platformAdmin = saveUser("staff-platform-" + run + "@test.com", User.Role.ADMIN, tenantA);

        ownerAToken = TestTokens.userToken(ownerA.getEmail());
        unscopedAdminToken = TestTokens.userToken(unscopedProviderAdmin.getEmail());
        staffAToken = TestTokens.userToken(staffA.getEmail());
        teacherAToken = TestTokens.userToken(teacherA.getEmail());
        learnerAToken = TestTokens.userToken(learnerA.getEmail());
        ownerBToken = TestTokens.userToken(ownerB.getEmail());
        platformToken = TestTokens.userToken(platformAdmin.getEmail());
    }

    // ── the workflow works ───────────────────────────────────────────────────────────────

    @Test
    void providerOwner_invitesStaffAndTheInvitationRecordsTheirProvider() throws Exception {
        String email = "staff-new-" + run + "@test.com";

        MvcResult result = mockMvc.perform(post("/v1/nfe/providers/" + providerA.getId() + "/staff/invitations")
                        .header("Authorization", "Bearer " + ownerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"responsibility\":\"TRAINING_COORDINATOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("PROVIDER_STAFF"))
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
        assertNotNull(token, "the inviter must receive the single-use token for out-of-band delivery");

        InstitutionInvitation invitation = invitationRepository.findByTokenAndIsDeletedFalse(token)
                .orElseThrow(() -> new AssertionError("invitation did not persist"));
        assertEquals(providerA.getId(), invitation.getProviderId(),
                "the invitation must record the provider the owner chose, not one inferred later");
        assertEquals(ownerA.getId(), invitation.getInvitedBy(), "the provisioning actor must be recorded");
    }

    @Test
    void redeemingAnOwnerIssuedInvitation_createsStaffBoundToThatExactProvider() throws Exception {
        String email = "staff-redeem-" + run + "@test.com";
        String token = invite(ownerAToken, providerA.getId(), email);

        activate(token, "Chosen-Secret-42");

        User created = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AssertionError("redeemer was not created"));
        assertEquals(User.Role.PROVIDER_STAFF, created.getRole());
        assertNotNull(created.getPasswordHash());
        assertFalse(created.getPasswordHash().equals("Chosen-Secret-42"),
                "the password must be stored hashed, never verbatim");

        InstitutionMembership membership = membershipRepository
                .findByUserIdAndIsActiveTrue(created.getId()).stream()
                .filter(m -> tenantA.equals(m.getInstitutionId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("membership was not created"));
        assertEquals(providerA.getId(), membership.getProviderId());
        assertEquals(InstitutionMembership.Role.TEACHER, membership.getRole(),
                "staff map to the membership role the platform already uses for them");
    }

    @Test
    void staffInvitation_bindsToTheChosenProviderNotTheInstitutionsFirst() throws Exception {
        // Two providers share tenantA. The owner of the SECOND one must still staff the second one -
        // this is exactly what the old findFirst() resolution could not guarantee.
        String email = "staff-second-" + run + "@test.com";
        String ownerOfBToken = TestTokens.userToken(
                saveUser("staff-ownerb2-" + run + "@test.com", User.Role.PROVIDER_ADMIN, tenantA).getEmail());
        User ownerOfB = userRepository.findByEmailAndIsDeletedFalse(
                        "staff-ownerb2-" + run + "@test.com").orElseThrow();
        bindOwner(ownerOfB, providerB);

        String token = invite(ownerOfBToken, providerB.getId(), email);
        activate(token, "Chosen-Secret-42");

        User created = userRepository.findByEmailAndIsDeletedFalse(email).orElseThrow();
        InstitutionMembership membership = membershipRepository
                .findByUserIdAndIsActiveTrue(created.getId()).stream()
                .filter(m -> tenantA.equals(m.getInstitutionId()))
                .findFirst()
                .orElseThrow();
        assertEquals(providerB.getId(), membership.getProviderId(),
                "staff must land on the provider the owner administered, not the institution's first");
    }

    @Test
    void owner_seesTheirRealRoster() throws Exception {
        mockMvc.perform(get("/v1/nfe/providers/" + providerA.getId() + "/staff")
                        .header("Authorization", "Bearer " + ownerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.userId=='" + staffA.getId() + "')]").exists())
                .andExpect(jsonPath("$.data[?(@.userId=='" + staffA.getId() + "')].globalRole")
                        .value("PROVIDER_STAFF"));
    }

    @Test
    void responsibilityRoundTrips() throws Exception {
        // Case is normalised so an owner is not forced to remember the exact casing, but the
        // shape is strict: a value with a space or punctuation is rejected rather than cleaned up,
        // because silently rewriting someone's words is worse than telling them the format.
        mockMvc.perform(put("/v1/nfe/providers/" + providerA.getId() + "/staff/" + staffA.getId()
                        + "/responsibility")
                        .header("Authorization", "Bearer " + ownerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsibility\":\"records_clerk\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.responsibility").value("RECORDS_CLERK"));

        assertEquals("RECORDS_CLERK", membershipFor(staffA).getResponsibility());
    }

    // ── boundaries: who may staff whom ────────────────────────────────────────────────────

    @Test
    void providerAdminWithoutProviderScope_cannotStaff() throws Exception {
        mockMvc.perform(post("/v1/nfe/providers/" + providerA.getId() + "/staff/invitations")
                        .header("Authorization", "Bearer " + unscopedAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"staff-nope-" + run + "@test.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherAndLearner_cannotReachStaffManagementAtAll() throws Exception {
        for (String token : new String[]{teacherAToken, learnerAToken}) {
            mockMvc.perform(get("/v1/nfe/providers/" + providerA.getId() + "/staff")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/v1/nfe/providers/" + providerA.getId() + "/staff/invitations")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"staff-nope2-" + run + "@test.com\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void providerStaff_cannotProvisionPeers() throws Exception {
        mockMvc.perform(post("/v1/nfe/providers/" + providerA.getId() + "/staff/invitations")
                        .header("Authorization", "Bearer " + staffAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"staff-nope3-" + run + "@test.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCannotReachAnotherProvidersRoster() throws Exception {
        // Reported as not found, not forbidden: a 403 here would confirm the provider exists.
        mockMvc.perform(get("/v1/nfe/providers/" + providerB.getId() + "/staff")
                        .header("Authorization", "Bearer " + ownerAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCannotInviteIntoAnotherTenant() throws Exception {
        mockMvc.perform(post("/v1/nfe/providers/" + UUID.randomUUID() + "/staff/invitations")
                        .header("Authorization", "Bearer " + ownerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"staff-nope4-" + run + "@test.com\"}"))
                .andExpect(status().isNotFound());
    }

    /**
     * The request body has no role field, so asking for one cannot be expressed. A client that
     * sends one anyway must still receive staff, not an owner.
     */
    @Test
    void askingForAnAdminRoleInTheBodyDoesNotProduceOne() throws Exception {
        String email = "staff-escalate-" + run + "@test.com";

        mockMvc.perform(post("/v1/nfe/providers/" + providerA.getId() + "/staff/invitations")
                        .header("Authorization", "Bearer " + ownerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"role\":\"PROVIDER_ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("PROVIDER_STAFF"));

        String token = latestToken(email);
        activate(token, "Chosen-Secret-42");
        assertEquals(User.Role.PROVIDER_STAFF,
                userRepository.findByEmailAndIsDeletedFalse(email).orElseThrow().getRole(),
                "an owner must never be able to mint a peer owner");
    }

    @Test
    void platformAdmin_canStillStaff() throws Exception {
        mockMvc.perform(post("/v1/nfe/providers/" + providerA.getId() + "/staff/invitations")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"staff-byclat-" + run + "@test.com\"}"))
                .andExpect(status().isCreated());
    }

    // ── boundaries: delegated governance (level B) ────────────────────────────────────────

    @Test
    void aGovernanceOfficerMayReviewButNotStaff() throws Exception {
        User officer = saveUser("staff-officer-" + run + "@test.com", User.Role.PROVIDER_ADMIN, tenantA);
        delegationRepository.save(AdminDelegation.builder()
                .delegatorId(platformAdmin.getId())
                .delegateId(officer.getId())
                .authority("PROVIDER_VERIFICATION")
                .permissions("[\"VIEW\",\"REVIEW\"]")
                .resourceIds("[\"" + providerA.getId() + "\"]")
                .status("ACTIVE")
                .startsAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(30))
                .isDeleted(false)
                .build());
        String officerToken = TestTokens.userToken(officer.getEmail());

        // Delegated authority is to review providers, not to staff them.
        mockMvc.perform(get("/v1/nfe/providers/" + providerA.getId() + "/staff")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/v1/nfe/providers/" + providerA.getId() + "/staff/invitations")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"staff-officer-staff-" + run + "@test.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aRevokedDelegationLosesItsScopeImmediately() throws Exception {
        User officer = saveUser("staff-officer-rev-" + run + "@test.com", User.Role.PROVIDER_ADMIN, tenantA);
        AdminDelegation delegation = delegationRepository.save(AdminDelegation.builder()
                .delegatorId(platformAdmin.getId())
                .delegateId(officer.getId())
                .authority("PROVIDER_VERIFICATION")
                .permissions("[\"VIEW\"]")
                .resourceIds("[\"" + providerA.getId() + "\"]")
                .status("ACTIVE")
                .startsAt(LocalDateTime.now().minusDays(1))
                .isDeleted(false)
                .build());
        String officerToken = TestTokens.userToken(officer.getEmail());
        mockMvc.perform(get("/v1/nfe/providers/" + providerA.getId() + "/staff")
                .header("Authorization", "Bearer " + officerToken)).andExpect(status().isOk());

        delegation.setStatus("REVOKED");
        delegation.setRevokedAt(LocalDateTime.now());
        delegationRepository.save(delegation);

        mockMvc.perform(get("/v1/nfe/providers/" + providerA.getId() + "/staff")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void anExpiredDelegationGrantsNothing() throws Exception {
        User officer = saveUser("staff-officer-exp-" + run + "@test.com", User.Role.PROVIDER_ADMIN, tenantA);
        delegationRepository.save(AdminDelegation.builder()
                .delegatorId(platformAdmin.getId())
                .delegateId(officer.getId())
                .authority("PROVIDER_VERIFICATION")
                .permissions("[\"VIEW\"]")
                .resourceIds("[\"" + providerA.getId() + "\"]")
                .status("ACTIVE")
                .startsAt(LocalDateTime.now().minusDays(30))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/v1/nfe/providers/" + providerA.getId() + "/staff")
                        .header("Authorization", "Bearer " + officerTokenFor(officer)))
                .andExpect(status().isForbidden());
    }

    // ── boundaries: lifecycle and revocation ──────────────────────────────────────────────

    @Test
    void removingStaffRevokesAccessButKeepsTheirAccount() throws Exception {
        User temp = saveUser("staff-temp-" + run + "@test.com", User.Role.PROVIDER_STAFF, tenantA);
        bindStaff(temp, providerA);
        // A second, unrelated membership that must survive the removal untouched.
        membershipRepository.save(InstitutionMembership.builder()
                .userId(temp.getId())
                .institutionId(tenantA)
                .role(InstitutionMembership.Role.STUDENT)
                .isActive(true)
                .isDeleted(false)
                .build());
        Long securityVersionBefore = userRepository.findByIdAndIsDeletedFalse(temp.getId()).orElseThrow()
                .getSecurityVersion();

        mockMvc.perform(delete("/v1/nfe/providers/" + providerA.getId() + "/staff/" + temp.getId())
                        .header("Authorization", "Bearer " + ownerAToken))
                .andExpect(status().isOk());

        InstitutionMembership staffRow = membershipRepository
                .findByProviderIdAndIsDeletedFalse(providerA.getId()).stream()
                .filter(m -> temp.getId().equals(m.getUserId()))
                .findFirst().orElseThrow();
        assertFalse(staffRow.getIsActive(), "the provider membership must be deactivated");

        User after = userRepository.findByIdAndIsDeletedFalse(temp.getId())
                .orElseThrow(() -> new AssertionError("the global account must survive removal"));
        assertTrue(after.getSecurityVersion() > securityVersionBefore,
                "sessions must be revoked by bumping securityVersion, not by waiting for expiry");
        assertTrue(after.getIsActive(), "removing a provider membership must not disable the account");

        assertTrue(membershipRepository.findByUserIdAndIsActiveTrue(temp.getId()).stream()
                        .anyMatch(m -> !providerA.getId().equals(m.getProviderId())),
                "an unrelated membership must be left alone");
    }

    @Test
    void anOwnerCannotBeRemovedThroughTheStaffEndpoint() throws Exception {
        mockMvc.perform(delete("/v1/nfe/providers/" + providerA.getId() + "/staff/" + ownerA.getId())
                        .header("Authorization", "Bearer " + ownerAToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void anOwnerCannotRemoveThemselves() throws Exception {
        User second = saveUser("staff-owner-second-" + run + "@test.com", User.Role.PROVIDER_ADMIN, tenantA);
        bindOwner(second, providerA);
        String token = TestTokens.userToken(second.getEmail());

        mockMvc.perform(delete("/v1/nfe/providers/" + providerA.getId() + "/staff/" + second.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    /**
     * The invitation was legitimately issued, then the issuer lost the provider. The token must
     * fail closed rather than survive on the strength of an authority its issuer no longer has.
     */
    @Test
    void anInvitationFailsRedemptionOnceTheIssuerStopsOwningTheProvider() throws Exception {
        // A dedicated owner, not the shared fixture: this test deliberately strips the issuer's
        // membership, and with @TestInstance(PER_CLASS) the methods run in an arbitrary order, so
        // demoting the shared ownerA would silently break every other test that follows it.
        User disposableOwner = saveUser("staff-owner-disposable-" + run + "@test.com",
                User.Role.PROVIDER_ADMIN, tenantA);
        bindOwner(disposableOwner, providerA);
        String disposableToken = TestTokens.userToken(disposableOwner.getEmail());

        String email = "staff-orphan-" + run + "@test.com";
        String token = invite(disposableToken, providerA.getId(), email);

        // The owner loses their provider membership after issuing.
        membershipRepository.findByProviderIdAndIsDeletedFalse(providerA.getId()).stream()
                .filter(m -> disposableOwner.getId().equals(m.getUserId()))
                .forEach(m -> { m.setIsActive(false); membershipRepository.save(m); });

        mockMvc.perform(post("/v1/admin/people/invitations/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/v1/admin/people/invitations/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"code\":\"" + lastMailedCode()
                                + "\",\"password\":\"Chosen-Secret-42\"}"))
                .andExpect(status().is4xxClientError());

        assertTrue(userRepository.findByEmailAndIsDeletedFalse(email).isEmpty(),
                "an invitation whose issuer lost their provider must not create an account");
    }

    @Test
    void invitingAnAddressThatAlreadyHasAnAccountIsRefused() throws Exception {
        mockMvc.perform(post("/v1/nfe/providers/" + providerA.getId() + "/staff/invitations")
                        .header("Authorization", "Bearer " + ownerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + learnerA.getEmail() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicatePendingInvitationsAreRefused() throws Exception {
        String email = "staff-dup-" + run + "@test.com";
        mockMvc.perform(post("/v1/nfe/providers/" + providerA.getId() + "/staff/invitations")
                .header("Authorization", "Bearer " + ownerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}")).andExpect(status().isCreated());

        mockMvc.perform(post("/v1/nfe/providers/" + providerA.getId() + "/staff/invitations")
                        .header("Authorization", "Bearer " + ownerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aMalformedResponsibilityIsRejected() throws Exception {
        mockMvc.perform(put("/v1/nfe/providers/" + providerA.getId() + "/staff/" + staffA.getId()
                        + "/responsibility")
                        .header("Authorization", "Bearer " + ownerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsibility\":\"<script>alert(1)</script>\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aSuspendedOrganisationCannotStaff() throws Exception {
        Institution suspended = institutionRepository.save(Institution.builder()
                .name("Suspended " + run)
                .code("SUS-" + run)
                .type(Institution.InstitutionType.TRAINING_PROVIDER)
                .country("Tanzania")
                .isActive(true)
                .status("SUSPENDED")
                .isDeleted(false)
                .build());
        EducationProvider provider = saveProvider("Suspended Provider " + run, suspended.getId());
        User suspendedOwner = saveUser("staff-sus-" + run + "@test.com", User.Role.PROVIDER_ADMIN, tenantA);
        bindOwner(suspendedOwner, provider);
        String token = TestTokens.userToken(suspendedOwner.getEmail());

        mockMvc.perform(post("/v1/nfe/providers/" + provider.getId() + "/staff/invitations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"staff-sus-new-" + run + "@test.com\"}"))
                .andExpect(status().isForbidden());
    }

    // ── global user directory (Platform Admin) ───────────────────────────────────────────

    @Test
    void theGlobalDirectoryShowsWhichProviderAStaffMemberBelongsTo() throws Exception {
        // Without provider memberships on the summary, a PROVIDER_STAFF row was indistinguishable
        // from any other, because `users` has no provider column - so the directory could not
        // answer "who works for this provider" at all.
        mockMvc.perform(get("/v1/platform-admin/users")
                        .header("Authorization", "Bearer " + platformToken)
                        .param("search", staffA.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        // Read the parsed value rather than matching it inline: the diagnostic message on failure
        // then names all three ids, which is what actually distinguishes "the service reported the
        // wrong provider" from "the test looked at the wrong row".
        assertEquals(java.util.List.of(providerA.getId().toString()),
                JsonPath.read(mockMvc.perform(get("/v1/platform-admin/users")
                                .header("Authorization", "Bearer " + platformToken)
                                .param("search", staffA.getEmail()))
                        .andReturn().getResponse().getContentAsString(),
                        "$.data.content[0].providerIds"),
                "providerA=" + providerA.getId()
                        + " membershipProviderId=" + membershipFor(staffA).getProviderId()
                        + " staffId=" + staffA.getId());
    }

    @Test
    void aPersonScopedToTwoProvidersIsListedOnceWithBoth() throws Exception {
        // Multi-membership is legitimate. Presenting it as two users would be the duplicate-identity
        // bug; collapsing it to the first membership would understate their access.
        User both = saveUser("staff-both-" + run + "@test.com", User.Role.PROVIDER_STAFF, tenantA);
        bindStaff(both, providerA);
        bindStaff(both, providerB);

        mockMvc.perform(get("/v1/platform-admin/users")
                        .header("Authorization", "Bearer " + platformToken)
                        .param("search", both.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].providerIds.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].membershipCount").value(2));
    }

    @Test
    void theProviderDirectoryReturnsOnlyThatProvidersPeople() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/providers/" + providerA.getId() + "/users")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id=='" + staffA.getId() + "')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.id=='" + ownerB.getId() + "')]").doesNotExist());
    }

    @Test
    void theGlobalDirectoryExposesNoCredentialMaterial() throws Exception {
        String body = mockMvc.perform(get("/v1/platform-admin/users")
                        .header("Authorization", "Bearer " + platformToken)
                        .param("search", staffA.getEmail()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        for (String forbidden : new String[]{"passwordHash", "password_hash", "token", "otp", "resetToken"}) {
            assertFalse(body.toLowerCase().contains(forbidden.toLowerCase()),
                    "the global directory must never expose " + forbidden);
        }
    }

    @Test
    void anInstitutionAdminCannotReachTheGlobalDirectory() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/users")
                        .header("Authorization", "Bearer " + TestTokens.userToken(
                                saveUser("staff-notplatform-" + run + "@test.com",
                                        User.Role.INSTITUTION_ADMIN, tenantA).getEmail())))
                .andExpect(status().isForbidden());
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────

    private String officerTokenFor(User user) {
        return TestTokens.userToken(user.getEmail());
    }

    private String invite(String token, UUID providerId, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/nfe/providers/" + providerId + "/staff/invitations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private void activate(String token, String password) throws Exception {
        mockMvc.perform(post("/v1/admin/people/invitations/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/v1/admin/people/invitations/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"code\":\"" + lastMailedCode()
                                + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());
    }

    private String lastMailedCode() {
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(otpMailService, atLeastOnce()).sendVerificationCode(anyString(), code.capture());
        return code.getValue();
    }

    private String latestToken(String email) {
        return invitationRepository.findAll().stream()
                .filter(i -> email.equals(i.getEmail()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("no invitation for " + email))
                .getToken();
    }

    private InstitutionMembership membershipFor(User user) {
        return membershipRepository.findByUserIdAndIsActiveTrue(user.getId()).stream()
                .filter(m -> providerA.getId().equals(m.getProviderId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no provider membership"));
    }

    private EducationProvider saveProvider(String name, UUID institutionId) {
        EducationProvider provider = EducationProvider.builder()
                .name(name)
                .providerType(EducationProvider.ProviderType.ORGANIZATION)
                .isActive(true)
                .isVerified(true)
                .build();
        provider.setInstitutionId(institutionId);
        return providerRepository.save(provider);
    }

    /** An owner: a PROVIDER_ADMIN whose membership names the provider they administer. */
    private void bindOwner(User user, EducationProvider provider) {
        membershipRepository.save(InstitutionMembership.builder()
                .userId(user.getId())
                .institutionId(provider.getInstitutionId())
                .role(InstitutionMembership.Role.ADMIN)
                .providerId(provider.getId())
                .isActive(true)
                .isDeleted(false)
                .build());
    }

    /** Staff: a PROVIDER_STAFF scoped to the provider. */
    private void bindStaff(User user, EducationProvider provider) {
        membershipRepository.save(InstitutionMembership.builder()
                .userId(user.getId())
                .institutionId(provider.getInstitutionId())
                .role(InstitutionMembership.Role.TEACHER)
                .providerId(provider.getId())
                .isActive(true)
                .isDeleted(false)
                .build());
    }

    private User saveUser(String email, User.Role role, UUID institutionId) {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("Staff")
                .lastName("Tester")
                .role(role)
                .institutionId(institutionId)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        return user;
    }
}
