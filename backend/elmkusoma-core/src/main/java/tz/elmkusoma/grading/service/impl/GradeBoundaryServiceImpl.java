package tz.elmkusoma.grading.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.exception.ResourceNotFoundException;
import tz.elmkusoma.grading.domain.GradeBoundary;
import tz.elmkusoma.grading.domain.GradeBoundary;
import tz.elmkusoma.grading.dto.request.CreateGradeBoundaryRequest;
import tz.elmkusoma.grading.dto.response.GradeBoundaryResponse;
import tz.elmkusoma.grading.repository.GradeBoundaryRepository;
import tz.elmkusoma.grading.service.GradeBoundaryService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GradeBoundaryServiceImpl implements GradeBoundaryService {

    private final GradeBoundaryRepository gradeBoundaryRepository;

    @Override
    public GradeBoundaryResponse create(UUID institutionId, CreateGradeBoundaryRequest request) {
        GradeBoundary boundary = GradeBoundary.builder()
                .institutionId(institutionId)
                .gradingScaleId(request.getGradingScaleId())
                .gradeLabel(request.getGradeLabel())
                .gradeName(request.getGradeName())
                .minPercentage(request.getMinPercentage())
                .maxPercentage(request.getMaxPercentage())
                .gpaPoints(request.getGpaPoints())
                .sortOrder(request.getSortOrder())
                .build();

        GradeBoundary saved = gradeBoundaryRepository.save(boundary);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeBoundaryResponse> getByGradingScaleId(UUID gradingScaleId) {
        return gradeBoundaryRepository.findByGradingScaleIdAndIsDeletedFalse(gradingScaleId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeBoundaryResponse> getByInstitutionId(UUID institutionId) {
        return gradeBoundaryRepository.findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GradeBoundaryResponse getById(UUID id) {
        GradeBoundary boundary = gradeBoundaryRepository.findById(id)
                .filter(b -> !b.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Grade boundary not found"));
        return mapToResponse(boundary);
    }

    @Override
    public GradeBoundaryResponse update(UUID id, CreateGradeBoundaryRequest request) {
        GradeBoundary boundary = gradeBoundaryRepository.findById(id)
                .filter(b -> !b.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Grade boundary not found"));

        boundary.setGradeLabel(request.getGradeLabel());
        boundary.setGradeName(request.getGradeName());
        boundary.setMinPercentage(request.getMinPercentage());
        boundary.setMaxPercentage(request.getMaxPercentage());
        boundary.setGpaPoints(request.getGpaPoints());
        boundary.setSortOrder(request.getSortOrder());

        GradeBoundary saved = gradeBoundaryRepository.save(boundary);
        return mapToResponse(saved);
    }

    @Override
    public void delete(UUID id) {
        GradeBoundary boundary = gradeBoundaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade boundary not found"));
        boundary.setIsDeleted(true);
        gradeBoundaryRepository.save(boundary);
    }

    @Override
    @Transactional(readOnly = true)
    public String calculateGradeForPercentage(UUID gradingScaleId, BigDecimal percentage) {
        List<GradeBoundary> boundaries = gradeBoundaryRepository.findBoundariesByScaleId(gradingScaleId);
        
        for (GradeBoundary boundary : boundaries) {
            if (percentage.compareTo(boundary.getMinPercentage()) >= 0 &&
                percentage.compareTo(boundary.getMaxPercentage()) <= 0) {
                return boundary.getGradeLabel();
            }
        }
        return null;
    }

    private GradeBoundaryResponse mapToResponse(GradeBoundary boundary) {
        return GradeBoundaryResponse.builder()
                .id(boundary.getId())
                .gradingScaleId(boundary.getGradingScaleId())
                .gradeLabel(boundary.getGradeLabel())
                .gradeName(boundary.getGradeName())
                .minPercentage(boundary.getMinPercentage())
                .maxPercentage(boundary.getMaxPercentage())
                .gpaPoints(boundary.getGpaPoints())
                .sortOrder(boundary.getSortOrder())
                .build();
    }
}