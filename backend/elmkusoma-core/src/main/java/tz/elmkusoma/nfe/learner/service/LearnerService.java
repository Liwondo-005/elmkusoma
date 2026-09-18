package tz.elmkusoma.nfe.learner.service;

import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.nfe.learner.dto.LearnerRequest;
import tz.elmkusoma.nfe.learner.dto.LearnerResponse;

import java.util.List;
import java.util.UUID;

public interface LearnerService {

    LearnerResponse createLearner(UUID institutionId, LearnerRequest request);

    LearnerResponse getLearner(UUID institutionId, UUID learnerId);

    PageResponse<LearnerResponse> listLearners(UUID institutionId, int page, int size);

    LearnerResponse updateLearner(UUID institutionId, UUID learnerId, LearnerRequest request);

    void deleteLearner(UUID institutionId, UUID learnerId);

    List<LearnerResponse> getLearnersByProvider(UUID institutionId, UUID providerId);

    List<LearnerResponse> getLearnersByUser(UUID institutionId, UUID userId);
}
