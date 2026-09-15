package tz.elmkusoma.event.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class EventRegistrationRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    private String cancellationReason;
}
