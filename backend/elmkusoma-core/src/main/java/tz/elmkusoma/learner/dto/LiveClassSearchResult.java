package tz.elmkusoma.learner.dto;

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
public class LiveClassSearchResult {

    private UUID id;
    private String title;
    private String description;
    private String status;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
}
