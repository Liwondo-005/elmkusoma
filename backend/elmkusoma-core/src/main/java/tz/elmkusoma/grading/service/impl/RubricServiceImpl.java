package tz.elmkusoma.grading.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.common.exception.ResourceNotFoundException;
import tz.elmkusoma.grading.domain.GradingRubric;
import tz.elmkusoma.grading.domain.RubricCriteria;
import tz.elmkusoma.grading.dto.request.CreateRubricCriteriaRequest;
import tz.elmkusoma.grading.dto.request.CreateRubricRequest;
import tz.elmkusoma.grading.dto.response.RubricResponse;
import tz.elmkusoma.grading.repository.GradingRubricRepository;
import tz.elmkusoma.grading.repository.RubricCriteriaRepository;
import tz.elmkusoma.grading.service.RubricService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RubricServiceImpl implements RubricService {

    private final GradingRubricRepository rubricRepository;
    private final RubricCriteriaRepository criteriaRepository;
    private final AuditService auditService;
    private final UserRepository userRepository;

    @Override
    public RubricResponse create(UUID institutionId, CreateRubricRequest request,
                                 String userEmail, String userRole) {
        GradingRubric rubric = GradingRubric.builder()
                .name(request.getName())
                .description(request.getDescription())
                .subjectId(request.getSubjectId())
                .totalPoints(request.getTotalPoints())
                .isActive(true)
                .build();
        rubric.setInstitutionId(institutionId);
        GradingRubric saved = rubricRepository.save(rubric);

        if (request.getCriteria() != null && !request.getCriteria().isEmpty()) {
            int order = 0;
            for (CreateRubricCriteriaRequest criteriaRequest : request.getCriteria()) {
                criteriaRepository.save(toCriteria(saved.getId(), institutionId, criteriaRequest, order++));
            }
        }

        auditSafely(institutionId, userEmail, userRole, "GradingRubric", saved.getId(), saved.getName(),
                AuditLog.AuditAction.CREATE, null, Map.of("name", saved.getName()));
        return getById(saved.getId(), institutionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RubricResponse> getByInstitutionId(UUID institutionId) {
        return rubricRepository.findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .map(rubric -> toResponse(rubric, List.of()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RubricResponse getById(UUID id, UUID institutionId) {
        GradingRubric rubric = loadScoped(id, institutionId);
        List<RubricCriteria> criteria = criteriaRepository.findByRubricIdAndIsDeletedFalse(id);
        return toResponse(rubric, criteria);
    }

    @Override
    public RubricResponse addCriteria(UUID rubricId, UUID institutionId,
                                      List<CreateRubricCriteriaRequest> criteriaList,
                                      String userEmail, String userRole) {
        GradingRubric rubric = loadScoped(rubricId, institutionId);
        int order = criteriaRepository.findByRubricIdAndIsDeletedFalse(rubricId).size();
        for (CreateRubricCriteriaRequest criteriaRequest : criteriaList) {
            criteriaRepository.save(toCriteria(rubricId, institutionId, criteriaRequest, order++));
        }
        auditSafely(institutionId, userEmail, userRole, "RubricCriteria", rubricId, rubric.getName(),
                AuditLog.AuditAction.CREATE, null,
                Map.of("added", String.valueOf(criteriaList.size())));
        return getById(rubricId, institutionId);
    }

    @Override
    public void delete(UUID rubricId, UUID institutionId, String userEmail, String userRole) {
        GradingRubric rubric = loadScoped(rubricId, institutionId);
        rubric.setIsDeleted(true);
        rubricRepository.save(rubric);
        for (RubricCriteria criteria : criteriaRepository.findByRubricIdAndIsDeletedFalse(rubricId)) {
            criteria.setIsDeleted(true);
            criteriaRepository.save(criteria);
        }
        auditSafely(institutionId, userEmail, userRole, "GradingRubric", rubricId, rubric.getName(),
                AuditLog.AuditAction.DELETE, Map.of("name", rubric.getName()), null);
    }

    private GradingRubric loadScoped(UUID id, UUID institutionId) {
        GradingRubric rubric = rubricRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Rubric not found"));
        if (institutionId != null && !institutionId.equals(rubric.getInstitutionId())) {
            throw new ResourceNotFoundException("Rubric not found");
        }
        return rubric;
    }

    private RubricCriteria toCriteria(UUID rubricId, UUID institutionId,
                                      CreateRubricCriteriaRequest request, int fallbackOrder) {
        RubricCriteria criteria = RubricCriteria.builder()
                .rubricId(rubricId)
                .name(request.getName())
                .description(request.getDescription())
                .maxPoints(request.getMaxPoints())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : fallbackOrder)
                .build();
        criteria.setInstitutionId(institutionId);
        return criteria;
    }

    private RubricResponse toResponse(GradingRubric rubric, List<RubricCriteria> criteria) {
        List<RubricResponse.Criteria> criteriaResponses = new ArrayList<>();
        criteria.forEach(c -> criteriaResponses.add(RubricResponse.Criteria.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .maxPoints(c.getMaxPoints())
                .sortOrder(c.getSortOrder())
                .build()));
        return RubricResponse.builder()
                .id(rubric.getId())
                .name(rubric.getName())
                .description(rubric.getDescription())
                .subjectId(rubric.getSubjectId())
                .totalPoints(rubric.getTotalPoints())
                .isActive(rubric.getIsActive())
                .createdAt(rubric.getCreatedAt())
                .criteria(criteriaResponses)
                .build();
    }

    /** Audit write that can never fail the business transaction (null-safe in unit tests). */
    private void auditSafely(UUID institutionId, String userEmail, String userRole,
                             String entityType, UUID entityId, String entityName,
                             AuditLog.AuditAction action,
                             Map<String, Object> oldValues, Map<String, Object> newValues) {
        try {
            if (auditService == null) {
                return;
            }
            UUID actorId = userEmail == null ? null
                    : userRepository.findByEmailAndIsDeletedFalse(userEmail).map(User::getId).orElse(null);
            auditService.recordAuditLog(institutionId, actorId, userEmail, userRole,
                    entityType, entityId, entityName, action, oldValues, newValues);
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(RubricServiceImpl.class)
                    .warn("Audit write failed for {} {}: {}", entityType, entityId, ex.getMessage());
        }
    }
}
