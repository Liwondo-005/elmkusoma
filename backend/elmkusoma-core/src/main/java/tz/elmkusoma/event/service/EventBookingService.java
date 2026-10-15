package tz.elmkusoma.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.domain.EventRegistration;
import tz.elmkusoma.event.repository.EventRegistrationRepository;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Booking a seat at a paid event.
 *
 * <p>The rule this service exists to hold: <b>a seat is held only against a confirmed payment.</b>
 * Events previously carried an {@code is_free} flag but no price and no link to the money, so a
 * paid event and a free one were indistinguishable to the payment domain and an attendee's place
 * could not be reconciled against the ledger at all.</p>
 *
 * <p>Booking therefore produces a registration in {@code PAYMENT_PENDING} together with a real
 * {@link Payment} in {@code PENDING}, and neither becomes anything else until the backend confirms
 * collection. Registration status and payment status stay separate facts throughout: a queued
 * prompt is a pending payment, not a booking, and neither is ever silently promoted.</p>
 *
 * <p>Seat capacity is consumed only on confirmation, so a payment abandoned at the prompt stage
 * holds nothing and cannot crowd out somebody who has paid.</p>
 */
@Service
@RequiredArgsConstructor
public class EventBookingService {

    /** Service type recorded against a ticket payment. */
    public static final String SERVICE_TYPE = "EVENT_TICKET";

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final PaymentRepository paymentRepository;

    /** What a booking attempt produced. */
    public record Booking(UUID eventId,
                          UUID registrationId,
                          UUID paymentId,
                          BigDecimal amount,
                          String currency,
                          String registrationStatus,
                          String paymentStatus,
                          boolean seatsHeld) {
    }

    /**
     * Books {@code seats} places for one attendee.
     *
     * <p>Price comes from the event's stored {@code ticketPrice} and nowhere else. A client cannot
     * name a price, and the amount is snapshotted onto the registration when the seat is finally
     * confirmed so a later repricing cannot restate what this attendee paid.</p>
     */
    @Transactional
    public Booking book(UUID eventId, UUID attendeeUserId, int seats) {
        if (seats < 1) {
            throw new IllegalArgumentException("A booking must be for at least one seat");
        }

        // Locked read, so the seat check below cannot race another booking for the last places.
        Event event = eventRepository.findByIdForUpdate(eventId)
                .filter(e -> !Boolean.TRUE.equals(e.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (registrationRepository
                .existsByEventIdAndUserIdAndIsDeletedFalse(eventId, attendeeUserId)) {
            throw new IllegalStateException("This attendee is already registered for the event");
        }

        if (!event.isPaid()) {
            // Free events keep the existing behaviour and never touch the payment domain.
            EventRegistration registration = EventRegistration.builder()
                    .eventId(eventId)
                    .institutionId(event.getInstitutionId())
                    .userId(attendeeUserId)
                    .status("REGISTERED")
                    .registeredAt(LocalDateTime.now())
                    .attended(false)
                    .seats(seats)
                    .build();
            registration = registrationRepository.saveAndFlush(registration);
            return new Booking(eventId, registration.getId(), null, BigDecimal.ZERO,
                    event.getTicketCurrency() == null ? "TZS" : event.getTicketCurrency(),
                    registration.getStatus(), "NOT_APPLICABLE", true);
        }

        BigDecimal unit = event.getTicketPrice();
        BigDecimal total = unit.multiply(BigDecimal.valueOf(seats));

        // Seats are NOT taken yet. Taking them now would let an abandoned prompt hold a place
        // away from someone who has paid; they are taken in confirmSeat once collection lands.
        EventRegistration registration = EventRegistration.builder()
                .eventId(eventId)
                .institutionId(event.getInstitutionId())
                .userId(attendeeUserId)
                .status("PAYMENT_PENDING")
                .registeredAt(LocalDateTime.now())
                .attended(false)
                .seats(seats)
                .build();
        registration = registrationRepository.saveAndFlush(registration);

        Payment payment = Payment.builder()
                .parentId(attendeeUserId)
                .studentId(attendeeUserId)
                .serviceType(SERVICE_TYPE)
                .serviceId(eventId)
                .amount(total)
                .currency(event.getTicketCurrency() == null ? "TZS" : event.getTicketCurrency())
                .status("PENDING")
                .build();
        payment.setInstitutionId(event.getInstitutionId());
        payment = paymentRepository.saveAndFlush(payment);

        registration.setPaymentId(payment.getId());
        registrationRepository.saveAndFlush(registration);

        return new Booking(eventId, registration.getId(), payment.getId(), total,
                payment.getCurrency(), registration.getStatus(), payment.getStatus(), false);
    }

    /**
     * Confirms a seat once its payment is genuinely collected.
     *
     * <p>Called only from the verification path, never from the request that created the payment.
     * This is the same separation that keeps a receipt from confirming itself.</p>
     */
    @Transactional
    public EventRegistration confirmSeat(UUID registrationId) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EventRegistration", "id", registrationId));

        if (!"PAYMENT_PENDING".equals(registration.getStatus())) {
            // Idempotent: a duplicate callback must not grant a second seat or extend anything.
            return registration;
        }

        Event event = eventRepository.findByIdForUpdate(registration.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event", "id", registration.getEventId()));

        // Capacity is checked at confirmation, not at request. If the event filled up while this
        // payment was in flight the payer is refunded rather than quietly overbooked.
        long held = registrationRepository.sumHeldSeats(event.getId());
        if (event.getMaxParticipants() != null
                && held + registration.getSeats() > event.getMaxParticipants()) {
            throw new IllegalStateException(
                    "The event filled up while this payment was in progress; the seat is not "
                            + "held and the payment is refundable");
        }

        Payment payment = registration.getPaymentId() == null ? null
                : paymentRepository.findById(registration.getPaymentId()).orElse(null);
        if (payment != null && !"COMPLETED".equals(payment.getStatus())) {
            throw new IllegalStateException(
                    "A seat is only confirmed once its payment is completed");
        }

        // Snapshot what this attendee actually paid, so a later reprice cannot restate it.
        if (payment != null) {
            registration.setAmountPaid(payment.getAmount());
        } else {
            registration.setAmountPaid(event.getTicketPrice()
                    .multiply(BigDecimal.valueOf(registration.getSeats())));
        }
        registration.setStatus("REGISTERED");
        return registrationRepository.saveAndFlush(registration);
    }

    /** Releases a seat and records why. Cancellation stays separate from any refund. */
    @Transactional
    public void cancelSeat(UUID registrationId, UUID actorUserId, String reason) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EventRegistration", "id", registrationId));

        boolean isAttendee = registration.getUserId().equals(actorUserId);
        boolean isOrganiser = registration.getEventId() != null
                && eventRepository.findById(registration.getEventId())
                        .map(e -> e.getOrganizerId().equals(actorUserId))
                        .orElse(false);
        if (!isAttendee && !isOrganiser) {
            throw new ForbiddenException("You may not cancel this registration");
        }

        registration.setStatus("CANCELLED");
        registration.setCancelledAt(LocalDateTime.now());
        registration.setCancellationReason(reason);
        registrationRepository.saveAndFlush(registration);
        // The payment is deliberately left alone. A cancellation is not a refund, and the two
        // are separate operations on separate records.
    }
}