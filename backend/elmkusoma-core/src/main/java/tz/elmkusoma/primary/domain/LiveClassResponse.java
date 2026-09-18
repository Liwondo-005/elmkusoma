package tz.elmkusoma.primary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_class_responses")
public class LiveClassResponse extends BaseEntity {

    @Column(name = "live_class_id", nullable = false)
    private UUID liveClassId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "activity_type", nullable = false, length = 30)
    private String activityType;

    @Column(name = "selected_answer", length = 500)
    private String selectedAnswer;

    @Column(name = "score")
    private Integer score;

    @Column(name = "responded_at", nullable = false)
    private LocalDateTime respondedAt;

    public LiveClassResponse() {
    }

    public UUID getLiveClassId() {
        return liveClassId;
    }

    public void setLiveClassId(UUID liveClassId) {
        this.liveClassId = liveClassId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
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
