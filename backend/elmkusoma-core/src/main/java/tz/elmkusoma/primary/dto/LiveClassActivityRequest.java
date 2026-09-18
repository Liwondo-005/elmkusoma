package tz.elmkusoma.primary.dto;

import jakarta.validation.constraints.NotBlank;

public class LiveClassActivityRequest {

    @NotBlank(message = "Activity type is required")
    private String activityType;

    @NotBlank(message = "Title is required")
    private String title;

    private String options;

    private String correctAnswer;

    private Integer orderIndex;

    public LiveClassActivityRequest() {
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
