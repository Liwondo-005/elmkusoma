package tz.elmkusoma.nfe.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nfe.assessment.domain.NfeAssessment;
import tz.elmkusoma.nfe.assessment.dto.AssessmentRequest;
import tz.elmkusoma.nfe.assessment.dto.AssessmentResponse;
import tz.elmkusoma.nfe.assessment.repository.NfeAssessmentRepository;
import tz.elmkusoma.nfe.assessment.service.NfeAssessmentService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NfeAssessmentServiceImpl implements NfeAssessmentService {

    private final NfeAssessmentRepository assessmentRepository;

    @Override
    public AssessmentResponse createAssessment(UUID institutionId, UUID providerId, AssessmentRequest request) {
        NfeAssessment assessment = NfeAssessment.builder()
                .providerId(providerId)
                .programId(request.getProgramId())
                .title(request.getTitle())
                .description(request.getDescription())
                .assessmentType(NfeAssessment.AssessmentType.valueOf(request.getAssessmentType()))
                .totalMarks(request.getTotalMarks())
                .passMarks(request.getPassMarks())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .build();
        assessment.setInstitutionId(institutionId);

        NfeAssessment saved = assessmentRepository.save(assessment);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentResponse getAssessment(UUID institutionId, UUID assessmentId) {
        NfeAssessment assessment = assessmentRepository.findByIdAndInstitutionId(assessmentId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("NFE Assessment", "id", assessmentId));
        return mapToResponse(assessment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AssessmentResponse> listAssessments(UUID institutionId, UUID providerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<NfeAssessment> allAssessments = assessmentRepository.findByProviderId(providerId, institutionId);

        int start = Math.min(page * size, allAssessments.size());
        int end = Math.min((page + 1) * size, allAssessments.size());
        List<NfeAssessment> paged = allAssessments.subList(start, end);

        PageImpl<NfeAssessment> assessmentPage = new PageImpl<>(
                paged, pageable, allAssessments.size());

        List<AssessmentResponse> content = assessmentPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                content,
                assessmentPage.getNumber(),
                assessmentPage.getSize(),
                assessmentPage.getTotalElements(),
                assessmentPage.getTotalPages(),
                assessmentPage.isFirst(),
                assessmentPage.isLast()
        );
    }

    @Override
    public AssessmentResponse updateAssessment(UUID institutionId, UUID assessmentId, AssessmentRequest request) {
        NfeAssessment assessment = assessmentRepository.findByIdAndInstitutionId(assessmentId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("NFE Assessment", "id", assessmentId));

        if (request.getProgramId() != null) assessment.setProgramId(request.getProgramId());
        if (request.getTitle() != null) assessment.setTitle(request.getTitle());
        if (request.getDescription() != null) assessment.setDescription(request.getDescription());
        if (request.getAssessmentType() != null) assessment.setAssessmentType(NfeAssessment.AssessmentType.valueOf(request.getAssessmentType()));
        if (request.getTotalMarks() != null) assessment.setTotalMarks(request.getTotalMarks());
        if (request.getPassMarks() != null) assessment.setPassMarks(request.getPassMarks());
        if (request.getTimeLimitMinutes() != null) assessment.setTimeLimitMinutes(request.getTimeLimitMinutes());
        if (request.getIsPublished() != null) assessment.setIsPublished(request.getIsPublished());
        if (request.getStartsAt() != null) assessment.setStartsAt(request.getStartsAt());
        if (request.getEndsAt() != null) assessment.setEndsAt(request.getEndsAt());

        NfeAssessment saved = assessmentRepository.save(assessment);
        return mapToResponse(saved);
    }

    @Override
    public void deleteAssessment(UUID institutionId, UUID assessmentId) {
        NfeAssessment assessment = assessmentRepository.findByIdAndInstitutionId(assessmentId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("NFE Assessment", "id", assessmentId));
        assessment.setIsDeleted(true);
        assessmentRepository.save(assessment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentResponse> getPublishedAssessments(UUID institutionId, UUID providerId) {
        return assessmentRepository.findByIsPublished(true, institutionId).stream()
                .filter(a -> a.getProviderId().equals(providerId))
                .map(this::mapToResponse)
                .toList();
    }

    private AssessmentResponse mapToResponse(NfeAssessment assessment) {
        return AssessmentResponse.builder()
                .id(assessment.getId())
                .institutionId(assessment.getInstitutionId())
                .providerId(assessment.getProviderId())
                .programId(assessment.getProgramId())
                .title(assessment.getTitle())
                .description(assessment.getDescription())
                .assessmentType(assessment.getAssessmentType().name())
                .totalMarks(assessment.getTotalMarks())
                .passMarks(assessment.getPassMarks())
                .timeLimitMinutes(assessment.getTimeLimitMinutes())
                .isPublished(assessment.getIsPublished())
                .startsAt(assessment.getStartsAt())
                .endsAt(assessment.getEndsAt())
                .createdAt(assessment.getCreatedAt())
                .build();
    }
}
