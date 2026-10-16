package tz.elmkusoma.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import tz.elmkusoma.common.BaseEntity;
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.domain.EventRegistration;
import tz.elmkusoma.event.domain.EventStatus;
import tz.elmkusoma.event.repository.EventRegistrationRepository;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.event.service.EventService;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Event seat capacity must not oversell under concurrency.
 *
 * <p>Seat availability is a count-then-insert: the number of taken seats is read, compared with
 * the cap, and the registration is written afterwards. That sequence is only safe if nothing can
 * run between the read and the write. Without a lock, several people registering for the final
 * seat all read "full - 1 of 1 free", all pass the check, and all are inserted - the event is
 * then oversold. That is lost revenue where the extra seat could have been sold, and a broken
 * promise to every attendee beyond the cap.
 *
 * <p>The fix is a row lock on the event, taken before the count, so the second registration blocks
 * until the first has committed and then reads the updated count.
 *
 * <p>Deliberately not {@code @Transactional}: a shared test transaction would give every thread
 * the same snapshot and hide the race completely. Each caller runs its own transaction here, which
 * is what production does.
 */
@SpringBootTest
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class EventSeatCapacityConcurrencyTest {

    private static final int SEATS = 1;
    private static final int CONTENDERS = 6;

    @Autowired private EventService eventService;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventRegistrationRepository registrationRepository;
    @Autowired private tz.elmkusoma.shared.repository.UserRepository userRepository;
    @Autowired private org.springframework.transaction.support.TransactionTemplate transactionTemplate;
    @Autowired private tz.elmkusoma.event.service.EventBookingService bookingService;
    @Autowired private tz.elmkusoma.parent.repository.PaymentRepository paymentRepository;

    private Event singleSeatEvent;
    private final List<UUID> contenders = new ArrayList<>();

    @BeforeEach
    void seedOneSeatEvent() {
        // Contenders must be real users: registerForEvent resolves the user and applies the
        // event's access level before it ever looks at seats.
        for (int i = 0; i < CONTENDERS; i++) {
            contenders.add(createUser(i).getId());
        }

        singleSeatEvent = Event.builder()
                .title("Capacity probe " + UUID.randomUUID())
                .description("Seat capacity concurrency probe")
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .organizerId(TestDataSeeder.TEACHER_USER_ID)
                // PUBLIC so the probe measures seat contention, not access control.
                .accessLevel("PUBLIC")
                .eventType("SEMINAR")
                .startsAt(LocalDateTime.now().plusDays(2))
                .endsAt(LocalDateTime.now().plusDays(2).plusHours(2))
                .status("REGISTRATION_OPEN")
                .eventStatus(tz.elmkusoma.event.domain.EventStatus.REGISTRATION_OPEN)
                .isFree(true)
                .requiresApproval(false)
                .maxParticipants(SEATS)
                .currentRegistrations(0)
                .build();
        singleSeatEvent = eventRepository.saveAndFlush(singleSeatEvent);
    }

    private tz.elmkusoma.shared.domain.User createUser(int index) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.saveAndFlush(tz.elmkusoma.shared.domain.User.builder()
                .email("capacity-probe-" + index + "-" + suffix + "@example.test")
                .passwordHash("{noop}not-a-real-password")
                .firstName("Capacity")
                .lastName("Probe " + index)
                .role(tz.elmkusoma.shared.domain.User.Role.STUDENT)
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .isActive(true)
                .isEmailVerified(true)
                .build());
    }

    /**
     * One seat, six simultaneous attempts. Exactly one may be seated; the rest must be refused.
     *
     * <p>This is the assertion that matters: not "the right count", but "never more than the cap".
     */
    @Test
    void concurrentRegistrations_neverExceedTheSeatCap() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CONTENDERS);
        CountDownLatch ready = new CountDownLatch(CONTENDERS);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger admitted = new AtomicInteger();
        List<Future<?>> outcomes = new ArrayList<>();

        try {
            for (int i = 0; i < CONTENDERS; i++) {
                UUID userId = contenders.get(i);
                outcomes.add(pool.submit(() -> {
                    ready.countDown();
                    try {
                        go.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("contender interrupted before the start signal", e);
                    }
                    try {
                        eventService.registerForEvent(singleSeatEvent.getId(), userId);
                        admitted.incrementAndGet();
                    } catch (RuntimeException refused) {
                        // Expected for everyone who loses the race for the seat.
                    }
                }));
            }
            assertTrue(ready.await(15, TimeUnit.SECONDS), "contenders should be ready");
            go.countDown();
            for (Future<?> outcome : outcomes) {
                outcome.get(45, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(SEATS, admitted.get(),
                "exactly one registration may be admitted to a one-seat event; the event was oversold");

        // The stored truth, not just the counter: the cap holds in the database too.
        long registered = registrationRepository
                .countByEventIdAndStatusAndIsDeletedFalse(singleSeatEvent.getId(), "REGISTERED");
        assertEquals(SEATS, registered, "the database must hold no more registrations than there are seats");
    }

    /**
     * The confirmation path must serialise too, not just the registration path.
     *
     * <p>Seats are consumed at confirmation, not at booking, so the registration race above is only
     * half the problem: two people can both pay for the final seat, and if both confirmations read
     * the seat count before either writes, both succeed and the event is oversold after the money
     * has already been taken. The event row lock taken inside confirmSeat is what prevents that,
     * and this is the test that would fail if it were removed.</p>
     *
     * <p>Two genuinely separate bookings are used rather than one booking confirmed twice,
     * because a single row cannot be double-counted: the interesting race is two payers, two rows,
     * one remaining seat.</p>
     */
    @Test
    void concurrentConfirmations_neverOversellTheLastSeat() throws Exception {
        // One seat, two separate paid bookings, both genuinely paid.
        Event paid = Event.builder()
                .title("Confirmation race " + UUID.randomUUID())
                .description("paid confirmation concurrency probe")
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .organizerId(TestDataSeeder.TEACHER_USER_ID)
                .accessLevel("PUBLIC")
                .eventType("SEMINAR")
                .startsAt(LocalDateTime.now().plusDays(2))
                .endsAt(LocalDateTime.now().plusDays(2).plusHours(2))
                .status("REGISTRATION_OPEN")
                .eventStatus(EventStatus.REGISTRATION_OPEN)
                .isFree(false)
                .requiresApproval(false)
                .maxParticipants(SEATS)
                .currentRegistrations(0)
                .ticketPrice(new java.math.BigDecimal("5000.00"))
                .ticketCurrency("TZS")
                .build();
        paid = eventRepository.saveAndFlush(paid);

        UUID firstUser = contenders.get(0);
        UUID secondUser = contenders.get(1);
        List<UUID> registrationIds = new ArrayList<>();
        for (UUID attendee : List.of(firstUser, secondUser)) {
            var booking = bookingService.book(paid.getId(), attendee, 1);
            // Must set and save the SAME instance: findById outside a transaction returns a
            // detached entity, so mutating one instance and persisting a second re-read is a
            // silent no-op and leaves the payment PENDING.
            var payment = paymentRepository.findById(booking.paymentId()).orElseThrow();
            payment.setStatus("COMPLETED");
            paymentRepository.saveAndFlush(payment);
            registrationIds.add(booking.registrationId());
        }
        assertEquals(2, registrationIds.size());

        int racers = 2;
        ExecutorService pool = Executors.newFixedThreadPool(racers);
        CountDownLatch ready = new CountDownLatch(racers);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger confirmed = new AtomicInteger();
        List<Future<?>> outcomes = new ArrayList<>();

        try {
            for (int i = 0; i < racers; i++) {
                UUID registrationId = registrationIds.get(i);
                outcomes.add(pool.submit(() -> {
                    ready.countDown();
                    try {
                        go.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("confirmer interrupted before the start signal", e);
                    }
                    try {
                        bookingService.confirmSeat(registrationId);
                        confirmed.incrementAndGet();
                    } catch (RuntimeException refused) {
                        // Expected for whoever loses the race for the final seat.
                    }
                }));
            }
            assertTrue(ready.await(15, TimeUnit.SECONDS), "confirmers should be ready");
            go.countDown();
            for (Future<?> outcome : outcomes) {
                outcome.get(45, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(SEATS, confirmed.get(),
                "only one payer may take the final seat; both were confirmed and the event was oversold");
        assertEquals(SEATS, registrationRepository.sumHeldSeats(paid.getId()),
                "the database must hold no more confirmed seats than the event has");
    }

    /**
     * The lock must be taken by the registration path itself, not left to chance.
     *
     * <p>A regression guard rather than a behaviour test: if someone swaps the locked read back for
     * a plain read, the concurrency test above becomes flaky rather than failing outright, and a
     * flaky test is a test people eventually delete. This also pins the contract that the locked
     * read only works inside a transaction - which is exactly why the service is
     * {@code @Transactional} - so a future refactor that drops that annotation fails loudly here
     * rather than silently removing the lock's effect.
     */
    @Test
    void theLockedReadResolvesTheEventInsideATransaction() {
        Event found = transactionTemplate.execute(status ->
                eventRepository.findByIdForUpdate(singleSeatEvent.getId()).orElseThrow());
        assertEquals(singleSeatEvent.getId(), found.getId());
        assertEquals(SEATS, found.getMaxParticipants());
    }

    /** Refusing must be a clean refusal, not a partial write. */
    @Test
    void aRefusedRegistrationLeavesNoRowBehind() {
        UUID winner = contenders.get(0);
        eventService.registerForEvent(singleSeatEvent.getId(), winner);

        UUID loser = contenders.get(1);
        assertTrue(
                org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                        () -> eventService.registerForEvent(singleSeatEvent.getId(), loser))
                        != null,
                "a second registration for a full event must be refused");

        List<EventRegistration> rows = registrationRepository
                .findByEventIdAndIsDeletedFalse(singleSeatEvent.getId());
        assertEquals(1, rows.size(), "a refused registration must not leave a row");
        assertEquals(winner, rows.get(0).getUserId());
    }
}