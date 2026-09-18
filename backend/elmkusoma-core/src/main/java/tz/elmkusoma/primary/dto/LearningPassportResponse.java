package tz.elmkusoma.primary.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class LearningPassportResponse {

    private UUID id;
    private Integer stampsEarned;
    private Integer totalStamps;
    private String currentCountry;
    private LocalDateTime lastActivity;

    public LearningPassportResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
