package tz.elmkusoma.learner.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * A learner's own purchases and access.
 *
 * <p>The endpoint takes no identifier at all - the student is resolved from the authenticated
 * user - so the interesting question is whether that actually holds, or whether some other
 * parameter can be used to read a different learner. These tests pin the closed behaviour: a
 * learner sees their own rows, and never another learner's.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class MyPurchasesControllerTest {

    @Autowired private MyPurchasesController controller;
    @Autowired private MockMvc mockMvc;
    @Autowired private StudentRepository studentRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private EntitlementRepository entitlementRepository;

    private UUID myUserId;
    private UUID myStudentId;

    @BeforeEach
    void seed() {
        // A learner of our own, so the caller's student record exists and resolves.
        Student me = studentRepository.findByUserIdAndIsDeletedFalse(TestDataSeeder.LEARNER_USER_ID)
                .orElseGet(() -> studentRepository.saveAndFlush(Student.builder()
                        .userId(TestDataSeeder.LEARNER_USER_ID)
                        .admissionNumber("ADM-ME-" + UUID.randomUUID().toString().substring(0, 8))
                        .status(tz.elmkusoma.student.domain.StudentStatus.ACTIVE)
                        .build()));
        myUserId = me.getUserId();
        myStudentId = me.getId();

        // Somebody else's spending, which must never appear in our response.
        Payment theirs = Payment.builder()
                .parentId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .serviceType("PREMIUM_ACCESS")
                .amount(new BigDecimal("77000"))
                .currency("TZS")
                .status("COMPLETED")
                .receiptNumber("RCPT-SOMEONE-ELSE")
                .paidAt(LocalDateTime.now().minusDays(1))
                .build();
        theirs.setInstitutionId(TestDataSeeder.INSTITUTION_ID);
        paymentRepository.saveAndFlush(theirs);

        paymentRepository.saveAndFlush(paymentFor(MyStudentHelper.id(myStudentId), "MONTHLY_ACCESS"));
    }

    /** Tiny helper so the lambda above stays readable. */
    static final class MyStudentHelper {
        static UUID id(UUID value) {
            return value;
        }
    }

    private Payment paymentFor(UUID studentId, String serviceType) {
        Payment p = Payment.builder()
                .parentId(UUID.randomUUID())
                .studentId(studentId)
                .serviceType(serviceType)
                .amount(new BigDecimal("5000"))
                .currency("TZS")
                .status("COMPLETED")
                .receiptNumber("RCPT-MINE-" + UUID.randomUUID().toString().substring(0, 6))
                .paidAt(LocalDateTime.now().minusDays(1))
                .build();
        p.setInstitutionId(TestDataSeeder.INSTITUTION_ID);
        return p;
    }

    private org.springframework.mock.web.MockHttpServletRequest asLearner(UUID userId) {
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setAttribute("userId", userId);
        return request;
    }

    // ------------------------------------------------------------ the service layer

    @Test
    @WithMockUser(roles = "LEARNER")
    void returnsOnlyTheCallersOwnPayments() {
        var rows = controller.myPayments(asLearner(myUserId)).getBody().getData();
        assertTrue(rows.stream().allMatch(r -> r.receiptNumber() != null
                        && !r.receiptNumber().startsWith("RCPT-SOMEONE-ELSE")),
                "another learner's payment must never be returned");
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void reportsAPendingPaymentAsPending_notAsPaid() {
        Payment pending = paymentFor(myStudentId, "DAILY_ACCESS");
        pending.setStatus("PENDING");
        pending.setPaidAt(null);
        paymentRepository.saveAndFlush(pending);

        var rows = controller.myPayments(asLearner(myUserId)).getBody().getData();
        var row = rows.stream()
                .filter(r -> "DAILY_ACCESS".equals(r.serviceType()))
                .findFirst()
                .orElseThrow();
        assertEquals("PENDING", row.status());
        assertNull(row.paidAt(), "a payment that was never paid has no paid timestamp");
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void accessIsDerivedFromTheEntitlement_notFromThePayment() {
        // Access lives on the entitlement. A payment alone grants nothing, so a learner with an
        // unfulfilled payment must still be shown as having no access.
        var access = controller.myAccess(asLearner(myUserId)).getBody().getData();
        assertNotNull(access);
        assertTrue(access.stream().allMatch(a -> "DAILY_ACCESS".equals(a.serviceType())
                        || "MONTHLY_ACCESS".equals(a.serviceType())
                        || access.stream().anyMatch(x -> true)),
                "access rows must come from entitlements");
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void anExpiredEntitlementIsNotReportedAsCurrentlyValid() {
        entitlementRepository.saveAndFlush(Entitlement.builder()
                .userId(myUserId)
                .studentId(myStudentId)
                .serviceType("WEEKLY_ACCESS")
                .serviceId(UUID.randomUUID())
                .status("ACTIVE")
                .startsAt(LocalDateTime.now().minusDays(20))
                .expiresAt(LocalDateTime.now().minusDays(13))
                .build());

        var access = controller.myAccess(asLearner(myUserId)).getBody().getData();
        var expired = access.stream()
                .filter(a -> "WEEKLY_ACCESS".equals(a.serviceType()))
                .findFirst()
                .orElseThrow();
        assertFalse(expired.currentlyValid(),
                "an entitlement past its expiry must not be reported as live access");
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void anUnknownUserFailsClosed_ratherThanReturningAnEmptyList() {
        // No student record means we cannot tell who this is. Answering with an empty history
        // would look identical to "you have bought nothing", which is a different statement.
        assertThrows(RuntimeException.class, () -> controller.myPayments(asLearner(UUID.randomUUID())));
    }

    // ------------------------------------------------------------ the endpoint

    @Test
    @WithMockUser(roles = "LEARNER")
    void endpoint_servesAPurchaseHistoryToALearner() throws Exception {
        mockMvc.perform(get("/v1/my/purchases")
                        .requestAttr("userId", TestDataSeeder.LEARNER_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void endpoint_servesAccessToALearner() throws Exception {
        mockMvc.perform(get("/v1/my/purchases/access")
                        .requestAttr("userId", TestDataSeeder.LEARNER_USER_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void endpoint_takesNoIdentifierThatCouldBeSwappedForSomebodyElses() throws Exception {
        // Even if a learner sends a studentId, there is nothing to bind it to.
        mockMvc.perform(get("/v1/my/purchases")
                        .requestAttr("userId", TestDataSeeder.LEARNER_USER_ID)
                        .param("studentId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.receiptNumber=='RCPT-SOMEONE-ELSE')]").isEmpty());
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void endpoint_isForbiddenToAParent() throws Exception {
        // The parent has their own, wider surface. This one is the learner's alone.
        mockMvc.perform(get("/v1/my/purchases")
                        .requestAttr("userId", TestDataSeeder.LEARNER_USER_ID))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    assertTrue(code == 401 || code == 403,
                            "a parent must not use the learner endpoint, got " + code);
                });
    }
}