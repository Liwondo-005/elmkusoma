package tz.elmkusoma.finance.service;

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
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Institution earnings reporting.
 *
 * <p>Two properties matter more than the arithmetic. First, an administrator of one institution
 * must never see another's money - that is the whole point of scoping this endpoint separately
 * from the Platform Admin command centre. Second, a payment with no approved commission policy
 * must not be silently counted as zero commission, because that turns an undecided business rule
 * into a reported revenue figure.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class EarningsServiceTest {

    @Autowired private EarningsService service;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private MockMvc mockMvc;

    private static final UUID MINE = TestDataSeeder.INSTITUTION_ID;
    private static final UUID THEIRS = UUID.randomUUID();

    @BeforeEach
    void seed() {
        // Ours, with an approved rate.
        Payment allocated = completed(MINE, "100000", "MONTHLY_ACCESS");
        allocated.setCommissionAmount(new BigDecimal("10000"));
        allocated.setProviderShareAmount(new BigDecimal("90000"));
        allocated.setCommissionStatus("RESOLVED");
        allocated.setCommissionPolicyKey("STD");
        allocated.setCommissionPolicyVersion(1);
        paymentRepository.saveAndFlush(allocated);

        // Ours, no approved rate at all.
        Payment unresolved = completed(MINE, "20000", "CERTIFICATE_REPRINT");
        // commissionAmount, providerShareAmount and status all stay null.
        paymentRepository.saveAndFlush(unresolved);

        // Theirs, generously allocated - must never appear in our figures.
        Payment foreign = completed(THEIRS, "500000", "MONTHLY_ACCESS");
        foreign.setCommissionAmount(new BigDecimal("50000"));
        foreign.setProviderShareAmount(new BigDecimal("450000"));
        foreign.setCommissionStatus("RESOLVED");
        foreign.setCommissionPolicyKey("STD");
        foreign.setCommissionPolicyVersion(1);
        paymentRepository.saveAndFlush(foreign);
    }

    private Payment completed(UUID institutionId, String amount, String serviceType) {
        Payment p = Payment.builder()
                .parentId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .serviceType(serviceType)
                .provider("stakaba")
                .amount(new BigDecimal(amount))
                .currency("TZS")
                .status("COMPLETED")
                .paidAt(LocalDateTime.now().minusDays(1))
                .build();
        p.setInstitutionId(institutionId);
        return p;
    }

    @Test
    void grossCountsOnlyThisInstitutionsCompletedPayments() {
        var s = service.summaryFor(MINE, null, null);
        assertEquals(2, s.completedPaymentCount(),
                "two completed payments belong to this institution");
        assertEquals(0, new BigDecimal("120000").compareTo(s.grossCollected().value()));
    }

    @Test
    void anotherInstitutionsMoneyIsNeverIncluded() {
        var s = service.summaryFor(MINE, null, null);
        assertTrue(s.grossCollected().value().compareTo(new BigDecimal("500000")) < 0,
                "a foreign institution's large payment must not leak into this summary");
        assertEquals(0, new BigDecimal("10000").compareTo(s.platformCommission().value()),
                "commission is only ours: the foreign row's 50,000 must not appear");
        assertEquals(0, new BigDecimal("90000").compareTo(s.providerShare().value()));
    }

    @Test
    void aPaymentWithNoApprovedRateIsReportedUnallocated_notAsZeroCommission() {
        // The core honesty property. Folding an undecided rate into commission would invent a
        // revenue figure that no approved policy supports.
        var s = service.summaryFor(MINE, null, null);

        assertEquals(1, s.unallocatedCount(), "one completed payment has no approved rate");
        assertFalse(s.unallocatedValue().resolved(),
                "the unallocated bucket is explicitly not a resolved figure");
        assertEquals(0, new BigDecimal("20000").compareTo(s.unallocatedValue().value()));
        assertNotNull(s.unallocatedValue().note(),
                "an unresolved figure must carry the reason it is unresolved");

        // Commission counts only the one payment that genuinely had a rate.
        assertEquals(0, new BigDecimal("10000").compareTo(s.platformCommission().value()));
    }

    @Test
    void profitIsNeverReported_asANumber() {
        var s = service.summaryFor(MINE, null, null);
        assertNotNull(s.profitabilityNote());
        assertTrue(s.profitabilityNote().toLowerCase().contains("not available"),
                "the reason profit is absent must be stated, not implied");
    }

    @Test
    void pendingAndUnpaidPaymentsAreExcluded() {
        Payment pending = Payment.builder()
                .parentId(UUID.randomUUID()).studentId(UUID.randomUUID())
                .serviceType("MONTHLY_ACCESS").amount(new BigDecimal("99999"))
                .currency("TZS").status("PENDING")
                .build();
        pending.setInstitutionId(MINE);
        paymentRepository.saveAndFlush(pending);

        var s = service.summaryFor(MINE, null, null);
        assertEquals(2, s.completedPaymentCount(),
                "a pending payment is not collected revenue and must not be counted");
    }

    @Test
    void refundedAmountIsReportedSeparatelyFromGross() {
        Payment refunded = completed(MINE, "30000", "DAILY_ACCESS");
        refunded.setRefundedAmount(new BigDecimal("30000"));
        paymentRepository.saveAndFlush(refunded);

        var s = service.summaryFor(MINE, null, null);
        assertTrue(s.refunded().value().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(s.grossCollected().value().compareTo(s.refunded().value()) > 0,
                "refunds are a separate figure, not silently netted off gross");
    }

    // ------------------------------------------------------------ the endpoint

    @Test
    @WithMockUser(roles = "INSTITUTION_ADMIN")
    void endpoint_servesASummaryToAnInstitutionAdmin() throws Exception {
        mockMvc.perform(get("/v1/institution/earnings/summary")
                        .header("X-Institution-Id", MINE)
                        // The institution filter normally populates these attributes; MockMvc does
                        // not run it, so they are supplied the way sibling controller tests do.
                        .requestAttr("userRole", "INSTITUTION_ADMIN")
                        .requestAttr("userId", TestDataSeeder.ADMIN_USER_ID)
                        .requestAttr("institutionId", MINE))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void endpoint_isForbiddenToALearner() throws Exception {
        mockMvc.perform(get("/v1/institution/earnings/summary")
                        .header("X-Institution-Id", MINE)
                        .requestAttr("userRole", "STUDENT")
                        .requestAttr("userId", TestDataSeeder.STUDENT_USER_ID)
                        .requestAttr("institutionId", MINE))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    assertTrue(code == 401 || code == 403,
                            "a learner must not read institutional earnings, got " + code);
                });
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void endpoint_isForbiddenToATeacher() throws Exception {
        // A teacher has no financial scope in the current role model. Earnings per teacher is not
        // implemented, and this test pins that it stays closed rather than opening by accident.
        mockMvc.perform(get("/v1/institution/earnings/summary")
                        
                        .header("X-Institution-Id", MINE)
                        .requestAttr("userRole", "TEACHER")
                        .requestAttr("userId", TestDataSeeder.TEACHER_USER_ID)
                        .requestAttr("institutionId", MINE).header("X-Institution-Id", MINE))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    assertTrue(code == 401 || code == 403,
                            "a teacher must not read institutional earnings, got " + code);
                });
    }
}