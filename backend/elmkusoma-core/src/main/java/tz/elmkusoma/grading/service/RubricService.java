package tz.elmkusoma.grading.service;

import tz.elmkusoma.grading.dto.request.CreateRubricCriteriaRequest;
import tz.elmkusoma.grading.dto.request.CreateRubricRequest;
import tz.elmkusoma.grading.dto.response.RubricResponse;

import java.util.List;
import java.util.UUID;

/** Rubric CRUD — wires the grading_rubrics / rubric_criteria tables (§36). */
public interface RubricService {

    RubricResponse create(UUID institutionId, CreateRubricRequest request, String userEmail, String userRole);

    List<RubricResponse> getByInstitutionId(UUID institutionId);

    RubricResponse getById(UUID id, UUID institutionId);

    RubricResponse addCriteria(UUID rubricId, UUID institutionId, List<CreateRubricCriteriaRequest> criteria,
                               String userEmail, String userRole);

    void delete(UUID rubricId, UUID institutionId, String userEmail, String userRole);
}
