package tz.elmkusoma.primary.dto;

public class ELmkusomaLabAttemptRequest {

    private String studentNotes;
    private Integer score;

    public ELmkusomaLabAttemptRequest() {
    }

    public String getStudentNotes() {
        return studentNotes;
    }

    public void setStudentNotes(String studentNotes) {
        this.studentNotes = studentNotes;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}
