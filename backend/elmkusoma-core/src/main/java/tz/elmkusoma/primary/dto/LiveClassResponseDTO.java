package tz.elmkusoma.primary.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class LiveClassResponseDTO {

    private UUID id;
    private String activityType;
    private String selectedAnswer;
    private Integer score;
    private LocalDateTime respondedAt;

    public LiveClassResponseDTO() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getSelectedAnswer() {
        return selectedAnswer;
    }

    public void setSelectedAnswer(String selectedAnswer) {
        this.selectedAnswer = selectedAnswer;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
}
