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
    @Autowired private EventRepository eventRepository;
    @Autowired private EventRegistrationRepository registrationRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private StudentRepository studentRepository;

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

        var other = studentRepository.saveAndFlush(Student.builder()
                .userId(UUID.randomUUID())
                .admissionNumber("ADM-TKT2-" + UUID.randomUUID().toString().substring(0, 8))
                .status(StudentStatus.ACTIVE)
                .build()).getUserId();

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

        var other = studentRepository.saveAndFlush(Student.builder()
                .userId(UUID.randomUUID())
                .admissionNumber("ADM-TKT3-" + UUID.randomUUID().toString().substring(0, 8))
                .status(StudentStatus.ACTIVE)
                .build()).getUserId();
        var second = booking.book(paidEvent.getId(), other, 2);
        completePayment(second.paymentId());
        var confirmed = booking.confirmSeat(second.registrationId());

        assertEquals("REGISTERED", confirmed.getStatus());
        assertEquals(2, registrationRepository.sumHeldSeats(paidEvent.getId()),
                "an abandoned prompt must not occupy seats from someone who paid");
    }
}