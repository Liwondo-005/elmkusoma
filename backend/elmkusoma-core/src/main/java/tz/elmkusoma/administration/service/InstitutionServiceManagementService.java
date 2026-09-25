package tz.elmkusoma.administration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.domain.InstitutionService;
import tz.elmkusoma.administration.dto.InstitutionServiceRequest;
import tz.elmkusoma.administration.dto.InstitutionServiceResponse;
import tz.elmkusoma.administration.repository.InstitutionServiceRepository;
import tz.elmkusoma.administration.repository.PlatformFeatureRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstitutionServiceManagementService {

    private final InstitutionServiceRepository institutionServiceRepository;
    private final PlatformFeatureRepository platformFeatureRepository;

    @Transactional(readOnly = true)
    public List<InstitutionServiceResponse> getServices(UUID institutionId) {
        List<InstitutionService> services = institutionServiceRepository.findByInstitutionIdAndIsDeletedFalse(institutionId);
        return services.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InstitutionServiceResponse getService(UUID institutionId, String featureKey) {
        InstitutionService service = institutionServiceRepository.findByInstitutionIdAndFeatureKey(institutionId, featureKey).orElse(null);
        if (service == null) {
            throw new ResourceNotFoundException("InstitutionService", "featureKey", featureKey);
        }
        return toResponse(service);
    }

    @Transactional
    public InstitutionServiceResponse enableService(UUID institutionId, InstitutionServiceRequest request, UUID userId) {
        // Verify platform feature exists
        platformFeatureRepository.findByFeatureKeyAndIsDeletedFalse(request.getFeatureKey())
                .orElseThrow(() -> new ResourceNotFoundException("PlatformFeature", "featureKey", request.getFeatureKey()));

        InstitutionService service = institutionServiceRepository.findByInstitutionIdAndFeatureKey(institutionId, request.getFeatureKey()).orElse(null);
        if (service == null) {
            service = InstitutionService.builder()
                    .institutionId(institutionId)
                    .featureKey(request.getFeatureKey())
                    .build();
        }

        service.setEnabled(true);
        service.setEnabledAt(LocalDateTime.now());
        service.setEnabledBy(userId);
        if (request.getConfiguration() != null) {
            service.setConfiguration(request.getConfiguration());
        }

        service = institutionServiceRepository.save(service);
        log.info("Enabled service {} for institution {} by user {}", request.getFeatureKey(), institutionId, userId);
        return toResponse(service);
    }

    @Transactional
    public InstitutionServiceResponse disableService(UUID institutionId, String featureKey) {
        InstitutionService service = institutionServiceRepository.findByInstitutionIdAndFeatureKey(institutionId, featureKey).orElse(null);
        if (service == null) {
            throw new ResourceNotFoundException("InstitutionService", "featureKey", featureKey);
        }

        service.setEnabled(false);
        service.setEnabledAt(null);
        service.setEnabledBy(null);

        service = institutionServiceRepository.save(service);
        log.info("Disabled service {} for institution {}", featureKey, institutionId);
        return toResponse(service);
    }

    @Transactional
    public InstitutionServiceResponse updateService(UUID institutionId, String featureKey, InstitutionServiceRequest request) {
        InstitutionService service = institutionServiceRepository.findByInstitutionIdAndFeatureKey(institutionId, featureKey).orElse(null);
        if (service == null) {
            throw new ResourceNotFoundException("InstitutionService", "featureKey", featureKey);
        }

        if (request.getConfiguration() != null) {
            service.setConfiguration(request.getConfiguration());
        }
        if (request.getEnabled() != null) {
            service.setEnabled(request.getEnabled());
            if (Boolean.TRUE.equals(request.getEnabled())) {
                service.setEnabledAt(LocalDateTime.now());
            } else {
                service.setEnabledAt(null);
            }
        }

        service = institutionServiceRepository.save(service);
        return toResponse(service);
    }

    @Transactional
    public void deleteService(UUID institutionId, String featureKey) {
        InstitutionService service = institutionServiceRepository.findByInstitutionIdAndFeatureKey(institutionId, featureKey).orElse(null);
        if (service == null) {
            throw new ResourceNotFoundException("InstitutionService", "featureKey", featureKey);
        }

        service.setIsDeleted(true);
        institutionServiceRepository.save(service);
    }

    private InstitutionServiceResponse toResponse(InstitutionService service) {
        String featureName = platformFeatureRepository.findByFeatureKeyAndIsDeletedFalse(service.getFeatureKey())
                .map(tz.elmkusoma.administration.domain.PlatformFeature::getName)
                .orElse(service.getFeatureKey());

        String description = platformFeatureRepository.findByFeatureKeyAndIsDeletedFalse(service.getFeatureKey())
                .map(tz.elmkusoma.administration.domain.PlatformFeature::getDescription)
                .orElse("");

        return InstitutionServiceResponse.builder()
                .id(service.getId())
                .institutionId(service.getInstitutionId())
                .featureKey(service.getFeatureKey())
                .featureName(featureName)
                .status(service.getEnabled() ? "ENABLED" : "DISABLED")
                .enabled(service.getEnabled())
                .configuration(service.getConfiguration())
                .enabledAt(service.getEnabledAt())
                .enabledBy(service.getEnabledBy() != null ? service.getEnabledBy().toString() : null)
                .description(description)
                .build();
    }
}