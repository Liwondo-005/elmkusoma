package tz.elmkusoma.nfe.assessment.service;

import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.nfe.assessment.dto.AssessmentRequest;
import tz.elmkusoma.nfe.assessment.dto.AssessmentResponse;

import java.util.List;
import java.util.UUID;

public interface NfeAssessmentService {

    AssessmentResponse createAssessment(UUID institutionId, UUID providerId, AssessmentRequest request);

    AssessmentResponse getAssessment(UUID institutionId, UUID assessmentId);

    PageResponse<AssessmentResponse> listAssessments(UUID institutionId, UUID providerId, int page, int size);

    AssessmentResponse updateAssessment(UUID institutionId, UUID assessmentId, AssessmentRequest request);

    void deleteAssessment(UUID institutionId, UUID assessmentId);

    List<AssessmentResponse> getPublishedAssessments(UUID institutionId, UUID providerId);
}
