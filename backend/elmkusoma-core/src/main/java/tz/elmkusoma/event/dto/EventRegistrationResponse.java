package tz.elmkusoma.event.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EventRegistrationResponse {
    private UUID id;
    private UUID eventId;
    private String eventTitle;
    private LocalDateTime eventStartsAt;
    private LocalDateTime eventEndsAt;
    private String eventLocation;
    private String eventMeetingUrl;
    private String eventType;
    private String status;
    private LocalDateTime registeredAt;
    private LocalDateTime cancelledAt;
    private Boolean attended;
    private Integer availableSpots;
}
