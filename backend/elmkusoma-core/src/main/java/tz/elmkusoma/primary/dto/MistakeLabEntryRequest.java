package tz.elmkusoma.primary.dto;

import jakarta.validation.constraints.NotBlank;

public class MistakeLabEntryRequest {

    @NotBlank(message = "Question is required")
    private String question;

    private String wrongAnswer;

    private String correctAnswer;

    private String explanation;

    private String subjectName;

    public MistakeLabEntryRequest() {
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getWrongAnswer() {
        return wrongAnswer;
    }

    public void setWrongAnswer(String wrongAnswer) {
        this.wrongAnswer = wrongAnswer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
}
