package tz.elmkusoma.parent.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.domain.RefundRequest;
import tz.elmkusoma.parent.dto.ParentPaymentResponse;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.pricing.service.CommissionService;
import tz.elmkusoma.pricing.service.PricingService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParentPaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private ParentRepository parentRepository;
    @Mock private EntitlementRepository entitlementRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private PricingService pricingService;
    @Mock private tz.elmkusoma.pricing.service.DiscountService discountService;
    @Mock private tz.elmkusoma.pricing.service.CommissionService commissionService;
    @Mock private ReceiptService receiptService;
    @Mock private tz.elmkusoma.finance.service.ReconciliationService reconciliationService;
    @Mock private RefundService refundService;
    @Mock private tz.elmkusoma.subscription.service.SubscriptionService subscriptionService;
    @Mock private tz.elmkusoma.event.service.EventBookingService eventBookingService;
    @Mock private tz.elmkusoma.event.repository.EventRegistrationRepository eventRegistrationRepository;
    @Mock private tz.elmkusoma.bundle.service.BundleGrantService bundleGrantService;
    @Mock private tz.elmkusoma.bundle.service.BundleTopUpService bundleTopUpService;
    @Mock private PaymentEventNotificationService paymentEventNotificationService;

    @InjectMocks
    private ParentPaymentService paymentService;

    private static final UUID PARENT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID INSTITUTION_ID = UUID.randomUUID();
    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID REFUND_ID = UUID.randomUUID();

    private Payment buildPayment(String status, BigDecimal amount) {
        Payment p = new Payment();
        p.setId(PAYMENT_ID);
        p.setParentId(PARENT_ID);
        p.setStudentId(STUDENT_ID);
        p.setInstitutionId(INSTITUTION_ID);
        p.setAmount(amount);
        p.setCurrency("TZS");
        p.setStatus(status);
        p.setServiceType("TUITION");
        p.setCreatedAt(LocalDateTime.now().minusDays(5));
        return p;
    }

    @Test
    void getPayments_returnsCorrectSummary() {
        Payment completed = buildPayment("COMPLETED", new BigDecimal("100000"));
        Payment pending = buildPayment("PENDING", new BigDecimal("50000"));
        when(paymentRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtDesc(PARENT_ID))
                .thenReturn(List.of(completed, pending));

        ParentPaymentResponse result = paymentService.getPayments(PARENT_ID);

        assertEquals(new BigDecimal("100000"), result.getPaidThisTerm());
        assertEquals(new BigDecimal("50000"), result.getOutstanding());
        assertEquals(1, result.getRecentPayments().size());
        assertEquals(1, result.getPendingPayments().size());
    }

    @Test
    void getPaymentsByStudent_returnsPayments() {
        Payment p = buildPayment("COMPLETED", new BigDecimal("75000"));
        when(paymentRepository.findByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(STUDENT_ID))
                .thenReturn(List.of(p));

        var result = paymentService.getPaymentsByStudent(STUDENT_ID);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("75000"), result.get(0).getAmount());
    }

    // ------------------------------------------------------------------
    // Gateway reference masking (Payment.md §28/§45, UI-06)
    // ------------------------------------------------------------------

    @Test
    void getPayments_neverHandsThePayerARawGatewayReference() {
        Payment completed = buildPayment("COMPLETED", new BigDecimal("100000"));
        completed.setProviderReference("PSP-REF-9911");
        when(paymentRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtDesc(PARENT_ID))
                .thenReturn(List.of(completed));

        ParentPaymentResponse result = paymentService.getPayments(PARENT_ID);

        assertEquals(1, result.getRecentPayments().size());
        assertEquals("****9911", result.getRecentPayments().get(0).getProviderReference());
    }

    @Test
    void maskProviderReference_keepsOnlyTheLastFourCharacters() {
        assertEquals("****9911", ParentPaymentService.maskProviderReference("PSP-REF-9911"));
        assertEquals("****C123", ParentPaymentService.maskProviderReference("M_ABC123"));
        // Leading or trailing whitespace is not part of the token and must not shift the
        // four characters that are kept.
        assertEquals("****9911", ParentPaymentService.maskProviderReference(" PSP-REF-9911 "));
    }

    @Test
    void maskProviderReference_leavesNothingToReadFromAShortOrMissingValue() {
        assertNull(ParentPaymentService.maskProviderReference(null));
        assertNull(ParentPaymentService.maskProviderReference(""));
        assertNull(ParentPaymentService.maskProviderReference("   "));
        // Four characters or fewer is the whole token, so masking any of it would leave the
        // entire reference on screen.
        assertEquals("****", ParentPaymentService.maskProviderReference("ABCD"));
        assertEquals("*", ParentPaymentService.maskProviderReference("A"));
    }

    @Test
    void maskProviderReference_isIdempotentSoTwoLayersCannotMaskItTwice() {
        String once = ParentPaymentService.maskProviderReference("PSP-REF-9911");
        assertEquals(once, ParentPaymentService.maskProviderReference(once));
        assertEquals("****9911", ParentPaymentService.maskProviderReference("****9911"));
    }

    // ------------------------------------------------------------------
    // Status timeline (Payment.md §29, UI-05)
    // ------------------------------------------------------------------

    private static AuditLog paymentAudit(LocalDateTime at, Map<String, Object> oldValues,
                                          Map<String, Object> newValues) {
        AuditLog entry = new AuditLog();
        entry.setEntityType("Payment");
        entry.setEntityId(PAYMENT_ID);
        entry.setCreatedAt(at);
        entry.setOldValues(oldValues);
        entry.setNewValues(newValues);
        return entry;
    }

    @Test
    void getPaymentTimeline_reportsTheStepsThatWereActuallyRecorded() {
        Payment payment = buildPayment("COMPLETED", new BigDecimal("100000"));
        LocalDateTime created = payment.getCreatedAt();
        LocalDateTime initiated = created.plusMinutes(10);
        LocalDateTime settled = created.plusHours(2);
        payment.setPaidAt(settled);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(auditLogRepository.findByEntityTypeAndEntityId("Payment", PAYMENT_ID)).thenReturn(List.of(
                paymentAudit(settled, null,
                        Map.of("status", "COMPLETED", "paidAt", settled.toString())),
                paymentAudit(initiated, Map.of("status", "PENDING"),
                        Map.of("providerReference", "PSP-REF-9911"))));

        var timeline = paymentService.getPaymentTimeline(PAYMENT_ID, PARENT_ID);

        assertEquals(3, timeline.size());
        assertEquals("CREATED", timeline.get(0).getKind());
        assertEquals("PAYMENT", timeline.get(0).getSource());
        assertEquals(created, timeline.get(0).getAt());
        // The initiation row only named the status in force then, so it is a snapshot.
        assertEquals("STATUS", timeline.get(1).getKind());
        assertEquals("PENDING", timeline.get(1).getToStatus());
        assertNull(timeline.get(1).getFromStatus());
        assertEquals("AUDIT", timeline.get(1).getSource());
        assertEquals("COMPLETED", timeline.get(2).getToStatus());
        // paidAt is the same moment the audit row recorded; one settlement, not two.
        assertEquals(1, timeline.stream()
                .filter(e -> "COMPLETED".equals(e.getToStatus())).count());
    }

    @Test
    void getPaymentTimeline_addsTheSettlementMomentOnlyWhenNoAuditRowRecordedIt() {
        Payment payment = buildPayment("COMPLETED", new BigDecimal("100000"));
        LocalDateTime settled = payment.getCreatedAt().plusHours(3);
        payment.setPaidAt(settled);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(auditLogRepository.findByEntityTypeAndEntityId("Payment", PAYMENT_ID))
                .thenReturn(List.of());

        var timeline = paymentService.getPaymentTimeline(PAYMENT_ID, PARENT_ID);

        assertEquals(2, timeline.size());
        assertEquals("SETTLED", timeline.get(1).getKind());
        assertEquals("COMPLETED", timeline.get(1).getToStatus());
        assertEquals("PAYMENT", timeline.get(1).getSource());
        assertEquals(settled, timeline.get(1).getAt());
    }

    @Test
    void getPaymentTimeline_neverInventsAStepThatWasNotRecorded() {
        Payment payment = buildPayment("PENDING", new BigDecimal("50000"));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(auditLogRepository.findByEntityTypeAndEntityId("Payment", PAYMENT_ID))
                .thenReturn(List.of());

        var timeline = paymentService.getPaymentTimeline(PAYMENT_ID, PARENT_ID);

        assertEquals(1, timeline.size());
        assertEquals("CREATED", timeline.get(0).getKind());
        assertNull(timeline.get(0).getFromStatus());
        assertNull(timeline.get(0).getToStatus());
        assertEquals("PAYMENT", timeline.get(0).getSource());
        assertEquals(payment.getCreatedAt(), timeline.get(0).getAt());
    }

    @Test
    void getPaymentTimeline_ignoresAuditRowsThatSayNothingAboutStatus() {
        Payment payment = buildPayment("PENDING", new BigDecimal("50000"));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(auditLogRepository.findByEntityTypeAndEntityId("Payment", PAYMENT_ID)).thenReturn(List.of(
                paymentAudit(payment.getCreatedAt().plusMinutes(1),
                        Map.of("amount", "50000"), Map.of("note", "metadata refresh"))));

        var timeline = paymentService.getPaymentTimeline(PAYMENT_ID, PARENT_ID);

        assertEquals(1, timeline.size());
    }

    @Test
    void getPaymentTimeline_ordersStepsByWhenTheyHappenedNotByWhenTheyWereReturned() {
        Payment payment = buildPayment("COMPLETED", new BigDecimal("100000"));
        LocalDateTime created = payment.getCreatedAt();
        LocalDateTime failed = created.plusHours(1);
        LocalDateTime completed = created.plusHours(2);
        payment.setPaidAt(completed);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        // deliberately newest first, the order the repository returns rows in
        when(auditLogRepository.findByEntityTypeAndEntityId("Payment", PAYMENT_ID)).thenReturn(List.of(
                paymentAudit(completed, Map.of("status", "PENDING"), Map.of("status", "COMPLETED")),
                paymentAudit(failed, Map.of("status", "PENDING"), Map.of("status", "FAILED"))));

        var timeline = paymentService.getPaymentTimeline(PAYMENT_ID, PARENT_ID);

        assertEquals(List.of("CREATED", "STATUS", "STATUS"),
                timeline.stream().map(e -> e.getKind()).toList());
        assertEquals("FAILED", timeline.get(1).getToStatus());
        assertEquals("COMPLETED", timeline.get(2).getToStatus());
    }

    @Test
    void getPaymentTimeline_refusesAPaymentThatIsNotTheCallers() {
        Payment payment = buildPayment("PENDING", new BigDecimal("50000"));
        payment.setParentId(UUID.randomUUID());
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(parentRepository.findByUserIdAndIsDeletedFalse(PARENT_ID))
                .thenReturn(Optional.empty());

        var foreign = assertThrows(tz.elmkusoma.exception.ResourceNotFoundException.class,
                () -> paymentService.getPaymentTimeline(PAYMENT_ID, PARENT_ID));

        UUID unknownId = UUID.randomUUID();
        when(paymentRepository.findById(unknownId)).thenReturn(Optional.empty());
        var unknown = assertThrows(tz.elmkusoma.exception.ResourceNotFoundException.class,
                () -> paymentService.getPaymentTimeline(unknownId, PARENT_ID));

        // Identical wording, so the answer cannot be used to tell a real id from a made-up one.
        assertEquals(unknown.getMessage(), foreign.getMessage());
        // No audit row is read before ownership is established.
        verify(auditLogRepository, never())
                .findByEntityTypeAndEntityId(any(String.class), any(UUID.class));
    }

    @Test
    void getPaymentTimeline_acceptsAPayerWhoPaidUnderTheirParentRecord() {
        Payment payment = buildPayment("PENDING", new BigDecimal("50000"));
        UUID parentRecordId = UUID.randomUUID();
        payment.setParentId(parentRecordId);
        Parent parentRecord = new Parent();
        parentRecord.setId(parentRecordId);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(parentRepository.findByUserIdAndIsDeletedFalse(PARENT_ID))
                .thenReturn(Optional.of(parentRecord));
        when(auditLogRepository.findByEntityTypeAndEntityId("Payment", PAYMENT_ID))
                .thenReturn(List.of());

        assertEquals(1, paymentService.getPaymentTimeline(PAYMENT_ID, PARENT_ID).size());
    }

    @Test
    void getPaymentTimeline_refusesADeletedPayment() {
        Payment payment = buildPayment("PENDING", new BigDecimal("50000"));
        payment.setIsDeleted(true);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

        var thrown = assertThrows(tz.elmkusoma.exception.ResourceNotFoundException.class,
                () -> paymentService.getPaymentTimeline(PAYMENT_ID, PARENT_ID));

        assertEquals("Payment not found", thrown.getMessage());
        verify(auditLogRepository, never())
                .findByEntityTypeAndEntityId(any(String.class), any(UUID.class));
    }

    private static final tz.elmkusoma.pricing.service.DiscountService.ResolvedDiscount NO_DISCOUNT =
            tz.elmkusoma.pricing.service.DiscountService.ResolvedDiscount.none();

    private static CommissionService.Allocation resolvedAllocation(BigDecimal net) {
        return new CommissionService.Allocation(
                CommissionService.AllocationStatus.RESOLVED,
                net, java.math.BigDecimal.ZERO, net,
                java.math.BigDecimal.ZERO, net,
                "PLATFORM_COMMISSION", 1, null, null);
    }

    @Test
    void initiatePayment_usesTheAuthoritativePolicyPrice_notTheCallerAmount() {
        when(pricingService.resolvePrice("TUITION", null))
                .thenReturn(new PricingService.ResolvedPrice(
                        new BigDecimal("100000"), "TZS", 1, "SERVICE_DEFAULT_PRICE", 30));
        when(discountService.resolve("TUITION", new BigDecimal("100000"))).thenReturn(NO_DISCOUNT);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment pay = invocation.getArgument(0);
            pay.setId(UUID.randomUUID());
            return pay;
        });

        Payment result = paymentService.initiatePayment(PARENT_ID, STUDENT_ID, INSTITUTION_ID,
                new BigDecimal("100000"), "TUITION", null, "Term 1 fees");

        assertNotNull(result.getId());
        assertEquals("PENDING", result.getStatus());
        assertEquals("TZS", result.getCurrency());
        // The stored amount comes from the policy, so a caller can never underpay by sending a
        // smaller figure in the request body.
        assertEquals(0, new BigDecimal("100000").compareTo(result.getAmount()));
        // The terms are snapshotted so a later price change cannot rewrite what was charged.
        assertEquals("SERVICE_DEFAULT_PRICE", result.getPolicyKey());
        assertEquals(1, result.getPolicyVersion());
        assertEquals(0, new BigDecimal("100000").compareTo(result.getGrossAmount()));
        assertEquals(0, new BigDecimal("100000").compareTo(result.getNetAmount()));
        assertEquals(30, result.getAccessDays());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void aBundleIsChargedTheTopUpTotalNotTheHeadlinePrice() {
        // BR-03 end to end. The bundle's policy price is 100000, but this learner already holds one
        // of its two services, so the authoritative figure is the other service alone. Validating
        // the caller against the headline price is what made the feature unusable: the correct
        // amount was rejected as a mismatch with the very policy that produced it.
        when(bundleGrantService.isBundleServiceType("TERM_BUNDLE")).thenReturn(true);
        when(pricingService.resolvePrice("TERM_BUNDLE", null))
                .thenReturn(new PricingService.ResolvedPrice(
                        new BigDecimal("100000"), "TZS", 1, "SERVICE_DEFAULT_PRICE", 30));
        when(bundleTopUpService.quoteByServiceType(
                org.mockito.ArgumentMatchers.eq("TERM_BUNDLE"),
                org.mockito.ArgumentMatchers.eq(STUDENT_ID),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(new tz.elmkusoma.bundle.service.BundleTopUpService.TopUpQuote(
                        List.of(), new BigDecimal("60000"), "TZS", false, 1, 1));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment pay = invocation.getArgument(0);
            pay.setId(UUID.randomUUID());
            return pay;
        });

        Payment result = paymentService.initiatePayment(PARENT_ID, STUDENT_ID, INSTITUTION_ID,
                new BigDecimal("60000"), "TERM_BUNDLE", null, "Top up");

        assertEquals(0, new BigDecimal("60000").compareTo(result.getAmount()),
                "the stored amount must be what this learner actually owes, not the bundle price");
        assertEquals(30, result.getAccessDays(), "terms are still snapshotted from the policy");
        verify(discountService, org.mockito.Mockito.never()).resolve(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void aBundlePurchaseCannotBeInitiatedWhenNothingIsLeftToBuy() {
        when(bundleGrantService.isBundleServiceType("TERM_BUNDLE")).thenReturn(true);
        when(pricingService.resolvePrice("TERM_BUNDLE", null))
                .thenReturn(new PricingService.ResolvedPrice(
                        new BigDecimal("100000"), "TZS", 1, "SERVICE_DEFAULT_PRICE", 30));
        when(bundleTopUpService.quoteByServiceType(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(new tz.elmkusoma.bundle.service.BundleTopUpService.TopUpQuote(
                        List.of(), BigDecimal.ZERO, "TZS", true, 0, 2));

        assertThrows(IllegalStateException.class, () -> paymentService.initiatePayment(
                        PARENT_ID, STUDENT_ID, INSTITUTION_ID, BigDecimal.ZERO,
                        "TERM_BUNDLE", null, "Already covered"),
                "taking money for access the learner already holds is not a purchase");
        verify(paymentRepository, org.mockito.Mockito.never()).save(any(Payment.class));
    }

    @Test
    void aBundlePurchaseStillCannotBeUnderpaidByNamingAnAmount() {
        // The quote being server-side must not become a hole. Whether the caller sends the right
        // figure or a cheap one, what gets stored is the server's top-up total - the caller's
        // number is never the one that reaches the gateway.
        when(bundleGrantService.isBundleServiceType("TERM_BUNDLE")).thenReturn(true);
        when(pricingService.resolvePrice("TERM_BUNDLE", null))
                .thenReturn(new PricingService.ResolvedPrice(
                        new BigDecimal("100000"), "TZS", 1, "SERVICE_DEFAULT_PRICE", 30));
        when(bundleTopUpService.quoteByServiceType(anyString(), any(), any()))
                .thenReturn(new tz.elmkusoma.bundle.service.BundleTopUpService.TopUpQuote(
                        List.of(), new BigDecimal("60000"), "TZS", false, 1, 1));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment pay = invocation.getArgument(0);
            pay.setId(UUID.randomUUID());
            return pay;
        });

        Payment result = paymentService.initiatePayment(PARENT_ID, STUDENT_ID, INSTITUTION_ID,
                new BigDecimal("1"), "TERM_BUNDLE", null, "Cheap bundle");

        assertEquals(0, new BigDecimal("60000").compareTo(result.getAmount()),
                "a caller naming TZS 1 must not become the stored amount");
    }

    @Test
    void initiatePayment_deductsAnApprovedDiscountServerSide() {
        when(pricingService.resolvePrice("ONLINE_COURSE", null))
                .thenReturn(new PricingService.ResolvedPrice(
                        new BigDecimal("2000"), "TZS", 1, "SERVICE_DEFAULT_PRICE", 30));
        // The discount is resolved by the server from an ACTIVE rule, never sent by the caller.
        when(discountService.resolve("ONLINE_COURSE", new BigDecimal("2000")))
                .thenReturn(new tz.elmkusoma.pricing.service.DiscountService.ResolvedDiscount(
                        new BigDecimal("200.00"), "PROMO", 2, "Launch offer", true));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment pay = invocation.getArgument(0);
            pay.setId(UUID.randomUUID());
            return pay;
        });

        Payment result = paymentService.initiatePayment(PARENT_ID, STUDENT_ID, INSTITUTION_ID,
                new BigDecimal("2000"), "ONLINE_COURSE", null, "Course");

        assertEquals(0, new BigDecimal("2000").compareTo(result.getGrossAmount()));
        assertEquals(0, new BigDecimal("200.00").compareTo(result.getDiscountAmount()));
        assertEquals(0, new BigDecimal("1800.00").compareTo(result.getNetAmount()));
        assertEquals(0, new BigDecimal("1800.00").compareTo(result.getAmount()));
    }

    @Test
    void initiatePayment_replaysOnTheSameIdempotencyKeyInsteadOfChargingTwice() {
        when(pricingService.resolvePrice("TUITION", null))
                .thenReturn(new PricingService.ResolvedPrice(
                        new BigDecimal("100000"), "TZS", 1, "SERVICE_DEFAULT_PRICE", 30));
        when(discountService.resolve("TUITION", new BigDecimal("100000"))).thenReturn(NO_DISCOUNT);

        Payment first = buildPayment("PENDING", new BigDecimal("100000"));
        when(paymentRepository.findByIdempotencyKeyAndIsDeletedFalse("key-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(first));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment pay = invocation.getArgument(0);
            pay.setId(UUID.randomUUID());
            return pay;
        });

        Payment created = paymentService.initiatePayment(PARENT_ID, STUDENT_ID, INSTITUTION_ID,
                new BigDecimal("100000"), "TUITION", null, "Term 1 fees", "key-1");
        Payment replayed = paymentService.initiatePayment(PARENT_ID, STUDENT_ID, INSTITUTION_ID,
                new BigDecimal("100000"), "TUITION", null, "Term 1 fees", "key-1");

        // Only one payment is ever written; the retry returns the one already created.
        assertEquals(first.getId(), replayed.getId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        assertNotNull(created.getId());
    }

    @Test
    void initiatePayment_rejectsACallerAmountThatDisagreesWithThePolicy() {
        when(pricingService.resolvePrice("TUITION", null))
                .thenReturn(new PricingService.ResolvedPrice(
                        new BigDecimal("100000"), "TZS", 1, "SERVICE_DEFAULT_PRICE", 30));
        // The service reports a mismatch rather than storing it, and nothing is persisted.
        doThrow(new PricingService.PricingConflictException("amount mismatch"))
                .when(pricingService).requireAmountMatches(eq(new BigDecimal("1")), any());

        assertThrows(PricingService.PricingConflictException.class, () ->
                paymentService.initiatePayment(PARENT_ID, STUDENT_ID, INSTITUTION_ID,
                        new BigDecimal("1"), "TUITION", null, "Term 1 fees"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void verifyPayment_marksCompleteAndGrantsEntitlement() {
        Payment pending = buildPayment("PENDING", new BigDecimal("100000"));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(entitlementRepository.findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                STUDENT_ID, "TUITION", null)).thenReturn(Optional.empty());
        when(entitlementRepository.save(any(Entitlement.class))).thenAnswer(i -> i.getArgument(0));
        when(commissionService.allocate(eq("TUITION"), any(), any(), any()))
                .thenReturn(resolvedAllocation(new BigDecimal("100000")));
        when(receiptService.issueFor(any(Payment.class), any())).thenReturn(null);

        Payment result = paymentService.verifyPayment(PAYMENT_ID, "ref-123", UUID.randomUUID());

        assertEquals("COMPLETED", result.getStatus());
        assertEquals("ref-123", result.getProviderReference());
        assertNotNull(result.getPaidAt());
        verify(entitlementRepository).save(any(Entitlement.class));
        // The allocation is stamped onto the transaction at confirmation, not recomputed later.
        verify(commissionService).allocate(eq("TUITION"), any(), any(), any());
        // A receipt is issued exactly once, for the confirmed payment.
        verify(receiptService).issueFor(any(Payment.class), any());
    }

    @Test
    void verifyPayment_stillCompletesWhenReceiptIssuanceFails() {
        // A missing document must never erase a real confirmed transaction.
        Payment pending = buildPayment("PENDING", new BigDecimal("5000"));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(entitlementRepository.findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                STUDENT_ID, "TUITION", null)).thenReturn(Optional.empty());
        when(entitlementRepository.save(any(Entitlement.class))).thenAnswer(i -> i.getArgument(0));
        when(commissionService.allocate(eq("TUITION"), any(), any(), any()))
                .thenReturn(resolvedAllocation(new BigDecimal("5000")));
        when(receiptService.issueFor(any(Payment.class), any()))
                .thenThrow(new IllegalStateException("receipt store unavailable"));

        Payment result = paymentService.verifyPayment(PAYMENT_ID, "ref-999", UUID.randomUUID());

        assertEquals("COMPLETED", result.getStatus());
        verify(reconciliationService).raiseReceiptFailure(any(Payment.class), any(Exception.class));
    }

    @Test
    void verifyPayment_idempotent_whenAlreadyCompleted() {
        Payment completed = buildPayment("COMPLETED", new BigDecimal("100000"));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completed));

        Payment result = paymentService.verifyPayment(PAYMENT_ID, "ref-456", UUID.randomUUID());

        assertEquals("COMPLETED", result.getStatus());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void verifyPayment_handsOverTheEventSeatTheTicketPaidFor() {
        // confirmSeat had no caller anywhere in main, so a paid booking collected the money and
        // never granted the place. The verified, persisted payment is the authority for the seat,
        // so verification must drive the handover.
        Payment pending = buildPayment("PENDING", new BigDecimal("5000"));
        pending.setServiceType(tz.elmkusoma.event.service.EventBookingService.SERVICE_TYPE);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(entitlementRepository.findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                eq(STUDENT_ID), anyString(), isNull())).thenReturn(Optional.empty());
        when(entitlementRepository.save(any(Entitlement.class))).thenAnswer(i -> i.getArgument(0));
        when(commissionService.allocate(anyString(), any(), any(), any()))
                .thenReturn(resolvedAllocation(new BigDecimal("5000")));
        when(receiptService.issueFor(any(Payment.class), any())).thenReturn(null);

        var registration = new tz.elmkusoma.event.domain.EventRegistration();
        registration.setId(UUID.randomUUID());
        when(eventRegistrationRepository.findByPaymentIdAndIsDeletedFalse(eq(PAYMENT_ID)))
                .thenReturn(Optional.of(registration));

        Payment result = paymentService.verifyPayment(PAYMENT_ID, "ref-ticket", UUID.randomUUID());

        assertEquals("COMPLETED", result.getStatus());
        verify(eventBookingService).confirmSeat(registration.getId());
    }

    @Test
    void verifyPayment_doesNotTouchSeatsForANonEventPayment() {
        Payment pending = buildPayment("PENDING", new BigDecimal("100000"));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(entitlementRepository.findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                eq(STUDENT_ID), eq("TUITION"), isNull())).thenReturn(Optional.empty());
        when(entitlementRepository.save(any(Entitlement.class))).thenAnswer(i -> i.getArgument(0));
        when(commissionService.allocate(eq("TUITION"), any(), any(), any()))
                .thenReturn(resolvedAllocation(new BigDecimal("100000")));
        when(receiptService.issueFor(any(Payment.class), any())).thenReturn(null);

        paymentService.verifyPayment(PAYMENT_ID, "ref-tui", UUID.randomUUID());

        verify(eventBookingService, never()).confirmSeat(any());
    }

    private Payment subscriptionPayment(String status) {
        Payment p = buildPayment(status, new BigDecimal("30000"));
        p.setServiceType("MONTHLY_ACCESS");
        p.setAccessDays(30);
        return p;
    }

    @Test
    void verifyPayment_opensTheAuditableSubscriptionForASubscriptionTier() {
        // SubscriptionService existed but nothing called it, so a month bought for money produced
        // an entitlement and no subscription record at all - leaving cancellation and expiry with
        // no row to act on and the sold period reconstructible only from the entitlement.
        Payment pending = subscriptionPayment("PENDING");
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(entitlementRepository.findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                eq(STUDENT_ID), eq("MONTHLY_ACCESS"), isNull())).thenReturn(Optional.empty());
        when(entitlementRepository.save(any(Entitlement.class))).thenAnswer(i -> i.getArgument(0));
        when(commissionService.allocate(eq("MONTHLY_ACCESS"), any(), any(), any()))
                .thenReturn(resolvedAllocation(new BigDecimal("30000")));
        when(receiptService.issueFor(any(Payment.class), any())).thenReturn(null);

        Payment result = paymentService.verifyPayment(PAYMENT_ID, "ref-sub", UUID.randomUUID());

        assertEquals("COMPLETED", result.getStatus());
        verify(subscriptionService).activateFromVerifiedPayment(any(Payment.class), any(Entitlement.class));
    }

    @Test
    void verifyPayment_doesNotOpenASubscriptionForASingleServicePurchase() {
        // An event ticket or a one-off course purchase has no recurring period. Offering it to the
        // subscription service would be asking it to invent a period nobody priced.
        Payment pending = buildPayment("PENDING", new BigDecimal("100000"));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(entitlementRepository.findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                eq(STUDENT_ID), eq("TUITION"), isNull())).thenReturn(Optional.empty());
        when(entitlementRepository.save(any(Entitlement.class))).thenAnswer(i -> i.getArgument(0));
        when(commissionService.allocate(eq("TUITION"), any(), any(), any()))
                .thenReturn(resolvedAllocation(new BigDecimal("100000")));
        when(receiptService.issueFor(any(Payment.class), any())).thenReturn(null);

        paymentService.verifyPayment(PAYMENT_ID, "ref-tui", UUID.randomUUID());

        verify(subscriptionService, never()).activateFromVerifiedPayment(any(), any());
    }

    @Test
    void verifyPayment_stillCompletesWhenSubscriptionActivationFails() {
        // Same rule as the receipt: money was really collected and access was really granted, so a
        // derived reporting record failing must not erase the payment.
        Payment pending = subscriptionPayment("PENDING");
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(entitlementRepository.findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                eq(STUDENT_ID), eq("MONTHLY_ACCESS"), isNull())).thenReturn(Optional.empty());
        when(entitlementRepository.save(any(Entitlement.class))).thenAnswer(i -> i.getArgument(0));
        when(commissionService.allocate(eq("MONTHLY_ACCESS"), any(), any(), any()))
                .thenReturn(resolvedAllocation(new BigDecimal("30000")));
        when(receiptService.issueFor(any(Payment.class), any())).thenReturn(null);
        org.mockito.Mockito.doThrow(new IllegalStateException("subscription store unavailable"))
                .when(subscriptionService).activateFromVerifiedPayment(any(), any());

        Payment result = paymentService.verifyPayment(PAYMENT_ID, "ref-fail", UUID.randomUUID());

        assertEquals("COMPLETED", result.getStatus());
        verify(reconciliationService).raiseReceiptFailure(any(Payment.class), any(Exception.class));
    }

    private Entitlement activeEntitlement(LocalDateTime startsAt, LocalDateTime expiresAt) {
        return Entitlement.builder()
                .userId(PARENT_ID)
                .studentId(STUDENT_ID)
                .institutionId(INSTITUTION_ID)
                .serviceType("TRANSPORT")
                .serviceId(null)
                .paymentId(PAYMENT_ID)
                .status("ACTIVE")
                .startsAt(startsAt)
                .expiresAt(expiresAt)
                .build();
    }

    @Test
    void hasEntitlement_returnsTrue_whenActiveAndUnexpired() {
        when(entitlementRepository.findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                STUDENT_ID, "TRANSPORT", null))
                .thenReturn(Optional.of(activeEntitlement(
                        LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(20))));
        assertTrue(paymentService.hasEntitlement(STUDENT_ID, "TRANSPORT", null));
    }

    @Test
    void hasEntitlement_returnsFalse_whenNotActive() {
        Entitlement revoked = activeEntitlement(LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(20));
        revoked.setStatus("REVOKED");
        when(entitlementRepository.findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                STUDENT_ID, "TRANSPORT", null)).thenReturn(Optional.of(revoked));
        assertFalse(paymentService.hasEntitlement(STUDENT_ID, "TRANSPORT", null));
    }

    @Test
    void hasEntitlement_returnsFalse_afterExpiry_evenThoughStatusIsStillActive() {
        // A lapsed entitlement is never flipped by anything, so status alone stays ACTIVE
        // forever. Reading expiry as well is what stops paid content being served after
        // the access the learner paid for has run out.
        when(entitlementRepository.findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                STUDENT_ID, "TRANSPORT", null))
                .thenReturn(Optional.of(activeEntitlement(
                        LocalDateTime.now().minusDays(40), LocalDateTime.now().minusDays(10))));
        assertFalse(paymentService.hasEntitlement(STUDENT_ID, "TRANSPORT", null));
    }

    @Test
    void hasEntitlement_returnsFalse_beforeStartsAt() {
        when(entitlementRepository.findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(
                STUDENT_ID, "TRANSPORT", null))
                .thenReturn(Optional.of(activeEntitlement(
                        LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30))));
        assertFalse(paymentService.hasEntitlement(STUDENT_ID, "TRANSPORT", null));
    }

    @Test
    void accessDuration_isRollingAndServiceSpecific() {
        assertEquals(java.time.Duration.ofHours(24),
                ParentPaymentService.accessDurationFor("DAILY_ACCESS"));
        assertEquals(java.time.Duration.ofDays(7),
                ParentPaymentService.accessDurationFor("WEEKLY_ACCESS"));
        assertEquals(java.time.Duration.ofDays(30),
                ParentPaymentService.accessDurationFor("MONTHLY_ACCESS"));
        assertEquals(java.time.Duration.ofDays(30),
                ParentPaymentService.accessDurationFor("PREMIUM_ACCESS"));
        // Case-insensitive, so a catalogue value written in any casing resolves the same.
        assertEquals(java.time.Duration.ofHours(24),
                ParentPaymentService.accessDurationFor("daily_access"));
        // An ordinary service purchase keeps the historical 30-day grant.
        assertEquals(java.time.Duration.ofDays(30),
                ParentPaymentService.accessDurationFor("TUITION"));
        assertEquals(java.time.Duration.ofDays(30),
                ParentPaymentService.accessDurationFor((String) null));
    }

    @Test
    void refundPayment_delegatesToTheGovernedLifecycle_ratherThanMovingMoneyItself() {
        // The legacy endpoint used to be a second money-moving path: it set the status and
        // revoked the entitlement but never recorded refundedAmount, so the payment still looked
        // fully refundable and the governed lifecycle would refund it a second time. It now runs
        // the whole lifecycle, which is the only writer of refundedAmount.
        Payment completed = buildPayment("COMPLETED", new BigDecimal("5000"));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(completed));
        RefundRequest opened = RefundRequest.builder().id(REFUND_ID).build();
        when(refundService.requestRefund(eq(PAYMENT_ID), any(), any(), any(), any()))
                .thenReturn(opened);
        Payment refunded = buildPayment("REFUNDED", new BigDecimal("5000"));
        refunded.setRefundedAmount(new BigDecimal("5000"));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(refunded));

        UUID admin = UUID.randomUUID();
        Payment result = paymentService.refundPayment(PAYMENT_ID, admin, "card stolen");

        // Full remaining balance, and the entitlement is revoked as the legacy contract requires.
        verify(refundService).requestRefund(eq(PAYMENT_ID), eq(admin), eq(new BigDecimal("5000")),
                eq("card stolen"), isNull());
        verify(refundService).approve(REFUND_ID, admin, "card stolen", true);
        verify(refundService).completeRefund(REFUND_ID, admin, null);
        // This service no longer writes the payment itself - RefundService owns that now.
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(entitlementRepository, never()).save(any(Entitlement.class));
        assertEquals("REFUNDED", result.getStatus());
        assertEquals(0, new BigDecimal("5000").compareTo(result.getRefundedAmount()));
    }

    @Test
    void refundPayment_stillRevokesEntitlementThroughTheGovernedDecision() {
        // Revocation is expressed as the approver's decision, which is what the governed
        // lifecycle applies at completion.
        Payment completed = buildPayment("COMPLETED", new BigDecimal("5000"));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(completed));
        when(refundService.requestRefund(eq(PAYMENT_ID), any(), any(), any(), any()))
                .thenReturn(RefundRequest.builder().id(REFUND_ID).build());
        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(buildPayment("REFUNDED", new BigDecimal("5000"))));

        paymentService.refundPayment(PAYMENT_ID, UUID.randomUUID(), "duplicate charge");

        ArgumentCaptor<Boolean> revoke = ArgumentCaptor.forClass(Boolean.class);
        verify(refundService).approve(eq(REFUND_ID), any(), any(), revoke.capture());
        assertEquals(Boolean.TRUE, revoke.getValue());
    }

    @Test
    void refundPayment_refusesWhenNoRefundableBalanceRemains() {
        // The exact cross-path outcome this increment fixes: once the governed lifecycle has
        // recorded a refund, the balance is exhausted and the legacy endpoint must refuse
        // rather than hand back the same money again.
        Payment alreadyRefunded = buildPayment("COMPLETED", new BigDecimal("5000"));
        alreadyRefunded.setRefundedAmount(new BigDecimal("5000"));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(alreadyRefunded));

        assertThrows(IllegalStateException.class, () ->
                paymentService.refundPayment(PAYMENT_ID, UUID.randomUUID(), "again"));

        verify(refundService, never()).requestRefund(any(), any(), any(), any(), any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void refundPayment_stillRefusesANonCompletedPayment() {
        // Legacy contract preserved exactly: only a completed payment can be refunded.
        Payment pending = buildPayment("PENDING", new BigDecimal("5000"));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(pending));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                paymentService.refundPayment(PAYMENT_ID, UUID.randomUUID(), "too early"));

        assertEquals("Only completed payments can be refunded", ex.getMessage());
        verify(refundService, never()).requestRefund(any(), any(), any(), any(), any());
    }

    @Test
    void refundPayment_fallsBackToADefaultReasonWhenNoneIsGiven() {
        Payment completed = buildPayment("COMPLETED", new BigDecimal("5000"));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(completed));
        when(refundService.requestRefund(eq(PAYMENT_ID), any(), any(), any(), any()))
                .thenReturn(RefundRequest.builder().id(REFUND_ID).build());
        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(buildPayment("REFUNDED", new BigDecimal("5000"))));

        paymentService.refundPayment(PAYMENT_ID, UUID.randomUUID(), "  ");

        verify(refundService).requestRefund(eq(PAYMENT_ID), any(), any(), eq("Refunded by admin"), isNull());
    }
}
