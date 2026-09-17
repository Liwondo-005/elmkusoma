package tz.elmkusoma.administration.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tz.elmkusoma.common.BaseEntity;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "system_settings")
public class SystemSetting extends BaseEntity {

    @Column(name = "setting_key", nullable = false)
    private String settingKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "setting_value", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> settingValue;

    @Column(name = "setting_type", nullable = false)
    private String settingType = "STRING";

    @Column(name = "description")
    private String description;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

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

    public static SystemSetting of(String settingKey, Map<String, Object> settingValue,
                                   String settingType, String description, Boolean isPublic,
                                   UUID institutionId) {
        SystemSetting setting = new SystemSetting();
        setting.settingKey = settingKey;
        setting.settingValue = settingValue;
        setting.settingType = settingType;
        setting.description = description;
        setting.isPublic = isPublic;
        setting.setInstitutionId(institutionId);
        return setting;
    }
}