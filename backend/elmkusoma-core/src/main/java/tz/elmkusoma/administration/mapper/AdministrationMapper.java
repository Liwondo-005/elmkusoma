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
        return SettingResponse.of(
                setting.getId(),
                setting.getInstitutionId(),
                setting.getSettingKey(),
                setting.getSettingValue(),
                setting.getSettingType(),
                setting.getDescription(),
                setting.getIsPublic(),
                setting.getCreatedAt(),
                setting.getUpdatedAt()
        );
    }

    public SystemSetting toSettingEntity(SettingRequest request, UUID institutionId) {
        return SystemSetting.of(
                request.getSettingKey(),
                request.getSettingValue(),
                request.getSettingType() != null ? request.getSettingType() : "STRING",
                request.getDescription(),
                request.getIsPublic() != null ? request.getIsPublic() : false,
                institutionId
        );
    }

    public RoleResponse toRoleResponse(CustomRole role, List<String> permissions) {
        return RoleResponse.of(
                role.getId(),
                role.getInstitutionId(),
                role.getName(),
                role.getDisplayName(),
                role.getDescription(),
                role.getIsSystemRole(),
                role.getIsActive(),
                permissions,
                role.getCreatedAt()
        );
    }

    public ImportJobResponse toImportJobResponse(DataImportJob job) {
        return ImportJobResponse.of(
                job.getId(),
                job.getInstitutionId(),
                job.getImportedBy(),
                job.getImportType(),
                job.getFileName(),
                job.getStatus().name(),
                job.getTotalRows(),
                job.getProcessedRows(),
                job.getSuccessfulRows(),
                job.getFailedRows(),
                job.getErrorLog(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }
}