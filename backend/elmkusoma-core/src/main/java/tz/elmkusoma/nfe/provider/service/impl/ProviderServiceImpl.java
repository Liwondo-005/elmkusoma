package tz.elmkusoma.nfe.provider.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nfe.provider.domain.EducationProvider;
import tz.elmkusoma.nfe.provider.dto.ProviderRequest;
import tz.elmkusoma.nfe.provider.dto.ProviderResponse;
import tz.elmkusoma.nfe.provider.dto.ProviderStatsResponse;
import tz.elmkusoma.nfe.provider.repository.EducationProviderRepository;
import tz.elmkusoma.nfe.provider.service.ProviderService;
import tz.elmkusoma.nfe.program.repository.NfeProgramRepository;
import tz.elmkusoma.nfe.learner.repository.NfeLearnerRepository;
import tz.elmkusoma.nfe.session.repository.NfeSessionRepository;
import tz.elmkusoma.nfe.certificate.repository.NfeCertificateRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProviderServiceImpl implements ProviderService {

    private final EducationProviderRepository providerRepository;
    private final NfeProgramRepository programRepository;
    private final NfeLearnerRepository learnerRepository;
    private final NfeSessionRepository sessionRepository;
    private final NfeCertificateRepository certificateRepository;
    private final tz.elmkusoma.shared.repository.InstitutionRepository institutionRepository;

    @Override
    public ProviderResponse createProvider(UUID institutionId, ProviderRequest request) {
        if (providerRepository.existsByNameAndInstitutionIdAndIsDeletedFalse(request.getName(), institutionId)) {
            throw new IllegalArgumentException("Provider with this name already exists in this institution");
        }

        EducationProvider provider = EducationProvider.builder()
                .name(request.getName())
                .providerType(EducationProvider.ProviderType.valueOf(request.getProviderType()))
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .website(request.getWebsite())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .country(request.getCountry())
                .contactPersonName(request.getContactPersonName())
                .contactPersonEmail(request.getContactPersonEmail())
                .contactPersonPhone(request.getContactPersonPhone())
                .isActive(true)
                .isVerified(false)
                .build();
        provider.setInstitutionId(institutionId);

        EducationProvider saved = providerRepository.save(provider);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderResponse getProvider(UUID institutionId, UUID providerId) {
        EducationProvider provider = providerRepository.findByIdAndInstitutionId(providerId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Education Provider", "id", providerId));
        return mapToResponse(provider);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProviderResponse> listProviders(UUID institutionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<EducationProvider> allProviders = providerRepository.findAllByInstitutionId(institutionId);

        int start = Math.min(page * size, allProviders.size());
        int end = Math.min((page + 1) * size, allProviders.size());
        List<EducationProvider> paged = allProviders.subList(start, end);

        Page<EducationProvider> providerPage = new org.springframework.data.domain.PageImpl<>(
                paged, pageable, allProviders.size());

        List<ProviderResponse> content = providerPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                content,
                providerPage.getNumber(),
                providerPage.getSize(),
                providerPage.getTotalElements(),
                providerPage.getTotalPages(),
                providerPage.isFirst(),
                providerPage.isLast()
        );
    }

    @Override
    public ProviderResponse updateProvider(UUID institutionId, UUID providerId, ProviderRequest request) {
        EducationProvider provider = providerRepository.findByIdAndInstitutionId(providerId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Education Provider", "id", providerId));

        if (request.getName() != null) provider.setName(request.getName());
        if (request.getProviderType() != null) provider.setProviderType(EducationProvider.ProviderType.valueOf(request.getProviderType()));
        if (request.getDescription() != null) provider.setDescription(request.getDescription());
        if (request.getLogoUrl() != null) provider.setLogoUrl(request.getLogoUrl());
        if (request.getWebsite() != null) provider.setWebsite(request.getWebsite());
        if (request.getEmail() != null) provider.setEmail(request.getEmail());
        if (request.getPhone() != null) provider.setPhone(request.getPhone());
        if (request.getAddress() != null) provider.setAddress(request.getAddress());
        if (request.getCity() != null) provider.setCity(request.getCity());
        if (request.getCountry() != null) provider.setCountry(request.getCountry());
        if (request.getContactPersonName() != null) provider.setContactPersonName(request.getContactPersonName());
        if (request.getContactPersonEmail() != null) provider.setContactPersonEmail(request.getContactPersonEmail());
        if (request.getContactPersonPhone() != null) provider.setContactPersonPhone(request.getContactPersonPhone());

        EducationProvider saved = providerRepository.save(provider);
        return mapToResponse(saved);
    }

    @Override
    public void deleteProvider(UUID institutionId, UUID providerId) {
        EducationProvider provider = providerRepository.findByIdAndInstitutionId(providerId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Education Provider", "id", providerId));
        provider.setIsDeleted(true);
        providerRepository.save(provider);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderResponse> getActiveProviders(UUID institutionId) {
        return providerRepository.findActiveByInstitutionId(institutionId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * B-01: resolves the provider for the caller's institution and provisions it on first use.
     *
     * <p>The seven provider resources all hang off an {@code nfe_education_providers} row, but
     * nothing in the product ever created one, so the whole workspace reported
     * "No education provider exists for this institution yet" and the assessment/attendance/
     * certificate lists were permanently empty. The provider identity is derived from the
     * institution the platform admin already approved -- no second model, no duplicate data.
     */
    @Override
    public ProviderResponse getOrCreateProviderForInstitution(UUID institutionId, String actor) {
        List<EducationProvider> existing = providerRepository.findAllByInstitutionId(institutionId);
        for (EducationProvider p : existing) {
            if (!Boolean.TRUE.equals(p.getIsDeleted())) {
                return mapToResponse(p);
            }
        }

        tz.elmkusoma.shared.domain.Institution institution = institutionRepository
                .findByIdAndIsDeletedFalse(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", institutionId));

        EducationProvider provider = EducationProvider.builder()
                .name(institution.getName())
                // Institution.InstitutionType -> EducationProvider.ProviderType (two distinct
                // vocabularies; only the values below are accepted by the column CHECK constraint).
                .providerType(mapInstitutionType(institution))
                .description(institution.getDescription())
                .logoUrl(institution.getLogoUrl())
                .website(institution.getWebsite())
                .email(institution.getEmail())
                .phone(institution.getPhone())
                .address(institution.getAddress())
                .city(institution.getCity())
                .country(institution.getCountry())
                .isActive(!Boolean.FALSE.equals(institution.getIsActive()))
                // Approved institutions are verified at provisioning time; the platform can still
                // review or revoke it through the verification endpoint.
                .isVerified("ACTIVE".equalsIgnoreCase(String.valueOf(institution.getStatus())))
                .build();
        provider.setInstitutionId(institutionId);
        provider.setCreatedBy(actor);
        return mapToResponse(providerRepository.save(provider));
    }

    private EducationProvider.ProviderType mapInstitutionType(tz.elmkusoma.shared.domain.Institution institution) {
        String type = institution.getType() != null ? institution.getType().name() : null;
        if (type == null) {
            return EducationProvider.ProviderType.ORGANIZATION;
        }
        return switch (type) {
            case "TRAINING_PROVIDER" -> EducationProvider.ProviderType.TRAINING;
            case "COMPANY" -> EducationProvider.ProviderType.COMPANY;
            case "GOVERNMENT" -> EducationProvider.ProviderType.GOVERNMENT;
            default -> EducationProvider.ProviderType.ORGANIZATION;
        };
    }

    /**
     * B-12: platform approval flips the provider's verified flag. Before this there was no writer
     * for {@code is_verified} anywhere in the codebase, so operators always saw "not verified".
     */
    @Override
    public ProviderResponse setProviderVerified(UUID institutionId, UUID providerId, boolean verified) {
        EducationProvider provider = providerRepository.findByIdAndInstitutionId(providerId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Education Provider", "id", providerId));
        provider.setIsVerified(verified);
        if (verified) {
            provider.setIsActive(true);
        }
        return mapToResponse(providerRepository.save(provider));
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderStatsResponse getProviderStats(UUID institutionId) {
        long totalProviders = providerRepository.findAllByInstitutionId(institutionId).size();
        long activeProviders = providerRepository.findActiveByInstitutionId(institutionId).size();
        long totalPrograms = programRepository.countByInstitutionId(institutionId);
        long totalLearners = learnerRepository.countByInstitutionId(institutionId);
        long totalSessions = sessionRepository.countByInstitutionId(institutionId);
        long totalCertificates = certificateRepository.countByInstitutionId(institutionId);

        return ProviderStatsResponse.builder()
                .totalProviders(totalProviders)
                .activeProviders(activeProviders)
                .totalPrograms(totalPrograms)
                .activePrograms(totalPrograms)
                .totalLearners(totalLearners)
                .activeLearners(totalLearners)
                .totalSessions(totalSessions)
                .completedSessions(totalSessions)
                .totalCertificates(totalCertificates)
                .build();
    }

    private ProviderResponse mapToResponse(EducationProvider provider) {
        return ProviderResponse.builder()
                .id(provider.getId())
                .institutionId(provider.getInstitutionId())
                .name(provider.getName())
                .providerType(provider.getProviderType().name())
                .description(provider.getDescription())
                .logoUrl(provider.getLogoUrl())
                .website(provider.getWebsite())
                .email(provider.getEmail())
                .phone(provider.getPhone())
                .address(provider.getAddress())
                .city(provider.getCity())
                .country(provider.getCountry())
                .contactPersonName(provider.getContactPersonName())
                .contactPersonEmail(provider.getContactPersonEmail())
                .contactPersonPhone(provider.getContactPersonPhone())
                .isActive(provider.getIsActive())
                .isVerified(provider.getIsVerified())
                .createdAt(provider.getCreatedAt())
                .build();
    }
}
