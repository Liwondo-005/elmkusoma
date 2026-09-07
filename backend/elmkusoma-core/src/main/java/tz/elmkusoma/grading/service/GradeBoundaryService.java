package tz.elmkusoma.grading.service;

import tz.elmkusoma.grading.domain.GradeBoundary;
import tz.elmkusoma.grading.domain.GradeBoundary;
import tz.elmkusoma.grading.dto.request.CreateGradeBoundaryRequest;
import tz.elmkusoma.grading.dto.response.GradeBoundaryResponse;

import java.util.List;
import java.util.UUID;

public interface GradeBoundaryService {

    GradeBoundaryResponse create(UUID institutionId, CreateGradeBoundaryRequest request);

    List<GradeBoundaryResponse> getByGradingScaleId(UUID gradingScaleId);

    List<GradeBoundaryResponse> getByInstitutionId(UUID institutionId);

    GradeBoundaryResponse getById(UUID id);

    GradeBoundaryResponse update(UUID id, CreateGradeBoundaryRequest request);

    void delete(UUID id);

    String calculateGradeForPercentage(UUID gradingScaleId, java.math.BigDecimal percentage);
}