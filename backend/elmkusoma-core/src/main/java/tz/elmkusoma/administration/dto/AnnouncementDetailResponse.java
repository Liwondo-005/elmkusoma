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
public class AnnouncementDetailResponse {
    private UUID id;
    private String title;
    private String content;
    private String priority;
    private String audienceType;
    private List<UUID> targetDistrictIds;
    private List<UUID> targetInstitutionIds;
    private long recipientCount;
    private String sentBy;
    private LocalDateTime sentAt;
    private LocalDateTime expiresAt;
}
