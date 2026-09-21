package tz.elmkusoma.primary.dto;

import jakarta.validation.constraints.NotBlank;

public class PortfolioItemRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String fileUrl;

    @NotBlank(message = "Portfolio type is required")
    private String portfolioType;

    private String subjectName;

    private String content;

    public PortfolioItemRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getPortfolioType() {
        return portfolioType;
    }

    public void setPortfolioType(String portfolioType) {
        this.portfolioType = portfolioType;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
