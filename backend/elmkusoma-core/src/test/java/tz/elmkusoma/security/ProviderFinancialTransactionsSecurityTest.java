package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RO-07 tenant isolation for {@code GET /v1/provider/financial/transactions} through the real
 * filter chain (JwtAuthenticationFilter → JwtRequestAttributeFilter → OrganizationContextResolver →
 * URL fence → method security → controller scope helper → GlobalExceptionHandler).
 *
 * <p>The endpoint is a money surface, so what matters most is not that a provider can read their
 * own sales - it is that no request shape lets one provider read another institution's sales. The
 * institution id comes only from the server-resolved request attributes; a spoofed
 * {@code X-Institution-Id} header is rejected by the organization-context filter, a spoofed
 * query parameter is simply never bound, and a caller with no institution at all is refused
 * rather than defaulted to an unscoped query.</p>
 *
 * <p>Two institutions (the seeded tenant A + a fresh tenant B), one payment in each.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProviderFinancialTransactionsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstitutionMembershipRepository membershipRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private UUID tenantA;
    private Institution instB;

    private User providerAdminA;
    private User providerStaffA;

    private Payment ownPayment;
    private Payment foreignPayment;

    private String providerAdminAToken;
    private String providerStaffAToken;
    private String parentAToken;
    private String studentAToken;
    private String orphanProviderToken;

    @BeforeAll
    void createFixtures() {
        String run = UUID.randomUUID().toString().substring(0, 8);
        tenantA = TestDataSeeder.INSTITUTION_ID;
        instB = saveInstitution("Provider Fin B " + run);

        providerAdminA = saveUser("provider-fin-admin-a-" + run + "@test.com",
                User.Role.PROVIDER_ADMIN, tenantA);
        providerStaffA = saveUser("provider-fin-staff-a-" + run + "@test.com",
                User.Role.PROVIDER_STAFF, tenantA);
        User parentA = saveUser("provider-fin-parent-a-" + run + "@test.com",
                User.Role.PARENT, tenantA);
        User orphanProvider = saveUserWithoutInstitution(
                "provider-fin-orphan-" + run + "@test.com", User.Role.PROVIDER_ADMIN);

        providerAdminAToken = TestTokens.userToken(providerAdminA.getEmail());
        providerStaffAToken = TestTokens.userToken(providerStaffA.getEmail());
        parentAToken = TestTokens.userToken(parentA.getEmail());
        studentAToken = TestTokens.studentToken();
        orphanProviderToken = TestTokens.userToken(orphanProvider.getEmail());

        ownPayment = savePayment(tenantA, "PFT-OWN-" + run);
        foreignPayment = savePayment(instB.getId(), "PFT-FOREIGN-" + run);
    }

    // ── the happy path is only happy when it is scoped ──

    @Test
    void providerAdmin_seesOnlyOwnInstitutionTransactions() throws Exception {
        mockMvc.perform(get("/v1/provider/financial/transactions")
                        .header("Authorization", "Bearer " + providerAdminAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].paymentId")
                        .value(hasItem(ownPayment.getId().toString())))
                .andExpect(jsonPath("$.data.content[*].paymentId")
                        .value(not(hasItem(foreignPayment.getId().toString()))));
    }

    @Test
    void providerStaff_seesOnlyOwnInstitutionTransactions() throws Exception {
        mockMvc.perform(get("/v1/provider/financial/transactions")
                        .header("Authorization", "Bearer " + providerStaffAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].paymentId")
                        .value(hasItem(ownPayment.getId().toString())))
                .andExpect(jsonPath("$.data.content[*].paymentId")
                        .value(not(hasItem(foreignPayment.getId().toString()))));
    }

    // ── client-supplied institution is never trusted ──

    @Test
    void providerAdmin_spoofedForeignInstitutionHeader_403() throws Exception {
        // Implemented behaviour: the organization-context filter rejects the request outright
        // before the controller runs, so a foreign header is a hard 403 - never an ignored
        // header that falls back to the caller's own scope, and never a foreign row set.
        mockMvc.perform(get("/v1/provider/financial/transactions")
                        .header("Authorization", "Bearer " + providerAdminAToken)
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    assertEquals(403, code,
                            "a foreign X-Institution-Id must be refused outright, got " + code);
                });
    }

    /**
     * Implemented behaviour: no parameter binds to the institution, so the spoofed query
     * parameter is ignored and the response carries the caller's own institution's rows only.
     */
    @Test
    void providerAdmin_spoofedInstitutionQueryParam_ignoredAndOwnRowsOnly() throws Exception {
        mockMvc.perform(get("/v1/provider/financial/transactions")
                        .param("institutionId", instB.getId().toString())
                        .header("Authorization", "Bearer " + providerAdminAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].paymentId")
                        .value(hasItem(ownPayment.getId().toString())))
                .andExpect(jsonPath("$.data.content[*].paymentId")
                        .value(not(hasItem(foreignPayment.getId().toString()))));
    }

    // ── a caller with no institution is refused, not defaulted to an unscoped query ──

    @Test
    void providerAdmin_withoutInstitution_403() throws Exception {
        mockMvc.perform(get("/v1/provider/financial/transactions")
                        .header("Authorization", "Bearer " + orphanProviderToken))
                .andExpect(status().isForbidden());
    }

    // ── roles that must not reach a provider money surface at all ──

    @Test
    void parent_roleForbidden_403() throws Exception {
        mockMvc.perform(get("/v1/provider/financial/transactions")
                        .header("Authorization", "Bearer " + parentAToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void student_roleForbidden_403() throws Exception {
        mockMvc.perform(get("/v1/provider/financial/transactions")
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isForbidden());
    }

    // ── fixtures ──

    private Institution saveInstitution(String name) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .code("PF-" + UUID.randomUUID().toString().substring(0, 8))
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());
    }

    private User saveUser(String email, User.Role role, UUID institutionId) {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("ProviderFin")
                .lastName("Tester")
                .role(role)
                .institutionId(institutionId)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        InstitutionMembership.Role membershipRole = switch (role) {
            case STUDENT -> InstitutionMembership.Role.STUDENT;
            case PARENT -> InstitutionMembership.Role.PARENT;
            case PROVIDER_ADMIN, PROVIDER_STAFF -> InstitutionMembership.Role.ADMIN;
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

    /** A provider-role account with no institution and no membership: must be refused outright. */
    private User saveUserWithoutInstitution(String email, User.Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("ProviderFin")
                .lastName("Orphan")
                .role(role)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
    }

    private Payment savePayment(UUID institutionId, String receipt) {
        Payment p = Payment.builder()
                .parentId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .serviceType("MONTHLY_ACCESS")
                .provider("stakaba")
                .providerReference("PFT_" + UUID.randomUUID())
                .amount(new BigDecimal("15000"))
                .currency("TZS")
                .status("COMPLETED")
                .paidAt(LocalDateTime.now().minusDays(1))
                .build();
        p.setInstitutionId(institutionId);
        p.setReceiptNumber(receipt);
        p.setDescription("Provider financial scope fixture");
        p.setCommissionAmount(new BigDecimal("1500"));
        p.setProviderShareAmount(new BigDecimal("13500"));
        p.setCommissionStatus("RESOLVED");
        return paymentRepository.saveAndFlush(p);
    }
}
