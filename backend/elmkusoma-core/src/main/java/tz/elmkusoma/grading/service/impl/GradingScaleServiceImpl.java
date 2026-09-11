package tz.elmkusoma.grading.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.grading.domain.GradingScale;
import tz.elmkusoma.grading.dto.request.CreateGradingScaleRequest;
import tz.elmkusoma.grading.dto.response.GradeBoundaryResponse;
import tz.elmkusoma.grading.dto.response.GradingScaleResponse;
import tz.elmkusoma.grading.repository.GradingScaleRepository;
import tz.elmkusoma.grading.repository.GradeBoundaryRepository;
import tz.elmkusoma.grading.service.GradingScaleService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GradingScaleServiceImpl implements GradingScaleService {

    private final GradingScaleRepository gradingScaleRepository;
    private final GradeBoundaryRepository gradeBoundaryRepository;

    @Override
    public GradingScaleResponse create(UUID institutionId, CreateGradingScaleRequest request) {
        GradingScale scale = GradingScale.builder()
                .institutionId(institutionId)
                .name(request.getName())
                .description(request.getDescription())
                .scaleType(GradingScale.ScaleType.valueOf(request.getScaleType()))
                .minValue(request.getMinValue())
                .maxValue(request.getMaxValue())
                .isDefault(request.getIsDefault())
                .build();

        GradingScale saved = gradingScaleRepository.save(scale);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradingScaleResponse> getByInstitutionId(UUID institutionId) {
        return gradingScaleRepository.findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GradingScaleResponse getById(UUID id) {
        GradingScale scale = gradingScaleRepository.findById(id)
                .filter(s -> !s.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Grading scale not found"));
        return mapToResponse(scale);
    }

    @Override
    public GradingScaleResponse update(UUID id, CreateGradingScaleRequest request) {
        GradingScale scale = gradingScaleRepository.findById(id)
                .filter(s -> !s.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Grading scale not found"));

        scale.setName(request.getName());
        scale.setDescription(request.getDescription());
        scale.setScaleType(GradingScale.ScaleType.valueOf(request.getScaleType()));
        scale.setMinValue(request.getMinValue());
        scale.setMaxValue(request.getMaxValue());
        scale.setIsDefault(request.getIsDefault());

        GradingScale saved = gradingScaleRepository.save(scale);
        return mapToResponse(saved);
    }

    @Override
    public void delete(UUID id) {
        GradingScale scale = gradingScaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grading scale not found"));
        scale.setIsDeleted(true);
        gradingScaleRepository.save(scale);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeBoundaryResponse> getBoundaries(UUID scaleId) {
        if (!gradingScaleRepository.existsById(scaleId)) {
            throw new ResourceNotFoundException("Grading scale not found");
        }
        return gradeBoundaryRepository.findByGradingScaleIdAndIsDeletedFalse(scaleId)
                .stream()
                .map(gb -> GradeBoundaryResponse.builder()
                        .id(gb.getId())
                        .gradingScaleId(gb.getGradingScaleId())
                        .gradeLabel(gb.getGradeLabel())
                        .gradeName(gb.getGradeName())
                        .minPercentage(gb.getMinPercentage())
                        .maxPercentage(gb.getMaxPercentage())
                        .gpaPoints(gb.getGpaPoints())
                        .sortOrder(gb.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private GradingScaleResponse mapToResponse(GradingScale scale) {
        return GradingScaleResponse.builder()
                .id(scale.getId())
                .institutionId(scale.getInstitutionId())
                .name(scale.getName())
                .description(scale.getDescription())
                .scaleType(scale.getScaleType().name())
                .minValue(scale.getMinValue())
                .maxValue(scale.getMaxValue())
                .isDefault(scale.getIsDefault())
                .isActive(scale.getIsActive())
                .createdAt(scale.getCreatedAt())
                .updatedAt(scale.getUpdatedAt())
                .build();
    }
}