package tz.elmkusoma.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.domain.EventRegistration;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.event.repository.EventRegistrationRepository;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Booking a seat at a paid event.
 *
 * <p>The property under test is the one that was missing: a seat must be held only against a
 * confirmed payment. Before this, an event had a boolean but no price and no link to the money,
 * so an attendee's place could not be reconciled against the ledger and a paid event was
 * indistinguishable from a free one.</p>
 */
@SpringBootTest
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class EventBookingServiceTest {

    @Autowired private EventBookingService booking;
    @Autowired private tz.elmkusoma.event.service.EventService eventService;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventRegistrationRepository registrationRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private tz.elmkusoma.shared.repository.UserRepository userRepository;

    private Event paidEvent;
    private Event freeEvent;
    private UUID attendee;

    @BeforeEach
    void seed() {
        attendee = studentRepository.saveAndFlush(Student.builder()
                .userId(TestDataSeeder.STUDENT_USER_ID)
                .admissionNumber("ADM-TKT-" + UUID.randomUUID().toString().substring(0, 8))
                .status(StudentStatus.ACTIVE)
                .build()).getUserId();

        paidEvent = event("Paid workshop", false, "200.00", 5);
        freeEvent = event("Free seminar", true, null, 5);
    }

    private Event event(String title, boolean isFree, String price, int capacity) {
        Event e = Event.builder()
                .title(title + " " + UUID.randomUUID())
                .description("booking probe")
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .organizerId(TestDataSeeder.TEACHER_USER_ID)
                .accessLevel("PUBLIC")
                .eventType("SEMINAR")
                .startsAt(LocalDateTime.now().plusDays(3))
                .endsAt(LocalDateTime.now().plusDays(3).plusHours(2))
                .status("REGISTRATION_OPEN")
                // Event.eventStatus defaults to DRAFT via @Builder.Default, so a builder-created
                // event carries eventStatus=DRAFT even when status says REGISTRATION_OPEN. Production
                // create/update always goes through EventServiceImpl.applyStatus, which sets both.
                // The fixture must do the same or it builds events no real code path could produce.
                .eventStatus(tz.elmkusoma.event.domain.EventStatus.REGISTRATION_OPEN)
                .isFree(isFree)
                .requiresApproval(false)
                .maxParticipants(capacity)
                .currentRegistrations(0)
                .build();
        if (price != null) {
            e.setTicketPrice(new BigDecimal(price));
            e.setTicketCurrency("TZS");
        }
        return eventRepository.saveAndFlush(e);
    }


    /**
     * A second attendee: a real User as well as a Student, because booking now resolves the
     * caller and checks what they may do. A Student with no User behind it is not an attendee
     * the application can authorise, and the booking is correctly refused.
     */
    private UUID newAttendee() {
        return newAttendeeIn(TestDataSeeder.INSTITUTION_ID);
    }

    /**
     * An attendee belonging to a specific institution.
     *
     * <p>The User id is read back from the saved row rather than from the value we generate:
     * {@code BaseEntity} generates its own identifier, so a locally minted id would leave the
     * Student pointing at a user that does not exist, and booking would correctly refuse it.</p>
     */
    private UUID newAttendeeIn(UUID institutionId) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        tz.elmkusoma.shared.domain.User saved = userRepository.saveAndFlush(
                tz.elmkusoma.shared.domain.User.builder()
                        .email("tkt-" + unique + "@test.go.tz")
                        .passwordHash("{noop}x")
                        .firstName("Tkt")
                        .lastName("Attendee")
                        .role(tz.elmkusoma.shared.domain.User.Role.STUDENT)
                        .institutionId(institutionId)
                        .isActive(true)
                        .isEmailVerified(true)
                        .build());
        studentRepository.saveAndFlush(Student.builder()
                .userId(saved.getId())
                .admissionNumber("ADM-TKT-" + unique)
                .status(StudentStatus.ACTIVE)
                .build());
        return saved.getId();
    }

    private Payment completePayment(UUID paymentId) {
        Payment p = paymentRepository.findById(paymentId).orElseThrow();
        p.setStatus("COMPLETED");
        p.setPaidAt(LocalDateTime.now());
        return paymentRepository.saveAndFlush(p);
    }

    // ------------------------------------------------------- free events unchanged

    @Test
    void aFreeEventStillBooksWithoutTouchingThePaymentDomain() {
        var result = booking.book(freeEvent.getId(), attendee, 1);

        assertNull(result.paymentId(), "a free event must not create a payment");
        assertEquals("REGISTERED", result.registrationStatus(), "a free booking is immediate");
        assertTrue(result.seatsHeld());
        assertEquals(0, registrationRepository.sumHeldSeats(freeEvent.getId()) - 1,
                "one seat held");
    }

    // ------------------------------------------------------- the core property

    @Test
    void aPaidEventDoesNotHoldASeatUntilThePaymentIsConfirmed() {
        var result = booking.book(paidEvent.getId(), attendee, 1);

        assertEquals("PAYMENT_PENDING", result.registrationStatus(),
                "a queued prompt is a pending payment, not a booking");
        assertFalse(result.seatsHeld(), "no seat may be taken before the money arrives");
        assertNotNull(result.paymentId(), "a real payment must exist");
        assertEquals(0, registrationRepository.sumHeldSeats(paidEvent.getId()),
                "an unpaid booking holds no capacity at all");
    }

    @Test
    void aSeatIsConfirmedOnlyAfterThePaymentIsCompleted() {
        var booking0 = booking.book(paidEvent.getId(), attendee, 1);

        // Still pending: confirming must refuse rather than grant the seat.
        assertThrows(IllegalStateException.class,
                () -> booking.confirmSeat(booking0.registrationId()),
                "a seat must not be granted while its payment is still pending");

        completePayment(booking0.paymentId());
        var confirmed = booking.confirmSeat(booking0.registrationId());

        assertEquals("REGISTERED", confirmed.getStatus());
        assertEquals(1, registrationRepository.sumHeldSeats(paidEvent.getId()),
                "the seat is held once the payment is confirmed");
    }

    @Test
    void confirmationSnapshotsWhatWasActuallyPaid() {
        var b = booking.book(paidEvent.getId(), attendee, 2);
        completePayment(b.paymentId());
        var confirmed = booking.confirmSeat(b.registrationId());

        // 200 per seat x 2 seats
        assertEquals(0, new BigDecimal("400").compareTo(confirmed.getAmountPaid()));
        assertEquals(2, confirmed.getSeats());
        assertEquals(2, registrationRepository.sumHeldSeats(paidEvent.getId()));
    }

    @Test
    void confirmationIsIdempotent() {
        var b = booking.book(paidEvent.getId(), attendee, 1);
        completePayment(b.paymentId());

        booking.confirmSeat(b.registrationId());
        booking.confirmSeat(b.registrationId());
        booking.confirmSeat(b.registrationId());

        assertEquals(1, registrationRepository.sumHeldSeats(paidEvent.getId()),
                "a repeated callback must not grant extra seats");
    }

    @Test
    void priceComesFromTheEventAndNotFromTheCaller() {
        // The attendee cannot name a price: the amount is derived from the stored ticket price.
        var b = booking.book(paidEvent.getId(), attendee, 1);
        assertEquals(0, new BigDecimal("200").compareTo(b.amount()));
        assertEquals("TZS", b.currency());
    }

    // ------------------------------------------------------- group bookings

    @Test
    void aGroupBookingConsumesEverySeatItBuys() {
        var b = booking.book(paidEvent.getId(), attendee, 3);
        completePayment(b.paymentId());
        booking.confirmSeat(b.registrationId());

        assertEquals(3, registrationRepository.sumHeldSeats(paidEvent.getId()),
                "three seats in one booking must consume three seats, not one");
    }

    @Test
    void aGroupBookingLargerThanCapacityIsRefusedAtConfirmation() {
        // Capacity is 5. Ask for 6: the payment may be in flight, but the seat is not granted.
        var b = booking.book(paidEvent.getId(), attendee, 6);
        completePayment(b.paymentId());

        assertThrows(IllegalStateException.class, () -> booking.confirmSeat(b.registrationId()),
                "an event must never be overbooked");
        assertEquals(0, registrationRepository.sumHeldSeats(paidEvent.getId()));
    }

    @Test
    void aGroupBookingThatFitsTheRemainingCapacityIsAccepted() {
        var first = booking.book(paidEvent.getId(), attendee, 3);
        completePayment(first.paymentId());
        booking.confirmSeat(first.registrationId());

        UUID other = newAttendee();

        var second = booking.book(paidEvent.getId(), other, 2);
        completePayment(second.paymentId());
        var confirmed = booking.confirmSeat(second.registrationId());

        assertEquals("REGISTERED", confirmed.getStatus());
        assertEquals(5, registrationRepository.sumHeldSeats(paidEvent.getId()),
                "3 + 2 fills a five-seat event exactly");
    }

    // ------------------------------------------------------- cancellation

    @Test
    void cancellingReleasesTheSeatWithoutTouchingThePayment() {
        var b = booking.book(paidEvent.getId(), attendee, 1);
        completePayment(b.paymentId());
        booking.confirmSeat(b.registrationId());
        assertEquals(1, registrationRepository.sumHeldSeats(paidEvent.getId()));

        booking.cancelSeat(b.registrationId(), attendee, "cannot attend");

        assertEquals(0, registrationRepository.sumHeldSeats(paidEvent.getId()),
                "a cancelled seat goes back to the event");
        // Cancellation is not a refund. The payment must be untouched for the refund lifecycle
        // to own, and the two stay separate operations.
        Payment payment = paymentRepository.findById(b.paymentId()).orElseThrow();
        assertEquals("COMPLETED", payment.getStatus(),
                "cancelling a booking must not quietly refund or alter the payment");
    }

    @Test
    void aSeatCannotBeBookedTwiceByTheSameAttendee() {
        booking.book(paidEvent.getId(), attendee, 1);
        assertThrows(IllegalStateException.class,
                () -> booking.book(paidEvent.getId(), attendee, 1));
    }

    @Test
    void aZeroSeatBookingIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> booking.book(paidEvent.getId(), attendee, 0),
                "a booking that consumes no seats would hold a place for free");
    }

    @Test
    void anUnpaidBookingDoesNotBlockTheEventForSomebodyWhoPays() {
        // The first attendee abandons their prompt; the second must still be able to buy.
        booking.book(paidEvent.getId(), attendee, 4);

        UUID other = newAttendee();
        var second = booking.book(paidEvent.getId(), other, 2);
        completePayment(second.paymentId());
        var confirmed = booking.confirmSeat(second.registrationId());

        assertEquals("REGISTERED", confirmed.getStatus());
        assertEquals(2, registrationRepository.sumHeldSeats(paidEvent.getId()),
                "an abandoned prompt must not occupy seats from someone who paid");
    }

    // ------------------------------------------------------- authorization
    //
    // The paid booking path originally performed no eligibility check at all, while the free
    // registration path enforces access level, institution and organiser ownership. Payment is
    // not a way around access control: buying must never grant more access than registering does.

    @Test
    void aPrivateEventCannotBeBookedBySomebodyWhoIsNotTheOrganiser() {
        Event privateEvent = event("Private paid workshop", false, "200.00", 5);
        privateEvent.setAccessLevel("PRIVATE");
        eventRepository.saveAndFlush(privateEvent);

        assertThrows(ForbiddenException.class,
                () -> booking.book(privateEvent.getId(), attendee, 1),
                "a paid booking must respect the same PRIVATE restriction as a free one");
    }

    @Test
    void aPrivateEventCanStillBeBookedByItsOwnOrganiser() {
        Event privateEvent = event("Organiser paid workshop", false, "200.00", 5);
        privateEvent.setAccessLevel("PRIVATE");
        privateEvent.setOrganizerId(TestDataSeeder.TEACHER_USER_ID);
        eventRepository.saveAndFlush(privateEvent);

        var result = booking.book(privateEvent.getId(), TestDataSeeder.TEACHER_USER_ID, 1);
        assertEquals("PAYMENT_PENDING", result.registrationStatus());
    }

    @Test
    void anAttendeeCannotBookAnEventInAnotherInstitution() {
        Event foreign = event("Other institution paid workshop", false, "200.00", 5);
        UUID mine = TestDataSeeder.INSTITUTION_ID;
        UUID theirs = UUID.randomUUID();
        foreign.setInstitutionId(theirs);
        // The shared helper builds PUBLIC events, and a PUBLIC event is deliberately open to
        // every institution. Institution scoping is the rule for everything else, so this one is
        // explicitly institution-scoped.
        foreign.setAccessLevel("INSTITUTION");
        eventRepository.saveAndFlush(foreign);

        UUID outsider = newAttendeeIn(mine);
        assertThrows(IllegalArgumentException.class,
                () -> booking.book(foreign.getId(), outsider, 1),
                "institution scoping must apply to paid bookings too");

        // And the same attendee may still book an event in their own institution.
        var allowed = booking.book(paidEvent.getId(), outsider, 1);
        assertEquals("PAYMENT_PENDING", allowed.registrationStatus());
    }

    @Test
    void anAttendeeWithNoUserRecordCannotBook() {
        // Fails closed: an identity the system cannot resolve is not an identity it can authorise.
        UUID ghostUserId = UUID.randomUUID();
        studentRepository.saveAndFlush(Student.builder()
                .userId(ghostUserId)
                .admissionNumber("ADM-GHOST-" + ghostUserId.toString().substring(0, 8))
                .status(StudentStatus.ACTIVE)
                .build());

        assertThrows(ResourceNotFoundException.class,
                () -> booking.book(paidEvent.getId(), ghostUserId, 1));
    }

    // ------------------------------------------------------- confirmation integrity

    @Test
    void aSeatIsNotConfirmedWhenThereIsNoPaymentAtAll() {
        // The previous check only refused when a payment EXISTED and was not complete, so a
        // registration with no linked payment fell through and was handed a seat with nothing
        // verified. Absence of evidence is not evidence of payment.
        EventRegistration orphanRegistration = EventRegistration.builder()
                .eventId(paidEvent.getId())
                .institutionId(paidEvent.getInstitutionId())
                .userId(attendee)
                .status("PAYMENT_PENDING")
                .registeredAt(LocalDateTime.now())
                .attended(false)
                .seats(1)
                .build();
        EventRegistration orphan = registrationRepository.saveAndFlush(orphanRegistration);
        UUID orphanId = orphan.getId();

        assertThrows(IllegalStateException.class, () -> booking.confirmSeat(orphanId),
                "a registration with no payment must never be granted a seat");
        assertEquals(0, registrationRepository.sumHeldSeats(paidEvent.getId()));
    }

    @Test
    void aSeatIsNotConfirmedWhenThePaymentFailedOrIsStillPending() {
        // Only COMPLETED may hand over a seat. A FAILED payment means the money never arrived,
        // and a PENDING one means it has not arrived yet; both previously sat behind the same
        // single branch and needed to be proved individually.
        for (String status : new String[] {"FAILED", "PENDING", "CANCELLED"}) {
            var b = booking.book(paidEvent.getId(), newAttendeeIn(paidEvent.getInstitutionId()), 1);
            Payment payment = paymentRepository.findById(b.paymentId()).orElseThrow();
            payment.setStatus(status);
            paymentRepository.saveAndFlush(payment);

            assertThrows(IllegalStateException.class, () -> booking.confirmSeat(b.registrationId()),
                    "a seat must not be granted while the payment is " + status);

            EventRegistration still = registrationRepository.findById(b.registrationId()).orElseThrow();
            assertEquals("PAYMENT_PENDING", still.getStatus(),
                    "registration must stay unconfirmed while the payment is " + status);
            assertNull(still.getAmountPaid(),
                    "no amount may be recorded as paid while the payment is " + status);
        }
        assertEquals(0, registrationRepository.sumHeldSeats(paidEvent.getId()),
                "no seat may be held by any of the unpaid attempts");
    }

    @Test
    void aBookingAgainstACancelledEventIsRefusedAndChargesNobody() {
        // The paid path checked access level only, so a cancelled or already-started event could
        // still be bought. Payment must not be a way past the lifecycle gate.
        Event cancelledDraft = event("Cancelled lifecycle probe", false, "5000.00", 10);
        cancelledDraft.setEventStatus(tz.elmkusoma.event.domain.EventStatus.CANCELLED);
        cancelledDraft.setStatus("CANCELLED");
        Event cancelled = eventRepository.saveAndFlush(cancelledDraft);

        assertThrows(IllegalStateException.class, () -> booking.book(cancelled.getId(), attendee, 1),
                "a cancelled event must refuse new bookings");

        Event startedDraft = event("Already started probe", false, "5000.00", 10);
        startedDraft.setStartsAt(LocalDateTime.now().minusDays(1));
        startedDraft.setTimezone(null);
        Event started = eventRepository.saveAndFlush(startedDraft);

        assertThrows(IllegalArgumentException.class, () -> booking.book(started.getId(), attendee, 1),
                "an event that has already started must refuse new bookings");

        // Nothing may be charged for a refused booking.
        assertEquals(0L, paymentRepository.findAll().stream()
                .filter(p -> "EVENT_TICKET".equals(p.getServiceType()))
                .filter(p -> cancelled.getId().equals(p.getServiceId())
                        || started.getId().equals(p.getServiceId()))
                .count(), "a refused booking must not create a payment");
    }

    @Test
    void theAmountConfirmedIsWhatWasCharged_notWhatTheEventCostsNow() {
        // Repricing between booking and confirmation must not restate what was paid. Reading the
        // ticket price at confirmation time would quietly change the figure.
        var b = booking.book(paidEvent.getId(), attendee, 1);
        completePayment(b.paymentId());

        paidEvent.setTicketPrice(new BigDecimal("999.00"));
        eventRepository.saveAndFlush(paidEvent);

        var confirmed = booking.confirmSeat(b.registrationId());
        assertEquals(0, new BigDecimal("200").compareTo(confirmed.getAmountPaid()),
                "the snapshot must be what was charged, not today's list price");
    }

    // ------------------------------------------------- the attendee is not locked out forever
    //
    // (event_id, user_id) is unique, so an attendee has exactly one registration row per event.
    // The guard this replaced counted every row including cancelled ones, so one abandoned or
    // failed attempt locked them out permanently: nothing was collected, no seat was taken, and
    // there was no way back in.

    @Test
    void aCancelledRegistrationLetsTheAttendeeBookTheEventAgain() {
        var first = booking.book(freeEvent.getId(), attendee, 1);
        booking.cancelSeat(first.registrationId(), attendee, "could not attend");

        var second = booking.book(freeEvent.getId(), attendee, 1);

        assertNotNull(second.registrationId());
        assertEquals(first.registrationId(), second.registrationId(),
                "the row must be reused, because (event_id, user_id) is unique and no second "
                        + "registration row can be inserted for the same attendee");
        assertEquals(1, registrationRepository.sumHeldSeats(freeEvent.getId()),
                "cancelling released the seat, and rebooking takes exactly one again");
    }

    @Test
    void aBookingWhosePaymentWasCancelledLetsTheAttendeeTryAgain() {
        var first = booking.book(paidEvent.getId(), attendee, 1);
        // The payment was cancelled or failed. The seat was never taken and nothing was collected.
        Payment abandoned = paymentRepository.findById(first.paymentId()).orElseThrow();
        abandoned.setStatus("CANCELLED");
        paymentRepository.saveAndFlush(abandoned);

        var second = booking.book(paidEvent.getId(), attendee, 1);

        assertNotNull(second.paymentId(), "a second, live payment must be raised for the retry");
        assertEquals("PENDING", paymentRepository.findById(second.paymentId()).orElseThrow().getStatus());
        assertEquals(0L, registrationRepository.sumHeldSeats(paidEvent.getId()),
                "an unconfirmed booking holds no seat either before or after the retry");
    }

    @Test
    void aConfirmedSeatStillBlocksASecondBooking() {
        // The guard that was loosened must still refuse the case it was written for: an attendee
        // who already holds a seat. Rebooking over a confirmed seat would double-charge them or
        // silently over-allocate.
        var held = booking.book(paidEvent.getId(), attendee, 2);
        completePayment(held.paymentId());
        booking.confirmSeat(held.registrationId());

        assertThrows(IllegalStateException.class,
                () -> booking.book(paidEvent.getId(), attendee, 1),
                "an attendee who already holds confirmed seats must not be able to book again");
        assertEquals(2, registrationRepository.sumHeldSeats(paidEvent.getId()),
                "the refused attempt must not have changed what is held");
    }

    @Test
    void aLivePaymentStillBlocksASecondBooking() {
        // The other half of the loosened guard: an attempt that is genuinely still in flight must
        // keep its place, or a double-click would raise two payments for one attendee.
        booking.book(paidEvent.getId(), attendee, 1);

        assertThrows(IllegalStateException.class,
                () -> booking.book(paidEvent.getId(), attendee, 1),
                "an in-flight payment must not be abandoned by a second booking attempt");
    }

    @Test
    void cancellingAGroupBookingRestoresEverySeatItHeld() {
        // Cancellation used to recount capacity as a row count, so a group holding several seats
        // on one registration released them all but reported one. An event could then be reopened
        // while still full, or show a count that no longer matched what was held.
        var group = booking.book(freeEvent.getId(), attendee, 3);
        assertEquals(3, registrationRepository.sumHeldSeats(freeEvent.getId()));

        eventService.cancelRegistration(freeEvent.getId(), attendee, "group cannot attend");

        assertEquals(0, registrationRepository.sumHeldSeats(freeEvent.getId()),
                "every seat the group held must be released");
        var reloaded = eventRepository.findById(freeEvent.getId()).orElseThrow();
        assertEquals(0, reloaded.getCurrentRegistrations(),
                "the published count must be seats held, not rows - a group of three is three "
                        + "seats, and counting rows would understate the event as one seat short "
                        + "of what was actually freed");
    }
}