package tz.elmkusoma.nfe.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionRequest {

    @NotNull(message = "Provider ID is required")
    private UUID providerId;

    private UUID programId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Session type is required")
    private String sessionType;

    @NotNull(message = "Scheduled date is required")
    private LocalDateTime scheduledAt;

    private Integer durationMinutes;

    private String meetingUrl;

    private Integer maxParticipants;

    private String status;
}
