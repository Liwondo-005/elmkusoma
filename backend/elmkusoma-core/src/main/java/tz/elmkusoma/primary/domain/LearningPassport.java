package tz.elmkusoma.primary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "learning_passports")
public class LearningPassport extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "stamps_earned", nullable = false)
    private Integer stampsEarned = 0;

    @Column(name = "total_stamps", nullable = false)
    private Integer totalStamps = 100;

    @Column(name = "current_country", length = 100)
    private String currentCountry;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    public LearningPassport() {
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

    public Integer getStampsEarned() {
        return stampsEarned;
    }

    public void setStampsEarned(Integer stampsEarned) {
        this.stampsEarned = stampsEarned;
    }

    public Integer getTotalStamps() {
        return totalStamps;
    }

    public void setTotalStamps(Integer totalStamps) {
        this.totalStamps = totalStamps;
    }

    public String getCurrentCountry() {
        return currentCountry;
    }

    public void setCurrentCountry(String currentCountry) {
        this.currentCountry = currentCountry;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
}
