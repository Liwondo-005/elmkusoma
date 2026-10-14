package tz.elmkusoma.grading.service;

import tz.elmkusoma.grading.domain.GradeBoundary;
import tz.elmkusoma.grading.domain.GradeBoundary;
import tz.elmkusoma.grading.dto.request.CreateGradeBoundaryRequest;
import tz.elmkusoma.grading.dto.response.GradeBoundaryResponse;

import java.util.List;
import java.util.UUID;

public interface GradeBoundaryService {

    GradeBoundaryResponse create(UUID institutionId, CreateGradeBoundaryRequest request);

    /** Institution-scoped read; platform {@code ADMIN} bypasses the institution check. */
    List<GradeBoundaryResponse> getByGradingScaleId(UUID gradingScaleId, UUID institutionId, String userRole);

    List<GradeBoundaryResponse> getByInstitutionId(UUID institutionId);

    /** Institution-scoped read; platform {@code ADMIN} bypasses the institution check. */
    GradeBoundaryResponse getById(UUID id, UUID institutionId, String userRole);

    /** Institution-scoped update; platform {@code ADMIN} bypasses the institution check. */
    GradeBoundaryResponse update(UUID id, UUID institutionId, String userRole, CreateGradeBoundaryRequest request);

    /** Institution-scoped delete; platform {@code ADMIN} bypasses the institution check. */
    void delete(UUID id, UUID institutionId, String userRole);

    String calculateGradeForPercentage(UUID gradingScaleId, java.math.BigDecimal percentage);
}