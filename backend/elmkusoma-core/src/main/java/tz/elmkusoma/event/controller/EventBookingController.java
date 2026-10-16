package tz.elmkusoma.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.event.service.EventBookingService;

import java.util.UUID;

/**
 * Booking a place at an event, including a paid one.
 *
 * <p>Separate from the free registration path on purpose: a free booking is immediate, while a
 * paid booking creates a real pending payment and holds nothing until that payment is confirmed.
 * Merging them would make "registered" mean two different things.</p>
 *
 * <p>The attendee is taken from the authenticated user and the seats from the body; neither can
 * be used to book for somebody else or to obtain a seat the event does not have.</p>
 */
@RestController
@RequestMapping("/v1/learner/events")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OTHER_LEARNER', 'STUDENT', 'LEARNER')")
@Tag(name = "Learner - Event Booking")
public class EventBookingController {

    private final EventBookingService bookingService;

    /** Seats to hold. Absent means one; never zero or negative. */
    public record BookSeatsRequest(Integer seats) {
    }

    @PostMapping("/{eventId}/bookings")
    @Operation(summary = "Book a place. A paid event creates a pending payment and holds no seat until it is confirmed.")
    public ResponseEntity<ApiResponse<EventBookingService.Booking>> book(
            @PathVariable UUID eventId,
            @RequestBody(required = false) BookSeatsRequest body,
            HttpServletRequest request) {

        Object userIdAttr = request.getAttribute("userId");
        if (!(userIdAttr instanceof UUID userId)) {
            throw new IllegalStateException("Authenticated user id is missing from the request");
        }
        int seats = body == null || body.seats() == null ? 1 : body.seats();

        return ResponseEntity.ok(ApiResponse.success(bookingService.book(eventId, userId, seats)));
    }
}