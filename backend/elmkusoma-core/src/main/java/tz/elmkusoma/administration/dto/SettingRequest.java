package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public class SettingRequest {

    @NotBlank(message = "Setting key is required")
    private String settingKey;
    private Map<String, Object> settingValue;
    private String settingType;
    private String description;
    private Boolean isPublic;

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

    public static SettingRequest of(String settingKey, Map<String, Object> settingValue,
                                    String settingType, String description, Boolean isPublic) {
        SettingRequest req = new SettingRequest();
        req.settingKey = settingKey;
        req.settingValue = settingValue;
        req.settingType = settingType;
        req.description = description;
        req.isPublic = isPublic;
        return req;
    }
}