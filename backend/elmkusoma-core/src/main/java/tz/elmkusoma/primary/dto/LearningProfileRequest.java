package tz.elmkusoma.primary.dto;

import jakarta.validation.constraints.NotBlank;

public class LearningProfileRequest {

    @NotBlank(message = "Learning style is required")
    private String learningStyle;

    private String strengths;

    private String interests;

    private String goals;

    public LearningProfileRequest() {
    }

    public String getLearningStyle() {
        return learningStyle;
    }

    public void setLearningStyle(String learningStyle) {
        this.learningStyle = learningStyle;
    }

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public String getGoals() {
        return goals;
    }

    public void setGoals(String goals) {
        this.goals = goals;
    }
}
