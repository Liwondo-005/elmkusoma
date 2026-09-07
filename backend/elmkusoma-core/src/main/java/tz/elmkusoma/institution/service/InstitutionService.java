package tz.elmkusoma.institution.service;

import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.institution.dto.request.CreateInstitutionRequest;
import tz.elmkusoma.institution.dto.request.UpdateInstitutionRequest;
import tz.elmkusoma.institution.dto.response.InstitutionResponse;

import java.util.UUID;

public interface InstitutionService {

    InstitutionResponse createInstitution(CreateInstitutionRequest request, UUID ownerUserId);

    InstitutionResponse getInstitution(UUID id);

    PageResponse<InstitutionResponse> listInstitutions(int page, int size);

    InstitutionResponse updateInstitution(UUID id, UpdateInstitutionRequest request);

    void deleteInstitution(UUID id);

    InstitutionResponse activateInstitution(UUID id);

    InstitutionResponse deactivateInstitution(UUID id);
}
