package tz.elmkusoma.nfe.learner.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.OwnershipGuard;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nfe.learner.domain.NfeLearner;
import tz.elmkusoma.nfe.learner.dto.LearnerRequest;
import tz.elmkusoma.nfe.learner.dto.LearnerResponse;
import tz.elmkusoma.nfe.learner.repository.NfeLearnerRepository;
import tz.elmkusoma.nfe.learner.service.LearnerService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LearnerServiceImpl implements LearnerService {

    private final NfeLearnerRepository learnerRepository;
    private final OwnershipGuard ownershipGuard;

    @Override
    public LearnerResponse createLearner(UUID institutionId, LearnerRequest request) {
        NfeLearner learner = NfeLearner.builder()
                .providerId(request.getProviderId())
                .userId(request.getUserId())
                .participantNumber(request.getParticipantNumber())
                .occupation(request.getOccupation())
                .organization(request.getOrganization())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();
        learner.setInstitutionId(institutionId);

        NfeLearner saved = learnerRepository.save(learner);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LearnerResponse getLearner(UUID institutionId, UUID learnerId) {
        NfeLearner learner = learnerRepository.findByIdAndInstitutionId(learnerId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Learner", "id", learnerId));
        ownershipGuard.verifyInstitution(learner.getInstitutionId(), institutionId);
        return mapToResponse(learner);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LearnerResponse> listLearners(UUID institutionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<NfeLearner> allLearners = learnerRepository.findAllByInstitutionId(institutionId);

        int start = Math.min(page * size, allLearners.size());
        int end = Math.min((page + 1) * size, allLearners.size());
        List<NfeLearner> paged = allLearners.subList(start, end);

        Page<NfeLearner> learnerPage = new org.springframework.data.domain.PageImpl<>(
                paged, pageable, allLearners.size());

        List<LearnerResponse> content = learnerPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                content,
                learnerPage.getNumber(),
                learnerPage.getSize(),
                learnerPage.getTotalElements(),
                learnerPage.getTotalPages(),
                learnerPage.isFirst(),
                learnerPage.isLast()
        );
    }

    @Override
    public LearnerResponse updateLearner(UUID institutionId, UUID learnerId, LearnerRequest request) {
        NfeLearner learner = learnerRepository.findByIdAndInstitutionId(learnerId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Learner", "id", learnerId));
        ownershipGuard.verifyInstitution(learner.getInstitutionId(), institutionId);

        if (request.getProviderId() != null) learner.setProviderId(request.getProviderId());
        if (request.getUserId() != null) learner.setUserId(request.getUserId());
        if (request.getParticipantNumber() != null) learner.setParticipantNumber(request.getParticipantNumber());
        if (request.getOccupation() != null) learner.setOccupation(request.getOccupation());
        if (request.getOrganization() != null) learner.setOrganization(request.getOrganization());
        if (request.getStatus() != null) learner.setStatus(request.getStatus());

        NfeLearner saved = learnerRepository.save(learner);
        return mapToResponse(saved);
    }

    @Override
    public void deleteLearner(UUID institutionId, UUID learnerId) {
        NfeLearner learner = learnerRepository.findByIdAndInstitutionId(learnerId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Learner", "id", learnerId));
        ownershipGuard.verifyInstitution(learner.getInstitutionId(), institutionId);
        learner.setIsDeleted(true);
        learnerRepository.save(learner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearnerResponse> getLearnersByProvider(UUID institutionId, UUID providerId) {
        return learnerRepository.findByProviderId(providerId).stream()
                .filter(l -> institutionId.equals(l.getInstitutionId()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearnerResponse> getLearnersByUser(UUID institutionId, UUID userId) {
        return learnerRepository.findByUserId(userId).stream()
                .filter(l -> institutionId.equals(l.getInstitutionId()))
                .map(this::mapToResponse)
                .toList();
    }

    private LearnerResponse mapToResponse(NfeLearner learner) {
        return LearnerResponse.builder()
                .id(learner.getId())
                .institutionId(learner.getInstitutionId())
                .providerId(learner.getProviderId())
                .userId(learner.getUserId())
                .participantNumber(learner.getParticipantNumber())
                .occupation(learner.getOccupation())
                .organization(learner.getOrganization())
                .status(learner.getStatus())
                .createdAt(learner.getCreatedAt())
                .build();
    }
}
