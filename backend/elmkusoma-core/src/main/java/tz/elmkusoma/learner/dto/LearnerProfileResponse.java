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
public class LearnerProfileResponse {

    private UUID id;
    private UUID userId;
    private String interests;
    private String bio;
    private String avatarUrl;
    private String learningGoal;
    private String userFullName;
    private String userEmail;
    private LocalDateTime createdAt;
}
