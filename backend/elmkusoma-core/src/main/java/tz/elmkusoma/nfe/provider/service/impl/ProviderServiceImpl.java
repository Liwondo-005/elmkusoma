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
import tz.elmkusoma.nfe.provider.repository.EducationProviderRepository;
import tz.elmkusoma.nfe.provider.service.ProviderService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProviderServiceImpl implements ProviderService {

    private final EducationProviderRepository providerRepository;

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
