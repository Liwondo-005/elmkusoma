package tz.elmkusoma.course.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateLiveClassRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 300, message = "Title must not exceed 300 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotBlank(message = "Scheduled time is required")
    private String scheduledAt;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 480, message = "Duration must not exceed 480 minutes")
    private Integer durationMinutes;

    private UUID subjectId;

    private UUID classGroupId;

    /** Optional Lesson link (Lesson ↔ Live Class connection). */
    private UUID lessonId;

    @Min(value = 1, message = "Max participants must be at least 1")
    @Max(value = 10000, message = "Max participants must not exceed 10000")
    private Integer maxParticipants;

    private Boolean recordingEnabled;

    private String sessionType;

    private String timezone;

    private Boolean isRecurring;

    private String recurrencePattern;

    private String recurrenceEndDate;

    private Boolean lobbyEnabled;

    /**
     * Declared broadcast source: BROWSER, MOBILE, USB_CAMERA, PROFESSIONAL_CAMERA,
     * OBS, ENCODER, STUDIO, OTHER. Optional — null/blank means BROWSER (the
     * existing laptop-camera default), keeping older clients fully compatible.
     */
    private String broadcastSource;

    /**
     * Representative card image, as a URL returned by the cover upload endpoint.
     *
     * <p>A URL rather than a file: uploads go through their own endpoint so the image can be
     * validated and stored once, then referenced by the class. Absolute or root-relative only.</p>
     */
    @jakarta.validation.constraints.Pattern(
            regexp = "^$|^(https?://|/)[\\x20-\\x7E]+$",
            message = "Cover image URL must start with http://, https:// or /")
    @jakarta.validation.constraints.Size(max = 1000,
            message = "Cover image URL must not exceed 1000 characters")
    private String coverImageUrl;

    /**
     * Who may discover and join this class.
     *
     * <p>Accepted but never trusted to grant visibility on its own: a class is publicly
     * discoverable only when it is also PUBLISHED by a Platform Admin, which is what stops a
     * teacher from self-publishing. Defaults to INSTITUTION, preserving current behaviour.</p>
     */
    @jakarta.validation.constraints.Pattern(
            regexp = "PUBLIC|AUTHENTICATED|INSTITUTION|PRIVATE|INVITED|RESTRICTED",
            message = "Access level must be one of: PUBLIC, AUTHENTICATED, INSTITUTION, PRIVATE, INVITED, RESTRICTED")
    private String accessLevel;
}
