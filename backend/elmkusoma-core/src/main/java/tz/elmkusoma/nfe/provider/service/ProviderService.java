package tz.elmkusoma.nfe.provider.service;

import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.nfe.provider.dto.ProviderRequest;
import tz.elmkusoma.nfe.provider.dto.ProviderResponse;

import java.util.List;
import java.util.UUID;

public interface ProviderService {

    ProviderResponse createProvider(UUID institutionId, ProviderRequest request);

    ProviderResponse getProvider(UUID institutionId, UUID providerId);

    PageResponse<ProviderResponse> listProviders(UUID institutionId, int page, int size);

    ProviderResponse updateProvider(UUID institutionId, UUID providerId, ProviderRequest request);

    void deleteProvider(UUID institutionId, UUID providerId);

    List<ProviderResponse> getActiveProviders(UUID institutionId);
}
