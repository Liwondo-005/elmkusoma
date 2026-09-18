package tz.elmkusoma.primary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_badges")
public class StudentBadge extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "badge_name", nullable = false, length = 100)
    private String badgeName;

    @Column(name = "badge_type", nullable = false, length = 50)
    private String badgeType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "points")
    private Integer points;

    @Column(name = "awarded_at", nullable = false)
    private LocalDateTime awardedAt;

    public StudentBadge() {
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public String getBadgeName() {
        return badgeName;
    }

    public void setBadgeName(String badgeName) {
        this.badgeName = badgeName;
    }

    public String getBadgeType() {
        return badgeType;
    }

    public void setBadgeType(String badgeType) {
        this.badgeType = badgeType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public LocalDateTime getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(LocalDateTime awardedAt) {
        this.awardedAt = awardedAt;
    }
}
