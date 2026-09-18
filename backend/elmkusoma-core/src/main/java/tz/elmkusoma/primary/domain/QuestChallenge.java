package tz.elmkusoma.primary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quest_challenges")
public class QuestChallenge extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "quest_type", nullable = false, length = 30)
    private String questType;

    @Column(name = "difficulty", nullable = false, length = 10)
    private String difficulty;

    @Column(name = "subject_name", length = 100)
    private String subjectName;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "score")
    private Integer score;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public QuestChallenge() {
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
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

    public String getQuestType() {
        return questType;
    }

    public void setQuestType(String questType) {
        this.questType = questType;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
