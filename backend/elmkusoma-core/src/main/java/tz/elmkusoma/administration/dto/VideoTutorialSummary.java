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
public class VideoTutorialSummary {
    private UUID id;
    private String title;
    private String status;
    private Integer durationSeconds;
    private String recordingUrl;
    private UUID institutionId;
    private String institutionName;
    private LocalDateTime createdAt;
}
