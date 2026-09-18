package tz.elmkusoma.primary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "learning_profiles")
public class LearningProfile extends BaseEntity {

    @Column(name = "student_id", nullable = false, unique = true)
    private UUID studentId;

    @Column(name = "learning_style", nullable = false, length = 30)
    private String learningStyle;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "interests", columnDefinition = "TEXT")
    private String interests;

    @Column(name = "goals", columnDefinition = "TEXT")
    private String goals;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    public LearningProfile() {
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
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
