package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingRequest {

    @NotBlank(message = "Setting key is required")
    private String settingKey;

    private Map<String, Object> settingValue;

    private String settingType;

    private String description;

    private Boolean isPublic;
}
