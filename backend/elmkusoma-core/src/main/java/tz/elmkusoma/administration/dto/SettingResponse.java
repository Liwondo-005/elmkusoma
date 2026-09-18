package tz.elmkusoma.administration.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class SettingResponse {

    private UUID id;
    private UUID institutionId;
    private String settingKey;
    private Map<String, Object> settingValue;
    private String settingType;
    private String description;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getInstitutionId() { return institutionId; }
    public void setInstitutionId(UUID institutionId) { this.institutionId = institutionId; }
    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
    public Map<String, Object> getSettingValue() { return settingValue; }
    public void setSettingValue(Map<String, Object> settingValue) { this.settingValue = settingValue; }
    public String getSettingType() { return settingType; }
    public void setSettingType(String settingType) { this.settingType = settingType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static SettingResponse of(UUID id, UUID institutionId, String settingKey,
                                     Map<String, Object> settingValue, String settingType,
                                     String description, Boolean isPublic,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        SettingResponse response = new SettingResponse();
        response.id = id;
        response.institutionId = institutionId;
        response.settingKey = settingKey;
        response.settingValue = settingValue;
        response.settingType = settingType;
        response.description = description;
        response.isPublic = isPublic;
        response.createdAt = createdAt;
        response.updatedAt = updatedAt;
        return response;
    }
}