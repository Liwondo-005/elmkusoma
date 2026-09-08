package tz.elmkusoma.administration.mapper;

import org.springframework.stereotype.Component;
import tz.elmkusoma.administration.domain.*;
import tz.elmkusoma.administration.dto.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AdministrationMapper {

    public SettingResponse toSettingResponse(SystemSetting setting) {
        return SettingResponse.builder()
                .id(setting.getId())
                .institutionId(setting.getInstitutionId())
                .settingKey(setting.getSettingKey())
                .settingValue(setting.getSettingValue())
                .settingType(setting.getSettingType())
                .description(setting.getDescription())
                .isPublic(setting.getIsPublic())
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }

    public SystemSetting toSettingEntity(SettingRequest request, UUID institutionId) {
        SystemSetting setting = SystemSetting.builder()
                .settingKey(request.getSettingKey())
                .settingValue(request.getSettingValue())
                .settingType(request.getSettingType() != null ? request.getSettingType() : "STRING")
                .description(request.getDescription())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .build();
        setting.setInstitutionId(institutionId);
        return setting;
    }

    public RoleResponse toRoleResponse(CustomRole role, List<String> permissions) {
        return RoleResponse.builder()
                .id(role.getId())
                .institutionId(role.getInstitutionId())
                .name(role.getName())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .isSystemRole(role.getIsSystemRole())
                .isActive(role.getIsActive())
                .permissions(permissions)
                .createdAt(role.getCreatedAt())
                .build();
    }

    public ImportJobResponse toImportJobResponse(DataImportJob job) {
        return ImportJobResponse.builder()
                .id(job.getId())
                .institutionId(job.getInstitutionId())
                .importedBy(job.getImportedBy())
                .importType(job.getImportType())
                .fileName(job.getFileName())
                .status(job.getStatus().name())
                .totalRows(job.getTotalRows())
                .processedRows(job.getProcessedRows())
                .successfulRows(job.getSuccessfulRows())
                .failedRows(job.getFailedRows())
                .errorLog(job.getErrorLog())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
