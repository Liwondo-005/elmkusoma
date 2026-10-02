package tz.elmkusoma.oversight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Nationaladmin.md §23 — create a jurisdictional announcement. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementRequest {

    @NotBlank
    @Size(max = 300)
    private String title;

    @NotEmpty
    private String content;

    @Size(max = 20)
    private String priority;

    /** NATIONWIDE | REGION | DISTRICT */
    @NotBlank
    private String audienceType;

    private UUID audienceRegionId;

    private UUID audienceDistrictId;

    /** Optional — announcements scheduled for the future stay SCHEDULED until then. */
    private LocalDateTime scheduledAt;
}
