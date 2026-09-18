package tz.elmkusoma.primary.dto;

import jakarta.validation.constraints.NotBlank;

public class DiscoveryEntryRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Question is required")
    private String question;

    @NotBlank(message = "Discovery type is required")
    private String discoveryType;

    private String subjectName;

    public DiscoveryEntryRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getDiscoveryType() {
        return discoveryType;
    }

    public void setDiscoveryType(String discoveryType) {
        this.discoveryType = discoveryType;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
}
