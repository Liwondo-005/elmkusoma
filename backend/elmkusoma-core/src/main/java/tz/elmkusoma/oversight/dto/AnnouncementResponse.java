package tz.elmkusoma.oversight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Nationaladmin.md §23 — announcement as rendered by oversight surfaces. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementResponse {
    private UUID id;
    private String title;
    private String content;
    private String priority;
    private String audienceType;
    private UUID audienceRegionId;
    private String audienceRegionName;
    private UUID audienceDistrictId;
    private String audienceDistrictName;
    private String status;
    private LocalDateTime scheduledAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private String authorName;
    private String institutionName;
}
