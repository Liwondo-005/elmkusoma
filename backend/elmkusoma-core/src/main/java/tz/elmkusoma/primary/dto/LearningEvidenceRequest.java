package tz.elmkusoma.primary.dto;

import jakarta.validation.constraints.NotBlank;

public class LearningEvidenceRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Evidence type is required")
    private String evidenceType;

    private String description;

    private String evidenceUrl;

    private String subjectName;

    private Integer points;

    public LearningEvidenceRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }
}
