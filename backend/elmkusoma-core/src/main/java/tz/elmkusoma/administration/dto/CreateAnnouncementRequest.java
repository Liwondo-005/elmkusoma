package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAnnouncementRequest {
    private @jakarta.validation.constraints.NotBlank(message = "Title is required") String title;
    private @jakarta.validation.constraints.NotBlank(message = "Content is required") String content;
    private String priority;
    private @jakarta.validation.constraints.NotBlank(message = "Audience type is required") String audienceType;
    private List<UUID> targetDistrictIds;
    private List<UUID> targetInstitutionIds;
    private LocalDateTime expiresAt;
}
