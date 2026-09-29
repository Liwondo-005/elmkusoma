package tz.elmkusoma.grading.service;

import tz.elmkusoma.grading.dto.response.GradebookResponse;

import java.util.UUID;

public interface GradebookService {

    /**
     * Aggregate gradebook for a class: learners of the class (from class assignments
     * and active enrollments) with every class assignment's submission state and every
     * class assessment's result, plus totals — all in one request.
     */
    GradebookResponse getGradebook(UUID classGroupId, UUID institutionId);
}
