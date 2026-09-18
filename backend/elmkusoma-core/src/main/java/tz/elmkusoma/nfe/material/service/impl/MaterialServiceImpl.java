package tz.elmkusoma.nfe.material.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.OwnershipGuard;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nfe.material.domain.NfeMaterial;
import tz.elmkusoma.nfe.material.dto.MaterialRequest;
import tz.elmkusoma.nfe.material.dto.MaterialResponse;
import tz.elmkusoma.nfe.material.repository.NfeMaterialRepository;
import tz.elmkusoma.nfe.material.service.MaterialService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterialServiceImpl implements MaterialService {

    private final NfeMaterialRepository materialRepository;
    private final OwnershipGuard ownershipGuard;

    @Override
    public MaterialResponse createMaterial(UUID institutionId, MaterialRequest request) {
        NfeMaterial material = NfeMaterial.builder()
                .providerId(request.getProviderId())
                .programId(request.getProgramId())
                .title(request.getTitle())
                .description(request.getDescription())
                .materialType(NfeMaterial.MaterialType.valueOf(request.getMaterialType()))
                .contentUrl(request.getContentUrl())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isFree(request.getIsFree() != null ? request.getIsFree() : false)
                .build();
        material.setInstitutionId(institutionId);

        NfeMaterial saved = materialRepository.save(material);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialResponse getMaterial(UUID institutionId, UUID materialId) {
        NfeMaterial material = materialRepository.findByIdAndInstitutionId(materialId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Material", "id", materialId));
        ownershipGuard.verifyInstitution(material.getInstitutionId(), institutionId);
        return mapToResponse(material);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MaterialResponse> listMaterials(UUID institutionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<NfeMaterial> allMaterials = materialRepository.findAllByInstitutionId(institutionId);

        int start = Math.min(page * size, allMaterials.size());
        int end = Math.min((page + 1) * size, allMaterials.size());
        List<NfeMaterial> paged = allMaterials.subList(start, end);

        Page<NfeMaterial> materialPage = new org.springframework.data.domain.PageImpl<>(
                paged, pageable, allMaterials.size());

        List<MaterialResponse> content = materialPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                content,
                materialPage.getNumber(),
                materialPage.getSize(),
                materialPage.getTotalElements(),
                materialPage.getTotalPages(),
                materialPage.isFirst(),
                materialPage.isLast()
        );
    }

    @Override
    public MaterialResponse updateMaterial(UUID institutionId, UUID materialId, MaterialRequest request) {
        NfeMaterial material = materialRepository.findByIdAndInstitutionId(materialId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Material", "id", materialId));
        ownershipGuard.verifyInstitution(material.getInstitutionId(), institutionId);

        if (request.getProviderId() != null) material.setProviderId(request.getProviderId());
        if (request.getProgramId() != null) material.setProgramId(request.getProgramId());
        if (request.getTitle() != null) material.setTitle(request.getTitle());
        if (request.getDescription() != null) material.setDescription(request.getDescription());
        if (request.getMaterialType() != null) material.setMaterialType(NfeMaterial.MaterialType.valueOf(request.getMaterialType()));
        if (request.getContentUrl() != null) material.setContentUrl(request.getContentUrl());
        if (request.getSortOrder() != null) material.setSortOrder(request.getSortOrder());
        if (request.getIsFree() != null) material.setIsFree(request.getIsFree());

        NfeMaterial saved = materialRepository.save(material);
        return mapToResponse(saved);
    }

    @Override
    public void deleteMaterial(UUID institutionId, UUID materialId) {
        NfeMaterial material = materialRepository.findByIdAndInstitutionId(materialId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Material", "id", materialId));
        ownershipGuard.verifyInstitution(material.getInstitutionId(), institutionId);
        material.setIsDeleted(true);
        materialRepository.save(material);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialResponse> getMaterialsByProvider(UUID institutionId, UUID providerId) {
        return materialRepository.findByProviderId(providerId).stream()
                .filter(m -> institutionId.equals(m.getInstitutionId()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialResponse> getMaterialsByProgram(UUID institutionId, UUID programId) {
        return materialRepository.findByProgramId(programId).stream()
                .filter(m -> institutionId.equals(m.getInstitutionId()))
                .map(this::mapToResponse)
                .toList();
    }

    private MaterialResponse mapToResponse(NfeMaterial material) {
        return MaterialResponse.builder()
                .id(material.getId())
                .institutionId(material.getInstitutionId())
                .providerId(material.getProviderId())
                .programId(material.getProgramId())
                .title(material.getTitle())
                .description(material.getDescription())
                .materialType(material.getMaterialType().name())
                .contentUrl(material.getContentUrl())
                .sortOrder(material.getSortOrder())
                .isFree(material.getIsFree())
                .createdAt(material.getCreatedAt())
                .build();
    }
}
