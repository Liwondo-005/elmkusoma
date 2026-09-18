package tz.elmkusoma.primary.dto;

import java.util.UUID;

public class LearningProfileResponse {

    private UUID id;
    private String learningStyle;
    private String strengths;
    private String interests;
    private String goals;
    private Integer totalPoints;
    private Integer level;

    public LearningProfileResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }
}
