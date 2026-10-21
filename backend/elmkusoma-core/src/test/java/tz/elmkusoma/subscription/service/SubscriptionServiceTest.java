package tz.elmkusoma.subscription.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.domain.Entitlement;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.subscription.domain.Subscription;
import tz.elmkusoma.subscription.repository.SubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Behaviour of the subscription lifecycle foundation.
 *
 * <p>Pure unit tests: no Spring context, no database, no shared fixture. They are
 * designed to be safe to run alongside other work, but the module's shared
 * {@code target/} directory is contended by another developer, so they still need
 * to be run before any of this is treated as verified.</p>
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ParentRepository parentRepository;

    @Mock
    private tz.elmkusoma.parent.service.PaymentEventNotificationService paymentEventNotificationService;

    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        // The payer row is locked before any period is read, so the lock must succeed for the
        // activation tests to reach the period logic at all.
        org.mockito.Mockito.lenient()
                .when(parentRepository.findByIdForUpdate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(new tz.elmkusoma.parent.domain.Parent()));
        service = new SubscriptionService(subscriptionRepository, auditLogRepository, parentRepository,
                paymentEventNotificationService);
    }

    private Payment paidPayment(Integer accessDays) {
        Payment payment = Payment.builder()
                .parentId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .institutionId(UUID.randomUUID())
                .amount(new java.math.BigDecimal("5000"))
                .currency("TZS")
                .serviceType("PREMIUM_ACCESS")
                .status("COMPLETED")
                .accessDays(accessDays)
                .build();
        payment.setId(PAYMENT_ID);
        return payment;
    }

    private Entitlement entitlementStartingAt(LocalDateTime at) {
        Entitlement entitlement = Entitlement.builder()
                .status("ACTIVE")
                .startsAt(at)
                .expiresAt(at.plusDays(30))
                .build();
        entitlement.setId(UUID.randomUUID());
        return entitlement;
    }

    @Test
    void activatesFromAVerifiedPayment_usingTheApprovedAccessDuration() {
        when(subscriptionRepository.findByPaymentIdAndIsDeletedFalse(PAYMENT_ID))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            return s;
        });
        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        Entitlement entitlement = entitlementStartingAt(start);

        Subscription result = service.activateFromVerifiedPayment(paidPayment(30), entitlement);

        assertEquals(Subscription.STATUS_ACTIVE, result.getStatus());
        assertEquals(start, result.getPeriodStart());
        // The period comes from the approved accessDays snapshot, not an invented rule.
        assertEquals(start.plusDays(30), result.getPeriodEnd());
        assertEquals(PAYMENT_ID, result.getPaymentId());
        assertEquals(entitlement.getId(), result.getEntitlementId());
    }

    @Test
    void aRepeatedConfirmation_reusesTheExistingSubscription() {
        Subscription existing = Subscription.builder()
                .status(Subscription.STATUS_ACTIVE)
                .periodStart(LocalDateTime.now())
                .periodEnd(LocalDateTime.now().plusDays(30))
                .paymentId(PAYMENT_ID)
                .build();
        existing.setId(UUID.randomUUID());
        when(subscriptionRepository.findByPaymentIdAndIsDeletedFalse(PAYMENT_ID))
                .thenReturn(Optional.of(existing));

        Subscription result = service.activateFromVerifiedPayment(paidPayment(30), null);

        assertEquals(existing.getId(), result.getId(), "a duplicate callback must not open a second period");
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void refusesToOpenASubscriptionFromAnUnverifiedPayment() {
        Payment pending = paidPayment(30);
        pending.setStatus("PENDING");

        assertThrows(IllegalStateException.class,
                () -> service.activateFromVerifiedPayment(pending, null));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void refusesToGuessAPeriodWhenThePaymentCarriesNoApprovedDuration() {
        when(subscriptionRepository.findByPaymentIdAndIsDeletedFalse(PAYMENT_ID))
                .thenReturn(Optional.empty());

        // Silently assuming 30 days here would sell a period nobody priced.
        assertThrows(IllegalStateException.class,
                () -> service.activateFromVerifiedPayment(paidPayment(null), null));
        assertThrows(IllegalStateException.class,
                () -> service.activateFromVerifiedPayment(paidPayment(0), null));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void cancellationRequiresAPolicySuppliedEffectiveDate() {
        when(subscriptionRepository.findByIdForUpdate(any())).thenReturn(Optional.empty());
        UUID unknown = UUID.randomUUID();

        assertThrows(tz.elmkusoma.exception.ResourceNotFoundException.class,
                () -> service.cancel(unknown, LocalDateTime.now(), "no longer needed"));
    }

    @Test
    void cancellingWithoutAnEffectiveDateRefusesRatherThanChoosingOne() {
        Subscription subscription = Subscription.builder()
                .status(Subscription.STATUS_ACTIVE)
                .periodStart(LocalDateTime.now())
                .periodEnd(LocalDateTime.now().plusDays(30))
                .paymentId(PAYMENT_ID)
                .build();
        subscription.setId(UUID.randomUUID());
        when(subscriptionRepository.findByIdForUpdate(subscription.getId()))
                .thenReturn(Optional.of(subscription));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.cancel(subscription.getId(), null, "stopping"));
        assertTrue(ex.getMessage().toLowerCase().contains("effective date"),
                "the refusal must name the missing policy input: " + ex.getMessage());
        assertEquals(Subscription.STATUS_ACTIVE, subscription.getStatus(),
                "a refused cancellation must leave the subscription untouched");
    }

    @Test
    void cancellingWithASuppliedEffectiveDateRecordsIt() {
        Subscription subscription = Subscription.builder()
                .status(Subscription.STATUS_ACTIVE)
                .periodStart(LocalDateTime.now())
                .periodEnd(LocalDateTime.now().plusDays(30))
                .paymentId(PAYMENT_ID)
                .build();
        subscription.setId(UUID.randomUUID());
        when(subscriptionRepository.findByIdForUpdate(subscription.getId()))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime effectiveAt = LocalDateTime.now().plusDays(30);

        Subscription result = service.cancel(subscription.getId(), effectiveAt, "moving away");

        assertEquals(Subscription.STATUS_CANCELLED, result.getStatus());
        assertEquals(effectiveAt, result.getCancelledAt());
        assertEquals("moving away", result.getCancellationReason());
    }

    @Test
    void expireElapsed_marksOnlyActivePeriodsThatHaveRunOut() {
        Subscription elapsed = Subscription.builder()
                .status(Subscription.STATUS_ACTIVE)
                .periodStart(LocalDateTime.now().minusDays(31))
                .periodEnd(LocalDateTime.now().minusDays(1))
                .paymentId(UUID.randomUUID())
                .build();
        elapsed.setId(UUID.randomUUID());
        when(subscriptionRepository.findAllByStatusAndPeriodEndLessThanEqualAndIsDeletedFalse(
                org.mockito.ArgumentMatchers.eq(Subscription.STATUS_ACTIVE), any()))
                .thenReturn(java.util.List.of(elapsed));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = service.expireElapsed(LocalDateTime.now());

        assertEquals(1, count);
        assertEquals(Subscription.STATUS_EXPIRED, elapsed.getStatus());
    }

    // ------------------------------------------------------------- renewal

    private Subscription periodEndingAt(LocalDateTime end) {
        Subscription subscription = Subscription.builder()
                .status(Subscription.STATUS_ACTIVE)
                .periodStart(end.minusDays(30))
                .periodEnd(end)
                .paymentId(UUID.randomUUID())
                .build();
        subscription.setId(UUID.randomUUID());
        return subscription;
    }

    private void givenActivePeriods(Subscription... periods) {
        when(subscriptionRepository.findActivePeriodsForPayerAndServiceForUpdate(
                any(), any(), any(), any())).thenReturn(List.of(periods));
    }

    private void stubSave() {
        when(subscriptionRepository.save(any())).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            return s;
        });
    }

    @Test
    void renewingWhileAPeriodIsLive_chainsTheNewPeriodAfterIt() {
        when(subscriptionRepository.findByPaymentIdAndIsDeletedFalse(PAYMENT_ID))
                .thenReturn(Optional.empty());
        Subscription live = periodEndingAt(LocalDateTime.now().plusDays(10));
        givenActivePeriods(live);
        stubSave();

        Subscription renewed = service.activateFromVerifiedPayment(paidPayment(30), null);

        assertEquals(live.getPeriodEnd(), renewed.getPeriodStart(),
                "a renewal must begin where paid access ends, not restart at today");
        assertEquals(live.getPeriodEnd().plusDays(30), renewed.getPeriodEnd());
        assertEquals(Subscription.STATUS_ACTIVE, renewed.getStatus());
    }

    @Test
    void renewalNeverShortensOrRewritesThePeriodAlreadyPaid() {
        when(subscriptionRepository.findByPaymentIdAndIsDeletedFalse(PAYMENT_ID))
                .thenReturn(Optional.empty());
        Subscription live = periodEndingAt(LocalDateTime.now().plusDays(10));
        UUID payment = live.getPaymentId();
        LocalDateTime start = live.getPeriodStart();
        LocalDateTime end = live.getPeriodEnd();
        givenActivePeriods(live);
        stubSave();

        Subscription renewed = service.activateFromVerifiedPayment(paidPayment(30), null);

        assertEquals(Subscription.STATUS_ACTIVE, live.getStatus(), "the old record is not mutated");
        assertEquals(payment, live.getPaymentId());
        assertEquals(start, live.getPeriodStart());
        assertEquals(end, live.getPeriodEnd());
        assertNotEquals(live.getId(), renewed.getId(), "renewal adds a record, it does not edit one");
    }

    @Test
    void renewalAfterALapseStartsNowRatherThanBackfillingTheGap() {
        when(subscriptionRepository.findByPaymentIdAndIsDeletedFalse(PAYMENT_ID))
                .thenReturn(Optional.empty());
        Subscription lapsed = periodEndingAt(LocalDateTime.now().minusDays(5));
        givenActivePeriods(lapsed);
        stubSave();

        Subscription renewed = service.activateFromVerifiedPayment(paidPayment(30), null);

        assertTrue(renewed.getPeriodStart().isAfter(LocalDateTime.now().minusMinutes(1)),
                "the days access genuinely lapsed are not invented back into the new period");
        assertEquals(30, java.time.Duration.between(
                renewed.getPeriodStart(), renewed.getPeriodEnd()).toDays(),
                "exactly the approved duration is granted");
    }

    @Test
    void aStaleActiveStatusDoesNotOverrideAnElapsedPeriod() {
        when(subscriptionRepository.findByPaymentIdAndIsDeletedFalse(PAYMENT_ID))
                .thenReturn(Optional.empty());
        // Status still says ACTIVE because the sweep has not run; the dates must win.
        Subscription elapsedButLabelledActive = periodEndingAt(LocalDateTime.now().minusDays(1));
        givenActivePeriods(elapsedButLabelledActive);
        stubSave();

        Subscription renewed = service.activateFromVerifiedPayment(paidPayment(30), null);

        assertTrue(renewed.getPeriodStart().isAfter(elapsedButLabelledActive.getPeriodEnd()),
                "an elapsed period must not be treated as live just because its label says ACTIVE");
    }

    @Test
    void theLatestOfSeveralChainedPeriodsIsTheOneRenewalFollows() {
        when(subscriptionRepository.findByPaymentIdAndIsDeletedFalse(PAYMENT_ID))
                .thenReturn(Optional.empty());
        LocalDateTime now = LocalDateTime.now();
        Subscription older = periodEndingAt(now.plusDays(1));
        Subscription newest = periodEndingAt(now.plusDays(31));
        givenActivePeriods(newest, older);
        stubSave();

        Subscription renewed = service.activateFromVerifiedPayment(paidPayment(30), null);

        assertEquals(newest.getPeriodEnd(), renewed.getPeriodStart(),
                "chained periods must not cause a renewal to rewind into an already-covered span");
    }

    @Test
    void aFirstPurchaseWithNoPriorPeriodIsUnaffected() {
        when(subscriptionRepository.findByPaymentIdAndIsDeletedFalse(PAYMENT_ID))
                .thenReturn(Optional.empty());
        givenActivePeriods();
        stubSave();

        Subscription first = service.activateFromVerifiedPayment(paidPayment(30), null);

        assertEquals(Subscription.STATUS_ACTIVE, first.getStatus());
        assertEquals(30, java.time.Duration.between(
                first.getPeriodStart(), first.getPeriodEnd()).toDays(),
                "the first period is still the approved 30 days");
    }

    @Test
    void renewalDoesNotSaveOrMutateAnyPriorRow() {
        when(subscriptionRepository.findByPaymentIdAndIsDeletedFalse(PAYMENT_ID))
                .thenReturn(Optional.empty());
        Subscription live = periodEndingAt(LocalDateTime.now().plusDays(10));
        givenActivePeriods(live);
        stubSave();

        service.activateFromVerifiedPayment(paidPayment(30), null);

        // History is append-only: exactly one save, for the new row.
        org.mockito.Mockito.verify(subscriptionRepository, org.mockito.Mockito.times(1)).save(any());
    }

    // ------------------------------------------------- lifecycle state machine

    @Test
    void expiryIsAbsorbed_soNothingReopensALapsedPeriod() {
        Subscription subscription = Subscription.builder()
                .status(Subscription.STATUS_EXPIRED)
                .periodStart(LocalDateTime.now().minusDays(31))
                .periodEnd(LocalDateTime.now().minusDays(1))
                .build();

        assertFalse(subscription.canTransitionTo(Subscription.STATUS_ACTIVE),
                "a stale webhook must not resurrect lapsed access");
        assertFalse(subscription.canTransitionTo(Subscription.STATUS_CANCELLED));
    }

    @Test
    void periodEvaluationDoesNotDependOnAScheduledJob() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        Subscription inside = Subscription.builder()
                .periodStart(now.minusDays(5))
                .periodEnd(now.plusDays(5))
                .build();
        Subscription outside = Subscription.builder()
                .periodStart(now.minusDays(40))
                .periodEnd(now.minusDays(10))
                .build();

        assertTrue(inside.isWithinPeriod(now));
        assertFalse(inside.isPeriodElapsed(now));
        assertFalse(outside.isWithinPeriod(now));
        assertTrue(outside.isPeriodElapsed(now));
    }

    @Test
    void anActiveSubscriptionMayOnlyMoveForward() {
        Subscription active = Subscription.builder().status(Subscription.STATUS_ACTIVE).build();
        assertTrue(active.canTransitionTo(Subscription.STATUS_CANCELLED));
        assertTrue(active.canTransitionTo(Subscription.STATUS_EXPIRED));
        assertFalse(active.canTransitionTo("SOMETHING_ELSE"));

        Subscription cancelled = Subscription.builder().status(Subscription.STATUS_CANCELLED).build();
        assertTrue(cancelled.canTransitionTo(Subscription.STATUS_EXPIRED));
        assertFalse(cancelled.canTransitionTo(Subscription.STATUS_ACTIVE));
    }
}