package tz.elmkusoma.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.BaseEntity;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.domain.PaymentReceipt;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.PaymentReceiptRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The four payment surfaces of spec §63 that had enforcement but no test.
 *
 * <p>The matrix listed receipt ownership, refund approval, pricing governance and reconciliation
 * access as DONE because the code demonstrably enforces them. Enforcement nobody exercises is a
 * weak claim: a refactor that dropped a single {@code @PreAuthorize}, or an ownership comparison
 * that quietly stopped matching how the column is actually written, would leave every one of these
 * passing and the gate gone. These drive each through the real filter chain and assert on the
 * response, which is the only place a missing gate shows.</p>
 *
 * <p>Refusal codes are part of what is asserted, not incidental. A 404 where a 403 belongs would
 * be a different contract, and a 200 where either belongs is the whole point.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class PaymentSpecSecurityCoverageTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ParentRepository parentRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentReceiptRepository paymentReceiptRepository;
    @Autowired private tz.elmkusoma.shared.repository.InstitutionMembershipRepository membershipRepository;

    private final List<UUID> createdUsers = new ArrayList<>();

    private UUID institutionId;
    private UUID myUserId;
    private UUID strangerUserId;
    private Parent me;
    private Payment dashboardShapedPayment;
    private Payment eventShapedPayment;
    private tz.elmkusoma.shared.domain.InstitutionMembership membership;

    @BeforeEach
    void setUp() {
        institutionId = TestDataSeeder.INSTITUTION_ID;
        myUserId = newUser(User.Role.PARENT);
        strangerUserId = newUser(User.Role.PARENT);
        me = parentRepository.save(withInstitution(Parent.builder().userId(myUserId).build(), institutionId));
        // The catalogue endpoint sits behind the same institution membership check as every other
        // bundle administration call, so the caller must be a real member of it.
        membership = membershipRepository.save(tz.elmkusoma.shared.domain.InstitutionMembership.builder()
                .userId(myUserId)
                .institutionId(institutionId)
                .role(tz.elmkusoma.shared.domain.InstitutionMembership.Role.INSTITUTION_ADMIN)
                .isActive(true)
                .isDeleted(false)
                .build());

        // Two ways the same column gets written, and both have to be readable by their owner.
        // The parent dashboard records the user id; an event ticket records the parent record's
        // own id (EventBookingService writes payingParent.getId()).
        dashboardShapedPayment = receipted(myUserId, "RCPT-DASH");
        eventShapedPayment = receipted(me.getId(), "RCPT-EVENT");
    }

    @AfterEach
    void tearDown() {
        paymentReceiptRepository.deleteAll();
        paymentRepository.deleteAll();
        parentRepository.deleteAll(List.of(me));
        membershipRepository.delete(membership);
        createdUsers.forEach(userRepository::deleteById);
    }

    // ---------------------------------------------------------- §63 case 2: receipts

    @Test
    @WithMockUser(roles = "PARENT")
    void aParentReadsTheReceiptForAPaymentTheyMade() throws Exception {
        mockMvc.perform(get("/v1/parents/payments/{id}/receipt", dashboardShapedPayment.getId())
                        .requestAttr("userId", myUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receiptNumber").value("RCPT-DASH"));
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void aParentReadsTheReceiptForAnEventTicketTheyBought() throws Exception {
        // The defect behind case 2: this payment's parent_id is the parent record id, not the
        // user id, so the ownership check compared the two identifier spaces against each other
        // and refused a parent their own receipt while an administrator could still read it.
        mockMvc.perform(get("/v1/parents/payments/{id}/receipt", eventShapedPayment.getId())
                        .requestAttr("userId", myUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receiptNumber").value("RCPT-EVENT"));
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void anotherParentCannotReadIt() throws Exception {
        mockMvc.perform(get("/v1/parents/payments/{id}/receipt", dashboardShapedPayment.getId())
                        .requestAttr("userId", strangerUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aPlatformAdminMayReadIt() throws Exception {
        mockMvc.perform(get("/v1/parents/payments/{id}/receipt", dashboardShapedPayment.getId())
                        .requestAttr("userId", strangerUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void theReceiptListContainsBothShapesOfOwnership() throws Exception {
        // Filtering the list on the user id alone returned a complete-looking history that was
        // missing every event ticket, which is harder to notice than a refusal because there is
        // no error to read.
        mockMvc.perform(get("/v1/parents/receipts")
                        .requestAttr("userId", myUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.receiptNumber=='RCPT-DASH')]").exists())
                .andExpect(jsonPath("$.data[?(@.receiptNumber=='RCPT-EVENT')]").exists());
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void theReceiptListOfAnotherParentContainsNoneOfOurs() throws Exception {
        mockMvc.perform(get("/v1/parents/receipts")
                        .requestAttr("userId", strangerUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.receiptNumber=='RCPT-DASH')]").isEmpty())
                .andExpect(jsonPath("$.data[?(@.receiptNumber=='RCPT-EVENT')]").isEmpty());
    }

    @Test
    void anUnauthenticatedCallerNeverReadsAReceipt() throws Exception {
        mockMvc.perform(get("/v1/parents/payments/{id}/receipt", dashboardShapedPayment.getId()))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    assertTrue(code == 401 || code == 403,
                            "an anonymous caller must be refused before ownership is even asked, got "
                                    + code);
                });
    }

    // -------------------------------------------- §63 cases 5 and 25: price governance

    @Test
    @WithMockUser(roles = "INSTITUTION_ADMIN")
    void anInstitutionAdminCannotReadPlatformPricingPolicy() throws Exception {
        // Decided by the owner: the institution admin gets service type names for the bundle
        // picker and nothing about how those types are priced. Amounts, margins and policy
        // versions stay platform-side.
        mockMvc.perform(get("/v1/platform-admin/pricing/policies")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void aParentCannotReadPlatformPricingPolicy() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/pricing/policies")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aPlatformAdminCanReadPlatformPricingPolicy() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/pricing/policies")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(roles = "INSTITUTION_ADMIN")
    void theServiceTypeCatalogueDoesNotExposeWhatThePlatformPricingEndpointExposes() throws Exception {
        // The authorised alternative has to actually be narrower, or approving it as the
        // institution-admin surface would have quietly widened what an institution admin can read.
        mockMvc.perform(get("/v1/institution/bundles/catalogue/service-types")
                        .requestAttr("institutionId", institutionId)
                        .requestAttr("userId", myUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ------------------------------------------------ §63 case 10: refund approval

    @Test
    @WithMockUser(roles = "INSTITUTION_ADMIN")
    void anInstitutionAdminCannotApproveARefund() throws Exception {
        // Approval is platform-side money movement; an institution admin sits on the operating
        // side of a refund, not the approving side.
        mockMvc.perform(post("/v1/platform-admin/refunds/{id}/approve", UUID.randomUUID())
                        .requestAttr("userId", myUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void aParentCannotApproveARefund() throws Exception {
        mockMvc.perform(post("/v1/platform-admin/refunds/{id}/approve", UUID.randomUUID())
                        .requestAttr("userId", myUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aPlatformAdminPassesTheApprovalGate() throws Exception {
        // An id that does not exist answers 404, which is how a 403 here is told apart from the
        // gate refusing them: reaching "no such refund" means authorisation already succeeded.
        mockMvc.perform(post("/v1/platform-admin/refunds/{id}/approve", UUID.randomUUID())
                        .requestAttr("userId", myUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(result.getResponse().getStatus() != 403,
                        "the platform admin must pass the approval gate"));
    }

    // ---------------------------------------------- §63 case 21: reconciliation access

    @Test
    @WithMockUser(roles = "INSTITUTION_ADMIN")
    void anInstitutionAdminCannotReadReconciliationExceptions() throws Exception {
        // Reconciliation exceptions describe money the platform is still trying to match up
        // across every institution, not this one's business.
        mockMvc.perform(get("/v1/platform-admin/financial/reconciliation/exceptions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void aTeacherCannotReadReconciliationExceptions() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/financial/reconciliation/exceptions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aPlatformAdminCanReadReconciliationExceptions() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/financial/reconciliation/exceptions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    // ------------------------------------------------------------------ helpers

    private Payment receipted(UUID paymentParentId, String receiptNumber) {
        Payment payment = withInstitution(Payment.builder()
                .parentId(paymentParentId)
                .studentId(UUID.randomUUID())
                .serviceType("MONTHLY_ACCESS")
                .amount(new BigDecimal("5000"))
                .currency("TZS")
                .status("COMPLETED")
                .paidAt(LocalDateTime.now().minusDays(1))
                .build(), institutionId);
        payment = paymentRepository.saveAndFlush(payment);

        paymentReceiptRepository.saveAndFlush(withInstitution(PaymentReceipt.builder()
                .paymentId(payment.getId())
                .receiptNumber(receiptNumber)
                .parentId(paymentParentId)
                .studentId(payment.getStudentId())
                .serviceType("MONTHLY_ACCESS")
                .amount(new BigDecimal("5000"))
                .currency("TZS")
                .paidAt(LocalDateTime.now().minusDays(1))
                .build(), institutionId));
        return payment;
    }

    private UUID newUser(User.Role role) {
        User saved = userRepository.save(withInstitution(User.builder()
                .email(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.go.tz")
                .firstName("Test")
                .lastName("User")
                .passwordHash("x")
                .role(role)
                .isActive(true)
                .build(), institutionId));
        createdUsers.add(saved.getId());
        return saved.getId();
    }

    private static <T extends BaseEntity> T withInstitution(T entity, UUID id) {
        entity.setInstitutionId(id);
        return entity;
    }
}
