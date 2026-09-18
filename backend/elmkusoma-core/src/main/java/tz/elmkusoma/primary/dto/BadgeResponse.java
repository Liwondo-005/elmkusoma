package tz.elmkusoma.primary.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class BadgeResponse {

    private UUID id;
    private String badgeName;
    private String badgeType;
    private String description;
    private String iconUrl;
    private Integer points;
    private LocalDateTime awardedAt;

    public BadgeResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
