package tz.elmkusoma.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.subscription.service.SubscriptionService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * The subscription expiry sweep as the scheduler runs it.
 *
 * <p>{@link SubscriptionService#expireElapsed} already has its own test covering which rows it
 * picks, so this covers the part that test cannot: that the {@code @Scheduled} method itself is
 * wired to it. Dead schedule methods are a real failure mode here - the service was originally
 * written and left uncalled for a long time - and a method that rethrows would let one bad run
 * stop the recurring task for good.</p>
 */
@ExtendWith(MockitoExtension.class)
class CoreSchedulerExpirySweepTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private tz.elmkusoma.parent.service.ParentPaymentService parentPaymentService;

    @InjectMocks
    private CoreScheduler scheduler;

    @Test
    void theScheduledMethodActuallyDelegatesToTheExpiryService() {
        scheduler.expireElapsedSubscriptions();

        // If the call were ever removed or short-circuited, elapsed periods would quietly stay
        // ACTIVE forever while every unit test still passed.
        verify(subscriptionService).expireElapsed(any());
        verifyNoMoreInteractions(subscriptionService);
    }

    @Test
    void itPassesTheCurrentTimeSoTheQuerySelectsWhatHasActuallyRunOut() {
        LocalDateTime before = LocalDateTime.now();

        scheduler.expireElapsedSubscriptions();

        ArgumentCaptor<LocalDateTime> now = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(subscriptionService).expireElapsed(now.capture());

        LocalDateTime after = LocalDateTime.now();
        assertTrue(!now.getValue().isBefore(before) && !now.getValue().isAfter(after),
                "the sweep must evaluate against the current time, but passed " + now.getValue()
                        + " between " + before + " and " + after);
    }

    @Test
    void aFailingExpiryServiceDoesNotStopTheRecurringSweep() {
        doThrow(new RuntimeException("ledger unavailable")).when(subscriptionService)
                .expireElapsed(any());

        // A rethrow here would let one failed run take the whole scheduled task down, leaving every
        // later sweep silently unscheduled rather than merely late.
        assertDoesNotThrow(() -> scheduler.expireElapsedSubscriptions());
    }

    @Test
    void aFailedRunDoesNotDisableLaterRuns() {
        doThrow(new RuntimeException("transient")).when(subscriptionService).expireElapsed(any());

        scheduler.expireElapsedSubscriptions();
        assertDoesNotThrow(() -> scheduler.expireElapsedSubscriptions());

        // The guard is per-invocation: swallowing one failure must not leave the sweep unwired.
        verify(subscriptionService, org.mockito.Mockito.times(2)).expireElapsed(any());
    }

    // ---------------------------------------------------------------- entitlements

    @Test
    void theEntitlementSweepIsWiredToItsService() {
        scheduler.expireElapsedEntitlements();

        // The same failure the subscription sweep was originally shipped with: the service written,
        // the scheduler left uncalled, and every elapsed entitlement reporting ACTIVE forever
        // while all unit tests still pass.
        verify(parentPaymentService).expireElapsedEntitlements(any());
    }

    @Test
    void itEvaluatesEntitlementsAgainstTheCurrentTime() {
        LocalDateTime before = LocalDateTime.now();

        scheduler.expireElapsedEntitlements();

        ArgumentCaptor<LocalDateTime> now = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(parentPaymentService).expireElapsedEntitlements(now.capture());

        LocalDateTime after = LocalDateTime.now();
        assertTrue(!now.getValue().isBefore(before) && !now.getValue().isAfter(after),
                "the sweep must evaluate against the current time, but passed " + now.getValue()
                        + " between " + before + " and " + after);
    }

    @Test
    void aFailingEntitlementSweepDoesNotStopTheRecurringSweep() {
        doThrow(new RuntimeException("entitlements unavailable")).when(parentPaymentService)
                .expireElapsedEntitlements(any());

        // A rethrow here would let one failed run take the whole scheduled task down, leaving every
        // later sweep silently unscheduled rather than merely late.
        assertDoesNotThrow(() -> scheduler.expireElapsedEntitlements());
    }
}
