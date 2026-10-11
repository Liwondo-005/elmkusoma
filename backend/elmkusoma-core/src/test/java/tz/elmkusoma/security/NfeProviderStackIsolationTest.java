package tz.elmkusoma.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
 * NFE provider-stack isolation and reference validation (audit B-18).
 *
 * <p>The provider stack had ZERO backend coverage: {@code ProviderIsolationSecurityTest} never
 * touched a {@code /v1/nfe/**} route. These tests lock in the Phase 2 behaviour through the real
 * filter chain:
 *
 * <ul>
 *   <li>B-01 {@code GET /v1/nfe/providers/me} provisions the provider for the caller's
 *       institution, so the workspace is operable without an out-of-band DB row.</li>
 *   <li>B-15 client-supplied provider/learner references are validated against the caller's
 *       institution (404) instead of being written verbatim.</li>
 *   <li>B-16 {@code providerId} is immutable on update.</li>
 *   <li>B-03 a certificate without a learner is a clean 400, not a 409 integrity error.</li>
 *   <li>B-12 a suspended organisation cannot write, and platform approval flips is_verified.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class NfeProviderStackIsolationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InstitutionRepository institutionRepository;
    @Autowired
    private InstitutionMembershipRepository membershipRepository;
    @Autowired
    private EducationProviderRepository providerRepository;

    private UUID tenantA;
    private Institution instB;
    private UUID instBId;
    private String run;
private User providerA;
private String tokenA;
private String platformAdminToken;

    @BeforeEach
    void setUp() {
        run = UUID.randomUUID().toString().substring(0, 8);
        tenantA = TestDataSeeder.INSTITUTION_ID;
        instB = saveInstitution("NFE Stack B " + run, Institution.InstitutionType.SECONDARY);
        instBId = instB.getId();
        providerA = saveUser("nfe-admin-a-" + run + "@test.com", User.Role.PROVIDER_ADMIN, tenantA);
        tokenA = TestTokens.userToken(providerA.getEmail());
        // Start from a clean slate: no provider row for either tenant.
        providerRepository.findAllByInstitutionId(tenantA)
                .forEach(p -> { p.setIsDeleted(true); providerRepository.save(p); });
    }

    @AfterEach
    void tearDown() {
        providerRepository.findAllByInstitutionId(tenantA)
                .forEach(p -> { p.setIsDeleted(true); providerRepository.save(p); });
    }

    // ---------------------------------------------------------------- B-01

    @Test
    @DisplayName("B-01: GET /v1/nfe/providers/me provisions the provider for the caller's institution")
    void providerMeProvisionsProviderOnFirstUse() throws Exception {
        assertTrue(providerRepository.findAllByInstitutionId(tenantA).stream()
                        .noneMatch(p -> !Boolean.TRUE.equals(p.getIsDeleted())),
                "precondition: tenant A starts with no live provider row");

        mockMvc.perform(get("/v1/nfe/providers/me")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Institution-Id", tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").isNotEmpty());

        // A live row now exists, so the seven dependent resources become operable.
        assertTrue(providerRepository.findAllByInstitutionId(tenantA).stream()
                .anyMatch(p -> !Boolean.TRUE.equals(p.getIsDeleted())),
                "provider row must exist after /me");

        // Idempotent: a second call must not create a second provider.
        mockMvc.perform(get("/v1/nfe/providers/me")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Institution-Id", tenantA.toString()))
                .andExpect(status().isOk());
        long live = providerRepository.findAllByInstitutionId(tenantA).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted())).count();
        assertEquals(1, live, "repeated /me must not duplicate the provider");
    }

    @Test
    @DisplayName("B-05: a provider of another institution is never provisioned to a caller")
    void providerMeIsScopedToCallersInstitution() throws Exception {
        mockMvc.perform(get("/v1/nfe/providers/me")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Institution-Id", instBId.toString()))
                .andExpect(status().isForbidden());

        assertTrue(providerRepository.findAllByInstitutionId(instBId).stream()
                        .noneMatch(p -> !Boolean.TRUE.equals(p.getIsDeleted())),
                "cross-tenant /me must be refused before anything is provisioned");
    }

    // ---------------------------------------------------------------- B-15

    @Test
    @DisplayName("B-15: a program cannot be created against another institution's provider")
    void programCreateRejectsForeignProvider() throws Exception {
        UUID foreignProvider = provisionProvider(instBId);

        MvcResult result = mockMvc.perform(post("/v1/nfe/programs")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Institution-Id", tenantA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of(
                                "providerId", foreignProvider.toString(),
                                "title", "Foreign program " + run,
                                "programType", "COURSE"))))
                .andExpect(status().isNotFound())
                .andReturn();

        assertNotNull(result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("B-15: a program created with the caller's own provider succeeds")
    void programCreateAcceptsOwnProvider() throws Exception {
        UUID ownProvider = provisionProvider(tenantA);

        MvcResult result = mockMvc.perform(post("/v1/nfe/programs")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Institution-Id", tenantA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of(
                                "providerId", ownProvider.toString(),
                                "title", "Own program " + run,
                                "programType", "COURSE"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Own program " + run))
                .andReturn();

        assertNotNull(result.getResponse().getContentAsString());
    }

    // ---------------------------------------------------------------- B-16

    @Test
    @DisplayName("B-16: providerId cannot be reassigned to another tenant's provider on update")
    void programUpdateRejectsForeignProviderId() throws Exception {
        UUID ownProvider = provisionProvider(tenantA);
        UUID foreignProvider = provisionProvider(instBId);

        MvcResult created = mockMvc.perform(post("/v1/nfe/programs")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Institution-Id", tenantA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of(
                                "providerId", ownProvider.toString(),
                                "title", "Reassign target " + run,
                                "programType", "COURSE"))))
                .andExpect(status().isCreated())
                .andReturn();

        UUID programId = UUID.fromString(
                JSON.readTree(created.getResponse().getContentAsString()).get("data").get("id").asText());

        // An attempt to repoint the program at a foreign provider is refused outright.
        mockMvc.perform(put("/v1/nfe/programs/" + programId)
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Institution-Id", tenantA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of(
                                "providerId", foreignProvider.toString(),
                                "title", "Reassigned " + run,
                                "programType", "COURSE"))))
                .andExpect(status().isBadRequest());

        // And the row is untouched.
        mockMvc.perform(get("/v1/nfe/programs/" + programId)
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Institution-Id", tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerId").value(ownProvider.toString()));
    }

    // ---------------------------------------------------------------- B-03

    @Test
    @DisplayName("B-03: a certificate without a learner is rejected with 400, not a 409 integrity error")
    void certificateCreateWithoutLearnerIsBadRequest() throws Exception {
        UUID ownProvider = provisionProvider(tenantA);

        mockMvc.perform(post("/v1/nfe/certificates/providers/" + ownProvider)
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Institution-Id", tenantA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of(
                                "certificateType", "COMPLETION",
                                "title", "No learner " + run,
                                "studentName", "Someone"))))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- B-12

    @Test
    @DisplayName("B-12: platform approval flips is_verified, which previously had no writer at all")
    void verificationEndpointSetsVerifiedFlag() throws Exception {
        UUID ownProvider = provisionProvider(tenantA);

        EducationProvider before = providerRepository.findByIdAndInstitutionId(ownProvider, tenantA).orElseThrow();
        assertFalse(Boolean.TRUE.equals(before.getIsVerified()), "precondition: not verified yet");

        mockMvc.perform(put("/v1/nfe/providers/" + ownProvider + "/verification")
                        .header("Authorization", "Bearer " + platformToken())
                        .header("X-Institution-Id", tenantA.toString())
                        .param("verified", "true"))
                .andExpect(status().isOk());

        EducationProvider after = providerRepository.findByIdAndInstitutionId(ownProvider, tenantA).orElseThrow();
        assertTrue(Boolean.TRUE.equals(after.getIsVerified()), "is_verified must now be true");
    }

    /**
     * Verification is the platform's trust decision, so a provider may not grant it to itself.
     *
     * <p>This endpoint previously accepted PROVIDER_ADMIN and INSTITUTION_ADMIN and an earlier
     * version of this test asserted that self-verification succeeded. That was a governance
     * hole, not a contract worth keeping: it let a provider approve its own provider record and
     * sidestepped the real review workflow in {@code /v1/verifications}. The write is now
     * platform-only, so the provider's own request is denied and changes nothing.</p>
     */
    @Test
    @DisplayName("B-12: a provider cannot verify its own provider record")
    void providerAdminCannotSelfVerify() throws Exception {
        UUID ownProvider = provisionProvider(tenantA);

        mockMvc.perform(put("/v1/nfe/providers/" + ownProvider + "/verification")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Institution-Id", tenantA.toString())
                        .param("verified", "true"))
                .andExpect(status().isForbidden());

        EducationProvider after = providerRepository.findByIdAndInstitutionId(ownProvider, tenantA).orElseThrow();
        assertFalse(Boolean.TRUE.equals(after.getIsVerified()),
                "a denied self-verification must not flip the flag");
    }

    /** A platform admin acting against a tenant they do not belong to still gets nothing. */
    @Test
    @DisplayName("B-12: platform approval is still tenant-scoped")
    void platformAdminCannotVerifyAProviderInAnotherTenant() throws Exception {
        UUID foreignProvider = provisionProvider(instBId);

        mockMvc.perform(put("/v1/nfe/providers/" + foreignProvider + "/verification")
                        .header("Authorization", "Bearer " + platformToken())
                        .header("X-Institution-Id", tenantA.toString())
                        .param("verified", "true"))
                .andExpect(status().isNotFound());

        EducationProvider after = providerRepository.findByIdAndInstitutionId(foreignProvider, instBId).orElseThrow();
        assertFalse(Boolean.TRUE.equals(after.getIsVerified()));
    }

    @Test
    @DisplayName("B-12: a suspended organisation cannot write provider resources")
    void suspendedInstitutionCannotCreatePrograms() throws Exception {
        UUID ownProvider = provisionProvider(tenantA);
        instB.setStatus("SUSPENDED");
        instB.setIsActive(false);
        institutionRepository.save(instB);

        mockMvc.perform(post("/v1/nfe/programs")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Institution-Id", instBId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of(
                                "providerId", ownProvider.toString(),
                                "title", "Suspended org " + run,
                                "programType", "COURSE"))))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- helpers

    private UUID provisionProvider(UUID institutionId) {
        EducationProvider p = EducationProvider.builder()
                .name("Provider " + UUID.randomUUID().toString().substring(0, 6))
                .providerType(EducationProvider.ProviderType.ORGANIZATION)
                .isActive(true)
                .isVerified(false)
                .build();
        p.setInstitutionId(institutionId);
        return providerRepository.save(p).getId();
    }

    private Institution saveInstitution(String name, Institution.InstitutionType type) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .code("NFE-" + UUID.randomUUID().toString().substring(0, 8))
                .type(type)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());
    }

    /**
     * A platform ADMIN, created on demand and cached.
     *
     * <p>TestDataSeeder's admin@elmkusoma.tz is an INSTITUTION_ADMIN, which /v1/nfe/providers
     * verification now rejects, so a real ADMIN is needed to exercise the approval path.</p>
     */
    private synchronized String platformToken() {
        if (platformAdminToken == null) {
            String email = "nfe-platform-" + run + "@test.com";
            saveUser(email, User.Role.ADMIN, tenantA);
            platformAdminToken = TestTokens.userToken(email);
        }
        return platformAdminToken;
    }

    private User saveUser(String email, User.Role role, UUID institutionId) {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("NfeStack")
                .lastName("Tester")
                .role(role)
                .institutionId(institutionId)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        membershipRepository.save(InstitutionMembership.builder()
                .userId(user.getId())
                .institutionId(institutionId)
                .role(InstitutionMembership.Role.ADMIN)
                .isActive(true)
                .build());
        return user;
    }
}