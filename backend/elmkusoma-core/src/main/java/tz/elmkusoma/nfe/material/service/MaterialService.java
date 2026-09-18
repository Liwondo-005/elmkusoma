package tz.elmkusoma.nfe.material.service;

import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.nfe.material.dto.MaterialRequest;
import tz.elmkusoma.nfe.material.dto.MaterialResponse;

import java.util.List;
import java.util.UUID;

public interface MaterialService {

    MaterialResponse createMaterial(UUID institutionId, MaterialRequest request);

    MaterialResponse getMaterial(UUID institutionId, UUID materialId);

    PageResponse<MaterialResponse> listMaterials(UUID institutionId, int page, int size);

    MaterialResponse updateMaterial(UUID institutionId, UUID materialId, MaterialRequest request);

    void deleteMaterial(UUID institutionId, UUID materialId);

    List<MaterialResponse> getMaterialsByProvider(UUID institutionId, UUID providerId);

    List<MaterialResponse> getMaterialsByProgram(UUID institutionId, UUID programId);
}
