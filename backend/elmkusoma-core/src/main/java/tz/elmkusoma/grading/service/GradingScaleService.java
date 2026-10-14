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

    /** Institution-scoped read; platform {@code ADMIN} bypasses the institution check. */
    GradingScaleResponse getById(UUID id, UUID institutionId, String userRole);

    /** Institution-scoped update; platform {@code ADMIN} bypasses the institution check. */
    GradingScaleResponse update(UUID id, UUID institutionId, String userRole, CreateGradingScaleRequest request);

    /** Institution-scoped delete; platform {@code ADMIN} bypasses the institution check. */
    void delete(UUID id, UUID institutionId, String userRole);

    List<GradeBoundaryResponse> getBoundaries(UUID scaleId);
}