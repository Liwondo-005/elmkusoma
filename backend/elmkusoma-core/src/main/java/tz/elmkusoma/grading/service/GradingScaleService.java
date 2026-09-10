package tz.elmkusoma.grading.service;

import tz.elmkusoma.grading.domain.GradingScale;
import tz.elmkusoma.grading.dto.request.CreateGradingScaleRequest;
import tz.elmkusoma.grading.dto.response.GradeBoundaryResponse;
import tz.elmkusoma.grading.dto.response.GradingScaleResponse;

import java.util.List;
import java.util.UUID;

public interface GradingScaleService {

    GradingScaleResponse create(UUID institutionId, CreateGradingScaleRequest request);

    List<GradingScaleResponse> getByInstitutionId(UUID institutionId);

    GradingScaleResponse getById(UUID id, UUID institutionId);

    GradingScaleResponse update(UUID id, UUID institutionId, CreateGradingScaleRequest request);

    void delete(UUID id, UUID institutionId);

    List<GradeBoundaryResponse> getBoundaries(UUID scaleId);
}