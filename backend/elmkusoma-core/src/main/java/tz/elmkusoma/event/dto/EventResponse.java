package tz.elmkusoma.event.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EventResponse {
    private UUID id;
    private UUID institutionId;
    private UUID organizerId;
    private String organizerName;
    private String title;
    private String description;
    private String eventType;
    private String category;
    private String location;
    private String meetingUrl;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Integer durationMinutes;
    private Integer maxParticipants;
    private Integer registeredCount;
    private Integer availableSpots;
    private String status;
    private String thumbnailUrl;
    private String tags;
    private Boolean isFree;
    private Boolean requiresApproval;
    private Boolean isRegistered;
    private String registrationStatus;
    private Integer materialCount;
    private Boolean hasRecording;
    private LocalDateTime createdAt;
}
