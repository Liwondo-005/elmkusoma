package tz.elmkusoma.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.domain.EventRegistration;
import tz.elmkusoma.event.domain.EventStatus;
import tz.elmkusoma.event.repository.EventRegistrationRepository;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.ParentStudentLink;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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

    /** Recorded in {@code Payment.purchaserType} when the attendee pays for themselves. */
    public static final String PURCHASER_TYPE_USER = "USER";

    /** Recorded in {@code Payment.beneficiaryType} when an event organiser earns the ticket. */
    public static final String BENEFICIARY_TYPE_ORGANIZER = "ORGANIZER";

    /**
     * The parent who pays for this attendee, or {@code null} when nobody is linked.
     *
     * <p>A missing link is not an error. A teacher or staff member buying their own ticket is a
     * paying attendee with no guardian, and refusing the sale - or inventing a payer - would be
     * worse than recording the purchase honestly. The payment records the attendee as the
     * purchaser, and the absence of a parent simply stays absent.
     *
     * <p>Resolved through {@code parent_student_links}, the only relationship that authorises a
     * parent to act for a child, and never inferred from a matching id. The primary guardian wins;
     * with several links and no primary the first is chosen, so the same payer is picked every
     * time for the same attendee.
     */
    private Parent resolvePayingParent(UUID studentId) {
        List<ParentStudentLink> links = parentStudentLinkRepository.findAllByStudentId(studentId);
        if (links.isEmpty()) {
            return null;
        }
        UUID parentId = links.stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsPrimary()))
                .findFirst()
                .orElse(links.get(0))
                .getParentId();
        // Resolved through the repository rather than the link's lazy association: parent_id is
        // the authoritative value and this avoids depending on a proxy being initialised.
        return parentRepository.findById(parentId).orElse(null);
    }

    // Mirrors EventServiceImpl's sets so both registration paths agree on what "open" means.
    private static final Set<EventStatus> REGISTRATION_OPEN_STATUSES =
            EnumSet.of(EventStatus.PUBLISHED, EventStatus.REGISTRATION_OPEN);
    private static final Set<EventStatus> REGISTRATION_BLOCKED_STATUSES =
            EnumSet.of(EventStatus.CANCELLED, EventStatus.RESCHEDULED,
                    EventStatus.FULL, EventStatus.FAILED);

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ParentStudentLinkRepository parentStudentLinkRepository;
    private final ParentRepository parentRepository;

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

        assertEligibleToBook(event, attendeeUserId);

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

        // Resolved where they genuinely exist, and tolerated as absent where they do not. Neither
        // lookup is an authorisation decision: access is already decided by assertEligibleToBook,
        // and the payer of record is the attendee themselves, recorded on the payment above.
        Student attendee = studentRepository.findByUserIdAndIsDeletedFalse(attendeeUserId).orElse(null);
        Parent payingParent = attendee == null ? null : resolvePayingParent(attendee.getId());

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
                // Who paid is recorded explicitly, in its own identifier space. Previously the
                // attendee's user id was written into parent_id and student_id, neither of which
                // is a users.id reference, so payer-scoped reporting, refunds and the parent inbox
                // could never see the money an event collected.
                .purchaserId(attendeeUserId)
                .purchaserType(PURCHASER_TYPE_USER)
                // Populated only when they are true. A staff member or teacher buying their own
                // ticket has no parent account and no learner record, and inventing either would
                // attach the payment to a person who does not exist.
                .parentId(payingParent == null ? null : payingParent.getId())
                .studentId(attendee == null ? null : attendee.getId())
                // Who earns this. The organiser is the authoritative owner of the event and is
                // recorded here at creation, where the event is known. Without it a ticket payment
                // named no beneficiary at all, so an organiser's earnings could not be stated
                // without guessing from their current profile.
                .beneficiaryId(event.getOrganizerId())
                .beneficiaryType(BENEFICIARY_TYPE_ORGANIZER)
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
        // Read under the event row lock BEFORE deciding whether anything is left to do. Read
        // first, two concurrent confirmations of the same booking would both see PAYMENT_PENDING,
        // both pass this guard, and both add their seats to the event.
        EventRegistration probe = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EventRegistration", "id", registrationId));
        Event event = eventRepository.findByIdForUpdate(probe.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event", "id", probe.getEventId()));

        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EventRegistration", "id", registrationId));

        if (!"PAYMENT_PENDING".equals(registration.getStatus())) {
            // Idempotent: a duplicate callback must not grant a second seat or extend anything.
            return registration;
        }



        // Capacity is checked at confirmation, not at request. If the event filled up while this
        // payment was in flight the payer is refunded rather than quietly overbooked.
        long held = registrationRepository.sumHeldSeats(event.getId());
        if (event.getMaxParticipants() != null
                && held + registration.getSeats() > event.getMaxParticipants()) {
            throw new IllegalStateException(
                    "The event filled up while this payment was in progress; the seat is not "
                            + "held and the payment is refundable");
        }

        // A PAYMENT_PENDING registration must have a payment, and that payment must be COMPLETED.
        // The earlier form of this check only refused when a payment existed and was not complete,
        // so a registration with no linked payment fell straight through and was given a seat with
        // nothing verified. Absence of evidence is not evidence of payment.
        if (registration.getPaymentId() == null) {
            throw new IllegalStateException(
                    "This registration has no payment, so the seat cannot be confirmed");
        }
        Payment payment = paymentRepository.findById(registration.getPaymentId())
                .orElseThrow(() -> new IllegalStateException(
                        "The payment for this registration no longer exists"));
        if (!"COMPLETED".equals(payment.getStatus())) {
            throw new IllegalStateException(
                    "A seat is only confirmed once its payment is completed");
        }

        // Snapshot what was actually charged. Deliberately read from the payment and never from
        // event.getTicketPrice(): repricing the event between booking and confirmation must not
        // restate what this attendee was charged.
        registration.setAmountPaid(payment.getAmount());
        registration.setStatus("REGISTERED");
        return registrationRepository.saveAndFlush(registration);
    }

    /**
     * The same eligibility rules the free registration path applies.
     *
     * <p>This check was missing when the paid path was added, which meant a learner could buy a
     * place at an event they were not allowed to register for: a PRIVATE event reserved to its
     * organizer, an event belonging to another institution, or one whose registration had
     * already closed. Payment is not a way around access control - paying more must never buy
     * more access than the free path grants.</p>
     */
    private void assertEligibleToBook(Event event, UUID attendeeUserId) {
        User attendee = userRepository.findById(attendeeUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", attendeeUserId));

        assertRegistrationIsOpen(event);
        assertEventHasNotStarted(event);
        assertAccessLevelAllowsBooking(event, attendee);
    }

    /**
     * Lifecycle gate, mirroring the free registration path.
     *
     * <p>The paid path originally checked access level only. That let somebody buy a place at an
     * event that had been cancelled, had filled up, or whose registration had closed - the very
     * events the free path refuses - and it charged them for it. A closed event must be closed
     * whoever is trying to pay.</p>
     */
    private void assertRegistrationIsOpen(Event event) {
        EventStatus status = currentStatusOf(event);

        if (REGISTRATION_BLOCKED_STATUSES.contains(status)) {
            if (status == EventStatus.FULL) {
                throw new IllegalStateException("This event is full - registration is closed");
            }
            throw new IllegalStateException("This event has been " + status.name().toLowerCase()
                    + " - registration is not possible");
        }
        if (!REGISTRATION_OPEN_STATUSES.contains(status)) {
            throw new IllegalArgumentException("This event is not available for registration");
        }
    }

    /** Exact mirror of {@code EventServiceImpl.hasEventStarted}, timezone handling included. */
    private void assertEventHasNotStarted(Event event) {
        if (event.getStartsAt() == null) {
            return;
        }
        boolean started;
        String tz = event.getTimezone();
        if (tz == null || tz.isBlank()) {
            started = event.getStartsAt().isBefore(LocalDateTime.now());
        } else {
            try {
                ZoneId zone = ZoneId.of(tz);
                started = event.getStartsAt().isBefore(LocalDateTime.now(zone));
            } catch (Exception ex) {
                started = event.getStartsAt().isBefore(LocalDateTime.now());
            }
        }
        if (started) {
            throw new IllegalArgumentException("Cannot register for a past event");
        }
    }

    /**
     * Access level, matching {@code EventServiceImpl.assertCanRegister} rule for rule.
     *
     * <p>The institution-admin allowance for a private event is deliberately reproduced here. It was
     * missing when the paid path was added, which meant an institution admin could register for a
     * private event for nothing but was refused when trying to pay for the same place - the free
     * path and the paid path disagreed about who is allowed in.</p>
     */
    private void assertAccessLevelAllowsBooking(Event event, User attendee) {
        String level = (event.getAccessLevel() == null || event.getAccessLevel().isBlank())
                ? "INSTITUTION" : event.getAccessLevel().trim().toUpperCase();

        if ("PUBLIC".equals(level)) {
            return;
        }

        if ("PRIVATE".equals(level)) {
            if (attendee.getId() != null && attendee.getId().equals(event.getOrganizerId())) {
                return;
            }
            if (isPlatformAdmin(attendee.getRole())
                    || (attendee.getRole() == User.Role.INSTITUTION_ADMIN
                        && sameInstitution(attendee, event))) {
                return;
            }
            throw new ForbiddenException("This event is restricted to its organizer");
        }

        if (isPlatformAdmin(attendee.getRole())) {
            return;
        }
        if (event.getInstitutionId() != null && attendee.getInstitutionId() != null
                && !event.getInstitutionId().equals(attendee.getInstitutionId())) {
            throw new IllegalArgumentException("This event is not available in your institution");
        }
    }

    private static boolean sameInstitution(User user, Event event) {
        return event.getInstitutionId() != null && user.getInstitutionId() != null
                && event.getInstitutionId().equals(user.getInstitutionId());
    }

    private static EventStatus currentStatusOf(Event event) {
        if (event.getEventStatus() != null) {
            return event.getEventStatus();
        }
        if (event.getStatus() != null && !event.getStatus().isBlank()) {
            try {
                return EventStatus.fromString(event.getStatus());
            } catch (IllegalArgumentException ex) {
                return EventStatus.DRAFT;
            }
        }
        return EventStatus.DRAFT;
    }

    private static boolean isPlatformAdmin(User.Role role) {
        return role == User.Role.ADMIN || role == User.Role.NATIONAL_ADMIN;
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